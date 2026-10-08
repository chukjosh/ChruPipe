package com.newpipeweb.database.repositories

import com.newpipeweb.database.tables.*
import com.newpipeweb.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

// ─────────────────────────────────────────────
// HISTORY REPOSITORY
// ─────────────────────────────────────────────

object HistoryRepository {

    fun getAll(): List<HistoryModel> = transaction {
        HistoryTable.selectAll()
            .orderBy(HistoryTable.watchedAt, SortOrder.DESC)
            .map { row ->
                HistoryModel(
                    id = row[HistoryTable.id],
                    videoId = row[HistoryTable.videoId],
                    title = row[HistoryTable.title],
                    uploader = row[HistoryTable.uploader],
                    thumbnailUrl = row[HistoryTable.thumbnailUrl],
                    duration = row[HistoryTable.duration],
                    watchedAt = row[HistoryTable.watchedAt].format(formatter),
                    watchedSeconds = row[HistoryTable.watchedSeconds]
                )
            }
    }

    fun add(request: AddToHistoryRequest) = transaction {
        // Remove old entry for same video if exists (update to latest watch)
        HistoryTable.deleteWhere { videoId eq request.videoId }
        HistoryTable.insert {
            it[videoId] = request.videoId
            it[title] = request.title
            it[uploader] = request.uploader
            it[thumbnailUrl] = request.thumbnailUrl
            it[duration] = request.duration
            it[watchedAt] = LocalDateTime.now()
            it[watchedSeconds] = request.watchedSeconds
        }
    }

    fun import(items: List<HistoryModel>): CategoryImportSummary = transaction {
        var added = 0
        var updated = 0
        var alreadyExisted = 0

        items.forEach { item ->
            val parsedWatchedAt = LocalDateTime.parse(item.watchedAt)
            val existing = HistoryTable.selectAll()
                .where { HistoryTable.videoId eq item.videoId }
                .firstOrNull()

            if (existing == null) {
                HistoryTable.insert {
                    it[videoId] = item.videoId
                    it[title] = item.title
                    it[uploader] = item.uploader
                    it[thumbnailUrl] = item.thumbnailUrl
                    it[duration] = item.duration
                    it[HistoryTable.watchedAt] = parsedWatchedAt
                    it[HistoryTable.watchedSeconds] = item.watchedSeconds
                }
                added += 1
            } else {
                val currentWatchedAt = existing[HistoryTable.watchedAt]
                val currentSeconds = existing[HistoryTable.watchedSeconds]
                val shouldUpdate = parsedWatchedAt.isAfter(currentWatchedAt) || item.watchedSeconds > currentSeconds

                if (shouldUpdate) {
                    HistoryTable.update({ HistoryTable.videoId eq item.videoId }) {
                        it[HistoryTable.title] = item.title
                        it[HistoryTable.uploader] = item.uploader
                        it[HistoryTable.thumbnailUrl] = item.thumbnailUrl
                        it[HistoryTable.duration] = item.duration
                        it[HistoryTable.watchedAt] = parsedWatchedAt
                        it[HistoryTable.watchedSeconds] = maxOf(currentSeconds, item.watchedSeconds)
                    }
                    updated += 1
                } else {
                    alreadyExisted += 1
                }
            }
        }

        CategoryImportSummary(
            added = added,
            updated = updated,
            alreadyExisted = alreadyExisted
        )
    }

    fun deleteOne(id: Int) = transaction {
        HistoryTable.deleteWhere { HistoryTable.id eq id }
    }

    fun clearAll() = transaction {
        HistoryTable.deleteAll()
    }
}

// ─────────────────────────────────────────────
// WATCHLIST REPOSITORY
// ─────────────────────────────────────────────

object WatchlistRepository {

    fun getAll(): List<WatchlistModel> = transaction {
        WatchlistTable.selectAll()
            .orderBy(WatchlistTable.addedAt, SortOrder.DESC)
            .map { row ->
                WatchlistModel(
                    id = row[WatchlistTable.id],
                    videoId = row[WatchlistTable.videoId],
                    title = row[WatchlistTable.title],
                    uploader = row[WatchlistTable.uploader],
                    thumbnailUrl = row[WatchlistTable.thumbnailUrl],
                    duration = row[WatchlistTable.duration],
                    addedAt = row[WatchlistTable.addedAt].format(formatter)
                )
            }
    }

