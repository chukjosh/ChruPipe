package com.chrupipe.routes

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
import java.net.InetAddress
import java.net.URI

private val proxyClient = HttpClient(CIO) {
    expectSuccess = false
    engine {
        // 0 = no timeout — the proxy is a pure pipe; killing it mid-stream
        // causes a "Cannot write to a channel" crash on the already-committed
        // response. Let the client's own TCP connection determine liveness.
        requestTimeout = 0
    }
}

private const val PROXY_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

/**
 * Proxies media stream URLs through the backend so the browser can play
 * SoundCloud, PeerTube, media.ccc.de, etc. without CDN CORS blocks.
 *
 * GET /proxy?url=https://...
 * Supports Range requests for seeking in the HTML5 player.
 */
fun Route.streamProxyRoutes() {
    get("/proxy") {
        val rawUrl = call.parameters["url"]?.trim()?.takeIf { it.isNotBlank() }
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'url' parameter")

        if (!isAllowedProxyUrl(rawUrl)) {
            return@get call.respond(HttpStatusCode.Forbidden, "URL not allowed")
        }

        val rangeHeader = call.request.headers[HttpHeaders.Range]

        try {
            val upstream = proxyClient.prepareGet(rawUrl) {
                headers {
                    append(HttpHeaders.UserAgent, PROXY_USER_AGENT)
                    rangeHeader?.let { append(HttpHeaders.Range, it) }

                    call.request.headers["Referer"]?.let { append("Referer", it) }
                    call.request.headers["Origin"]?.let { append("Origin", it) }
                    call.request.headers[HttpHeaders.Accept]?.let { append(HttpHeaders.Accept, it) }
                    call.request.headers[HttpHeaders.Cookie]?.let { append(HttpHeaders.Cookie, it) }

                    append("Referer", "https://soundcloud.com/")
                    append("Origin", "https://soundcloud.com")
                }
            }.execute()

            val body = upstream.bodyAsChannel()
            val contentType = upstream.headers[HttpHeaders.ContentType]
                    ?.let { runCatching { ContentType.parse(it) }.getOrNull() }
                    ?: ContentType.Application.OctetStream

            val upstreamAcceptRanges = upstream.headers[HttpHeaders.AcceptRanges]
            val upstreamContentRange = upstream.headers[HttpHeaders.ContentRange]
            val titleParam = call.parameters["title"]

            call.respond(object : OutgoingContent.WriteChannelContent() {
                override val status: HttpStatusCode? = upstream.status
                override val contentType: ContentType = contentType
                override val contentLength: Long? = upstream.contentLength()

                override val headers: Headers = Headers.build {
                    if (titleParam != null && titleParam.isNotBlank()) {
                        val ext = contentType.contentSubtype.takeIf { it.isNotBlank() } ?: "mp4"
                        val safeTitle = titleParam.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(100)
                        append(HttpHeaders.ContentDisposition, "inline; filename=\"$safeTitle.$ext\"")
                    }
                    append(HttpHeaders.AcceptRanges, upstreamAcceptRanges ?: "bytes")
                    if (upstreamContentRange != null) {
                        append(HttpHeaders.ContentRange, upstreamContentRange)
                    }
                }

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    try {
                        val buffer = ByteArray(8192)
                        while (!body.isClosedForRead) {
                            val read = body.readAvailable(buffer, 0, buffer.size)
                            if (read <= 0) break
                            channel.writeFully(buffer, 0, read)
                            channel.flush()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // The browser can cancel a range request or switch quality
                        // while this coroutine is writing. A closed response channel
                        // is an expected client disconnect, not an upstream failure.
                        if (channel.isClosedForWrite) return
                        println("[ERROR] Proxy stream interrupted for $rawUrl: ${e.message}")
                        channel.close(e)
                    }
                }
            })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("[ERROR] Proxy failed for $rawUrl: ${e.message}")
            call.respond(
                HttpStatusCode.BadGateway,
                "Failed to fetch upstream resource: ${e.message ?: "unknown error"}"
            )
        }
    }
}

/** Only proxy public http(s) URLs; block obvious SSRF targets. */
private fun isAllowedProxyUrl(url: String): Boolean {
    return try {
        val uri = URI(url)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false

        val host = uri.host?.lowercase() ?: return false
        if (host == "localhost" || host.endsWith(".localhost")) return false

        val address = InetAddress.getByName(host)
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress) {
            return false
        }
        if (address.isSiteLocalAddress) return false

        true
    } catch (_: Exception) {
        false
    }
}
