/**
 * ExtractorService.kt
 *
 * Central service that wraps NewPipeExtractor for ALL supported services:
 * - YouTube
 * - SoundCloud
 * - PeerTube
 * - Bandcamp
 * - media.ccc.de
 * - Odysee (LBRY)
 *
 * Instead of hardcoding YouTube, every function accepts either:
 *   - A `serviceId` int (0=YouTube, 1=SoundCloud, etc.)
 *   - A full URL (service is auto-detected from the URL)
 *
 * NewPipeExtractor service IDs:
 *   0 = YouTube       https://youtube.com
 *   1 = SoundCloud    https://soundcloud.com
 *   2 = media.ccc.de  https://media.ccc.de
 *   3 = PeerTube      https://peertube.tv (and any instance)
 *   4 = Bandcamp      https://bandcamp.com
 *   5 = Odysee        https://odysee.com
 */

package com.chrupipe.services

import com.chrupipe.models.*
import com.chrupipe.util.StorageSettingsRepository
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo

object ExtractorService {

    private const val TRENDING_LIMIT = 30

    /**
     * YouTube trending feeds supported by NewPipeExtractor.
     * The main trending page is preferred, followed by category-specific kiosks.
     */
    private val YOUTUBE_TRENDING_KIOSK_URLS = listOf(
        "https://www.youtube.com/feed/trending",
        "https://www.youtube.com/gaming/trending",
        "https://charts.youtube.com/charts/TrendingVideos/RightNow",
        "https://www.youtube.com/podcasts/popularepisodes",
        "https://charts.youtube.com/charts/TrendingTrailers",
    )

    // ─────────────────────────────────────────────────────────
    // Service registry — maps string names to NewPipe services
    // ─────────────────────────────────────────────────────────

    /**
     * All supported services by their string identifier.
     * Used to map the `service` query parameter to the actual service object.
     */
    val SUPPORTED_SERVICES: Map<String, ServiceInfo> = mapOf(
        "youtube"    to ServiceInfo(0, "YouTube",      "https://youtube.com",      supportsTrending = true,  supportsComments = true),
        "soundcloud" to ServiceInfo(1, "SoundCloud",   "https://soundcloud.com",   supportsTrending = true,  supportsComments = false),
        "mediaccc"   to ServiceInfo(2, "media.ccc.de", "https://media.ccc.de",     supportsTrending = true,  supportsComments = false),
        "peertube"   to ServiceInfo(3, "PeerTube",     "https://peertube.tv",      supportsTrending = true,  supportsComments = false),
        "bandcamp"   to ServiceInfo(4, "Bandcamp",     "https://bandcamp.com",     supportsTrending = false, supportsComments = false),
        "odysee"     to ServiceInfo(5, "Odysee",       "https://odysee.com",       supportsTrending = false, supportsComments = false),
    )

    /**
     * Metadata about a supported service — returned to the frontend
     * so it can show the correct service selector options.
     */
    data class ServiceInfo(
        val id: Int,
        val name: String,
        val baseUrl: String,
        val supportsTrending: Boolean,
        val supportsComments: Boolean
    )

    private val SERVICE_NAME_BY_EXTRACTOR_ID: Map<Int, String> =
        SUPPORTED_SERVICES.entries.associate { (key, info) -> info.id to key }

    /** Maps NewPipeExtractor service IDs to our API service keys (e.g. 2 → "mediaccc"). */
    fun serviceKeyFromExtractorId(serviceId: Int): String =
        SERVICE_NAME_BY_EXTRACTOR_ID[serviceId] ?: "unknown"

    /**
     * Resolve a service name string to the NewPipeExtractor StreamingService.
     * Defaults to YouTube if the name is unrecognised.
     */
    private fun resolveService(serviceName: String): StreamingService {
        val info = SUPPORTED_SERVICES[serviceName.lowercase()]
            ?: SUPPORTED_SERVICES["youtube"]!!
        return NewPipe.getService(info.id)
    }

    /**
     * Auto-detect the service from a full URL.
     * NewPipeExtractor can identify which service owns any given URL.
     * Falls back to YouTube on failure.
     */
    private fun serviceFromUrl(url: String): StreamingService {
        return try {
            NewPipe.getServiceByUrl(url)
        } catch (e: Exception) {
            NewPipe.getService(0) // YouTube fallback
        }
    }