    fun add(request: AddToWatchlistRequest) = transaction {
        WatchlistTable.insertIgnore {
            it[videoId] = request.videoId
            it[title] = request.title
            it[uploader] = request.uploader
            it[thumbnailUrl] = request.thumbnailUrl
            it[duration] = request.duration
            it[addedAt] = LocalDateTime.now()
        }
    }

    fun import(items: List<WatchlistModel>): CategoryImportSummary = transaction {
        var added = 0
        var alreadyExisted = 0

        items.forEach { item ->
            val existing = WatchlistTable.selectAll()
                .where { WatchlistTable.videoId eq item.videoId }
                .firstOrNull()

            if (existing != null) {
                alreadyExisted += 1
                return@forEach
            }

            WatchlistTable.insert {
                it[videoId] = item.videoId
                it[title] = item.title
                it[uploader] = item.uploader
                it[thumbnailUrl] = item.thumbnailUrl
                it[duration] = item.duration
                it[addedAt] = LocalDateTime.parse(item.addedAt)
            }
            added += 1
        }

        CategoryImportSummary(added = added, alreadyExisted = alreadyExisted)
    }

    fun remove(id: Int) = transaction {
        WatchlistTable.deleteWhere { WatchlistTable.id eq id }
    }
}

// ─────────────────────────────────────────────
// PLAYLIST REPOSITORY
// ─────────────────────────────────────────────

object PlaylistRepository {

    fun getAll(): List<PlaylistModel> = transaction {
        PlaylistsTable.selectAll()
            .orderBy(PlaylistsTable.createdAt, SortOrder.DESC)
            .map { row ->
                val videoCount = PlaylistItemsTable
                    .select(PlaylistItemsTable.id)
                    .where { PlaylistItemsTable.playlistId eq row[PlaylistsTable.id] }
                    .count().toInt()

                val thumbnail = PlaylistItemsTable
                    .selectAll()
                    .where { PlaylistItemsTable.playlistId eq row[PlaylistsTable.id] }
                    .orderBy(PlaylistItemsTable.position)
                    .firstOrNull()
                    ?.get(PlaylistItemsTable.thumbnailUrl)

                PlaylistModel(
                    id = row[PlaylistsTable.id],
                    name = row[PlaylistsTable.name],
                    description = row[PlaylistsTable.description],
                    createdAt = row[PlaylistsTable.createdAt].format(formatter),
                    videoCount = videoCount,
                    thumbnailUrl = thumbnail
                )
            }
    }

    fun getAllWithVideos(): List<PlaylistWithVideos> = transaction {
        PlaylistsTable.selectAll()
            .orderBy(PlaylistsTable.createdAt, SortOrder.DESC)
            .map { playlistRow ->
                val playlistId = playlistRow[PlaylistsTable.id]
                val videos = PlaylistItemsTable.selectAll()
                    .where { PlaylistItemsTable.playlistId eq playlistId }
                    .orderBy(PlaylistItemsTable.position)
                    .map { row ->
                        PlaylistVideoModel(
                            id = row[PlaylistItemsTable.id],
                            videoId = row[PlaylistItemsTable.videoId],
                            url = row[PlaylistItemsTable.url],
                            title = row[PlaylistItemsTable.title],
                            uploader = row[PlaylistItemsTable.uploader],
                            thumbnailUrl = row[PlaylistItemsTable.thumbnailUrl],
                            duration = row[PlaylistItemsTable.duration],
                            addedAt = row[PlaylistItemsTable.addedAt].format(formatter),
                            position = row[PlaylistItemsTable.position]
                        )
                    }

                PlaylistWithVideos(
                    id = playlistId,
                    name = playlistRow[PlaylistsTable.name],
                    description = playlistRow[PlaylistsTable.description],
                    createdAt = playlistRow[PlaylistsTable.createdAt].format(formatter),
                    videos = videos
                )
            }
    }

