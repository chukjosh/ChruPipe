package com.chrupipe.routes

import com.chrupipe.database.repositories.DownloadRepository
import com.chrupipe.models.StartDownloadRequest
import com.chrupipe.util.resolveDownloadsDir
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private val httpClient = HttpClient(CIO) {
    expectSuccess = false
    engine {
        requestTimeout = 0
    }
}

private val downloadJobs = ConcurrentHashMap<Int, Job>()
private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
private const val PROGRESS_UPDATE_BYTES = 1024L * 1024L

private fun Application.launchDownload(
    downloadId: Int,
    streamUrl: String,
    audioStreamUrl: String?,
    subtitleUrl: String?,
    subtitleLanguage: String?,
    filePath: String,
    resume: Boolean
): Job = launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
    try {
        val outputFile = File(filePath)
        if (audioStreamUrl != null || subtitleUrl != null) {
            muxDownload(downloadId, streamUrl, audioStreamUrl, subtitleUrl, subtitleLanguage, outputFile)
            DownloadRepository.markCompleted(downloadId, outputFile.length())
            return@launch
        }

        val existingBytes = if (resume && outputFile.exists()) outputFile.length() else 0L
        val response = httpClient.get(streamUrl) {
            headers {
                append(HttpHeaders.UserAgent, defaultStreamUserAgent())
                if (existingBytes > 0) append(HttpHeaders.Range, "bytes=$existingBytes-")
            }
        }
        val appendToFile = existingBytes > 0 && response.status == HttpStatusCode.PartialContent
        val startingBytes = if (appendToFile) existingBytes else 0L
        val responseLength = response.contentLength() ?: -1L
        val contentLength = if (responseLength > 0) responseLength + startingBytes else responseLength
        var downloadedBytes = startingBytes

        response.bodyAsChannel().also { channel ->
            BufferedOutputStream(
                FileOutputStream(outputFile, appendToFile),
                DOWNLOAD_BUFFER_SIZE
            ).use { outputStream ->
                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                var lastProgressUpdateAt = System.currentTimeMillis()
                var lastProgressUpdateBytes = startingBytes
                while (!channel.isClosedForRead) {
                    currentCoroutineContext().ensureActive()
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read > 0) {
                        outputStream.write(buffer, 0, read)
                        downloadedBytes += read.toLong()
                        val now = System.currentTimeMillis()
                        if (downloadedBytes - lastProgressUpdateBytes >= PROGRESS_UPDATE_BYTES ||
                            now - lastProgressUpdateAt >= PROGRESS_UPDATE_INTERVAL_MS
                        ) {
                            DownloadRepository.updateProgress(downloadId, downloadedBytes, contentLength)
                            lastProgressUpdateAt = now
                            lastProgressUpdateBytes = downloadedBytes
                        }
                    }
                }
            }
        }

        DownloadRepository.markCompleted(downloadId, downloadedBytes)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        println("Download failed for $downloadId: ${e.message}")
        e.printStackTrace()
        DownloadRepository.markFailed(downloadId)
    } finally {
        downloadJobs.remove(downloadId)
    }
}

private suspend fun muxDownload(
    downloadId: Int,
    videoUrl: String,
    audioUrl: String?,
    subtitleUrl: String?,
    subtitleLanguage: String?,
    outputFile: File
) {
    outputFile.delete()
    val videoFile = File.createTempFile("newpipe-video-", ".stream", outputFile.parentFile)
    val audioFile = audioUrl?.let { File.createTempFile("newpipe-audio-", ".stream", outputFile.parentFile) }
    val subtitleFile = subtitleUrl?.let { File.createTempFile("newpipe-subtitle-", ".vtt", outputFile.parentFile) }
    try {
        val videoBytes = downloadMuxInput(videoUrl, videoFile, downloadId, 0)
        val audioBytes = if (audioUrl != null && audioFile != null) {
            downloadMuxInput(audioUrl, audioFile, downloadId, videoBytes)
        } else {
            0L
        }
        if (subtitleUrl != null && subtitleFile != null) {
            downloadMuxInput(subtitleUrl, subtitleFile, downloadId, videoBytes + audioBytes)
        }
        muxLocalTracks(downloadId, videoFile, audioFile, subtitleFile, subtitleLanguage, outputFile)
    } finally {
        videoFile.delete()
        audioFile?.delete()
        subtitleFile?.delete()
    }
}

