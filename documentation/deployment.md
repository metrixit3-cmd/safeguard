# Deployment

Use an HTTPS reverse proxy or managed hosting platform for TLS, production `NODE_ENV`, suitable proxy trust configuration, request monitoring and process lifecycle. Set a unique high-entropy `JWT_SECRET`, `ADMIN_EMAILS` and a temporary high-entropy `ADMIN_BOOTSTRAP_SECRET` only for controlled admin provisioning, and a managed `MONGODB_URI`; never commit credentials. Restrict MongoDB network access and encrypt backups.

Behind one trusted TLS-terminating proxy or Cloudflare Tunnel, set `TRUST_PROXY=1` so Express recognizes HTTPS and emits Secure; SameSite=None session cookies. Credentialed CORS accepts the exact HTTPS request host, loopback HTTP for local development, and exact additional origins configured through `CORS_ALLOWED_ORIGINS`. Avoid wildcard CORS for tunnel domains because arbitrary tunnel tenants could otherwise read credentialed account data.

The current process uses in-memory rate limits and one Node service. It has no durable command queue, outbound push/email provider, billing service, native phone client or production availability/backup monitoring. Configure CSP/provider domains deliberately if MapLibre or tile endpoints change. Review map provider terms, attribution, traffic policy and availability before commercial scale. Open map data and tiles are external services; map/building coverage varies.

Before accepting customer locations, complete authorization/IDOR testing, incident recovery and data-deletion workflows, accessibility and browser testing, monitoring, backup restoration checks and Kenyan privacy/legal review.