    fun getById(id: Int): PlaylistWithVideos? = transaction {
        val playlist = PlaylistsTable.selectAll()
            .where { PlaylistsTable.id eq id }
            .firstOrNull() ?: return@transaction null

        val videos = PlaylistItemsTable.selectAll()
            .where { PlaylistItemsTable.playlistId eq id }
            .orderBy(PlaylistItemsTable.position)
            .map { row ->
                PlaylistVideoModel(
                    id = row[PlaylistItemsTable.id],
                    videoId = row[PlaylistItemsTable.videoId],
                    url = row[PlaylistItemsTable.url],
                    title = row[PlaylistItemsTable.title],
                    uploader = row[PlaylistItemsTable.uploader],
                    thumbnailUrl = row[PlaylistItemsTable.thumbnailUrl],
                    duration = row[PlaylistItemsTable.duration],
                    addedAt = row[PlaylistItemsTable.addedAt].format(formatter),
                    position = row[PlaylistItemsTable.position]
                )
            }

        PlaylistWithVideos(
            id = playlist[PlaylistsTable.id],
            name = playlist[PlaylistsTable.name],
            description = playlist[PlaylistsTable.description],
            createdAt = playlist[PlaylistsTable.createdAt].format(formatter),
            videos = videos
        )
    }

    fun create(request: CreatePlaylistRequest): Int = transaction {
        PlaylistsTable.insert {
            it[name] = request.name
            it[description] = request.description
            it[createdAt] = LocalDateTime.now()
        }[PlaylistsTable.id]
    }

    fun import(playlists: List<PlaylistWithVideos>): CategoryImportSummary = transaction {
        var added = 0
        var alreadyExisted = 0

        playlists.forEach { playlist ->
            val existing = PlaylistsTable.selectAll()
                .where { PlaylistsTable.name eq playlist.name }
                .firstOrNull()

            if (existing != null) {
                alreadyExisted += 1
                return@forEach
            }

            val playlistId = PlaylistsTable.insert {
                it[name] = playlist.name
                it[description] = playlist.description
                it[createdAt] = LocalDateTime.now()
            }[PlaylistsTable.id]

            playlist.videos.forEachIndexed { index, video ->
                PlaylistItemsTable.insert {
                    it[PlaylistItemsTable.playlistId] = playlistId
                    it[videoId] = video.videoId
                    it[PlaylistItemsTable.url] = video.url
                    it[title] = video.title
                    it[uploader] = video.uploader
                    it[thumbnailUrl] = video.thumbnailUrl
                    it[duration] = video.duration
                    it[addedAt] = LocalDateTime.parse(video.addedAt)
                    it[PlaylistItemsTable.position] = index
                }
            }
            added += 1
        }

        CategoryImportSummary(added = added, alreadyExisted = alreadyExisted)
    }

    fun delete(id: Int) = transaction {
        PlaylistItemsTable.deleteWhere { playlistId eq id }
        PlaylistsTable.deleteWhere { PlaylistsTable.id eq id }
    }

    fun addVideo(playlistId: Int, request: AddToPlaylistRequest) = transaction {
        val position = PlaylistItemsTable
            .select(PlaylistItemsTable.id)
            .where { PlaylistItemsTable.playlistId eq playlistId }
            .count().toInt()

        PlaylistItemsTable.insert {
            it[PlaylistItemsTable.playlistId] = playlistId
            it[videoId] = request.videoId
            it[PlaylistItemsTable.url] = request.url
            it[title] = request.title
            it[uploader] = request.uploader
            it[thumbnailUrl] = request.thumbnailUrl
            it[duration] = request.duration
            it[addedAt] = LocalDateTime.now()
            it[PlaylistItemsTable.position] = position
        }
    }

    fun removeVideo(playlistId: Int, videoItemId: Int) = transaction {
        PlaylistItemsTable.deleteWhere {
            (PlaylistItemsTable.playlistId eq playlistId) and
            (PlaylistItemsTable.id eq videoItemId)
        }
    }
}

// ─────────────────────────────────────────────
// SUBSCRIPTION REPOSITORY
// ─────────────────────────────────────────────

object SubscriptionRepository {

