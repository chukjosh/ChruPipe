package com.locostream.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Data class representing user‑defined storage locations */
@Serializable
data class StorageSettings(
    val downloadsDir: String? = null,
    val dataDir: String? = null,
    val trendingCountry: String? = "US"
)

/** Repository for reading/writing the settings.json file */
object StorageSettingsRepository {
    // Use OS‑specific config directory; fall back to user home .config
    private val configDir: File = run {
        val env = System.getenv("APPDATA")
        if (env != null) File(env, "LocoStream")
        else File(System.getProperty("user.home"), ".config/LocoStream")
    }
    private val settingsFile = File(configDir, "settings.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun load(): StorageSettings? = if (settingsFile.exists()) {
        json.decodeFromString(StorageSettings.serializer(), settingsFile.readText())
    } else null

    fun save(settings: StorageSettings) {
        if (!configDir.exists()) configDir.mkdirs()
        settingsFile.writeText(json.encodeToString(StorageSettings.serializer(), settings))
    }
}
