import type { StreamModel, StreamUrl } from '../types'

/** Build the watch page path for any service. */
export function watchPath(video: { id: string; url?: string }): string {
  if (video.url?.startsWith('http')) {
    return `/watch?url=${encodeURIComponent(video.url)}`
  }
  return `/watch/${video.id}`
}

/**
 * Thumbnails load fine cross-origin in <img> tags — no proxy needed.
 * Proxying every card thumbnail was overloading /api/proxy and causing 500s.
 */
export function thumbnailUrl(url: string): string {
  return url || ''
}

/** Route playable media through our backend proxy to avoid CDN CORS blocks. */
export function proxyMediaUrl(url: string, title?: string): string {
  if (!url || url.startsWith('/api/')) return url
  const safeTitle = title ? title.replace(/[^a-zA-Z0-9.-]/g, '_').substring(0, 100) : ''
  const titleParam = safeTitle ? `&title=${encodeURIComponent(safeTitle)}` : ''
  return `/api/proxy?url=${encodeURIComponent(url)}${titleParam}`
}

export function isHlsSource(url: string, format?: string): boolean {
  const f = format?.toUpperCase() ?? ''
  const u = url.toLowerCase()
  return f.includes('M3U8') || u.includes('.m3u8') || u.includes('application/vnd.apple.mpegurl')
}

export interface HlsLevelInfo {
  height: number
  levelIndex: number
  codecSet?: string
  videoCodec?: string
  audioCodec?: string
  supported?: boolean
}

export interface HlsQualityOption {
  height: number
  levelIndex: number
}

function resolutionHeight(quality: string): number | null {
  const match = quality.match(/(\d+)/)
  if (!match) return null
  const value = Number.parseInt(match[1], 10)
  return Number.isFinite(value) ? value : null
}

export function pickProgressiveFallback(
  stream: StreamModel,
  preferredQuality: string,
): StreamUrl | null {
  const progressiveVideo = stream.videoStreams.filter(s =>
    !s.isVideoOnly && !isHlsSource(s.url, s.format),
  )

  const normalizedPreferred = normalizePreferredQuality(preferredQuality)
  const preferredHeight = normalizedPreferred === 'auto'
    ? null
    : resolutionHeight(normalizedPreferred)

  if (preferredHeight !== null) {
    const exactMatch = progressiveVideo.find(s => resolutionHeight(s.quality) === preferredHeight)
    if (exactMatch) return exactMatch

    const lowerMatch = [...progressiveVideo]
      .sort((a, b) => (resolutionHeight(b.quality) ?? 0) - (resolutionHeight(a.quality) ?? 0))
      .find(s => (resolutionHeight(s.quality) ?? 0) < preferredHeight)
    if (lowerMatch) return lowerMatch
  }

  return progressiveVideo[0]
    ?? stream.audioStreams.find(s => !isHlsSource(s.url, s.format))
    ?? null
}

export function pickDownloadStreams(
  stream: StreamModel,
  preferredQuality: string,
): { video: StreamUrl | null; audio: StreamUrl | null } {
  const audioStreams = stream.audioStreams
    .filter(s => !isHlsSource(s.url, s.format))
    .sort((a, b) => (resolutionHeight(b.quality) ?? 0) - (resolutionHeight(a.quality) ?? 0))
  const candidates = stream.videoStreams
    .filter(s => !isHlsSource(s.url, s.format) && (!s.isVideoOnly || audioStreams.length > 0))
    .sort((a, b) => {
      const heightDifference = (resolutionHeight(b.quality) ?? 0) - (resolutionHeight(a.quality) ?? 0)
      return heightDifference || Number(a.isVideoOnly) - Number(b.isVideoOnly)
    })

  const normalizedPreferred = normalizePreferredQuality(preferredQuality)
  const preferredHeight = normalizedPreferred === 'auto'
    ? null
    : resolutionHeight(normalizedPreferred)
  const video = preferredHeight === null
    ? candidates[0] ?? null
    : candidates.find(s => resolutionHeight(s.quality) === preferredHeight)
      ?? candidates.find(s => (resolutionHeight(s.quality) ?? 0) < preferredHeight)
      ?? candidates[0]
      ?? null

  return {
    video,
    audio: video?.isVideoOnly ? audioStreams[0] ?? null : null,
  }
}

