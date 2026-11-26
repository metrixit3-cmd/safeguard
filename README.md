# SAFEGUARD

SAFEGUARD is an account-based phone companion for permissioned browser GPS, Theft Mode alerts, a 30-day account trial and timestamped security events.

## Run locally

Requires Node.js 20+. MongoDB is optional for local evaluation; without it, SAFEGUARD uses temporary process-memory storage. Set a random `JWT_SECRET` of at least 32 characters. For admin access, configure `ADMIN_EMAILS` plus a temporary random `ADMIN_BOOTSTRAP_SECRET`, provision the first admin at `/admin-setup.html`, then remove the bootstrap secret. Public signup cannot claim an admin role.

1. Install dependencies with `npm install`.
2. Optionally start MongoDB at `mongodb://127.0.0.1:27017/safeguard`.
3. Set `JWT_SECRET` and any admin provisioning variables.
4. From the project root, run `npm start` and keep that terminal open.
5. Open `http://127.0.0.1:4173` in the browser. Do not double-click `public/index.html` or open it as a `file://` URL; that bypasses Express, so API requests, authentication, and interactive maps cannot work.

In memory mode, users, devices, locations, settings and events are cleared when the server stops. `/api/health` reports the active storage mode.

## Connect a phone

Open `/phone` on a phone, sign in with an existing account, or create an account there and confirm the password. Pairing happens automatically. Browser location requires HTTPS and permission; updates are sent every three seconds while the phone companion is active.

## Included

- Express REST service, MongoDB persistence and memory fallback.
- bcrypt authentication, 30-day trial, customer/admin roles, a protected bootstrap flow and live admin overview.
- Account-password phone pairing with per-browser device deduplication.
- Browser GPS, persistent Theft Mode state, timestamped events and companion siren preference.
- MapLibre dark, street and satellite map styles with reported-location markers and building extrusions.
- Credentialed CORS for the exact same host over HTTPS (or loopback HTTP) and explicit `CORS_ALLOWED_ORIGINS` entries. Additional cross-origin frontends must be allowlisted.

Browser software cannot lock the Android/iOS operating system, block USB flashing or reset, detect SIM changes, track a powered-off phone, or bypass browser audio controls. Those functions require platform-approved native software and device management. External email/push delivery and paid billing are not configured.

See [setup](documentation/setup.md), [API](documentation/api.md), [security](documentation/security.md), [architecture](documentation/architecture.md), and [deployment](documentation/deployment.md).


