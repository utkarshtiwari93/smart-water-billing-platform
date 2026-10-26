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

Open http://localhost:5173. The dev server proxies `/api` to `http://localhost:8080`, so no CORS setup is needed.
If the backend runs elsewhere: `VITE_BACKEND_URL=http://host:port npm run dev`.

Sign in with the seeded admin (`admin@water.local` and your `ADMIN_PASSWORD`).

## Login with Google

1. In Google Cloud Console, open your OAuth Web client and add `http://localhost:5173` under
   **Authorized JavaScript origins**.
2. Put the same Client ID in `.env`: `VITE_GOOGLE_CLIENT_ID=...apps.googleusercontent.com`
3. Restart `npm run dev`. The backend must have the same value in `GOOGLE_CLIENT_ID`.

First-time Google users are asked for their Apartment ID (the backend requires it to create the account).

## What is inside

**Resident**
- My usage: consumption chart for 7/30/90 days, totals, latest meter reading, latest bill
- My invoices, and a printable itemized invoice (Print / Save as PDF)
- Profile: change name and password

**Admin**
- Overview with a setup checklist
- Apartments, Households (with meter numbers), Residents (link a resident to a flat)
- Water usage: readings chart and table, add a reading, bulk CSV upload with a row-by-row error report
- Tariff plans (rate slabs)
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