private suspend fun downloadMuxInput(
    url: String,
    outputFile: File,
    downloadId: Int,
    priorBytes: Long
): Long {
    val client = streamClient(url)
    val userAgent = userAgentForStream(client)
    val response = if (client in setOf("ANDROID", "IOS", "VISIONOS")) {
        httpClient.post(url) {
            header(HttpHeaders.UserAgent, userAgent)
            setBody(ByteArray(0))
        }
    } else {
        httpClient.get(url) {
            headers {
                append(HttpHeaders.UserAgent, userAgent)
                append(HttpHeaders.Referrer, "https://www.youtube.com/")
                append(HttpHeaders.Origin, "https://www.youtube.com")
                append(HttpHeaders.Accept, "*/*")
            }
        }
    }
    if (!response.status.isSuccess()) {
        throw IllegalStateException("YouTube stream request failed with HTTP ${response.status.value}")
    }

    var downloadedBytes = 0L
    var lastProgressUpdateBytes = priorBytes
    val channel = response.bodyAsChannel()
    BufferedOutputStream(FileOutputStream(outputFile), DOWNLOAD_BUFFER_SIZE).use { outputStream ->
        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
        while (!channel.isClosedForRead) {
            currentCoroutineContext().ensureActive()
            val read = channel.readAvailable(buffer, 0, buffer.size)
            if (read > 0) {
                outputStream.write(buffer, 0, read)
                downloadedBytes += read
                val totalDownloaded = priorBytes + downloadedBytes
                if (totalDownloaded - lastProgressUpdateBytes >= PROGRESS_UPDATE_BYTES) {
                    DownloadRepository.updateProgress(downloadId, totalDownloaded, -1)
                    lastProgressUpdateBytes = totalDownloaded
                }
            }
        }
    }
    DownloadRepository.updateProgress(downloadId, priorBytes + downloadedBytes, -1)
    return downloadedBytes
}

private fun streamClient(url: String): String? = URI(url).rawQuery
        ?.split('&')
        ?.firstOrNull { it.startsWith("c=") }
        ?.substringAfter('=')
        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }

private fun userAgentForStream(client: String?): String {
    return when (client) {
        "ANDROID" -> YoutubeParsingHelper.getAndroidUserAgent(null)
        "IOS" -> YoutubeParsingHelper.getIosUserAgent(null)
        "VISIONOS" -> YoutubeParsingHelper.getVisionOsUserAgent(null)
        else -> defaultStreamUserAgent()
    }
}

private fun defaultStreamUserAgent(): String =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

private suspend fun muxLocalTracks(
    downloadId: Int,
    videoFile: File,
    audioFile: File?,
    subtitleFile: File?,
    subtitleLanguage: String?,
    outputFile: File
) {
    val ffmpeg = System.getenv("FFMPEG_PATH") ?: "ffmpeg"
    val command = mutableListOf(
        ffmpeg,
        "-y", "-loglevel", "error", "-stats_period", "0.5", "-progress", "pipe:1",
        "-i", videoFile.absolutePath
    )
    var nextInputIndex = 1
    val audioInputIndex = if (audioFile != null) nextInputIndex++ else null
    if (audioFile != null) command.addAll(listOf("-i", audioFile.absolutePath))
    val subtitleInputIndex = if (subtitleFile != null) nextInputIndex else null
    if (subtitleFile != null) command.addAll(listOf("-i", subtitleFile.absolutePath))

    command.addAll(listOf("-map", "0:v:0"))
    command.addAll(if (audioInputIndex != null) {
        listOf("-map", "$audioInputIndex:a:0")
    } else {
        listOf("-map", "0:a?")
    })
    if (subtitleInputIndex != null) {
        command.addAll(listOf("-map", "$subtitleInputIndex:0", "-c:s", "srt"))
        subtitleLanguage?.let { command.addAll(listOf("-metadata:s:s:0", "language=$it")) }
    }
    command.addAll(listOf("-c:v", "copy", "-c:a", "copy", "-f", "matroska", outputFile.absolutePath))

    val process = ProcessBuilder(command).redirectErrorStream(true).start()
    val outputLines = LinkedBlockingQueue<String>()
    val reader = Thread {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { outputLines.offer(it) }
        }
        outputLines.offer("")
    }.apply {
        isDaemon = true
        start()
    }

    try {
        var outputClosed = false
        var lastProgressUpdate = 0L
        val recentOutput = mutableListOf<String>()
        while (process.isAlive || !outputClosed) {
            currentCoroutineContext().ensureActive()
            val line = outputLines.poll(500, TimeUnit.MILLISECONDS)
            if (line != null) {
                if (line.isEmpty()) {
                    outputClosed = true
                } else {
                    if (recentOutput.size == 5) recentOutput.removeAt(0)
                    recentOutput.add(line)
                }
            }

            val now = System.currentTimeMillis()
            if (now - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                DownloadRepository.updateProgress(downloadId, outputFile.length(), -1)
                lastProgressUpdate = now
            }
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IllegalStateException("FFmpeg exited with code $exitCode. ${recentOutput.joinToString(" ")}")
        }
    } finally {
        if (process.isAlive) process.destroyForcibly()
        reader.interrupt()
    }
}

