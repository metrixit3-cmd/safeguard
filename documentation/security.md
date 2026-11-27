# Security and privacy

## Implemented

- bcrypt password hashes (cost 12); no plaintext password storage.
- 12-hour signed JWT in an HttpOnly session cookie. Local HTTP requests use SameSite=Lax; HTTPS requests recognized through a trusted proxy or matching same-host Origin use SameSite=None; Secure for tunnel access.
- Customer/admin role checks protect `/admin.html` and all `/api/admin/*` routes. Admin assignment is through a secret-protected bootstrap endpoint plus the `ADMIN_EMAILS` allowlist, or trusted database provisioning; public signup cannot submit a role.
- Separate 30-day device token scoped to one phone record; re-linking rotates prior phone tokens.
- Phone account credentials are verified before pairing the browser-generated device identifier; only a hash of that identifier is stored.
- Owner checks on customer device and event routes.
- Routine administrator overview, customer, device, and theft-case responses omit GPS coordinates and FCM tokens. Location reads and remote commands require a reason-bound, 15-minute, one-use support escalation token and are logged.
- Admin action audit records are append-only through the application API; no audit update/delete route exists.
- Credentialed CORS for the exact HTTPS request host (plus loopback HTTP for development). Other cross-origin hosts require exact `CORS_ALLOWED_ORIGINS` entries; arbitrary localhost ports and wildcard tunnel CORS are not trusted.
- CSP, request-size limits and login/API rate limits.

## Operational caveats

Admin customer/device views expose limited account contact and device metadata to admin-role accounts. Restrict those accounts, use HTTPS, keep `JWT_SECRET` confidential, protect MongoDB and backups, and define data retention/deletion controls before commercial use. Application-level append-only audit logging is not cryptographically tamper-proof against a database administrator with write privileges; deploy a restricted append-only database role or immutable backup/retention control for that guarantee. Set `TRUST_PROXY=1` only behind a trusted single reverse proxy so forwarded HTTPS is recognized.

There is no MFA/passkey, session listing, database field-level encryption, encrypted backup process, or external abuse monitoring. Password reset requires configured SMTP. Rate limiting and FCM delivery counters are process-local. Phone tokens can be revoked when the owner signs in again or unpairs the phone. Browser location can be spoofed by a modified client and is not independent proof of physical location. The repository includes the backend FCM bridge but not an Android/iOS agent; OS-level actions require an enrolled native client that validates and acknowledges commands.


