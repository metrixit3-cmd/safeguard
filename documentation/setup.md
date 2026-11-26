# Development setup

## Prerequisites

- Node.js 20 or newer.
- MongoDB at `mongodb://127.0.0.1:27017/safeguard` for persistent accounts and devices (optional for local evaluation).
- A random `JWT_SECRET` of at least 32 characters for a non-loopback host or production.

## Start

1. Run `npm install`.
2. Optionally start MongoDB.
3. Set `JWT_SECRET`; set `ADMIN_EMAILS` and a temporary `ADMIN_BOOTSTRAP_SECRET` only when provisioning an admin.
4. Run `npm start` from the project root and keep the server terminal open.
5. Browse to `http://127.0.0.1:4173`. Do not open `public/index.html` directly as a `file://` URL; the browser then bypasses Express and the API, sign-in, and interactive map will not work.

When MongoDB is unavailable, the server automatically uses process-memory storage for authentication, devices, settings and events. `GET /api/health` reports `database: memory`; all data is cleared when the process stops. If MongoDB comes online after memory-mode accounts were created, restart the server to switch storage modes safely.

## Administrator access

Set `ADMIN_EMAILS` to a comma-separated list of administrator addresses and set `ADMIN_BOOTSTRAP_SECRET` to a random secret of at least 32 characters. Create an admin with `POST /api/auth/register-admin` using that secret, then remove `ADMIN_BOOTSTRAP_SECRET` and restart the service. Normal public signup cannot claim a reserved admin email or request an admin role. Existing accounts may also be promoted through trusted database provisioning by setting `role: "admin"`.

## Tunnel and phone testing

Geolocation is restricted to secure contexts. A phone cannot use geolocation when it visits plain HTTP at a computer's LAN IP. Use an HTTPS Cloudflare Tunnel, localtunnel, or TLS-terminating reverse proxy. For a trusted single reverse proxy, set `TRUST_PROXY=1` so Express recognizes forwarded HTTPS. Same-host HTTPS login requests also let the app infer HTTPS from the browser Origin and set `SameSite=None; Secure`; local HTTP sessions use `SameSite=Lax`. Do not enable proxy trust when the server is directly exposed. `HOST=0.0.0.0` is only needed for a controlled private LAN/proxy setup; keep the endpoint private.

Credentialed CORS accepts the exact request host over HTTPS, plus exact origins listed in `CORS_ALLOWED_ORIGINS` (comma-separated). Local loopback hosts are also accepted over HTTP for development. A same-host trycloudflare.com or loca.lt tunnel works over HTTPS; a different frontend host must be explicitly listed. Responses echo the allowed requesting origin and never use wildcard credentials. Avoid wildcard tunnel origins because tunnel tenants are untrusted.

Use a test account/device and rotate the configured JWT secret after testing. Precise locations are saved in MongoDB or temporary process memory.





