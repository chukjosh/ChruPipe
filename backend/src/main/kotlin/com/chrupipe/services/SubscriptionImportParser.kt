package com.chrupipe.services

import com.chrupipe.models.SubscribeRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

object SubscriptionImportParser {
    private val json = Json {
        ignoreUnknownKeys = true
        allowTrailingComma = true
        isLenient = true
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
            .distinctBy { it.lowercase() }
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

        val root = json.parseToJsonElement(raw)
        val entries = when (root) {
            is JsonArray -> root
            is JsonObject -> resolveJsonObjectEntries(root)
            else -> JsonArray(emptyList())
        }

        return json.decodeFromJsonElement<List<SubscriptionImportEntry>>(entries)
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
            .distinctBy { it.channelUrl.lowercase() }
    }

    private fun resolveJsonObjectEntries(root: JsonObject): JsonElement {
        val directEntry = root.takeIf { candidate ->
            candidate.containsKey("channelUrl") || candidate.containsKey("channelId")
        }
        if (directEntry != null) return JsonArray(listOf(directEntry))

        return root["data"]
            ?: root["subscriptions"]
            ?: root["items"]
            ?: root["entries"]
            ?: throw IllegalArgumentException("Import payload must be a JSON array or object containing a subscription list")
    }
}
