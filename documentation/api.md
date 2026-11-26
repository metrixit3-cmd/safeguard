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
- `GET /api/admin/overview` — admin-only overview with customer/device counts, currently active Theft Mode devices, audit events and queue status.

`/admin.html` and admin API data require the `admin` role. Provision admins through the one-time bootstrap secret and email allowlist or trusted database provisioning; public signup cannot set a role. Customers are routed to `/app.html`; admins are routed to `/admin.html`.

## Account devices

- `GET /api/devices` — current owner's devices, latest location and recent events.
- `GET /api/dashboard` — device list, counts, trial and storage mode.
- `GET /api/events` — current owner's timestamped security events.
- `PATCH /api/devices/:id/theft-mode` `{ active: boolean }` — updates Theft Mode and logs an event.
- `PATCH /api/devices/:id/protection` `{ sirenEnabled: boolean }` or `{ locationSharingEnabled: boolean }` — updates companion preferences and logs an event.
- `GET /api/devices/:id/events` — event history for an owned device.
- `POST /api/devices/pair-account` `{ email, password, name, platform, clientKey }` — verifies credentials, links/upserts the phone record for the browser identifier, revokes its prior phone token and returns a scoped phone JWT.

The phone identifier is a random UUID held by the browser and SHA-256 hashed in MongoDB. Repeated sign-in from the same browser and account updates the existing device record.

## Phone companion

- `GET /api/phone/state` — returns Theft Mode, siren/location preferences and linked-device identity.
- `POST /api/phone/location` `{ latitude, longitude, accuracy }` — validates and stores the latest browser GPS report. Disabled location sharing rejects reports. A location event is added at most once every 10 minutes.
- `POST /api/phone/unpair` — revokes the current phone token and records an unpair event.
- `GET /phone` — phone companion page, supporting account sign-in and account creation before pairing.

The phone token is separate from the account JWT and grants access only to its linked device. It expires after 30 days. The companion requests geolocation after sign-in and posts coordinates every three seconds while its page is active. Browser suspension and offline state can interrupt updates. Browser audio needs user interaction and cannot override device audio settings.

Passwords require at least 10 characters and at most 72 UTF-8 bytes. Login and phone-account sign-in requests have a 12-per-15-minute in-memory IP limit. POST/PATCH JSON requests are size-limited. CORS credentials are enabled only for the exact HTTPS request host (loopback HTTP for development) and explicitly configured origins documented in setup. API rate limiting is in-memory and must be coordinated or replaced before horizontally scaled deployment.


