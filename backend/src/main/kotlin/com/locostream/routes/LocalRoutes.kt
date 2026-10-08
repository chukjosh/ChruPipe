package com.locostream.routes

import com.locostream.database.repositories.*
import com.locostream.models.*
import com.locostream.services.ExtractorService
import com.locostream.services.SubscriptionImportParser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

// ─────────────────────────────────────────────
// HISTORY
// ─────────────────────────────────────────────

fun Route.historyRoutes() {
    route("/history") {
        get {
            call.respond(HistoryRepository.getAll())
        }
        get("/export") {
            call.respond(ExportEnvelope(schemaVersion = 1, type = "history", data = HistoryRepository.getAll()))
        }
        post {
            val request = call.receive<AddToHistoryRequest>()
            HistoryRepository.add(request)
            call.respond(HttpStatusCode.Created)
        }
        post("/import") {
            val raw = call.receiveText()
            val items = try {
                val array = parseImportArray(raw, "history")
                Json.decodeFromString<List<HistoryModel>>(array.toString())
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid import payload: ${e.message}")
            }
            call.respond(HistoryRepository.import(items))
        }
        delete {
            HistoryRepository.clearAll()
            call.respond(HttpStatusCode.NoContent)
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            HistoryRepository.deleteOne(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

// ─────────────────────────────────────────────
// WATCHLIST
// ─────────────────────────────────────────────

fun Route.watchlistRoutes() {
    route("/watchlist") {
        get {
            call.respond(WatchlistRepository.getAll())
        }
        get("/export") {
            call.respond(ExportEnvelope(schemaVersion = 1, type = "watchlist", data = WatchlistRepository.getAll()))
        }
        post {
            val request = call.receive<AddToWatchlistRequest>()
            WatchlistRepository.add(request)
            call.respond(HttpStatusCode.Created)
        }
        post("/import") {
            val raw = call.receiveText()
            val items = try {
                val array = parseImportArray(raw, "watchlist")
                Json.decodeFromString<List<WatchlistModel>>(array.toString())
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid import payload: ${e.message}")
            }
            call.respond(WatchlistRepository.import(items))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            WatchlistRepository.remove(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

// ─────────────────────────────────────────────
// PLAYLISTS
// ─────────────────────────────────────────────

fun Route.playlistRoutes() {
    route("/playlists") {
        get {
            call.respond(PlaylistRepository.getAll())
        }
        get("/export") {
            call.respond(ExportEnvelope(schemaVersion = 1, type = "playlists", data = PlaylistRepository.getAllWithVideos()))
        }
        post {
            val request = call.receive<CreatePlaylistRequest>()
            val id = PlaylistRepository.create(request)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }
        post("/import") {
            val raw = call.receiveText()
            val items = try {
                val array = parseImportArray(raw, "playlists")
                Json.decodeFromString<List<PlaylistWithVideos>>(array.toString())
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid import payload: ${e.message}")
            }
            call.respond(PlaylistRepository.import(items))
        }
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val playlist = PlaylistRepository.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Playlist not found")
            call.respond(playlist)
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            PlaylistRepository.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/{id}/videos") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid playlist id")
            val request = call.receive<AddToPlaylistRequest>()
            PlaylistRepository.addVideo(id, request)
            call.respond(HttpStatusCode.Created)
        }
        delete("/{id}/videos/{videoItemId}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid playlist id")
            val videoItemId = call.parameters["videoItemId"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid video item id")
            PlaylistRepository.removeVideo(id, videoItemId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

// ─────────────────────────────────────────────
// SUBSCRIPTIONS
// ─────────────────────────────────────────────

fun Route.subscriptionRoutes() {
    route("/subscriptions") {
        get {
            call.respond(SubscriptionRepository.getAll())
        }

        get("/export") {
            val format = call.request.queryParameters["format"]?.trim()?.lowercase() ?: "json"
            when (format) {
                "json" -> call.respond(
                    ExportEnvelope(
                        schemaVersion = 1,
                        type = "subscriptions",
                        data = SubscriptionRepository.getAll()
                    )
                )
                "txt", "text" -> call.respondText(
                    SubscriptionRepository.exportText(),
                    ContentType.Text.Plain
                )
                else -> call.respond(HttpStatusCode.BadRequest, "Unsupported export format: $format")
            }
        }

        post {
            val request = call.receive<SubscribeRequest>()
            val created = SubscriptionRepository.subscribe(request)
            call.respond(if (created) HttpStatusCode.Created else HttpStatusCode.OK)
        }

        post("/import") {
            val raw = call.receiveText()
            val requestedFormat = call.request.queryParameters["format"]?.trim()?.lowercase()
            val format = requestedFormat ?: detectImportFormat(raw, call.request.header(HttpHeaders.ContentType))
            val overwrite = call.request.queryParameters["overwrite"]?.toBoolean() ?: false

            val requests = try {
                when (format) {
                    "json" -> SubscriptionImportParser.parse(raw, "json")
                    "txt", "text" -> SubscriptionImportParser.parse(raw, "txt")
                        .mapNotNull { request ->
                            val url = request.channelUrl.trim()
                            try {
                                val channel = ExtractorService.getChannel(url)
                                SubscribeRequest(
                                    channelId = channel.id,
                                    channelName = channel.name,
                                    channelUrl = channel.url,
                                    avatarUrl = channel.avatarUrl,
                                    service = channel.service
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                    else -> return@post call.respond(HttpStatusCode.BadRequest, "Unsupported import format: $format")
                }
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid import payload: ${e.message}")
            }

            val summary = SubscriptionRepository.import(requests, overwrite)
            call.respond(summary)
        }

        delete {
            val request = call.receive<SubscriptionDeleteRequest>()
            call.respond(SubscriptionDeleteSummary(SubscriptionRepository.unsubscribeMany(request.ids)))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            SubscriptionRepository.unsubscribe(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun parseImportArray(rawBody: String, expectedType: String): JsonArray {
    val element = Json.parseToJsonElement(rawBody)
    if (element is JsonArray) return element

    val root = element as? JsonObject
        ?: throw IllegalArgumentException("Import payload must be a JSON array or object with a 'data' array")

    val actualType = (root["type"] as? JsonPrimitive)?.content
    if (actualType != null && actualType != expectedType) {
        throw IllegalArgumentException("Expected type '$expectedType' but got '$actualType'")
    }

    val dataNode = root["data"]
        ?: throw IllegalArgumentException("Import payload is missing the 'data' array")
    return dataNode.jsonArray
}

private fun detectImportFormat(rawBody: String, contentType: String?): String? {
    val normalizedContentType = contentType?.lowercase() ?: ""
    if (normalizedContentType.contains("json")) return "json"
    if (normalizedContentType.contains("text/plain") || normalizedContentType.contains("text")) return "txt"
    if (rawBody.trimStart().startsWith("[")) return "json"
    return "txt"
}

fun Route.dataRoutes() {
    route("/") {
        get {
            call.respond(
                mapOf(
                    "name" to "LocoStream Backend",
                    "status" to "live",
                    "message" to "Backend is running",
                    "docs" to "/docs",
                    "health" to "/health"
                )
            )
        }

        get("/health") {
            call.respond(
                mapOf(
                    "name" to "LocoStream Backend",
                    "status" to "healthy",
                    "message" to "Backend is running",
                    "docs" to "/docs",
                    "health" to "/health"
                )
            )
        }

        get("/export") {
            call.respond(
                CombinedExportEnvelope(
                    schemaVersion = 1,
                    subscriptions = SubscriptionRepository.getAll(),
                    playlists = PlaylistRepository.getAllWithVideos(),
                    history = HistoryRepository.getAll(),
                    watchlist = WatchlistRepository.getAll()
                )
            )
        }

        post("/import") {
            val raw = call.receiveText()
            val root = Json.parseToJsonElement(raw).jsonObject

            val importedSubscriptions = Json.decodeFromString<List<SubscriptionModel>>(
                root["subscriptions"]?.toString() ?: "[]"
            ).map { item ->
                SubscribeRequest(
                    channelId = item.channelId,
                    channelName = item.channelName,
                    channelUrl = item.channelUrl,
                    avatarUrl = item.avatarUrl,
                    service = item.service
                )
            }

            val summary = CombinedImportSummary(
                subscriptions = SubscriptionRepository.import(importedSubscriptions),
                playlists = PlaylistRepository.import(
                    Json.decodeFromString<List<PlaylistWithVideos>>(root["playlists"]?.toString() ?: "[]")
                ),
                history = HistoryRepository.import(
                    Json.decodeFromString<List<HistoryModel>>(root["history"]?.toString() ?: "[]")
                ),
                watchlist = WatchlistRepository.import(
                    Json.decodeFromString<List<WatchlistModel>>(root["watchlist"]?.toString() ?: "[]")
                )
            )
            call.respond(summary)
        }
    }
}

fun Route.docsRoutes() {
    get("/docs") {
        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>LocoStream API Docs</title>
                <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui.css" />
                <style>
                    :root {
                        color-scheme: light;
                        --page-background: #f6f8fb;
                        --page-text: #172033;
                        --panel-background: #ffffff;
                        --panel-border: #d8dee9;
                        --muted-text: #5f6b7a;
                        --button-background: #172033;
                        --button-text: #ffffff;
                    }
                    :root.dark-mode {
                        color-scheme: dark;
                        --page-background: #111827;
                        --page-text: #e5e7eb;
                        --panel-background: #1f2937;
                        --panel-border: #4b5563;
                        --muted-text: #b8c2d1;
                        --button-background: #e5e7eb;
                        --button-text: #111827;
                    }
                    html, body {
                        margin: 0;
                        min-height: 100%;
                        background: var(--page-background);
                        color: var(--page-text);
                        font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                    }
                    .docs-toolbar {
                        display: flex;
                        justify-content: flex-end;
                        padding: 12px 20px 0;
                    }
                    .theme-toggle {
                        border: 1px solid var(--panel-border);
                        border-radius: 6px;
                        background: var(--button-background);
                        color: var(--button-text);
                        cursor: pointer;
                        font-size: 14px;
                        font-weight: 600;
                        padding: 8px 12px;
                    }
                    .theme-toggle:focus-visible {
                        outline: 3px solid #60a5fa;
                        outline-offset: 2px;
                    }
                    #swagger-ui {
                        max-width: 1200px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .topbar { display: none; }
                    .swagger-ui .info .title,
                    .swagger-ui .info p,
                    .swagger-ui .info li,
                    .swagger-ui .opblock-tag,
                    .swagger-ui .opblock-summary-description,
                    .swagger-ui label,
                    .swagger-ui .parameter__name,
                    .swagger-ui .parameter__type,
                    .swagger-ui table thead tr th,
                    .swagger-ui table thead tr td {
                        color: var(--page-text);
                    }
                    .swagger-ui .info a,
                    .swagger-ui .info a:visited {
                        color: #2563eb;
                    }
                    :root.dark-mode .swagger-ui .info a,
                    :root.dark-mode .swagger-ui .info a:visited {
                        color: #93c5fd;
                    }
                    .swagger-ui .model-box,
                    .swagger-ui .opblock-description-wrapper,
                    .swagger-ui .opblock-external-docs-wrapper,
                    .swagger-ui section.models {
                        background: var(--panel-background);
                        border-color: var(--panel-border);
                    }
                    .swagger-ui .model,
                    .swagger-ui .model-title,
                    .swagger-ui .prop-type,
                    .swagger-ui .renderedMarkdown p,
                    .swagger-ui .response-col_description__inner p {
                        color: var(--page-text);
                    }
                    :root.dark-mode .swagger-ui .opblock-summary,
                    :root.dark-mode .swagger-ui .opblock-description-wrapper,
                    :root.dark-mode .swagger-ui .opblock-external-docs-wrapper,
                    :root.dark-mode .swagger-ui section.models,
                    :root.dark-mode .swagger-ui .model-box {
                        background: var(--panel-background);
                    }
                    :root.dark-mode .swagger-ui select,
                    :root.dark-mode .swagger-ui input,
                    :root.dark-mode .swagger-ui textarea {
                        background: #111827;
                        border-color: var(--panel-border);
                        color: var(--page-text);
                    }
                </style>
            </head>
            <body>
                <div class="docs-toolbar">
                    <button id="theme-toggle" class="theme-toggle" type="button" aria-label="Switch color theme">
                        Use dark mode
                    </button>
                </div>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui-bundle.js"></script>
                <script src="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui-standalone-preset.js"></script>
                <script>
                    const root = document.documentElement;
                    const themeToggle = document.getElementById('theme-toggle');
                    const savedTheme = window.localStorage.getItem('locostream-docs-theme');
                    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

                    function setTheme(theme) {
                        const isDark = theme === 'dark';
                        root.classList.toggle('dark-mode', isDark);
                        themeToggle.textContent = isDark ? 'Use light mode' : 'Use dark mode';
                        themeToggle.setAttribute('aria-pressed', String(isDark));
                        window.localStorage.setItem('locostream-docs-theme', isDark ? 'dark' : 'light');
                    }

                    setTheme(savedTheme || (prefersDark ? 'dark' : 'light'));
                    themeToggle.addEventListener('click', function () {
                        setTheme(root.classList.contains('dark-mode') ? 'light' : 'dark');
                    });

                    window.onload = function () {
                        if (typeof SwaggerUIBundle === 'undefined' || typeof SwaggerUIStandalonePreset === 'undefined') {
                            document.getElementById('swagger-ui').innerHTML = '<h2>Swagger UI failed to load.</h2><p>Check the browser network access or try again.</p>';
                            return;
                        }
                        SwaggerUIBundle({
                            url: '/openapi.yaml',
                            dom_id: '#swagger-ui',
                            deepLinking: true,
                            presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                            layout: 'BaseLayout',
                            theme: 'dark'
                        });
                    };
                </script>
            </body>
            </html>
        """.trimIndent()
        call.respondText(html, ContentType.Text.Html)
    }

    get("/openapi.yaml") {
        val yamlText = Thread.currentThread().contextClassLoader
            .getResourceAsStream("openapi/locostream-openapi.yaml")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: """
            openapi: 3.0.3
            info:
              title: LocoStream API
              version: 1.0.0
              description: Backend API documentation for LocoStream.
            servers:
              - url: http://127.0.0.1:8080
            paths:
              /:
                get:
                  summary: Backend status
                  responses:
                    '200':
                      description: Backend is live.
              /docs:
                get:
                  summary: Swagger UI page
                  responses:
                    '200':
                      description: Interactive API docs.
            """.trimIndent()

        call.respondText(yamlText, ContentType.parse("application/yaml"))
    }
}

// ─────────────────────────────────────────────
// FEED (latest videos from subscribed channels)
// ─────────────────────────────────────────────

fun Route.feedRoutes() {
    get("/feed") {
        val subscriptions = SubscriptionRepository.getAll()
        if (subscriptions.isEmpty()) {
            call.respond(emptyList<Any>())
            return@get
        }

        // Fetch latest videos from each subscribed channel (in parallel via coroutines)
        val feedVideos = subscriptions
            .flatMap { subscription ->
                try {
                    val channel = com.locostream.services.ExtractorService.getChannel(subscription.channelUrl)
                    channel.videos.take(5) // latest 5 per channel
                } catch (e: Exception) {
                    emptyList()
                }
            }
            .sortedByDescending { it.uploadDateTimestamp ?: Long.MIN_VALUE }

        call.respond(feedVideos)
    }
}
