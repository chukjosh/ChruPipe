package com.locostream.util

import java.io.File

/** Resolve the downloads directory in order:
 * 1. Environment variable DOWNLOADS_DIR
 * 2. User-configured path from settings.json (via StorageSettingsRepository)
 * 3. System default per OS
 */
fun resolveDownloadsDir(): File = resolvePath(
    envVar = "DOWNLOADS_DIR",
    defaultWindows = "${System.getenv("USERPROFILE")}\\Downloads\\LocoStream",
    defaultMac = "${System.getProperty("user.home")}/Downloads/LocoStream",
    defaultLinux = System.getenv("XDG_DOWNLOAD_DIR")?.let { "${it}/LocoStream" }
        ?: "${System.getProperty("user.home")}/Downloads/LocoStream"
)

/** Resolve the data (database) directory in order:
 * 1. Environment variable DATA_DIR
 * 2. User-configured path from settings.json
 * 3. System default per OS
 */
fun resolveDataDir(): File = resolvePath(
    envVar = "DATA_DIR",
    defaultWindows = "${System.getenv("APPDATA")}\\LocoStream",
    defaultMac = "${System.getProperty("user.home")}/Library/Application Support/LocoStream",
    defaultLinux = System.getenv("XDG_DATA_HOME")?.let { "${it}/LocoStream" }
        ?: "${System.getProperty("user.home")}/.local/share/LocoStream"
)

private fun resolvePath(
    envVar: String,
    defaultWindows: String,
    defaultMac: String,
    defaultLinux: String
): File {
    // 1. Environment variable
    System.getenv(envVar)?.let { return File(it) }

    // 2. Settings repository (custom path)
    val settings = StorageSettingsRepository.load()
    val customPath = when (envVar) {
        "DOWNLOADS_DIR" -> settings?.downloadsDir
        "DATA_DIR" -> settings?.dataDir
        else -> null
    }
    if (!customPath.isNullOrBlank()) return File(customPath)

    // 3. System default based on OS
    val os = System.getProperty("os.name").lowercase()
    val path = when {
        os.contains("win") -> defaultWindows
        os.contains("mac") -> defaultMac
        else -> defaultLinux
    }
    return File(path)
}
