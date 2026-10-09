package com.chrupipe

import com.chrupipe.database.DatabaseFactory
import com.chrupipe.plugins.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader

fun main() {
    // Load environment variables from .env if one exists (optional; the container
    // gets its configuration from real environment variables instead).
    dotenv { ignoreIfMissing = true }
    // Initialize NewPipeExtractor with an HTTP downloader
    // NewPipeExtractor v0.26.2 uses `init(Downloader)` to register a downloader
    NewPipe.init(NewPipeDownloader.getInstance())

    // Initialize the SQLite database
    DatabaseFactory.init()

    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        watchPaths = listOf("classes", "resources"),
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureRouting()
}
