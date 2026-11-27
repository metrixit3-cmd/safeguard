# Architecture

The Express service serves the public site, authentication page, customer workspace, admin workspace and phone companion. MongoDB/Mongoose is the persistent store; process-memory mode is used when MongoDB is unavailable during local evaluation.

## Identity and routing

`POST /api/auth/register` creates a customer account and starts a 30-day trial. `POST /api/auth/login` verifies credentials and returns the stored/configured role (`customer` or `admin`). Passwords use bcrypt cost 12. The account JWT is an HttpOnly cookie: SameSite=Lax over local HTTP and SameSite=None; Secure on HTTPS. Customer sessions land on `/overview` (`/app.html` remains an alias); admin sessions route to `/admin.html`. A protected bootstrap endpoint uses `ADMIN_EMAILS` plus `ADMIN_BOOTSTRAP_SECRET`; alternatively use trusted database provisioning to grant admin role. Admin pages and APIs enforce the role server-side.

## Device path

The phone companion signs in or creates an account over HTTPS, then pairs using account credentials. A browser-generated UUID is hashed and upserted by account so repeat sign-ins update that phone. The API returns a 30-day device-scoped JWT, stored in local storage; it is not an account token. The companion submits GPS every three seconds while active. MongoDB retains the latest coordinate and recent security events.

The dashboard uses MapLibre GL JS with OpenFreeMap Positron and building extrusions for its 3D Light style, Esri light gray canvas tiles for Street Map, and Esri World Imagery with transportation and place-label overlays for Satellite Hybrid. A pulsing marker shows the latest reported GPS coordinates or the documented default map center while waiting for the first report. Coverage and building heights vary by area and depend on external tile providers.

Google sign-in uses a server-side OAuth 2.0 authorization-code flow with `state` and `nonce` validation, verified Google identity claims, and a signed HttpOnly account cookie. Configure `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `APP_BASE_URL`; register the exact `${APP_BASE_URL}/api/auth/google/callback` redirect URI with Google.

## Admin operations

Admin APIs use paginated MongoDB queries and expose limited account/device metadata. Routine dashboards omit GPS coordinates and command payloads. `AdminAudit` records administrator requests as append-only records. A device-bound support escalation has a reason, a 15-minute lifetime, and separately scoped one-use authorization for location or command access. The service does not expose audit mutation routes, though deployment-level database permissions are needed to make the collection tamper-proof against database operators.

Customers can register a native agent for an owned device with `POST /api/devices/register-agent`. Device control updates are persisted and dispatched as high-priority FCM HTTP v1 data messages when the registered agent token and service-account credentials are configured. `/api/phone/commands` provides polling and acknowledgements. This repo does not contain the native Android/iOS app that consumes those messages, so FCM acceptance alone does not mean an OS action was performed.

## Theft Mode and platform limits

The owner toggles persistent Theft Mode state. New devices start with Theft Mode off. The phone companion checks the no-cache REST state endpoint every two seconds while open, allowing state synchronization through serverless requests. It can show an in-page red overlay, vibrate where supported, and play generated browser audio after a user gesture. This is not an OS lock, native device-management action, push delivery or guaranteed background alert. Native SIM, Android Enterprise and Apple MDM services are not integrated.

