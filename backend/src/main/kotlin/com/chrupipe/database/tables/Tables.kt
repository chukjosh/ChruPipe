package com.chrupipe.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object HistoryTable : Table("history") {
    val id = integer("id").autoIncrement()
    val videoId = varchar("video_id", 500)
    val url = varchar("url", 2000).default("")
    val title = varchar("title", 500)
    val uploader = varchar("uploader", 500)
    val thumbnailUrl = varchar("thumbnail_url", 2000)
    val duration = long("duration")
    val watchedAt = datetime("watched_at")
    val watchedSeconds = long("watched_seconds").default(0)
    override val primaryKey = PrimaryKey(id)
}

object WatchlistTable : Table("watchlist") {
    val id = integer("id").autoIncrement()
    val videoId = varchar("video_id", 500).uniqueIndex()
    val title = varchar("title", 500)
    val uploader = varchar("uploader", 500)
    val thumbnailUrl = varchar("thumbnail_url", 2000)
    val duration = long("duration")
    val addedAt = datetime("added_at")
    override val primaryKey = PrimaryKey(id)
}

object PlaylistsTable : Table("playlists") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 200)
    val description = varchar("description", 1000).default("")
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

object PlaylistItemsTable : Table("playlist_items") {
    val id = integer("id").autoIncrement()
    val playlistId = integer("playlist_id").references(PlaylistsTable.id)
    val videoId = varchar("video_id", 500)
    val url = varchar("url", 2000).default("")
    val title = varchar("title", 500)
    val uploader = varchar("uploader", 500)
    val thumbnailUrl = varchar("thumbnail_url", 2000)
    val duration = long("duration")
    val addedAt = datetime("added_at")
    val position = integer("position").default(0)
    override val primaryKey = PrimaryKey(id)
}

object SubscriptionsTable : Table("subscriptions") {
    val id = integer("id").autoIncrement()
    val channelId = varchar("channel_id", 500).uniqueIndex()
    val channelName = varchar("channel_name", 500)
    val channelUrl = varchar("channel_url", 1000)
    val avatarUrl = varchar("avatar_url", 2000).default("")
    val subscribedAt = datetime("subscribed_at")
    override val primaryKey = PrimaryKey(id)
}

object DownloadsTable : Table("downloads") {
    val id = integer("id").autoIncrement()
    val videoId = varchar("video_id", 500)
    val title = varchar("title", 500)
    val uploader = varchar("uploader", 500)
    val thumbnailUrl = varchar("thumbnail_url", 2000)
    val filePath = varchar("file_path", 2000)
    val fileSize = long("file_size").default(-1)
    val downloadedBytes = long("downloaded_bytes").default(0)
    val status = varchar("status", 20).default("PENDING")
    val quality = varchar("quality", 50)
    val isAudioOnly = bool("is_audio_only").default(false)
    val createdAt = datetime("created_at")
    // Stored so that FAILED downloads can be retried without going back to the watch page
    val streamUrl = varchar("stream_url", 4000).nullable()
    val audioStreamUrl = varchar("audio_stream_url", 4000).nullable()
    val subtitleUrl = varchar("subtitle_url", 4000).nullable()
    val subtitleLanguage = varchar("subtitle_language", 100).nullable()
    override val primaryKey = PrimaryKey(id)
}
