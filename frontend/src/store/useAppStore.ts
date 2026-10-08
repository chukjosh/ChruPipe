/**
 * useAppStore.ts
 *
 * Global Zustand store for persistent user preferences and player state.
 * All values here are saved to localStorage automatically via the `persist`
 * middleware, so settings survive page refreshes and browser restarts.
 *
 * To access state in a component:
 *   const theme = useAppStore(s => s.theme)
 *
 * To update state:
 *   const toggleTheme = useAppStore(s => s.toggleTheme)
 */

import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AppState {
  // ─── Theme ──────────────────────────────────────
  theme: 'dark' | 'light'
  toggleTheme: () => void

  // ─── Player preferences ─────────────────────────
  volume: number
  setVolume: (volume: number) => void

  playbackRate: number
  setPlaybackRate: (rate: number) => void

  // Default quality preference (e.g. "720p", "1080p", "auto")
  preferredQuality: string
  setPreferredQuality: (quality: string) => void

  // Whether to show subtitles by default when available
  subtitlesEnabled: boolean
  setSubtitlesEnabled: (enabled: boolean) => void

  // Preferred subtitle language code (e.g. "en", "fr")
  preferredSubtitleLang: string
  setPreferredSubtitleLang: (lang: string) => void

  // ─── SponsorBlock ───────────────────────────────
  // Master toggle — if false, SponsorBlock is completely disabled
  sponsorBlockEnabled: boolean
  setSponsorBlockEnabled: (enabled: boolean) => void

  // Which segment categories to skip automatically
  // Full list: https://wiki.sponsor.ajay.app/w/Segment_Categories
  sponsorBlockCategories: SponsorCategory[]
  setSponsorBlockCategories: (cats: SponsorCategory[]) => void

  // ─── Picture in Picture ─────────────────────────
  // Whether PiP is currently active (used to show/hide mini-player UI)
  isPiPActive: boolean
  setIsPiPActive: (active: boolean) => void

  // ─── Background audio ────────────────────────────
  // When true the player switches to audio-only stream for background play
  backgroundAudioMode: boolean
  setBackgroundAudioMode: (enabled: boolean) => void

  // ─── Current video ──────────────────────────────
  currentVideoId: string | null
  setCurrentVideoId: (id: string | null) => void

  // ─── Recent searches ────────────────────────────
  recentSearches: string[]
  addRecentSearch: (query: string) => void
  removeRecentSearch: (query: string) => void
  clearRecentSearches: () => void
}

/** All SponsorBlock segment category IDs */
export type SponsorCategory =
  | 'sponsor'       // Paid promotion
  | 'selfpromo'     // Self-promotion / unpaid
  | 'interaction'   // Like/subscribe reminders
  | 'intro'         // Intro animation / title card
  | 'outro'         // Outro / credits
  | 'preview'       // Preview of upcoming content
  | 'music_offtopic'// Non-music section in a music video
  | 'filler'        // Filler / tangent

/** Human-readable labels for the UI */
export const SPONSOR_CATEGORY_LABELS: Record<SponsorCategory, string> = {
  sponsor:        'Sponsor segments',
  selfpromo:      'Self-promotion',
  interaction:    'Interaction reminders',
  intro:          'Intro / title card',
  outro:          'Outro / credits',
  preview:        'Preview of content',
  music_offtopic: 'Off-topic in music video',
  filler:         'Filler / tangent',
}

export const useAppStore = create<AppState>()(
  persist(
    (set) => ({
      // ─── Theme ──────────────────────────────────
      theme: 'dark',
      toggleTheme: () =>
        set((state) => ({ theme: state.theme === 'dark' ? 'light' : 'dark' })),

      // ─── Player preferences ─────────────────────
      volume: 1,
      setVolume: (volume) => set({ volume }),

      playbackRate: 1,
      setPlaybackRate: (rate) => set({ playbackRate: rate }),

      preferredQuality: '720p',
      setPreferredQuality: (quality) => {
        const normalized = quality.trim()
        set({
          preferredQuality: normalized.toLowerCase() === 'auto' ? 'auto' : normalized || 'auto',
        })
      },

      subtitlesEnabled: false,
      setSubtitlesEnabled: (enabled) => set({ subtitlesEnabled: enabled }),

      preferredSubtitleLang: 'en',
      setPreferredSubtitleLang: (lang) => set({ preferredSubtitleLang: lang }),

      // ─── SponsorBlock ───────────────────────────
      // Disabled by default — user must opt in from Settings
      sponsorBlockEnabled: false,
      setSponsorBlockEnabled: (enabled) => set({ sponsorBlockEnabled: enabled }),

      // Default: only skip paid sponsor segments
      sponsorBlockCategories: ['sponsor'],
      setSponsorBlockCategories: (cats) => set({ sponsorBlockCategories: cats }),

      // ─── PiP ────────────────────────────────────
      isPiPActive: false,
      setIsPiPActive: (active) => set({ isPiPActive: active }),

      // ─── Background audio ────────────────────────
      backgroundAudioMode: false,
      setBackgroundAudioMode: (enabled) => set({ backgroundAudioMode: enabled }),

      // ─── Current video ──────────────────────────
      currentVideoId: null,
      setCurrentVideoId: (id) => set({ currentVideoId: id }),

      // ─── Recent searches ─────────────────────────
      recentSearches: [],
      addRecentSearch: (query) =>
        set((state) => ({
          recentSearches: [
            query,
            ...state.recentSearches.filter((q) => q !== query),
          ].slice(0, 10), // keep last 10 searches
        })),
      removeRecentSearch: (query) =>
        set((state) => ({
          recentSearches: state.recentSearches.filter((q) => q !== query),
        })),
      clearRecentSearches: () => set({ recentSearches: [] }),
    }),
    {
      name: 'newpipe-web-store', // localStorage key
      // Only persist user preferences, not transient UI state
      partialize: (state) => ({
        theme:                   state.theme,
        volume:                  state.volume,
        playbackRate:            state.playbackRate,
        preferredQuality:        state.preferredQuality,
        subtitlesEnabled:        state.subtitlesEnabled,
        preferredSubtitleLang:   state.preferredSubtitleLang,
        sponsorBlockEnabled:     state.sponsorBlockEnabled,
        sponsorBlockCategories:  state.sponsorBlockCategories,
        backgroundAudioMode:     state.backgroundAudioMode,
        recentSearches:          state.recentSearches,
      }),
    }
  )
)
