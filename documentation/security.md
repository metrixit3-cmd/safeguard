# Security and privacy

## Implemented

- bcrypt password hashes (cost 12); no plaintext password storage.
- 12-hour signed JWT in an HttpOnly session cookie. Local HTTP requests use SameSite=Lax; HTTPS requests recognized through a trusted proxy or matching same-host Origin use SameSite=None; Secure for tunnel access.
- Customer/admin role checks protect `/admin.html` and `/api/admin/overview`. Admin assignment is through a secret-protected bootstrap endpoint plus the `ADMIN_EMAILS` allowlist, or trusted database provisioning; public signup cannot submit a role.
- Separate 30-day device token scoped to one phone record; re-linking rotates prior phone tokens.
- Phone account credentials are verified before pairing the browser-generated device identifier; only a hash of that identifier is stored.
- Owner checks on customer device and event routes.
- Credentialed CORS for the exact HTTPS request host (plus loopback HTTP for development). Other cross-origin hosts require exact `CORS_ALLOWED_ORIGINS` entries; arbitrary localhost ports and wildcard tunnel CORS are not trusted.
- CSP, request-size limits and login/API rate limits.

## Operational caveats

Admin overview exposes customer contact details, device data and GPS reports to admin-role accounts. Restrict those accounts, use HTTPS, keep `JWT_SECRET` confidential, protect MongoDB and backups, and define data retention/deletion controls before commercial use. Set `TRUST_PROXY=1` only behind a trusted single reverse proxy so forwarded HTTPS is recognized.

There is no email verification, password reset/recovery, MFA/passkey, session listing/revocation, immutable audit log, database field-level encryption, encrypted backup process, or external abuse monitoring. Rate limiting is in-memory. Phone tokens can be revoked when the owner signs in again or unpairs the phone. Browser location can be spoofed by a modified client and is not independent proof of physical location.


