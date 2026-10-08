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

export function pickProgressiveFallback(
  stream: StreamModel,
  preferredQuality: string,
): StreamUrl | null {
  const progressiveVideo = stream.videoStreams.filter(s =>
    !s.isVideoOnly && !isHlsSource(s.url, s.format),
  )
  const preferredVideo = progressiveVideo.find(s =>
    s.quality.startsWith(preferredQuality),
  )

  return preferredVideo ?? progressiveVideo[0]
    ?? stream.audioStreams.find(s => !isHlsSource(s.url, s.format))
    ?? null
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