    fun getAll(): List<SubscriptionModel> = transaction {
        SubscriptionsTable.selectAll()
            .orderBy(SubscriptionsTable.channelName)
            .map { row ->
                SubscriptionModel(
                    id = row[SubscriptionsTable.id],
                    channelId = row[SubscriptionsTable.channelId],
                    channelName = row[SubscriptionsTable.channelName],
                    channelUrl = row[SubscriptionsTable.channelUrl],
                    avatarUrl = row[SubscriptionsTable.avatarUrl],
                    subscribedAt = row[SubscriptionsTable.subscribedAt].format(formatter)
                )
            }
    }

    fun exportText(): String = getAll()
        .joinToString(separator = "\n") { it.channelUrl.trim() }

    fun subscribe(request: SubscribeRequest): Boolean = transaction {
        val alreadyExists = SubscriptionsTable
            .selectAll()
            .where {
                (SubscriptionsTable.channelId eq request.channelId) or
                    (SubscriptionsTable.channelUrl eq request.channelUrl)
            }
            .count() > 0

        if (alreadyExists) {
            return@transaction false
        }

        SubscriptionsTable.insert {
            it[channelId] = request.channelId
            it[channelName] = request.channelName
            it[channelUrl] = request.channelUrl
            it[avatarUrl] = request.avatarUrl
            it[subscribedAt] = LocalDateTime.now()
        }

        true
    }

    fun import(requests: List<SubscribeRequest>, overwrite: Boolean = false): SubscriptionImportSummary = transaction {
        var added = 0
        var alreadySubscribed = 0
        var updated = 0
        var removed = 0

        if (overwrite) {
            removed = SubscriptionsTable.deleteAll()
        }

        requests.distinctBy { it.channelId to it.channelUrl }.forEach { request ->
            val existing = SubscriptionsTable
                .selectAll()
                .where {
                    (SubscriptionsTable.channelId eq request.channelId) or
                        (SubscriptionsTable.channelUrl eq request.channelUrl)
                }
                .firstOrNull()

            if (existing == null) {
                SubscriptionsTable.insert {
                    it[channelId] = request.channelId
                    it[channelName] = request.channelName
                    it[channelUrl] = request.channelUrl
                    it[avatarUrl] = request.avatarUrl
                    it[subscribedAt] = LocalDateTime.now()
                }
                added += 1
            } else {
                if (overwrite) {
                    SubscriptionsTable.update({ SubscriptionsTable.id eq existing[SubscriptionsTable.id] }) {
                        it[channelId] = request.channelId
                        it[channelName] = request.channelName
                        it[channelUrl] = request.channelUrl
                        it[avatarUrl] = request.avatarUrl
                    }
                    updated += 1
                } else {
                    alreadySubscribed += 1
                }
            }
        }

        SubscriptionImportSummary(
            added = added,
            alreadySubscribed = alreadySubscribed,
            updated = updated,
            removed = removed
        )
    }

    fun unsubscribe(id: Int) = transaction {
        SubscriptionsTable.deleteWhere { SubscriptionsTable.id eq id }
    }

    fun unsubscribeMany(ids: List<Int>): Int = transaction {
        ids.distinct().sumOf { id ->
            SubscriptionsTable.deleteWhere { SubscriptionsTable.id eq id }
        }
    }

    fun getAllChannelIds(): List<String> = transaction {
        SubscriptionsTable
            .select(SubscriptionsTable.channelId)
            .map { it[SubscriptionsTable.channelId] }
    }
}

// ─────────────────────────────────────────────
// DOWNLOAD REPOSITORY
// ─────────────────────────────────────────────

object DownloadRepository {

    fun getAll(): List<DownloadModel> = transaction {
        DownloadsTable.selectAll()
            .orderBy(DownloadsTable.createdAt, SortOrder.DESC)
            .map { it.toDownloadModel() }
    }

    fun getById(id: Int): DownloadModel? = transaction {
        DownloadsTable.selectAll()
            .where { DownloadsTable.id eq id }
            .firstOrNull()
            ?.toDownloadModel()
    }

