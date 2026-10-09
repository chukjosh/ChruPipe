/**
 * YouTubeRoutes.kt (now: ExtractionRoutes.kt in behaviour)
 *
 * API routes that call NewPipeExtractor via ExtractorService.
 * All routes now support a `service` query param to target different platforms.
 *
 * Examples:
 *   GET /search?q=lofi                         → searches YouTube (default)
 *   GET /search?q=lofi&service=soundcloud      → searches SoundCloud
 *   GET /stream?url=https://soundcloud.com/... → streams any URL
 *   GET /trending?service=soundcloud           → SoundCloud charts
 *   GET /services                              → list all supported services
 */

package com.chrupipe.routes

import com.chrupipe.models.ServiceInfoModel
import com.chrupipe.services.ExtractorService
import com.chrupipe.services.StreamExtractionErrorClassifier
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** List all supported streaming services — used by the frontend service selector */
fun Route.servicesRoutes() {
    get("/services") {
        val services = ExtractorService.SUPPORTED_SERVICES.map { (id, info) ->
            ServiceInfoModel(
                id                = id,
                name              = info.name,
                baseUrl           = info.baseUrl,
                supportsTrending  = info.supportsTrending,
                supportsComments  = info.supportsComments
            )
        }
        call.respond(services)
    }
}

/**
 * Search — supports all services via ?service= param.
 * GET /search?q=lofi
 * GET /search?q=lofi&service=soundcloud
 */
fun Route.searchRoutes() {
    get("/search") {
        val query = call.parameters["q"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing query parameter 'q'")
        val service = call.parameters["service"] ?: "youtube"
        val results = ExtractorService.search(query, service)
        call.respond(results)
    }
}

/**
 * Stream — accepts a full content URL; service is auto-detected.
 * GET /stream?url=https://www.youtube.com/watch?v=ID
 * GET /stream?url=https://soundcloud.com/artist/track
 */
fun Route.streamRoutes() {
    get("/stream") {
        val url = call.parameters["url"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'url' parameter")
        try {
            val stream = ExtractorService.getStreams(url)
            call.respond(stream)
        } catch (e: Exception) {
            val errorMsg = StreamExtractionErrorClassifier.describe(url, e)
            println("[ERROR] Stream extraction failed: $errorMsg")
            e.printStackTrace()
            throw IllegalArgumentException(errorMsg)
        }
    }

    // Keep legacy route for YouTube backward compatibility
    // GET /stream/{id} still works as YouTube shorthand
    get("/stream/{id}") {
        val id = call.parameters["id"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing video id")
        val url = "https://www.youtube.com/watch?v=$id"
        try {
            val stream = ExtractorService.getStreams(url)
            call.respond(stream)
        } catch (e: Exception) {
            val errorMsg = StreamExtractionErrorClassifier.describe(url, e)
            println("[ERROR] YouTube stream extraction failed: $errorMsg")
            e.printStackTrace()
            throw IllegalArgumentException(errorMsg)
        }
    }
}

/**
 * Channel — accepts a full channel URL; service is auto-detected.
 * GET /channel?url=https://www.youtube.com/channel/UC...
 * GET /channel?url=https://soundcloud.com/artist
 */
fun Route.channelRoutes() {
    get("/channel") {
        val url = call.parameters["url"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'url' parameter")
        val channel = ExtractorService.getChannel(url)
        call.respond(channel)
    }

    // Keep legacy YouTube shorthand: GET /channel/{id}
    get("/channel/{id}") {
        val id = call.parameters["id"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing channel id")
        val url = "https://www.youtube.com/channel/$id"
        val channel = ExtractorService.getChannel(url)
        call.respond(channel)
    }
}

/**
 * Trending — per service.
 * GET /trending                       → YouTube trending
 * GET /trending?service=soundcloud    → SoundCloud charts
 */
fun Route.trendingRoutes() {
    get("/trending") {
        val service = call.parameters["service"] ?: "youtube"
        val trending = ExtractorService.getTrending(service)
        call.respond(trending)
    }
}

/**
 * Comments — YouTube only; other services return empty list.
 * GET /comments?url=https://www.youtube.com/watch?v=ID
 */
fun Route.commentRoutes() {
    get("/comments") {
        val url = call.parameters["url"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'url' parameter")
        val comments = ExtractorService.getComments(url)
        call.respond(comments)
    }

    // Legacy: GET /comments/{videoId} (YouTube only)
    get("/comments/{videoId}") {
        val videoId = call.parameters["videoId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing video id")
        val url = "https://www.youtube.com/watch?v=$videoId"
        val comments = ExtractorService.getComments(url)
        call.respond(comments)
    }
}
