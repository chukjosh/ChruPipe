/**
 * Routing.kt
 *
 * Registers all route handlers with the Ktor application.
 * Split into two groups:
 *   - Extraction routes: call NewPipeExtractor for live online content
 *   - Local routes: read/write the SQLite database for user data
 */

package com.chrupipe.plugins

import com.chrupipe.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // ── NewPipeExtractor routes (online content) ──────────
        servicesRoutes()     // GET /services — list supported platforms
        searchRoutes()       // GET /search?q=&service=
        streamRoutes()       // GET /stream?url= or /stream/{youtubeId}
        streamProxyRoutes()  // GET /proxy?url= — CORS-safe media playback
        channelRoutes()      // GET /channel?url= or /channel/{youtubeId}
        trendingRoutes()     // GET /trending?service=
        commentRoutes()      // GET /comments?url= or /comments/{youtubeId}

        // ── Local database routes (user data) ─────────────────
        historyRoutes()      // GET/POST/DELETE /history
        watchlistRoutes()    // GET/POST/DELETE /watchlist
        playlistRoutes()     // GET/POST/DELETE /playlists
        subscriptionRoutes() // GET/POST/DELETE /subscriptions
        dataRoutes()         // GET/POST /export and /import
        docsRoutes()         // GET /docs and /openapi.yaml
        feedRoutes()         // GET /feed
        downloadRoutes()     // GET/POST/DELETE /downloads
        storageSettingsRoutes()   // GET/POST /settings/storage

    }
}
