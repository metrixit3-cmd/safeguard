# Deployment

## Vercel

Import the repository root as a Node.js project. `server.mjs` exports the Express app for Vercel Functions, while local `npm start` still starts an HTTP server. The authenticated customer and admin HTML is bundled as an imported private server module. Keep these pages out of `public/**`, which Vercel serves directly from its CDN.

Configure these Vercel environment variables for Production and Preview as needed:

- `MONGODB_URI`: a hosted MongoDB connection string, such as MongoDB Atlas. Configure Atlas network access so the function can connect.
- `JWT_SECRET`: a unique random secret of at least 32 characters.
- `ADMIN_EMAILS`: comma-separated administrator addresses.
- `ADMIN_BOOTSTRAP_SECRET`: a temporary high-entropy secret only while provisioning the first administrator; remove it afterward.
- `GOOGLE_CLIENT_ID`: optional Web OAuth client ID; configure the production website origin in Google Cloud Console.
- `SMTP_HOST`, `SMTP_USER`, `SMTP_PASS`, `APP_BASE_URL`: required for password-reset emails. `SMTP_PORT` defaults to `587`, `SMTP_SECURE` defaults to `false` (port 465 enables TLS), and `SMTP_FROM` defaults to `SMTP_USER`.

Production API routes require MongoDB. If it is missing or unreachable, they return a clear 503 response instead of creating volatile in-memory accounts. Check `/api/health` and require `{"status":"ok","database":"connected"}` before account/device use. Never set production `MONGODB_URI` to `127.0.0.1`.

## Render or another persistent Node host

Build command: `npm ci`. Start command: `npm start`. Bind the public listener to `0.0.0.0` and the platform-provided `PORT`; the server does this by default. Set the same database and secret environment variables above.

## Security and operations

Use HTTPS, suitable proxy trust configuration, request monitoring and process lifecycle management. Never commit credentials. Restrict MongoDB network access and encrypt backups. The Street Map layer uses OpenFreeMap vector tiles and 3D building extrusions; Satellite uses Esri World Imagery (native zoom 17). Both map surfaces share the same MapLibre implementation. Keep visible tile attribution and follow each provider's terms.

Behind one trusted TLS-terminating proxy or Cloudflare Tunnel, set `TRUST_PROXY=1` so Express recognizes HTTPS and emits Secure; SameSite=None session cookies. Credentialed CORS accepts the exact HTTPS request host, loopback HTTP for local development, and exact additional origins configured through `CORS_ALLOWED_ORIGINS`. Avoid wildcard CORS for tunnel domains because arbitrary tunnel tenants could otherwise read credentialed account data.

The current process uses in-memory rate limits and one Node service. It has no durable command queue, outbound push/email provider, billing service, native phone client or production availability/backup monitoring. Configure CSP/provider domains deliberately if MapLibre or tile endpoints change. Review map provider terms, attribution, traffic policy and availability before commercial scale. Open map data and tiles are external services; map/building coverage varies.

Before accepting customer locations, complete authorization/IDOR testing, incident recovery and data-deletion workflows, accessibility and browser testing, monitoring, backup restoration checks and Kenyan privacy/legal review.

