ChruPipe contribution guidelines
=================================

## AI policy

* Generative AI is welcome here as long as it's used responsibly. If you use AI to write a contribution, you are still fully responsible for it. Before opening a PR, make sure you
  * understand the project structure and where your change actually belongs,
  * understand every line of the generated code, not just that it compiles,
  * have actually run the relevant build (`./gradlew build` for backend, `pnpm run build` for frontend) and confirmed it passes, not just that an editor shows no errors,
  * have reviewed the diff yourself, including checking for anything overly broad (e.g. a fix that touches unrelated files, or a security-relevant change that needs a second look).
* AI is especially useful for diagnosing bugs, but make sure the actual root cause is fixed, not just the symptom. If AI proposes a fix, ask it to explain why the bug happens before applying anything.
* AI-generated documentation is welcome, but check it against the real behavior of the code before submitting. Outdated or wrong docs are worse than no docs.
* Don't paste an issue or PR description that AI generated without reading it yourself first. A vague or padded description makes review slower for everyone.

## Issue reporting / feature requests

* **Already reported?** Check [existing issues](../../issues) first.
* **Still relevant?** Confirm it reproduces on the latest `main`.
* **One issue per report.** If you're hitting multiple unrelated problems, open separate issues so each can be tracked and closed independently.
* **Include what you can:**
  * Steps to reproduce
  * What you expected vs what actually happened
  * Environment (Docker vs local dev, OS, browser if relevant)
  * Logs where relevant (`docker compose logs backend` / `docker compose logs frontend`), with secrets removed
* Feature requests are welcome. Large or vague requests may get split into smaller, separately trackable pieces so each can be reviewed and merged independently.

## Code contribution

### Guidelines

* Use descriptive names for variables, functions, and types. Avoid cryptic abbreviations.
* Match the existing structure and conventions of the file you're editing rather than introducing a new pattern.
* Keep PRs scoped to one issue or one fix. If you spot an unrelated bug while working on something else, open a separate issue for it instead of bundling it in.

### Before starting development

* If you want to work on an existing issue, leave a comment saying so first, this avoids duplicated effort.
* If there's no existing issue for what you want to change, open one first describing what you're planning. This gives a chance for feedback before you spend time on something that might need a different approach.

### Creating a Pull Request (PR)

* Branch off `dev` with a descriptive branch name. Never commit directly to `dev` or `main`.
* Open your PR **against `dev`**, not `main`. `main` only receives changes once they've been reviewed and tested on `dev`.
* **Test your code before submitting.** For backend changes, run `cd backend && ./gradlew build`. For frontend changes, run `cd frontend && pnpm install && pnpm run build`. Confirm both pass locally, don't rely solely on an editor's inline diagnostics.
* If your PR fixes an open issue, say so in the description, e.g. `Fixes #12`, so it closes automatically on merge.
* Keep commit messages descriptive. Prefixing with the type of change is encouraged where it fits:
  * `feat: add download pause and resume controls`
  * `fix: resolve shadowed thumbnailUrl in AddToPlaylistModal`
  * `chore: update .gitignore for improved file exclusions`
* Respond if changes are requested. A PR left unanswered for a long time may be closed.

## Building the app

### Backend (Kotlin / Ktor)

* Requires JDK 22.
* From `backend/`, run `./gradlew run` to start it on `http://localhost:8080`. First run downloads dependencies, this takes a couple of minutes.

### Frontend (React / Vite)

* Requires Node.js 22 LTS.
* This project uses `pnpm`. From `frontend/`, run `pnpm install` then `pnpm run dev` to start on `http://localhost:5173`. The dev server proxies `/api` requests to the backend automatically.

### Full stack with Docker

* From the project root, `docker compose up --build` runs backend, frontend, and nginx together. See the [README](./README.md) for ports, persistent data, and configuration details.

### API docs

* Once the backend is running, an interactive Swagger UI is available at `/docs`, useful for checking request/response shapes while developing.

## Communication

* Open an issue for bugs, feature requests, or questions about the project.
* If you're planning a larger change, open an issue first to discuss the approach before investing significant time.

Thank you.
