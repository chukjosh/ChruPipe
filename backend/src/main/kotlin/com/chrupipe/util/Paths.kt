package com.chrupipe.util

import java.io.File

/** Resolve the downloads directory in order:
 * 1. Environment variable DOWNLOADS_DIR
 * 2. User-configured path from settings.json (via StorageSettingsRepository)
 * 3. System default per OS
 */
fun resolveDownloadsDir(): File = resolvePath(
    envVar = "DOWNLOADS_DIR",
    defaultWindows = "${System.getenv("USERPROFILE")}\\Downloads\\ChruPipe",
    defaultMac = "${System.getProperty("user.home")}/Downloads/ChruPipe",
    defaultLinux = System.getenv("XDG_DOWNLOAD_DIR")?.let { "${it}/ChruPipe" }
        ?: "${System.getProperty("user.home")}/Downloads/ChruPipe",
    legacyWindows = "${System.getenv("USERPROFILE")}\\Downloads\\ChruPipe",
    legacyMac = "${System.getProperty("user.home")}/Downloads/ChruPipe",
    legacyLinux = System.getenv("XDG_DOWNLOAD_DIR")?.let { "${it}/ChruPipe" }
        ?: "${System.getProperty("user.home")}/Downloads/ChruPipe"
)

/** Resolve the data (database) directory in order:
 * 1. Environment variable DATA_DIR
 * 2. User-configured path from settings.json
 * 3. System default per OS
 */
fun resolveDataDir(): File = resolvePath(
    envVar = "DATA_DIR",
    defaultWindows = "${System.getenv("APPDATA")}\\ChruPipe",
    defaultMac = "${System.getProperty("user.home")}/Library/Application Support/ChruPipe",
    defaultLinux = System.getenv("XDG_DATA_HOME")?.let { "${it}/ChruPipe" }
        ?: "${System.getProperty("user.home")}/.local/share/ChruPipe",
    legacyWindows = "${System.getenv("APPDATA")}\\ChruPipe",
    legacyMac = "${System.getProperty("user.home")}/Library/Application Support/ChruPipe",
    legacyLinux = System.getenv("XDG_DATA_HOME")?.let { "${it}/ChruPipe" }
        ?: "${System.getProperty("user.home")}/.local/share/ChruPipe"
)

private fun resolvePath(
    envVar: String,
    defaultWindows: String,
    defaultMac: String,
    defaultLinux: String,
    legacyWindows: String,
    legacyMac: String,
    legacyLinux: String
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
    val paths = when {
        os.contains("win") -> defaultWindows to legacyWindows
        os.contains("mac") -> defaultMac to legacyMac
        else -> defaultLinux to legacyLinux
    }
    val currentPath = File(paths.first)
    val legacyPath = File(paths.second)
    return if (!currentPath.exists() && legacyPath.exists()) legacyPath else currentPath
}
