package com.chrupipe.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import java.net.InetAddress

fun Application.configureCORS() {
    val allowedOrigins = System.getenv("ALLOWED_ORIGINS")
        ?.split(",")
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?.toSet()
        ?: emptySet()

    install(CORS) {
        // Allow requests from the React frontend
        allowHost("localhost:5173")   // Vite dev server
        allowHost("localhost:3000")   // Alternative dev port
        allowHost("localhost:4173")   // Vite preview
        allowHost("localhost:80")     // Production frontend

        // Allow requests from any RFC-1918 private-network IP
        // (covers LAN access via 10.x, 172.16–31.x, 192.168.x)
        allowOrigins { origin ->
            origin in allowedOrigins || isPrivateNetworkOrigin(origin)
        }

        // Allow all headers and methods needed by the frontend
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Options)
    }
}

/**
 * Returns true if [origin] (e.g. "http://192.168.1.50:80") has a host that is
 * a private-network IP literal. Hostnames are explicitly rejected to prevent
 * DNS-rebinding style origin spoofing.
 */
private fun isPrivateNetworkOrigin(origin: String): Boolean {
    return try {
        val host = Url(origin).host.trimEnd('.')
        // Only accept literal IP addresses — never resolve hostnames,
        // to avoid DNS-rebinding style origin spoofing.
        if (!isIpLiteral(host)) return false
        val addr = InetAddress.getByName(host) // safe: no DNS lookup for an IP literal
        addr.isSiteLocalAddress || addr.isLoopbackAddress
    } catch (_: Exception) {
        false
    }
}

private fun isIpLiteral(host: String): Boolean {
    val ipv4Regex = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")
    return ipv4Regex.matches(host) || host.contains(':') // crude IPv6 literal check
}
