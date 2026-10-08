package com.locostream

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

/**
 * NewPipeExtractor requires a Downloader implementation to make HTTP requests.
 *
 * IMPORTANT: We do NOT set Accept-Encoding manually. Java's HttpURLConnection
 * automatically adds "Accept-Encoding: gzip" and transparently decompresses the
 * response when it controls this header. If we set it manually, Java skips
 * decompression and the extractor receives raw compressed binary data, causing
 * "Couldn't extract client id" and empty trending results.
 */
class NewPipeDownloader private constructor() : Downloader() {

    companion object {
        private var instance: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader {
            if (instance == null) {
                instance = NewPipeDownloader()
            }
            return instance!!
        }
    }

    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val connection = url.openConnection() as HttpURLConnection

        // Set request headers from NewPipeExtractor
        request.headers().forEach { (key, values) ->
            values.forEach { value ->
                connection.setRequestProperty(key, value)
            }
        }

        // Apply default headers only when NewPipeExtractor did not set them.
        if (connection.getRequestProperty("User-Agent").isNullOrBlank()) {
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
        }
        if (connection.getRequestProperty("Accept").isNullOrBlank()) {
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        }
        if (connection.getRequestProperty("Accept-Language").isNullOrBlank()) {
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }
        // NOTE: Accept-Encoding is intentionally NOT set here.
        // Java's HttpURLConnection handles gzip transparently when it sets this header itself.
        // Setting it manually disables auto-decompression, breaking response parsing.
        if (connection.getRequestProperty("Connection").isNullOrBlank()) {
            connection.setRequestProperty("Connection", "keep-alive")
        }
        if (connection.getRequestProperty("Upgrade-Insecure-Requests").isNullOrBlank()) {
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1")
        }

        // Set referer for SoundCloud requests and related CDN/script hosts.
        // This helps SoundCloud client-id extraction and API requests.
        val destination = url.toString()
        if ((destination.contains("soundcloud.com") || destination.contains("sndcdn.com")) &&
            connection.getRequestProperty("Referer").isNullOrBlank()) {
            connection.setRequestProperty("Referer", "https://soundcloud.com/")
        }
        if ((destination.contains("soundcloud.com") || destination.contains("sndcdn.com")) &&
            connection.getRequestProperty("Origin").isNullOrBlank()) {
            connection.setRequestProperty("Origin", "https://soundcloud.com")
        }

        connection.requestMethod = request.httpMethod()
        // SoundCloud client-id extraction can require several sequential requests.
        connection.connectTimeout = 60_000
        connection.readTimeout = 60_000
        connection.doInput = true

        // Handle POST body if present
        if (request.dataToSend() != null) {
            connection.doOutput = true
            connection.outputStream.write(request.dataToSend())
        }

        val responseCode = connection.responseCode

        // Read response — decompress manually only if Content-Encoding header is present,
        // which means Java did NOT auto-decompress (can happen when NewPipeExtractor sets
        // its own Accept-Encoding header in the request).
        val responseBody = try {
            val contentEncoding = connection.contentEncoding
            val rawStream: InputStream = connection.inputStream
            val decodedStream: InputStream = when {
                contentEncoding?.equals("gzip", ignoreCase = true) == true -> GZIPInputStream(rawStream)
                contentEncoding?.equals("deflate", ignoreCase = true) == true -> InflaterInputStream(rawStream)
                else -> rawStream
            }
            decodedStream.bufferedReader(Charsets.UTF_8).readText()
        } catch (e: Exception) {
            try {
                val errorEncoding = connection.getHeaderField("Content-Encoding")
                val errStream: InputStream = connection.errorStream ?: return Response(
                    responseCode, connection.responseMessage, emptyMap(), "", request.url()
                )
                val decodedErr: InputStream = when {
                    errorEncoding?.equals("gzip", ignoreCase = true) == true -> GZIPInputStream(errStream)
                    errorEncoding?.equals("deflate", ignoreCase = true) == true -> InflaterInputStream(errStream)
                    else -> errStream
                }
                decodedErr.bufferedReader(Charsets.UTF_8).readText()
            } catch (e2: Exception) {
                ""
            }
        }

        val responseHeaders = mutableMapOf<String, MutableList<String>>()
        connection.headerFields.forEach { (key, values) ->
            if (key != null) responseHeaders[key] = values.toMutableList()
        }

        return Response(
            responseCode,
            connection.responseMessage,
            responseHeaders,
            responseBody,
            request.url()
        )
    }
}
