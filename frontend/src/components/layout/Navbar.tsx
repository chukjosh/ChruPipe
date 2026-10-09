/**
 * Navbar.tsx
 *
 * Top navigation bar containing:
 * - App logo / home button
 * - Service selector (YouTube, SoundCloud, PeerTube, etc.)
 * - Search input
 * - Downloads shortcut
 * - Theme toggle
 *
 * The selected service is stored in the URL as ?service= so it persists
 * across navigation and can be bookmarked.
 */

import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Search, Sun, Moon, Download, Menu, Clock3, X } from 'lucide-react'
import { useAppStore } from '../../store/useAppStore'
import ServiceSelector from '../common/ServiceSelector'

interface NavbarProps {
  onToggleSidebar: () => void
}

export default function Navbar({ onToggleSidebar }: NavbarProps) {
  const [query, setQuery]         = useState('')
  const navigate                  = useNavigate()
  const [searchParams]            = useSearchParams()
  const {
    theme,
    toggleTheme,
    addRecentSearch,
    recentSearches,
    removeRecentSearch,
  } = useAppStore()
  const [showSearchHistory, setShowSearchHistory] = useState(false)

  // Preserve the currently selected service across searches
  const [service, setService] = useState(
    searchParams.get('service') ?? 'youtube'
  )

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (!query.trim()) return
    submitSearch(query.trim())
  }

  const submitSearch = (searchQuery: string) => {
    addRecentSearch(searchQuery)
    setQuery(searchQuery)
    setShowSearchHistory(false)
    navigate(`/search?q=${encodeURIComponent(searchQuery)}&service=${service}`)
  }

  const matchingSearches = recentSearches.filter(search =>
    !query.trim() || search.toLowerCase().includes(query.trim().toLowerCase())
  )

  return (
    <nav className="bg-neutral-900 border-b border-neutral-800 z-50 shrink-0
                   px-3 py-2 md:px-4 md:py-0 md:h-14">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:gap-3">
        <div className="flex items-center justify-between gap-2 md:justify-start">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onToggleSidebar}
              className="md:hidden p-2 rounded-lg hover:bg-neutral-800 transition-colors"
              aria-label="Toggle sidebar"
              title="Open menu"
            >
              <Menu size={20} className="text-neutral-300" />
            </button>

            {/* Logo / home button */}
            <button
              onClick={() => navigate('/')}
              className="flex items-center gap-2 shrink-0"
              aria-label="Go home"
            >
              <div className="w-8 h-8 bg-red-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-sm">CP</span>
              </div>
              <span className="font-bold text-lg hidden sm:block">ChruPipe</span>
            </button>
          </div>

          {/* Right actions */}
          <div className="flex items-center gap-1 shrink-0 md:ml-auto">
            <button
              onClick={() => navigate('/downloads')}
              className="p-2 hover:bg-neutral-800 rounded-lg transition-colors"
              title="Downloads"
              aria-label="Go to downloads"
            >
              <Download size={20} className="text-neutral-400" />
            </button>

            <button
              onClick={toggleTheme}
              className="p-2 hover:bg-neutral-800 rounded-lg transition-colors"
              title="Toggle theme"
              aria-label="Toggle dark/light mode"
            >
              {theme === 'dark'
                ? <Sun  size={20} className="text-neutral-400" />
                : <Moon size={20} className="text-neutral-400" />
              }
            </button>
          </div>
        </div>

        {/* Service selector + search bar */}
        <form onSubmit={handleSearch} className="flex w-full gap-2 md:flex-1 md:max-w-2xl md:mx-auto">
          <div className="w-28 sm:w-32 md:w-auto md:min-w-[140px] shrink-0">
            <ServiceSelector value={service} onChange={setService} className="h-10 md:h-auto" />
          </div>

          <div className="relative flex-1 min-w-0">
            <input
              type="text"
              value={query}
              onChange={e => {
                setQuery(e.target.value)
                setShowSearchHistory(true)
              }}
              onFocus={() => setShowSearchHistory(true)}
              onBlur={() => setTimeout(() => setShowSearchHistory(false), 150)}
              placeholder={`Search ${service === 'youtube' ? 'YouTube' : service}...`}
              className="input pr-12 h-10 md:h-auto"
              aria-label="Search"
            />
            {showSearchHistory && matchingSearches.length > 0 && (
              <div className="absolute left-0 right-0 top-full mt-2 z-50 bg-neutral-900 border border-neutral-700 rounded-lg shadow-xl overflow-hidden">
                <p className="px-3 py-2 text-xs text-neutral-500 uppercase tracking-wide">
                  Recent searches
                </p>
                {matchingSearches.map(search => (
                  <div key={search} className="flex items-center gap-2 px-3 py-2 hover:bg-neutral-800">
                    <button
                      type="button"
                      onMouseDown={event => event.preventDefault()}
                      onClick={() => submitSearch(search)}
                      className="flex items-center gap-2 min-w-0 flex-1 text-left text-sm text-neutral-300 hover:text-primary"
                    >
                      <Clock3 size={14} className="shrink-0 text-neutral-500" />
                      <span className="truncate">{search}</span>
                    </button>
                    <button
                      type="button"
                      onMouseDown={event => event.preventDefault()}
                      onClick={() => removeRecentSearch(search)}
                      className="p-1 text-neutral-500 hover:text-primary rounded"
                      aria-label={`Remove ${search} from recent searches`}
                      title="Remove search"
                    >
                      <X size={14} />
                    </button>
                  </div>
                ))}
              </div>
            )}
            <button
              type="submit"
              className="absolute right-2 top-1/2 -translate-y-1/2
                         p-1.5 hover:bg-neutral-700 rounded-lg transition-colors"
              aria-label="Submit search"
            >
              <Search size={18} className="text-neutral-400" />
            </button>
          </div>
        </form>
      </div>
    </nav>
  )
}