    /**
     * Extract a clean ID from any URL for use as a local key.
     * For YouTube this is the video ID; for other services it's the full URL.
     */
    fun extractId(url: String?): String {
        if (url == null) return ""
        // YouTube: extract ?v=ID
        val ytRegex = Regex("[?&]v=([^&]+)")
        val ytMatch = ytRegex.find(url)
        if (ytMatch != null) return ytMatch.groupValues[1]
        // For all other services, use the full URL as the ID
        return url
    }

    // ─────────────────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────────────────

    /**
     * Search for content on the specified service.
     *
     * @param query      Search terms
     * @param serviceName Service to search (e.g. "youtube", "soundcloud")
     */
    fun search(query: String, serviceName: String = "youtube"): SearchModel {
        val service = resolveService(serviceName)

        val searchInfo = try {
            SearchInfo.getInfo(
                service,
                service.searchQHFactory.fromQuery(query)
            )
        } catch (e: Exception) {
            return SearchModel(query = query, service = serviceName, items = emptyList())
        }

        val items = searchInfo.relatedItems
            .filterNotNull()
            .mapNotNull { item ->
                if (item !is StreamInfoItem) return@mapNotNull null
                try {
                    VideoModel(
                        id           = extractId(item.url),
                        title        = item.name ?: "Unknown",
                        uploader     = item.uploaderName ?: "Unknown",
                        uploaderUrl  = item.uploaderUrl ?: "",
                        duration     = item.duration,
                        viewCount    = item.viewCount,
                        uploadDate   = item.textualUploadDate ?: "",
                        uploadDateTimestamp = item.uploadDate?.instant?.toEpochMilli(),
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url ?: "",
                        isLive       = item.streamType == StreamType.LIVE_STREAM || item.streamType == StreamType.AUDIO_LIVE_STREAM,
                        url          = item.url ?: "",
                        service      = serviceName
                    )
                } catch (e: Exception) {
                    null
                }
            }

        return SearchModel(
            query    = query,
            service  = serviceName,
            items    = items,
            nextPage = searchInfo.nextPage?.url
        )
    }

    // ─────────────────────────────────────────────────────────
    // STREAM (by full URL — works for any service)
    // ─────────────────────────────────────────────────────────

    /**
     * Fetch stream URLs and metadata for any content URL.
     * The service is auto-detected from the URL.
     *
     * @param url Full content URL (e.g. YouTube watch URL, SoundCloud track URL)
     */
    @Suppress("DEPRECATION")
    fun getStreams(url: String): StreamModel {
        val service   = serviceFromUrl(url)
        val streamInfo = try {
            StreamInfo.getInfo(service, url)
        } catch (e: Exception) {
            throw IllegalArgumentException("Extraction failed for $url: ${e.message}")
        }

        // Combine regular + video-only streams
        val videoStreams = streamInfo.videoStreams.map { s ->
            StreamUrl(url = s.content, quality = s.resolution, format = s.format?.name ?: "UNKNOWN", isVideoOnly = false)
        } + streamInfo.videoOnlyStreams.map { s ->
            StreamUrl(url = s.content, quality = s.resolution, format = s.format?.name ?: "UNKNOWN", isVideoOnly = true)
        }

        val audioStreams = streamInfo.audioStreams.map { s ->
            StreamUrl(url = s.content, quality = "${s.averageBitrate}kbps", format = s.format?.name ?: "UNKNOWN", isVideoOnly = false)
        }

        val subtitles = streamInfo.subtitles.map { sub ->
            SubtitleTrack(
                url              = sub.content,
                languageCode     = sub.languageTag ?: "unknown",
                languageName     = sub.displayLanguageName ?: sub.languageTag ?: "Unknown",
                isAutoGenerated  = sub.isAutoGenerated
            )
        }

        val related = streamInfo.relatedItems
            .filterNotNull()
            .take(20)
            .mapNotNull { item ->
                if (item !is StreamInfoItem) return@mapNotNull null
                try {
                    VideoModel(
                        id           = extractId(item.url),
                        title        = item.name ?: "Unknown",
                        uploader     = item.uploaderName ?: "Unknown",
                        uploaderUrl  = item.uploaderUrl ?: "",
                        duration     = item.duration,
                        viewCount    = item.viewCount,
                        uploadDate   = item.textualUploadDate ?: "",
                        uploadDateTimestamp = item.uploadDate?.instant?.toEpochMilli(),
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url ?: "",
                        isLive       = item.streamType == StreamType.LIVE_STREAM || item.streamType == StreamType.AUDIO_LIVE_STREAM,
                        url          = item.url ?: "",
                        service      = serviceKeyFromExtractorId(service.serviceId)
                    )
                } catch (e: Exception) {
                    null
                }
            }

        return StreamModel(
            id           = extractId(url),
            title        = streamInfo.name,
            uploader     = streamInfo.uploaderName,
            uploaderUrl  = streamInfo.uploaderUrl ?: "",
            duration     = streamInfo.duration,
            videoStreams  = videoStreams,
            audioStreams  = audioStreams,
            hlsUrl        = streamInfo.hlsUrl,
            subtitles     = subtitles,
            relatedVideos = related,
            service       = serviceKeyFromExtractorId(service.serviceId),
            thumbnailUrl  = streamInfo.thumbnails.lastOrNull()?.url ?: ""
        )
    }

