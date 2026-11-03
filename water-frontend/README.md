# Smart Water Billing - Frontend

React web app for the Smart Water Usage Monitoring and Billing backend (Spring Boot).
Built with Vite, React 18, React Router and Recharts. No UI framework, plain CSS.

## Run it

1. Start MySQL and the Spring Boot backend (port 8080).
2. In this folder:

```bash
npm install
cp .env.example .env     # on Windows: copy .env.example .env
npm run dev
```

Open http://localhost:5173 for the public home page, with links to resident registration and sign-in.
The dev server proxies `/api` to `http://localhost:8080`, so no CORS setup is needed.
If the backend runs elsewhere: `VITE_BACKEND_URL=http://host:port npm run dev`.

Sign in with the seeded admin (`admin@water.local` and your `ADMIN_PASSWORD`).

## Login with Google

1. In Google Cloud Console, open your OAuth Web client and add `http://localhost:5173` under
   **Authorized JavaScript origins**.
2. Put the same Client ID in `.env`: `VITE_GOOGLE_CLIENT_ID=...apps.googleusercontent.com`
3. Restart `npm run dev` after changing `.env`. The backend uses `GOOGLE_CLIENT_ID`; this workspace has
   the same development Client ID as a fallback in `src/main/resources/application.yml`. Set
   `GOOGLE_CLIENT_ID` in other environments and restart the backend after changing it.

First-time Google users are asked for their Apartment ID (the backend requires it to create the account).
The Google sign-in button remains visible when the client ID is missing and explains that an administrator
needs to configure it; Google authentication cannot complete until both client IDs are set.

## What is inside

**Resident**
- My usage: consumption chart for 7/30/90 days, totals, latest meter reading, latest bill
- My invoices, and a printable itemized invoice (Print / Save as PDF)
- Alerts: leak and high-usage warnings (bell in the top bar shows the unread count, banner on the dashboard)
- Profile: change name and password

**Admin**
- Overview with a setup checklist
- Apartments, Households (with meter numbers), Residents (link a resident to a flat)
- Water usage: readings chart and table, add a reading, bulk CSV upload with a row-by-row error report
- Tariff plans (rate slabs)
- Alert settings: usage limit, leak detection on/off, email on/off, and a "Run alert check" button
- Alerts inbox (same page as residents, shows alerts for the whole apartment)
- Billing cycles with tabs: bulk purchases, adjustments, cost distribution preview (chart),
  invoices; finalize and archive actions

The admin works on one apartment at a time; use the selector in the top bar.

## Project layout

```
src/
  api.js            fetch wrapper (JWT header, error handling)
  auth.jsx          login state (token in localStorage)
  adminContext.jsx  selected apartment for the admin
  ui.jsx            small shared components (Card, Modal, FormModal, Tabs, ...)
  pages/            Login, Register, Profile, InvoiceDetail
  pages/admin/      admin screens
  pages/resident/   resident screens
```

## Build for production

```bash
npm run build      # output in dist/
```

A production build is served from another origin than the API, so set `VITE_API_BASE` to the backend URL
before building and enable CORS in Spring Boot for that origin (or serve both behind one reverse proxy).

## AquaBot

AquaBot is an optional floating chat widget shared by the resident and admin workspaces. It supports general
water-management and platform questions only; it cannot access resident-specific data or admin analytics.

The frontend widget is hidden unless `VITE_AQUABOT_ENABLED=true`. The backend endpoint is separately
controlled by `AQUABOT_ENABLED` and both flags default to `false`. Select one backend provider with
`AQUABOT_PROVIDER` (`none`, `ollama`, or `gemini`). Configure its settings in the Spring Boot backend's
environment before enabling the endpoint. The frontend never needs a provider API key.

| Variable | Purpose | Default |
|---|---|---|
| `AQUABOT_ENABLED` | Register the authenticated chat endpoint | `false` |
| `AQUABOT_PROVIDER` | Select exactly one provider: `ollama`, `gemini`, or disabled | `none` |
| `VITE_AQUABOT_ENABLED` | Show the frontend widget | `false` |
| `GEMINI_API_KEY` | Gemini credential, read only by Spring Boot | unset |
| `AQUABOT_GEMINI_MODEL` | Gemini model identifier; must be supplied explicitly | unset |
| `GEMINI_API_BASE_URL` | Gemini API base URL | `https://generativelanguage.googleapis.com/v1beta` |
| `AQUABOT_GEMINI_CONNECT_TIMEOUT_MS` | Gemini connection timeout | `5000` |
| `AQUABOT_GEMINI_READ_TIMEOUT_MS` | Gemini response timeout | `60000` |
| `AQUABOT_GEMINI_MAX_OUTPUT_TOKENS` | Maximum Gemini response tokens | `400` |
| `OLLAMA_BASE_URL` | Ollama server URL | `http://localhost:11434` |
| `AQUABOT_OLLAMA_MODEL` | Installed model tag to use; must be supplied explicitly | unset |
| `AQUABOT_CONNECT_TIMEOUT_MS` | Ollama connection timeout | `2000` |
| `AQUABOT_READ_TIMEOUT_MS` | Ollama response timeout | `60000` |
| `AQUABOT_MAX_OUTPUT_TOKENS` | Maximum Ollama response tokens | `400` |

For Gemini, set `GEMINI_API_KEY` and `AQUABOT_GEMINI_MODEL` in the Spring Boot IntelliJ Run Configuration.
Never put the Gemini key in a React `.env` file, frontend source, or browser request. Select `gemini` and
enable `AQUABOT_ENABLED` only after configuring these backend values; restart Spring Boot after changing
them. Keep `VITE_AQUABOT_ENABLED=false` until the backend is ready.

For Ollama, install/start Ollama and pull a model separately; no model is selected or downloaded
automatically. Set `AQUABOT_OLLAMA_MODEL` and select `ollama` only when configured. Missing provider
configuration or service returns an explicit error rather than a fabricated answer.
