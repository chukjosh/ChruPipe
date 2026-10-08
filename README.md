# LocoStream

A full-featured, self-hosted frontend for YouTube, SoundCloud, PeerTube and more, built on top of [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor).

Licensed under [GPL-3.0](LICENSE) - the same license as NewPipeExtractor.
No Google account required. No ads. No tracking.

> **Note:** LocoStream is an independent project and is not affiliated with or endorsed by TeamNewPipe or NewPipe e.V.
> It uses [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) where applicable, and credits the upstream developers and contributors for their work.
>
> LocoStream exists to make the same private, ad-free viewing experience accessible from
> any device with a browser, desktop, mobile, or otherwise, rather than being tied to a
> single platform. This matters more as Google increasingly locks down what can be
> sideloaded and run on Android, a web-based, self-hosted frontend isn't subject to
> those restrictions the way a native Android app is.

---
## Branches

- `dev` - active development happens here. If you want to contribute, branch off `dev` and open your PR against `dev`.
- `main` - stable, released code. Only updated from `dev` once changes have been reviewed and tested.

## Contributing

1. Fork the repo and create your branch from `dev`.
2. Make your changes and open a pull request back into `dev`.
3. Once reviewed and tested, changes get merged from `dev` into `main`.

See the [CONTRIBUTING guide](./CONTRIBUTING.md) for setup, build checks, and code style before opening a PR.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Prerequisites](#prerequisites)
4. [Quick Start](#quick-start)
5. [Running in Development](#running-in-development)
6. [Running with Docker](#running-with-docker)
7. [Configuration](#configuration)
8. [Desktop App (Tauri)](#desktop-app-tauri)
9. [User Guide](#user-guide)
10. [Settings Reference](#settings-reference)
11. [SponsorBlock](#sponsorblock)
12. [API Reference](#api-reference)
13. [Project Structure](#project-structure)
14. [Troubleshooting](#troubleshooting)

---

## Features

| Feature | Description |
|---|---|
| 🔍 Search | Search YouTube, SoundCloud, PeerTube without an account |
| 📺 Watch | Play videos with quality selector (144p–2160p) |
| 🎵 Audio only | Switch to background audio mode - saves bandwidth |
| 🖼️ Picture in Picture | Float video in a small window while browsing |
| 💬 Comments | Read comments without signing in |
| 🔤 Subtitles | Select subtitle language; auto-generated subtitles supported |
| ⏩ Resume | Automatically resumes where you left off |
| 📡 Feed | Latest videos from subscribed channels |
| 🔔 Subscriptions | Subscribe to channels - stored locally, no Google account |
| 🔖 Watchlist | Save videos to watch later |
| 📚 Playlists | Create and manage local playlists |
| 🕐 History | Full watch history with resume positions |
| ⬇️ Downloads | Download video or audio to your server |
| 📤 Export / Import | Back up or transfer subscriptions, playlists, history and watchlist |
| ⏭️ SponsorBlock | Opt-in automatic sponsor segment skipping |
| 🌙 Dark / Light | Toggle between dark and light theme |
| 🐳 Docker | One command to run the full stack |
| 🖥️ Desktop | Wrap as a native desktop app via Tauri |

---

## Supported Services

| Service | Search | Watch | Channel | Trending | Comments |
|---|---|---|---|---|---|
| YouTube | ✅ | ✅ | ✅ | ✅ | ✅ |
| SoundCloud | ✅ | ✅ | ✅ | ✅ | ❌ |
| PeerTube | ✅ | ✅ | ✅ | ✅ | ❌ |
| Bandcamp | ✅ | ✅ | ✅ | ❌ | ❌ |
| media.ccc.de | ✅ | ✅ | ✅ | ❌ | ❌ |
| Odysee | ✅ | ✅ | ✅ | ❌ | ❌ |

Switch services using the dropdown in the search bar. The service selector only shows services relevant to the current context (e.g. the trending tab only shows services with a trending feed).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Extraction engine | [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) (Java/Kotlin) |
| Backend API | [Ktor](https://ktor.io) (Kotlin) |
| Database | SQLite via [Exposed ORM](https://github.com/JetBrains/Exposed) |
| Frontend | [React](https://react.dev) + [Vite](https://vitejs.dev) + [Tailwind CSS](https://tailwindcss.com) |
| State management | [Zustand](https://github.com/pmndrs/zustand) |
| Data fetching | [TanStack Query](https://tanstack.com/query) |
| Desktop wrapper | [Tauri 2](https://tauri.app) |
| Container | Docker + Docker Compose |

---

## Dependencies

### Backend (Kotlin / JVM)

**Build tool & Language:**
| Component | Version | Purpose |
|---|---|---|
| Kotlin | 2.0.0 | JVM language |
| JDK | 22 | Java runtime |
| Gradle | 8.14 | Build system |

**Core libraries:**
| Library | Version | Purpose |
|---|---|---|
| Ktor Server Core | 2.3.12 | HTTP server framework |
| Ktor Server Netty | 2.3.12 | Async network engine |
| Ktor Content Negotiation | 2.3.12 | JSON/serialization middleware |
| Ktor CORS | 2.3.12 | Cross-origin request handling |
| Ktor Call Logging | 2.3.12 | Request/response logging |
| Ktor Status Pages | 2.3.12 | Error handling |
| Ktor Client CIO | 2.3.12 | HTTP client for file streaming |
| Ktor Server Swagger | 2.3.12 | Interactive API docs (`/docs`) |

**Extraction & Parsing:**
| Library | Version | Purpose |
|---|---|---|
| NewPipeExtractor | 0.26.4 (JitPack) | YouTube/SoundCloud/PeerTube extractor |
| Kotlinx Serialization | 1.6.3 | JSON serialization |

**Database:**
| Library | Version | Purpose |
|---|---|---|
| Exposed Core | 0.50.1 | ORM framework |
| Exposed DAO | 0.50.1 | Data access objects |
| Exposed JDBC | 0.50.1 | JDBC driver integration |
| Exposed Java Time | 0.50.1 | DateTime support |
| SQLite JDBC | 3.45.3.0 | SQLite driver |

**Async & Utilities:**
| Library | Version | Purpose |
|---|---|---|
| Kotlinx Coroutines | 1.8.1 | Async/await support |
| Logback | 1.5.6 | Logging implementation |
| dotenv-kotlin | 6.4.1 | Environment variable loading |

**Media tooling:**
| Tool | Version | Purpose |
|---|---|---|
| FFmpeg | System package | Losslessly mux separate video and audio streams |

**Testing:**
| Library | Version | Purpose |
|---|---|---|
| Ktor Server Tests | 2.3.12 | Integration testing |
| Kotlin Test | 2.0.0 | Unit testing framework |

### Frontend (Node.js / TypeScript)

**Build & Runtime:**
| Tool | Version | Purpose |
|---|---|---|
| Node.js | 22 LTS | JavaScript runtime |
| TypeScript | 5.4.5 | Type-safe JavaScript |
| Vite | 5.3.1 | Dev server & build tool |
| React Router | 6.30.3 | Client-side routing |

**UI & Styling:**
| Library | Version | Purpose |
|---|---|---|
| React | 18.3.1 | Component framework |
| React DOM | 18.3.1 | React rendering engine |
| Tailwind CSS | 3.4.4 | Utility-first CSS |
| PostCSS | 8.4.38 | CSS preprocessor |
| Autoprefixer | 10.4.19 | CSS vendor prefixes |
| clsx | 2.1.1 | Conditional CSS classes |
| Lucide React | 0.383.0 | Icon library |

**State & Data:**
| Library | Version | Purpose |
|---|---|---|
| TanStack Query | 5.40.0 | Server state management |
| Zustand | 4.5.2 | Client state (preferences, UI) |
| Axios | 1.16.1 | HTTP client |

**Media:**
| Library | Version | Purpose |
|---|---|---|
| React Player | 2.16.0 | Video/audio playback |
| hls.js | 1.6.16 | HLS playback |

**Dev Tools:**
| Tool | Version | Purpose |
|---|---|---|
| Vite React Plugin | 4.3.1 | Vite React JSX support |
| ESLint | Latest | Code linting |

### Desktop (Tauri 2 / Rust)

**Frontend wrapper:**
| Component | Version | Purpose |
|---|---|---|
| Tauri | 2.0.0 | Desktop app framework |
| Tauri CLI | 2.0.0 | Build & dev tool |
| Tauri API | 2.0.0 | Rust/JavaScript bridge |
| Tauri Shell Plugin | 2.0.0 | Shell command execution |

**Serialization:**
| Library | Version | Purpose |
|---|---|---|
| serde | 1.0 | Rust serialization |
| serde_json | 1.0 | JSON support |

**Rust toolchain:**
| Tool | Version | Purpose |
|---|---|---|
| Rust | Latest (via rustup) | Systems language |
| Cargo | Latest (via rustup) | Rust package manager |

### DevOps & Containerization

| Tool | Version | Purpose |
|---|---|---|
| Docker | Latest | Container runtime |
| Docker Compose | Latest | Multi-container orchestration |
| nginx | Latest | Reverse proxy & static server |
| Alpine Linux | 3.x | Lightweight base image |

---

## Prerequisites

### For the web app (backend + frontend)
| Tool | Version | Download |
|---|---|---|
| JDK | 22 | [adoptium.net](https://adoptium.net) |
| Node.js | 22 LTS | [nodejs.org](https://nodejs.org) |
| FFmpeg | Latest | Required for downloads with separate video/audio streams |
| Git | Any | [git-scm.com](https://git-scm.com) |

### For Docker (optional but recommended)
| Tool | Download |
|---|---|
| Docker Desktop | [docker.com/products/docker-desktop](https://docker.com/products/docker-desktop) |

### For the desktop app (optional)
| Tool | Download |
|---|---|
| Rust toolchain | [rustup.rs](https://rustup.rs) |
| C++ build tools | Windows: [Visual C++ Build Tools](https://visualstudio.microsoft.com/visual-cpp-build-tools/) |
| Xcode CLT | macOS: `xcode-select --install` |
| WebKit dev libs | Linux: see [Desktop App](#desktop-app-tauri) section |

---

## Quick Start

```bash
# 1. Clone or unzip the project
cd LocoStream

# 2. Start everything with Docker
docker compose up --build

# 3. Open in browser
open http://localhost:3000
```

That's it. The backend runs on port 8080, frontend on port 3000.

---

## Running in Development

Running without Docker gives you hot-reload on both frontend and backend.

### 1. Start the backend

```bash
cd backend

# First run: Gradle will download all dependencies including NewPipeExtractor
./gradlew run

# On Windows:
gradlew.bat run
```

The backend starts on **http://localhost:8080**.

On first run Gradle downloads dependencies - this takes 1–3 minutes.
Subsequent starts are much faster.
Install FFmpeg and make it available on `PATH` for merged high-resolution downloads.
Set `FFMPEG_PATH` if the executable is installed at a non-standard location. Docker installs FFmpeg automatically.

### 2. Start the frontend

> **Note:** This project uses `pnpm` for faster installs, lower disk usage, and consistent dependency management. You may use `npm` or `yarn` if you prefer, but `pnpm` is the recommended package manager for this project.

Open a new terminal:

```bash
cd frontend
pnpm install      # first run only
pnpm run dev
```

The frontend starts on **http://localhost:5173**.

The Vite dev server proxies all `/api` requests to the Ktor backend automatically,
so you don't need to configure CORS or URLs manually.

### 3. Open the app

Navigate to **http://localhost:5173** in your browser.

---

## Running with Docker

Docker runs the full stack (backend + frontend + nginx) in containers.

```bash
# Build and start
docker compose up --build

# Run in background
docker compose up --build -d

# Stop
docker compose down

# View logs
docker compose logs -f backend
docker compose logs -f frontend
```

### Ports
| Service | Port |
|---|---|
| Frontend (nginx) | http://localhost:3000 |
| Backend (Ktor) | http://localhost:8080 |

### Persistent data
Docker mounts two local directories as volumes:

| Directory | Contents |
|---|---|
| `./data/` | SQLite database (history, playlists, subscriptions, etc.) |
| `./downloads/` | Downloaded video and audio files |

These persist across container restarts. Back them up to keep your data.

---

## Configuration

The backend reads configuration from environment variables. Copy `.env.example` to `.env`
in the project root and adjust as needed - `.env` is only read at container start,
so re-run `docker compose up -d` after changing it (a plain `restart` will not pick up changes).

| Variable | Required | Description |
|---|---|---|
| `ALLOWED_ORIGINS` | No | Comma-separated list of full origins (e.g. `https://my.fqdn.tld`) to trust for CORS, in addition to `localhost` and private LAN IPs (`10.x`, `172.16–31.x`, `192.168.x`), which are always allowed. Needed if you access the app through a reverse proxy on a public domain. |
| `DATA_DIR` | No | Directory for the SQLite database. Defaults to `/app/data` in Docker. |
| `DOWNLOADS_DIR` | No | Directory for downloaded files. Defaults to `/app/downloads` in Docker. |

**Example `.env`:**
```env
ALLOWED_ORIGINS=https://my.fqdn.tld
```

By default (no `.env` needed), the app only trusts `localhost` and private network IPs -
hostnames are never resolved for CORS checks, so a domain must be explicitly added via
`ALLOWED_ORIGINS` to avoid being rejected.

---

## Desktop App (Tauri)

The desktop app wraps the web frontend in a native window using Tauri.
It requires the Ktor backend to be running separately.

### Install Rust

```bash
# Install rustup (the Rust toolchain manager)
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Restart your terminal, then verify:
rustc --version
cargo --version
```

### Install system dependencies

**Windows:** Install [Visual C++ Build Tools](https://visualstudio.microsoft.com/visual-cpp-build-tools/)
and select "Desktop development with C++".

**macOS:**
```bash
xcode-select --install
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install libwebkit2gtk-4.1-dev libgtk-3-dev \
  libayatana-appindicator3-dev librsvg2-dev build-essential
```

### Run the desktop app

```bash
# Make sure the backend is already running:
cd backend && ./gradlew run

# In a new terminal, start the frontend dev server:
cd frontend && pnpm run dev

# In another terminal, start the desktop app:
cd desktop
pnpm install
pnpm run dev
```

A native window opens pointing at the frontend. Any changes to the frontend
hot-reload in the window automatically.

### Build a distributable

```bash
cd desktop
pnpm run build
```

Output is in `desktop/src-tauri/target/release/bundle/`.
- Windows: `.msi` installer
- macOS: `.dmg`
- Linux: `.AppImage` or `.deb`

---

## User Guide

### Browsing & Searching

**Home page** shows trending videos from YouTube.

**Search** - type in the search bar at the top and press Enter or click the 🔍 icon.
Results come from YouTube (and other supported services if the URL is recognized).

Recent searches are saved locally and shown as you type.

### Watching a Video

Click any video card to open the watch page.

**Quality selector** - below the player toolbar, click any resolution badge to switch.
Your preferred quality is applied automatically (set in Settings).

**Playback speed** - use the browser's native controls (right-click the video).
Your default speed is set in Settings.

**Resume** - if you've watched part of a video before, it automatically picks up
from where you left off.

### Subtitles

1. Click the **CC** button in the player toolbar to enable subtitles.
2. Language buttons appear below the quality selector.
3. Click a language to switch.
4. Set your preferred default language in **Settings → Player → Preferred subtitle language**.

Auto-generated subtitles are labeled `(auto)`.

### Picture in Picture

Click the **PiP** button in the player toolbar. The video pops out into a small
floating window managed by your browser. You can browse other pages while the
video keeps playing. Click the X on the PiP window or press PiP again to return
to full mode.

> PiP requires a browser that supports the Picture-in-Picture API
> (Chrome, Edge, Safari, Firefox 116+).

### Background Audio Mode

Click the **Audio only** button to switch the player to an audio-only stream.
This is useful for music or podcasts - it uses significantly less data and CPU
than video streams.

Click the button again to switch back to video.

### Subscriptions

On any **Channel** page, click **Subscribe**. The subscription is saved locally
in the database - no Google account needed.

The **Feed** page shows the latest videos from all your subscribed channels,
sorted by upload date.

The **Subscriptions** page lists all channels you've subscribed to.
Click a channel name to go to its page, or click **Unsubscribe** to remove it.
Use **Export** / **Import** on this page to back up your subscriptions as JSON
or as a plain text list of channel URLs (one per line), or to bring in a list
generated elsewhere.

### Watchlist

On any video's watch page, click the **Save** (bookmark) button to add it to
your watchlist. Click again to remove it.

View your watchlist from the left sidebar.

### Playlists

**Creating a playlist:**
1. Go to **Library** in the sidebar.
2. Click **New Playlist**.
3. Enter a name and optional description.
4. Click **Create**.

**Adding a video to a playlist:**
1. Open the video's watch page.
2. Click the **Playlist** button.
3. Select an existing playlist or create a new one on the spot.
4. A checkmark confirms the video was added.

**Viewing a playlist:**
1. Go to **Library**.
2. Click on any playlist card.
3. Videos are listed in order. Click any to watch.

### Watch History

The **History** page shows all videos you've watched, most recent first.
Each entry stores your watch position so you can resume later.

- Click any entry to resume watching.
- Click the trash icon to remove a single entry.
- Click **Clear All** to wipe the full history.
- You can also clear history from **Settings → Privacy**.

### Downloads

On any video's watch page, click **Download**. The download starts immediately
in the background on the server.

Go to the **Downloads** page to:
- See download progress (with a progress bar for active downloads)
- Download the completed file to your device (click the ↓ icon)
- Delete downloads

Downloaded files are stored in the `./downloads/` directory on the server.

> **Note:** The download uses the currently selected stream URL. If you want
> audio only, enable Background Audio Mode first, then click Download.

### Comments

On the watch page, click **Show Comments** to load and display the comments
section. Click again to collapse it.

Pinned comments are marked 📌. Comments hearted by the creator are marked ❤️.

### Export / Import

Subscriptions, playlists, history and watchlist can each be exported and imported from
their own page, or all at once from **Settings → Export / Import All**. See
[API Reference](#api-reference) for the underlying endpoints and formats.

---

## Settings Reference

Open **Settings** from the left sidebar (gear icon at the bottom).

### Appearance

| Setting | Description |
|---|---|
| Theme | Toggle between dark mode (default) and light mode |

### Player

| Setting | Default | Description |
|---|---|---|
| Preferred quality | 720p | Automatically selects this quality when opening a video |
| Default playback speed | 1x | Videos start at this speed |
| Show subtitles by default | Off | Auto-enable CC when available |
| Preferred subtitle language | en | ISO 639-1 code for your preferred subtitle language |

### SponsorBlock

| Setting | Default | Description |
|---|---|---|
| Enable SponsorBlock | Off | Master toggle - must be enabled for any skipping to happen |
| Sponsor segments | ✅ | Paid promotions integrated into the video |
| Self-promotion | - | Creator's own unpaid promotions |
| Interaction reminders | - | Like/subscribe/comment calls to action |
| Intro / title card | - | Opening animation or title card |
| Outro / credits | - | Closing credits or endscreen |
| Preview of content | - | Teaser for what's coming up |
| Off-topic (music) | - | Non-music sections in music videos |
| Filler / tangent | - | Filler content or tangents unrelated to the topic |

See [SponsorBlock categories](https://wiki.sponsor.ajay.app/w/Segment_Categories) for detailed descriptions.

### Privacy & Data

| Action | Description |
|---|---|
| Clear history | Deletes all watch history from the database |
| Clear recent searches | Clears the local search history shown in the search bar |

### Export / Import All

| Action | Description |
|---|---|
| Export All | Downloads a single JSON file with subscriptions, playlists, history and watchlist |
| Import All | Restores from a previously exported JSON file |

---

## SponsorBlock

SponsorBlock is a community-driven project that crowdsources timestamps for
skippable segments in YouTube videos.

### How it works in LocoStream

1. Go to **Settings** and enable **SponsorBlock**.
2. Choose which segment categories you want to skip.
3. When you watch a video, the app fetches segment timestamps from
   `sponsor.ajay.app` using only the video ID.
4. When your playback position enters a segment, it automatically skips to
   the end of that segment.
5. A brief toast notification shows what was skipped.
6. A counter in the player toolbar shows how many segments were skipped.

### Privacy

- Only the **video ID** is sent to the SponsorBlock API.
- Your watch history, identity, and IP address are not associated with any
  user account in SponsorBlock.
- SponsorBlock is **disabled by default**. You must explicitly enable it in Settings.
- If the SponsorBlock API is unreachable, video playback continues normally.

### Contributing

If you notice incorrect segments or want to contribute timestamps, install the
[SponsorBlock browser extension](https://sponsor.ajay.app) or use the
[Android app](https://github.com/nicholasgasior/SponsorBlock).

---

## API Reference

All endpoints are served by the Ktor backend on port 8080.
In Docker, the frontend nginx config proxies `/api/*` to the backend.

An interactive Swagger UI covering every endpoint below is available at **`/docs`**
once the backend is running.

### YouTube / Extraction endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/search?q={query}` | Search for videos |
| GET | `/video/{id}` | Get video metadata |
| GET | `/stream/{id}` | Get playable stream URLs + subtitles |
| GET | `/channel/{id}` | Get channel info + recent videos |
| GET | `/trending` | Get trending videos |
| GET | `/comments/{videoId}` | Get video comments |

### History

| Method | Path | Description |
|---|---|---|
| GET | `/history` | Get all watch history (newest first) |
| POST | `/history` | Add or update a history entry |
| DELETE | `/history` | Clear all history |
| DELETE | `/history/{id}` | Remove a single entry |
| GET | `/history/export` | Export history as JSON |
| POST | `/history/import` | Import history from JSON |

**POST /history body:**
```json
{
  "videoId": "dQw4w9WgXcQ",
  "title": "Video title",
  "uploader": "Channel name",
  "thumbnailUrl": "https://...",
  "duration": 212,
  "watchedSeconds": 95
}
```

### Watchlist

| Method | Path | Description |
|---|---|---|
| GET | `/watchlist` | Get watchlist |
| POST | `/watchlist` | Add a video |
| DELETE | `/watchlist/{id}` | Remove a video |
| GET | `/watchlist/export` | Export watchlist as JSON |
| POST | `/watchlist/import` | Import watchlist from JSON |

### Playlists

| Method | Path | Description |
|---|---|---|
| GET | `/playlists` | List all playlists |
| POST | `/playlists` | Create a playlist |
| GET | `/playlists/{id}` | Get playlist with all videos |
| DELETE | `/playlists/{id}` | Delete a playlist |
| POST | `/playlists/{id}/videos` | Add a video to a playlist |
| DELETE | `/playlists/{id}/videos/{videoItemId}` | Remove a video |
| GET | `/playlists/export` | Export all playlists as JSON |
| POST | `/playlists/import` | Import playlists from JSON |

### Subscriptions & Feed

| Method | Path | Description |
|---|---|---|
| GET | `/subscriptions` | List all subscriptions |
| POST | `/subscriptions` | Subscribe to a channel |
| DELETE | `/subscriptions/{id}` | Unsubscribe |
| GET | `/feed` | Latest videos from subscribed channels |
| GET | `/subscriptions/export?format=json\|txt` | Export subscriptions as JSON or a plain text list of channel URLs |
| POST | `/subscriptions/import` | Import subscriptions from JSON or plain text |

### Downloads

| Method | Path | Description |
|---|---|---|
| GET | `/downloads` | List all downloads |
| POST | `/downloads` | Start a new download |
| GET | `/downloads/{id}` | Get download status |
| DELETE | `/downloads/{id}` | Delete download record + file |
| GET | `/downloads/{id}/file` | Download the completed file |
| POST | `/downloads/{id}/pause` | Pause an in-progress download |
| POST | `/downloads/{id}/resume` | Resume a paused download |
| POST | `/downloads/{id}/retry` | Retry a failed download |

### Export / Import All

| Method | Path | Description |
|---|---|---|
| GET | `/export` | Export subscriptions, playlists, history and watchlist as a single JSON file |
| POST | `/import` | Import all categories from a combined JSON export |

### System

| Method | Path | Description |
|---|---|---|
| GET | `/health` | Health check |
| GET | `/info` | Backend/version info |
| GET | `/docs` | Interactive Swagger UI |

---

## Project Structure

```
LocoStream/
│
├── backend/                         Kotlin + Ktor API server
│   ├── build.gradle.kts             Dependencies (NewPipeExtractor, Exposed, etc.)
│   ├── Dockerfile
│   └── src/main/kotlin/com/newpipeweb/
│       ├── Application.kt           Entry point
│       ├── NewPipeDownloader.kt     HTTP adapter for NewPipeExtractor
│       ├── plugins/                 Ktor plugin configs (CORS, routing, etc.)
│       ├── routes/                  API route handlers
│       ├── services/
│       │   └── YouTubeService.kt    All NewPipeExtractor calls
│       ├── database/
│       │   ├── tables/              SQLite table definitions
│       │   └── repositories/       Database CRUD operations
│       └── models/                  Serializable data classes
│
├── frontend/                        React + Vite + Tailwind
│   ├── src/
│   │   ├── App.tsx                  Router + layout
│   │   ├── main.tsx                 Entry point
│   │   ├── api/
│   │   │   ├── client.ts            All backend API calls
│   │   │   └── sponsorblock.ts      SponsorBlock API client
│   │   ├── components/
│   │   │   ├── layout/              Navbar, Sidebar
│   │   │   ├── video/               VideoCard, VideoGrid
│   │   │   ├── playlist/            AddToPlaylistModal
│   │   │   └── common/              LoadingSpinner, ErrorMessage, EmptyState
│   │   ├── hooks/
│   │   │   ├── index.ts             All TanStack Query hooks
│   │   │   └── useSponsorBlock.ts   SponsorBlock segment hook
│   │   ├── pages/                   One file per route
│   │   ├── store/
│   │   │   └── useAppStore.ts       Zustand global state + user preferences
│   │   └── types/
│   │       └── index.ts             TypeScript interfaces
│   ├── Dockerfile
│   └── nginx.conf                   Serves frontend + proxies /api to backend
│
├── desktop/                         Tauri 2 desktop wrapper
│   └── src-tauri/
│       ├── tauri.conf.json          Window config, points at frontend
│       ├── Cargo.toml               Rust dependencies
│       └── src/main.rs              Minimal Tauri entry point
│
├── data/                            SQLite database (auto-created, git-ignored)
├── downloads/                       Downloaded files (auto-created, git-ignored)
├── docker-compose.yml
├── .env.example                     Template for backend environment variables
├── CONTRIBUTING.md
└── README.md
```

---

## Troubleshooting

### Backend won't start

**`Could not find com.github.TeamNewPipe:NewPipeExtractor`**
- Make sure JitPack is in your repositories in `build.gradle.kts`
- Run `./gradlew dependencies` to force a dependency refresh
- Check your internet connection - JitPack needs to build the library on first fetch

**`Port 8080 already in use`**
- Something else is running on 8080
- Change the port in `Application.kt`: `port = 8081`
- Update `vite.config.ts` proxy target to match

### Videos won't play

**Stream URLs expire** - NewPipeExtractor stream URLs are valid for roughly 6 hours.
If you leave a video paused for a long time and come back, you may need to refresh
the page to get fresh stream URLs.

**`No playable stream found`** - YouTube occasionally changes its internal API.
Check the [NewPipeExtractor releases](https://github.com/TeamNewPipe/NewPipeExtractor/releases)
for a newer version and update `build.gradle.kts`.

### SponsorBlock not working

- Make sure it's **enabled in Settings** (it's off by default)
- Check at least one segment category is selected
- Open browser DevTools → Network tab and look for requests to `sponsor.ajay.app`
- If requests return 404, the video has no community-submitted segments yet

### CORS / 403 errors on API requests

If requests to `/api/*` fail with `403` when accessing the app from anywhere other
than `localhost` or a private LAN IP (e.g. through a reverse proxy on a public domain),
add that origin to `ALLOWED_ORIGINS` in your `.env` file and restart the stack with
`docker compose up -d` (not a plain `restart`, which won't reread `.env`).
See [Configuration](#configuration).

### Docker issues

**Backend container exits immediately**
```bash
docker compose logs backend
```
Look for the error. A common cause is a JDK version mismatch in the Dockerfile.
Verify that the base image is set to `eclipse-temurin:22-jre-alpine`. If a different or unsupported JDK version is specified, update it to a supported version.

**`bind: address already in use`**
Change the host port in `docker-compose.yml`:
```yaml
ports:
  - "8081:8080"   # host:container
```

**`ERR_PNPM_IGNORED_BUILDS` during frontend build**
Make sure `pnpm-workspace.yaml` is copied into the Docker build context (it must appear
in the frontend Dockerfile's `COPY` step alongside `package.json` and `pnpm-lock.yaml`),
and that `package.json` has a pinned `packageManager` field so Corepack installs a
consistent pnpm version.

### Desktop app (Tauri)

**`cargo not found`** - Install Rust: `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`

**`WebKit2GTK not found`** (Linux) - Install system dependencies:
```bash
sudo apt install libwebkit2gtk-4.1-dev libgtk-3-dev librsvg2-dev
```

**White screen in desktop window** - Make sure both the backend (`./gradlew run`)
and the frontend dev server (`pnpm run dev`) are running before launching Tauri.

---

<div align="center">
  <p>⭐ Star this repo if you found it helpful!</p>
</div>