    fun create(
        videoId: String, title: String, uploader: String,
        thumbnailUrl: String, filePath: String,
        quality: String, isAudioOnly: Boolean,
        streamUrl: String?, audioStreamUrl: String?,
        subtitleUrl: String?, subtitleLanguage: String?
    ): Int = transaction {
        DownloadsTable.insert {
            it[DownloadsTable.videoId] = videoId
            it[DownloadsTable.title] = title
            it[DownloadsTable.uploader] = uploader
            it[DownloadsTable.thumbnailUrl] = thumbnailUrl
            it[DownloadsTable.filePath] = filePath
            it[DownloadsTable.quality] = quality
            it[DownloadsTable.isAudioOnly] = isAudioOnly
            it[DownloadsTable.streamUrl] = streamUrl
            it[DownloadsTable.audioStreamUrl] = audioStreamUrl
            it[DownloadsTable.subtitleUrl] = subtitleUrl
            it[DownloadsTable.subtitleLanguage] = subtitleLanguage
            it[status] = "PENDING"
            it[createdAt] = LocalDateTime.now()
        }[DownloadsTable.id]
    }

    fun updateProgress(id: Int, downloadedBytes: Long, fileSize: Long) = transaction {
        DownloadsTable.update({ DownloadsTable.id eq id }) {
            it[DownloadsTable.downloadedBytes] = downloadedBytes
            it[DownloadsTable.fileSize] = fileSize
            it[status] = "DOWNLOADING"
        }
    }

    fun markPaused(id: Int) = transaction {
        DownloadsTable.update({ DownloadsTable.id eq id }) {
            it[status] = "PAUSED"
        }
    }

    fun markPending(id: Int) = transaction {
        DownloadsTable.update({ DownloadsTable.id eq id }) {
            it[status] = "PENDING"
        }
    }

    fun markCompleted(id: Int, fileSize: Long) = transaction {
        DownloadsTable.update({ DownloadsTable.id eq id }) {
            it[DownloadsTable.fileSize] = fileSize
            it[downloadedBytes] = fileSize
            it[status] = "COMPLETED"
        }
    }

    fun markFailed(id: Int) = transaction {
        DownloadsTable.update({ DownloadsTable.id eq id }) {
            it[status] = "FAILED"
        }
    }

    fun delete(id: Int) = transaction {
        DownloadsTable.deleteWhere { DownloadsTable.id eq id }
    }

    /**
     * Resets a FAILED download back to PENDING so it can be re-queued.
     * Returns the streamUrl needed to restart the background job, or null if
     * none was stored (e.g. old records created before this column existed).
     */
    fun resetForRetry(id: Int): Pair<String, String?>? = transaction {
        val row = DownloadsTable.selectAll()
            .where { DownloadsTable.id eq id }
            .firstOrNull() ?: return@transaction null
        val videoUrl = row[DownloadsTable.streamUrl] ?: return@transaction null
        val audioUrl = row[DownloadsTable.audioStreamUrl]
        DownloadsTable.update({ DownloadsTable.id eq id }) {
            it[status] = "PENDING"
            it[downloadedBytes] = 0
            it[fileSize] = -1
        }
        videoUrl to audioUrl
    }

    private fun ResultRow.toDownloadModel() = DownloadModel(
        id = this[DownloadsTable.id],
        videoId = this[DownloadsTable.videoId],
        title = this[DownloadsTable.title],
        uploader = this[DownloadsTable.uploader],
        thumbnailUrl = this[DownloadsTable.thumbnailUrl],
        filePath = this[DownloadsTable.filePath],
        fileSize = this[DownloadsTable.fileSize],
        downloadedBytes = this[DownloadsTable.downloadedBytes],
        status = this[DownloadsTable.status],
        quality = this[DownloadsTable.quality],
        isAudioOnly = this[DownloadsTable.isAudioOnly],
        createdAt = this[DownloadsTable.createdAt].format(formatter),
        streamUrl = this[DownloadsTable.streamUrl],
        audioStreamUrl = this[DownloadsTable.audioStreamUrl],
        subtitleUrl = this[DownloadsTable.subtitleUrl],
        subtitleLanguage = this[DownloadsTable.subtitleLanguage]
    )
}
