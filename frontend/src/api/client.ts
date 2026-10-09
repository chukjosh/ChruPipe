/**
 * client.ts
 *
 * All API calls to the ChruPipe Ktor backend.
 * Uses axios with a base URL of /api (proxied to localhost:8080 in dev,
 * and proxied by nginx in production Docker).
 *
 * Multi-service support: most YouTube-related functions accept an optional
 * `service` parameter (defaults to "youtube"). For non-YouTube content,
 * pass the full URL directly.
 */

import axios from 'axios'
import type {
  SearchModel, VideoModel, StreamModel, ChannelModel,
  CommentsModel, HistoryModel, WatchlistModel, PlaylistModel,
  PlaylistWithVideos, SubscriptionModel, DownloadModel, ServiceInfo
} from '../types'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

type JsonEnvelope<T> = {
  schemaVersion: number
  type: string
  data: T[]
}

const normalizePayload = <T>(type: string, payload: T[] | JsonEnvelope<T>) =>
  Array.isArray(payload) ? { schemaVersion: 1, type, data: payload } : payload

// ─────────────────────────────────────────────
// Services — list supported platforms
// ─────────────────────────────────────────────

export const servicesApi = {
  /** Get all supported streaming services for the service selector UI */
  getAll: () =>
    api.get<ServiceInfo[]>('/services').then(r => r.data),
}

// ─────────────────────────────────────────────
// Extraction endpoints (NewPipeExtractor)
// ─────────────────────────────────────────────

export const extractorApi = {
  /**
   * Search on any supported service.
   * @param q Search query
   * @param service Service ID (default: "youtube")
   */
  search: (q: string, service = 'youtube') =>
    api.get<SearchModel>('/search', { params: { q, service } }).then(r => r.data),

  /**
   * Get stream URLs for any content by its full URL.
   * Service is auto-detected from the URL on the backend.
   * @param url Full content URL (YouTube, SoundCloud, etc.)
   */
  getStream: (url: string) =>
    api.get<StreamModel>('/stream', { params: { url }, timeout: 120_000 }).then(r => r.data),

  /**
   * Get stream URLs for a YouTube video by ID (legacy shorthand).
   */
  getStreamById: (id: string) =>
    api.get<StreamModel>(`/stream/${id}`).then(r => r.data),

  /**
   * Get channel/artist info by full URL.
   */
  getChannel: (url: string) =>
    api.get<ChannelModel>('/channel', { params: { url } }).then(r => r.data),

  /**
   * Get YouTube channel by ID (legacy shorthand).
   */
  getChannelById: (id: string) =>
    api.get<ChannelModel>(`/channel/${id}`).then(r => r.data),

  /**
   * Get trending/featured content for a service.
   * @param service Service ID (default: "youtube")
   */
  getTrending: (service = 'youtube') =>
    api.get<VideoModel[]>('/trending', { params: { service } }).then(r => r.data),

  /**
   * Get comments for a video by full URL (YouTube only).
   */
  getComments: (url: string) =>
    api.get<CommentsModel>('/comments', { params: { url } }).then(r => r.data),
}

// ─────────────────────────────────────────────
// YouTube-specific helpers
// ─────────────────────────────────────────────

export const youtubeApi = {
  /** Get metadata for a YouTube video by ID */
  getVideo: (id: string) =>
    api.get<VideoModel>(`/video/${id}`).then(r => r.data),
}

// ─────────────────────────────────────────────
// History
// ─────────────────────────────────────────────

export const historyApi = {
  getAll: () =>
    api.get<HistoryModel[]>('/history').then(r => r.data),

  export: async () => {
    const response = await api.get<JsonEnvelope<HistoryModel>>('/history/export')
    return response.data.data
  },

  import: (payload: HistoryModel[] | JsonEnvelope<HistoryModel>) =>
    api.post<{ added: number; updated: number; alreadyExisted: number }>('/history/import', normalizePayload('history', payload), {
      headers: { 'Content-Type': 'application/json' },
    }).then(r => r.data),

  add: (data: {
    videoId: string; title: string; uploader: string
    thumbnailUrl: string; duration: number
    watchedSeconds?: number; service?: string; url?: string
  }) => api.post('/history', data),

  deleteOne: (id: number) =>
    api.delete(`/history/${id}`),

  clearAll: () =>
    api.delete('/history'),
}

// ─────────────────────────────────────────────
// Watchlist
// ─────────────────────────────────────────────

export const watchlistApi = {
  getAll: () =>
    api.get<WatchlistModel[]>('/watchlist').then(r => r.data),

  export: async () => {
    const response = await api.get<JsonEnvelope<WatchlistModel>>('/watchlist/export')
    return response.data.data
  },

  import: (payload: WatchlistModel[] | JsonEnvelope<WatchlistModel>) =>
    api.post<{ added: number; alreadyExisted: number }>('/watchlist/import', normalizePayload('watchlist', payload), {
      headers: { 'Content-Type': 'application/json' },
    }).then(r => r.data),

  add: (data: {
    videoId: string; title: string; uploader: string
    thumbnailUrl: string; duration: number; service?: string; url?: string
  }) => api.post('/watchlist', data),

  remove: (id: number) =>
    api.delete(`/watchlist/${id}`),
}

