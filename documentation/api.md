# REST API

All routes are served by the same Express service. Dashboard and admin pages authenticate with the `sg_session` HttpOnly cookie. Phone routes use a 30-day, device-scoped JWT stored by the companion in that browser's local storage. Phone GPS sign-in requires HTTPS.

## Health

- `GET /api/health` — service status and storage mode (`connected` or `memory`).

## Authentication and roles

- `POST /api/auth/register` `{ fullName, email, password }` — creates a customer account, starts a 30-day trial, and sets a session cookie. Admin addresses configured in `ADMIN_EMAILS` are reserved from public signup.
- `POST /api/auth/register-admin` `{ fullName, email, password, bootstrapSecret }` — creates an admin account for an address in `ADMIN_EMAILS` when `ADMIN_BOOTSTRAP_SECRET` matches. Disable/remove the bootstrap secret after provisioning.
- `POST /api/auth/login` `{ email, password }` — verifies credentials, sets a session cookie, and returns `user.role` (`customer` or `admin`).
- `POST /api/auth/logout` — clears the session cookie.
- `GET /api/auth/me` — authenticated account role, trial dates and storage mode.
- `GET /api/auth/google` and `GET /api/auth/google/callback` — state/nonce-protected Google OAuth 2.0 authorization-code flow; configure `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `APP_BASE_URL`, and the exact callback URI in Google Cloud.
- `GET /api/admin/overview`, `/api/admin/telemetry`, `/api/admin/customers`, `/api/admin/devices`, `/api/admin/theft-cases`, `/api/admin/support-escalations`, `/api/admin/audit` — admin-only, paginated overview and operational data. Routine admin responses omit customer GPS coordinates and command payloads.
- `PATCH /api/admin/customers/:id/status` — suspend or reactivate an account and revoke its existing sessions.
- `POST /api/admin/customers/:id/password-reset` — emails a one-time reset link; requires SMTP configuration.
- `POST /api/admin/support-escalations` — creates a reason-bound, device-bound token with one-use scopes (`location` or `commands`) that expires in 15 minutes.
- `GET /api/admin/devices/:id/location` and `POST /api/admin/devices/:id/command` — require an admin session and a matching `X-Support-Escalation` token. Location and command access are written to the append-only admin audit collection.

`/admin.html` and admin API data require the `admin` role. Provision admins through the one-time bootstrap secret and email allowlist or trusted database provisioning; public signup cannot set a role. Customers land on `/overview`; `/app.html` remains an alias. Admins are routed to `/admin.html`.

## Account devices

- `GET /api/devices` — current owner's devices, latest location and recent events.
- `GET /api/dashboard` — device list, counts, trial and storage mode.
- `GET /api/events` — current owner's timestamped security events.
- `PATCH /api/devices/:id` `{ name, platform }` — edits an owned device. Supported platform values are `iPhone`, `iPad`, `Android`, and `Other`.
- `PATCH /api/devices/:id/theft-mode` `{ active: boolean }` — updates Theft Mode and logs an event.
- `PATCH /api/devices/:id/protection` `{ sirenEnabled: boolean }` or `{ locationSharingEnabled: boolean }` — updates companion preferences and logs an event.
- `POST /api/devices/:id/command` `{ action, enabled? }` — owner-only command dispatch for `locate_now`, `maximum_volume_siren`, `theft_mode`, `full_lockdown_anti_flash`, or `sim_tamper_alert`.
- `POST /api/devices/register-agent` `{ deviceId, platform, osVersion, fcmToken, enrollmentMode }` — owner-authenticated native-agent registration; `platform` is `Android` or `iOS`, and enrollment mode is `DEVICE_OWNER` or `STANDARD` (iOS uses `STANDARD`). Returns a device-scoped agent token.
- `GET /api/devices/:id/events` — event history for an owned device.
- `POST /api/devices/pair-account` `{ email, password, name, platform, clientKey }` — verifies credentials, links/upserts the phone record for the browser identifier, revokes its prior phone token and returns a scoped phone JWT. New devices start with Theft Mode off. The companion detects iPhone/iPad/Android from browser platform hints and user-agent data.

The phone identifier is a random UUID held by the browser and SHA-256 hashed in MongoDB. Repeated sign-in from the same browser and account updates the existing device record.

## Phone companion

- `GET /api/phone/state` — returns Theft Mode, siren/location preferences and linked-device identity. The phone polls this no-cache endpoint every two seconds while connected.
- `GET /api/phone/commands` and `POST /api/phone/commands/ack` — native-agent command polling and acknowledgement using the device-scoped bearer token.
- `POST /api/phone/location` `{ latitude, longitude, accuracy }` — validates and stores the latest browser GPS report. Disabled location sharing rejects reports. A location event is added at most once every 10 minutes.
- `POST /api/phone/unpair` — revokes the current phone token and records an unpair event.
- `GET /phone` — phone companion page, supporting account sign-in and account creation before pairing.

The phone token is separate from the account JWT and grants access only to its linked device. It expires after 30 days. The companion requests geolocation after sign-in and posts coordinates every three seconds while its page is active. Browser suspension and offline state can interrupt updates. Browser audio needs user interaction and cannot override device audio settings.

Passwords require at least 10 characters and at most 72 UTF-8 bytes. Login and phone-account sign-in requests have a 12-per-15-minute in-memory IP limit. POST/PATCH JSON requests are size-limited. CORS credentials are enabled only for the exact HTTPS request host (loopback HTTP for development) and explicitly configured origins documented in setup. API rate limiting is in-memory and must be coordinated or replaced before horizontally scaled deployment.


