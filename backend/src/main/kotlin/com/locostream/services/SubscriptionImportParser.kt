package com.locostream.services

import com.locostream.models.SubscribeRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SubscriptionImportParser {
    private val json = Json {
        ignoreUnknownKeys = true
        allowTrailingComma = true
    }

    @Serializable
    private data class SubscriptionImportEntry(
        val channelId: String? = null,
        val channelName: String? = null,
        val channelUrl: String? = null,
        val avatarUrl: String? = null,
        val service: String? = null
    )

    fun parse(raw: String, format: String?): List<SubscribeRequest> {
        val normalizedFormat = format?.trim()?.lowercase() ?: "json"
        return when (normalizedFormat) {
            "txt", "text" -> parseText(raw)
            "json" -> parseJson(raw)
            else -> parseText(raw)
        }
    }

    fun parseText(raw: String): List<SubscribeRequest> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { url ->
                SubscribeRequest(
                    channelId = url,
                    channelName = url,
                    channelUrl = url,
                    avatarUrl = "",
                    service = "youtube"
                )
            }
            .toList()
    }

    fun parseJson(raw: String): List<SubscribeRequest> {
        if (raw.isBlank()) return emptyList()

        return json.decodeFromString<List<SubscriptionImportEntry>>(raw)
            .mapNotNull { entry ->
                val channelUrl = entry.channelUrl?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null

                SubscribeRequest(
                    channelId = entry.channelId?.takeIf { it.isNotBlank() } ?: channelUrl,
                    channelName = entry.channelName?.takeIf { it.isNotBlank() } ?: channelUrl,
                    channelUrl = channelUrl,
                    avatarUrl = entry.avatarUrl.orEmpty(),
                    service = entry.service?.takeIf { it.isNotBlank() } ?: "youtube"
                )
            }
    }
}