// ─────────────────────────────────────────────
// Playlists
// ─────────────────────────────────────────────

export const playlistApi = {
  getAll: () =>
    api.get<PlaylistModel[]>('/playlists').then(r => r.data),

  export: async () => {
    const response = await api.get<JsonEnvelope<PlaylistWithVideos>>('/playlists/export')
    return response.data.data
  },

  import: (payload: PlaylistWithVideos[] | JsonEnvelope<PlaylistWithVideos>) =>
    api.post<{ added: number; alreadyExisted: number }>('/playlists/import', normalizePayload('playlists', payload), {
      headers: { 'Content-Type': 'application/json' },
    }).then(r => r.data),

  getById: (id: number) =>
    api.get<PlaylistWithVideos>(`/playlists/${id}`).then(r => r.data),

  create: (data: { name: string; description?: string }) =>
    api.post<{ id: number }>('/playlists', data).then(r => r.data),

  delete: (id: number) =>
    api.delete(`/playlists/${id}`),

  addVideo: (playlistId: number, data: {
    videoId: string; title: string; uploader: string
    thumbnailUrl: string; duration: number; service?: string; url?: string
  }) => api.post(`/playlists/${playlistId}/videos`, data),

  removeVideo: (playlistId: number, videoItemId: number) =>
    api.delete(`/playlists/${playlistId}/videos/${videoItemId}`),
}

// ─────────────────────────────────────────────
// Subscriptions
// ─────────────────────────────────────────────

export const subscriptionApi = {
  getAll: () =>
    api.get<SubscriptionModel[]>('/subscriptions').then(r => r.data),

  export: async (format: 'json' | 'txt' = 'json') => {
    const response = await api.get<JsonEnvelope<SubscriptionModel> | string>(`/subscriptions/export`, {
      params: { format },
      responseType: format === 'txt' ? 'text' : 'json',
    })

    if (format === 'txt') return response.data as string
    return (response.data as JsonEnvelope<SubscriptionModel>).data as SubscriptionModel[]
  },

  import: (format: 'json' | 'txt', payload: string, overwrite = false) =>
    api.post<{ added: number; alreadySubscribed: number; updated: number; removed: number }>(`/subscriptions/import?format=${format}&overwrite=${overwrite}`, payload, {
      headers: {
        'Content-Type': format === 'json' ? 'application/json' : 'text/plain',
      },
    }).then(r => r.data),

  subscribe: (data: {
    channelId: string; channelName: string
    channelUrl: string; avatarUrl: string; service?: string
  }) => api.post('/subscriptions', data),

  unsubscribe: (id: number) =>
    api.delete(`/subscriptions/${id}`),

  unsubscribeMany: (ids: number[]) =>
    api.delete<{ deleted: number }>('/subscriptions', { data: { ids } }).then(r => r.data),
}

export const appDataApi = {
  exportAll: async () =>
    api.get<{ schemaVersion: number; subscriptions: SubscriptionModel[]; playlists: PlaylistWithVideos[]; history: HistoryModel[]; watchlist: WatchlistModel[] }>('/export').then(r => r.data),

  importAll: (payload: Record<string, unknown>) =>
    api.post('/import', payload, { headers: { 'Content-Type': 'application/json' } }).then(r => r.data),
}

// ─────────────────────────────────────────────
// Feed
// ─────────────────────────────────────────────

export const feedApi = {
  /** Latest videos from all subscribed channels across all services */
  getFeed: () =>
    api.get<VideoModel[]>('/feed').then(r => r.data),
}

// ─────────────────────────────────────────────
// Downloads
// ─────────────────────────────────────────────

export const downloadApi = {
  getAll: () =>
    api.get<DownloadModel[]>('/downloads').then(r => r.data),

  getById: (id: number) =>
    api.get<DownloadModel>(`/downloads/${id}`).then(r => r.data),

  start: (data: {
    videoId: string; title: string; uploader: string
    thumbnailUrl: string; streamUrl: string
    audioStreamUrl?: string; subtitleUrl?: string; subtitleLanguage?: string
    quality: string; isAudioOnly?: boolean; service?: string
  }) => api.post<{ id: number }>('/downloads', data).then(r => r.data),

  /** Re-queue a FAILED download using its stored stream URL */
  retry: (id: number) =>
    api.post<{ id: number }>(`/downloads/${id}/retry`).then(r => r.data),

  pause: (id: number) =>
    api.post(`/downloads/${id}/pause`),

  resume: (id: number) =>
    api.post<{ id: number }>(`/downloads/${id}/resume`).then(r => r.data),

  delete: (id: number) =>
    api.delete(`/downloads/${id}`),

  /** Direct URL to download the completed file to the user's device */
  getFileUrl: (id: number) => `/api/downloads/${id}/file`,
}

export const storageSettingsApi = {
  /** Get current storage paths */
  get: () => api.get<{
    downloadsDir: string
    dataDir: string
    trendingCountry?: string
  }>('/settings/storage').then(r => r.data),
  update: (payload: { downloadsDir?: string; dataDir?: string; trendingCountry?: string }) =>
    api.post('/settings/storage', payload),
};