    // ─────────────────────────────────────────────────────────
    // CHANNEL (by full URL)
    // ─────────────────────────────────────────────────────────

    /**
     * Fetch channel/artist info and their latest content.
     *
     * @param url Full channel URL (YouTube channel, SoundCloud artist, etc.)
     */
    fun getChannel(url: String): ChannelModel {
        val service     = serviceFromUrl(url)
        val channelInfo = ChannelInfo.getInfo(service, url)

        val videosTab = channelInfo.tabs.firstOrNull { 
            it.id == ChannelTabs.VIDEOS || it.id == ChannelTabs.TRACKS 
        } ?: channelInfo.tabs.firstOrNull()

        val rawVideos = if (videosTab != null) {
            try {
                ChannelTabInfo.getInfo(service, videosTab).relatedItems
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val videos = rawVideos
            .filterNotNull()
            .take(30)
            .mapNotNull { item ->
                if (item !is StreamInfoItem) return@mapNotNull null
                try {
                    VideoModel(
                        id           = extractId(item.url),
                        title        = item.name ?: "Unknown",
                        uploader     = channelInfo.name,
                        uploaderUrl  = channelInfo.url,
                        duration     = item.duration,
                        viewCount    = item.viewCount,
                        uploadDate   = item.textualUploadDate ?: "",
                        uploadDateTimestamp = item.uploadDate?.instant?.toEpochMilli(),
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url ?: "",
                        isLive       = item.streamType == StreamType.LIVE_STREAM || item.streamType == StreamType.AUDIO_LIVE_STREAM,
                        url          = item.url ?: "",
                        service      = service.serviceInfo.name.lowercase()
                    )
                } catch (e: Exception) {
                    null
                }
            }

        return ChannelModel(
            id              = extractId(url),
            name            = channelInfo.name,
            url             = channelInfo.url,
            description     = channelInfo.description ?: "",
            subscriberCount = channelInfo.subscriberCount,
            avatarUrl       = channelInfo.avatars.firstOrNull()?.url ?: "",
            bannerUrl       = channelInfo.banners.firstOrNull()?.url ?: "",
            videos          = videos,
            service         = service.serviceInfo.name.lowercase()
        )
    }

    // ─────────────────────────────────────────────────────────
    // TRENDING (per service)
    // ─────────────────────────────────────────────────────────

    /**
     * Trending/featured content for a service.
     * Not all services support trending — check SUPPORTED_SERVICES.supportsTrending.
     *
     * @param serviceName Service to fetch trending from
     */
    fun getTrending(serviceName: String = "youtube"): List<VideoModel> {
        val normalizedService = serviceName.lowercase()
        val serviceInfo = SUPPORTED_SERVICES[normalizedService]
            ?: SUPPORTED_SERVICES["youtube"]!!

        if (!serviceInfo.supportsTrending) return emptyList()

        val service = NewPipe.getService(serviceInfo.id)

        return try {
            if (normalizedService == "youtube") {
                val countryCode = StorageSettingsRepository.load()
                    ?.trendingCountry
                    ?.trim()
                    ?.uppercase()
                    ?.takeIf { it.isNotBlank() }
                getYoutubeTrending(service, countryCode)
            } else {
                val kioskUrl = when (normalizedService) {
                    "soundcloud" -> "https://soundcloud.com/charts/top"
                    "mediaccc"   -> "https://media.ccc.de/recent"
                    "peertube"   -> "https://peertube.tv/videos/trending"
                    else         -> serviceInfo.baseUrl
                }
                fetchKioskVideos(service, kioskUrl, normalizedService, TRENDING_LIMIT)
            }
        } catch (e: Exception) {
            println("ERROR: getTrending($serviceName) failed - ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun applyYoutubeTrendingCountry(service: StreamingService, countryCode: String?) {
        if (countryCode.isNullOrBlank()) return
        try {
            val country = ContentCountry(countryCode)
            NewPipe.setupLocalization(Localization("en", countryCode), country)
            service.kioskList.forceContentCountry(country)
        } catch (_: Exception) {
            // Ignore invalid country codes and fall back to extractor defaults.
        }
    }

    /**
     * YouTube no longer has a single trending feed. Merge items from the
     * category-specific trending kiosks NewPipeExtractor supports.
     */
    private fun getYoutubeTrending(service: StreamingService, countryCode: String?): List<VideoModel> {
        applyYoutubeTrendingCountry(service, countryCode)

        val seenIds = LinkedHashSet<String>()
        val videos = mutableListOf<VideoModel>()

        for (kioskUrl in YOUTUBE_TRENDING_KIOSK_URLS) {
            if (videos.size >= TRENDING_LIMIT) break
            try {
                val batch = fetchKioskVideos(
                    service,
                    kioskUrl,
                    "youtube",
                    TRENDING_LIMIT - videos.size
                )
                for (video in batch) {
                    if (seenIds.add(video.id)) {
                        videos.add(video)
                        if (videos.size >= TRENDING_LIMIT) break
                    }
                }
            } catch (e: Exception) {
                println("WARN: YouTube kiosk $kioskUrl failed - ${e.message}")
            }
        }

        println("DEBUG: getTrending(youtube) - ${videos.size} videos from ${YOUTUBE_TRENDING_KIOSK_URLS.size} kiosks")
        return videos
    }

    private fun fetchKioskVideos(
        service: StreamingService,
        kioskUrl: String,
        serviceName: String,
        limit: Int,
    ): List<VideoModel> {
        val kioskInfo = KioskInfo.getInfo(service, kioskUrl)
        return mapKioskItemsToVideos(kioskInfo.relatedItems, serviceName, limit)
    }

    private fun mapKioskItemsToVideos(
        items: List<InfoItem>,
        serviceName: String,
        limit: Int,
    ): List<VideoModel> {
        return items
            .filterNotNull()
            .take(limit)
            .mapNotNull { item ->
                if (item !is StreamInfoItem) return@mapNotNull null
                try {
                    VideoModel(
                        id           = extractId(item.url),
                        title        = item.name ?: "Unknown",
                        uploader     = item.uploaderName ?: "Unknown",
                        uploaderUrl  = item.uploaderUrl ?: "",
                        duration     = item.duration,
                        viewCount    = item.viewCount,
                        uploadDate   = item.textualUploadDate ?: "",
                        uploadDateTimestamp = item.uploadDate?.instant?.toEpochMilli(),
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url ?: "",
                        isLive       = item.streamType == StreamType.LIVE_STREAM || item.streamType == StreamType.AUDIO_LIVE_STREAM,
                        url          = item.url ?: "",
                        service      = serviceName
                    )
                } catch (_: Exception) {
                    null
                }
            }
    }

    // ─────────────────────────────────────────────────────────
    // COMMENTS (YouTube only — other services don't support it)
    // ─────────────────────────────────────────────────────────

    /**
     * Fetch comments for a YouTube video.
     * Returns empty list for non-YouTube URLs.
     *
     * @param url Full YouTube video URL
     */
    fun getComments(url: String): CommentsModel {
        val service = serviceFromUrl(url)

        // Only YouTube supports comments in NewPipeExtractor
        if (service.serviceId != 0) {
            return CommentsModel(videoId = extractId(url), comments = emptyList())
        }

        val commentsInfo = CommentsInfo.getInfo(service, url)

        val comments = commentsInfo.relatedItems
            .filterNotNull()
            .mapNotNull { comment ->
                try {
                    CommentModel(
                        id                  = comment.commentId,
                        author              = comment.uploaderName ?: "Anonymous",
                        authorAvatarUrl     = comment.uploaderAvatars.firstOrNull()?.url ?: "",
                        text                = comment.commentText?.content ?: "",
                        likeCount           = comment.likeCount,
                        isHeartedByUploader = comment.isHeartedByUploader,
                        isPinned            = comment.isPinned,
                        publishedDate       = comment.textualUploadDate ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

        return CommentsModel(
            videoId  = extractId(url),
            comments = comments,
            nextPage = commentsInfo.nextPage?.url
        )
    }
}
