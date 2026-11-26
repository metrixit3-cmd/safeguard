# Architecture

The Express service serves the public site, authentication page, customer workspace, admin workspace and phone companion. MongoDB/Mongoose is the persistent store; process-memory mode is used when MongoDB is unavailable during local evaluation.

## Identity and routing

`POST /api/auth/register` creates a customer account and starts a 30-day trial. `POST /api/auth/login` verifies credentials and returns the stored/configured role (`customer` or `admin`). Passwords use bcrypt cost 12. The account JWT is an HttpOnly cookie: SameSite=Lax over local HTTP and SameSite=None; Secure on HTTPS. Customer sessions route to `/app.html`; admin sessions route to `/admin.html`. A protected bootstrap endpoint uses `ADMIN_EMAILS` plus `ADMIN_BOOTSTRAP_SECRET`; alternatively use trusted database provisioning to grant admin role. Admin pages and APIs enforce the role server-side.

## Device path

The phone companion signs in or creates an account over HTTPS, then pairs using account credentials. A browser-generated UUID is hashed and upserted by account so repeat sign-ins update that phone. The API returns a 30-day device-scoped JWT, stored in local storage; it is not an account token. The companion submits GPS every three seconds while active. MongoDB retains the latest coordinate and recent security events.

The dashboard uses MapLibre GL JS with OpenFreeMap dark, CARTO street and Esri imagery styles, a mapped building extrusion layer, and markers only for reported GPS coordinates. Coverage and building heights vary by area and depend on external tile providers.

## Admin operations

`GET /api/admin/overview` requires an admin session and returns account/device counts, paired devices, active Theft Mode records and device audit events. Command queue count is zero because command dispatch is not configured.

## Theft Mode and platform limits

The owner toggles persistent Theft Mode state. The phone companion checks the state every 2.5 seconds while open. It can show an in-page red overlay, vibrate where supported, and play generated browser audio after a user gesture. This is not an OS lock, native device-management action, push delivery or guaranteed background alert. Native SIM, Android Enterprise and Apple MDM services are not integrated.