fun Route.downloadRoutes() {
    route("/downloads") {
        get {
            call.respond(DownloadRepository.getAll())
        }

        post {
            val request = call.receive<StartDownloadRequest>()
            val downloadsDir = resolveDownloadsDir()
            downloadsDir.mkdirs()

            val ext = when {
                request.isAudioOnly -> "m4a"
                request.audioStreamUrl != null || request.subtitleUrl != null -> "mkv"
                else -> "mp4"
            }
            val safeTitle = request.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(100)
            val safeVideoId = request.videoId.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50)
            val filePath = "${downloadsDir.absolutePath}/${safeTitle}_${safeVideoId}.$ext"

            val downloadId = DownloadRepository.create(
                videoId = request.videoId,
                title = request.title,
                uploader = request.uploader,
                thumbnailUrl = request.thumbnailUrl,
                filePath = filePath,
                quality = request.quality,
                isAudioOnly = request.isAudioOnly,
                streamUrl = request.streamUrl,
                audioStreamUrl = request.audioStreamUrl,
                subtitleUrl = request.subtitleUrl,
                subtitleLanguage = request.subtitleLanguage
            )

            val downloadJob = call.application.launchDownload(
                downloadId,
                request.streamUrl,
                request.audioStreamUrl,
                request.subtitleUrl,
                request.subtitleLanguage,
                filePath,
                resume = false
            )
            downloadJobs[downloadId] = downloadJob
            downloadJob.start()
            call.respond(HttpStatusCode.Accepted, mapOf("id" to downloadId))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val download = DownloadRepository.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(download)
        }

        post("/{id}/pause") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val download = DownloadRepository.getById(id)
                ?: return@post call.respond(HttpStatusCode.NotFound)
            if (download.status != "PENDING" && download.status != "DOWNLOADING") {
                return@post call.respond(HttpStatusCode.Conflict, "Download is not active")
            }

            downloadJobs.remove(id)?.cancelAndJoin()
            DownloadRepository.markPaused(id)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{id}/resume") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val download = DownloadRepository.getById(id)
                ?: return@post call.respond(HttpStatusCode.NotFound)
            if (download.status != "PAUSED") {
                return@post call.respond(HttpStatusCode.Conflict, "Only paused downloads can be resumed")
            }
            val streamUrl = download.streamUrl
                ?: return@post call.respond(HttpStatusCode.UnprocessableEntity, "No stream URL stored")

            DownloadRepository.markPending(id)
            val downloadJob = call.application.launchDownload(
                id,
                streamUrl,
                download.audioStreamUrl,
                download.subtitleUrl,
                download.subtitleLanguage,
                download.filePath,
                resume = download.audioStreamUrl == null
            )
            downloadJobs[id] = downloadJob
            downloadJob.start()
            call.respond(HttpStatusCode.Accepted, mapOf("id" to id))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            downloadJobs.remove(id)?.cancelAndJoin()
            val download = DownloadRepository.getById(id)
            download?.let { File(it.filePath).delete() }
            DownloadRepository.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{id}/retry") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val download = DownloadRepository.getById(id)
                ?: return@post call.respond(HttpStatusCode.NotFound)
            if (download.status != "FAILED") {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    "Only FAILED downloads can be retried (current status: ${download.status})"
                )
            }

            val streams = DownloadRepository.resetForRetry(id)
                ?: return@post call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    "No stream URL stored for this download, please restart it from the watch page"
                )
            val downloadJob = call.application.launchDownload(
                id,
                streams.first,
                streams.second,
                download.subtitleUrl,
                download.subtitleLanguage,
                download.filePath,
                resume = false
            )
            downloadJobs[id] = downloadJob
            downloadJob.start()
            call.respond(HttpStatusCode.Accepted, mapOf("id" to id))
        }

        get("/{id}/file") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val download = DownloadRepository.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            val file = File(download.filePath)
            if (!file.exists()) {
                return@get call.respond(HttpStatusCode.NotFound, "File not found on disk")
            }

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    file.name
                ).toString()
            )
            call.respondFile(file)
        }
    }
}