function hlsLevelIsPlayable(level: HlsLevelInfo): boolean {
  if (level.supported === false) return false

  const codecs = [level.videoCodec, level.audioCodec]
    .filter((codec): codec is string => Boolean(codec))
    .join(',') || level.codecSet || ''
  if (!codecs || typeof MediaSource === 'undefined' || !MediaSource.isTypeSupported) {
    return true
  }

  return ['video/mp4', 'video/mp2t'].some(type => {
    try {
      return MediaSource.isTypeSupported(`${type}; codecs="${codecs}"`)
    } catch {
      return false
    }
  })
}

/** Keep the first browser-playable HLS level for each resolution. */
export function getPlayableHlsQualities(levels: readonly HlsLevelInfo[]): HlsQualityOption[] {
  const bestByHeight = new Map<number, HlsLevelInfo>()

  levels.forEach(level => {
    if (level.height <= 0 || !hlsLevelIsPlayable(level)) return
    if (!bestByHeight.has(level.height)) bestByHeight.set(level.height, level)
  })

  return [...bestByHeight.values()]
    .sort((a, b) => a.height - b.height)
    .map(({ height, levelIndex }) => ({ height, levelIndex }))
}

export function normalizePreferredQuality(quality: string | null | undefined): string {
  if (!quality) return 'auto'
  const trimmed = quality.trim()
  return trimmed.toLowerCase() === 'auto' ? 'auto' : trimmed
}

export function pickPreferredHlsLevel(
  levels: readonly HlsLevelInfo[],
  preferredQuality: string,
): number {
  const playableLevels = getPlayableHlsQualities(levels)
  if (playableLevels.length === 0) return -1

  const normalizedPreferred = normalizePreferredQuality(preferredQuality)
  if (normalizedPreferred === 'auto') return -1

  const preferredHeight = Number.parseInt(normalizedPreferred.match(/\d+/)?.[0] ?? '', 10)
  if (!Number.isFinite(preferredHeight)) return playableLevels[0].levelIndex

  const exactMatch = playableLevels.find(level => level.height === preferredHeight)
  if (exactMatch) return exactMatch.levelIndex

  const lowerMatch = [...playableLevels]
    .reverse()
    .find(level => level.height < preferredHeight)
  if (lowerMatch) return lowerMatch.levelIndex

  return playableLevels[0].levelIndex
}

/**
 * Pick the best stream for HTML5 playback.
 * SoundCloud and similar services often expose audio-only streams.
 */
export function pickDefaultStream(
  stream: StreamModel,
  preferredQuality: string,
): { stream: StreamUrl | null; useHls: boolean; hlsUrl: string | null } {
  if (stream.service === 'youtube' && stream.hlsUrl) {
    return {
      stream: pickProgressiveFallback(stream, preferredQuality),
      useHls: true,
      hlsUrl: stream.hlsUrl,
    }
  }

  const video = pickProgressiveFallback(stream, preferredQuality)

  if (video) {
    return {
      stream: video,
      useHls: isHlsSource(video.url, video.format),
      hlsUrl: null,
    }
  }

  const audio = stream.audioStreams[0]
  if (audio) {
    return {
      stream: audio,
      useHls: isHlsSource(audio.url, audio.format),
      hlsUrl: null,
    }
  }

  if (stream.hlsUrl) {
    return { stream: null, useHls: true, hlsUrl: stream.hlsUrl }
  }

  return { stream: null, useHls: false, hlsUrl: null }
}
