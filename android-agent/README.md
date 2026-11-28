# SAFEGUARD Android Agent (DPC)

Standalone Kotlin/Jetpack Compose Android companion for SAFEGUARD. This directory is a separate Gradle project; the website and its Node.js/Vercel files are unchanged.

## What is included

- Material 3 account sign-in, encrypted HTTP-only session-cookie storage, device pairing, and Firebase token registration.
- Device Admin consent flow and Device Owner detection/provisioning helper.
- FCM handling for SAFEGUARD server commands (`full_lockdown_anti_flash`, `maximum_volume_siren`, `sim_tamper_alert`, `live_gps_polling`, `locate_now`, `theft_mode`) and the uppercase aliases `THEFT_LOCK_ON/OFF`, `SIREN_ON/OFF`, and `LOCATE_NOW`.
- Device Owner-only policy restrictions, a verified-owner lockdown screen, foreground siren and foreground location services.
- SIM state receiver that can lock locally while armed.

This is an initial native DPC project, not a certified Android Enterprise product or a guarantee against every recovery path. The standard Device Admin grant permits screen lock only. USB transfer, debugging, factory-reset and safe-boot restrictions plus Lock Task/kiosk controls require the app to be provisioned as **Device Owner**. Android policy does not provide SAFEGUARD a way to stop physical destruction, all OEM recovery/bootloader paths, or removal of the device from the network. The owner verification screen is a deterrent/control surface, not a substitute for a platform-supported enterprise enrollment and recovery design.

## Requirements

- Android Studio with a JDK 17 runtime and Android SDK Platform 36.
- Gradle/Android Gradle Plugin 8.13.2 (Android Studio can sync the Kotlin DSL project).
- A Firebase project with Firebase Cloud Messaging enabled and an Android app registered with application ID `com.safeguard.agent`.
- A SAFEGUARD HTTPS deployment with persistent MongoDB, `JWT_SECRET`, `FIREBASE_PROJECT_ID`, and server-side `FIREBASE_SERVICE_ACCOUNT` configured. The service account stays on the server; never put it in this Android project.

## Configure Firebase and backend URL

1. In Firebase Console, add an Android app whose package name is exactly `com.safeguard.agent`.
2. Download that app's `google-services.json` and place it at `android-agent/app/google-services.json`. It is ignored by Git. The Google Services Gradle plugin is applied only when that file exists.
3. Confirm the server Firebase project is the same Firebase project and has permission to send FCM HTTP v1 messages to this app.
4. The default API URL is `https://safeguard-theta-five.vercel.app/`. For a different deployment, set `safeguardApiBaseUrl=https://YOUR_PROJECT.vercel.app/` in `android-agent/gradle.properties` or Android Studio's Gradle properties. Keep the trailing slash.
5. Open the `android-agent` directory in Android Studio and sync Gradle. Connect a physical Android phone, enable USB debugging for installation, then use **Run** or build with `:app:assembleDebug`. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

Do not commit `google-services.json`, signing keystores, passwords, service-account JSON, or production signing credentials. The server accepts a signed-in account cookie for pairing/registration and a scoped phone bearer JWT for agent calls. The app stores session cookies and the agent JWT using AndroidX `EncryptedSharedPreferences`; the SAFEGUARD password is not written to disk.

## Owner sign-in and pairing

1. Install and open the agent, then sign in with the SAFEGUARD account email/password.
2. The app calls `POST /api/auth/login`, `POST /api/devices/pair-account`, and `POST /api/devices/register-agent` in sequence. Device type is Android, the name defaults to the manufacturer/model, and OS version plus FCM registration token are sent to the current server contract.
3. Pairing uses a stable, app-scoped hash derived from Android's resettable `ANDROID_ID`; SAFEGUARD does not request IMEI, serial number, or phone number. Location access is an explicit permission and separate foreground-service opt-in.
4. The app receives an account cookie from `/api/auth/login`; registration endpoints currently authenticate with that cookie. The token returned by `/api/devices/register-agent` is then used as `Authorization: Bearer …` for phone-agent endpoints.

Google-only SAFEGUARD accounts currently have no password accepted by this native form; set a password through the existing website's account recovery flow before using email/password pairing. No Google password is collected by the Android app.

## Device Admin vs Device Owner

**Standard Device Admin:** On the app screen, tap **Activate Device Admin** and accept Android's system confirmation. This enables `lockNow()` only. Android does not grant app-level authority to disable debugging, block reset, or hold an un-dismissible kiosk screen from this consent dialog.

**Development Device Owner (ADB):** This is intended for a test phone/emulator that can be wiped. Install the APK first, make sure the device has no accounts and no existing device/profile owner, then use the copied command shown in the app:

```sh
adb shell dpm set-device-owner com.safeguard.agent/.SafeguardDeviceAdminReceiver
```

Device Owner is assigned as part of managed setup. It is not a toggle the app can grant itself after a normal consumer setup; a device may need to be factory-reset to enroll. For production fleets, configure Android Enterprise QR/zero-touch enrollment and the supported managed Google Play deployment before provisioning.

## Runtime permissions and behavior

- Grant foreground location and notification permissions in the agent. For continued sharing while the app is in the background, grant **Allow all the time** location access when Android offers it. The service posts to the existing `POST /api/phone/location` endpoint every 30 seconds and on `LOCATE_NOW` when the server command and the owner's location opt-in permit it.
- The service can only report a current location while the phone is powered, has location enabled, has permission, has a usable fix, network access, and SAFEGUARD's backend is reachable. FCM is best-effort; do not treat it as guaranteed delivery on force-stop, offline, powered-off, or restricted devices.
- Allow notification policy access in system settings if the owner wants the siren to temporarily request an interruption-filter override. Alarm stream volume is raised while the siren runs and restored when it stops. Android controls DND access and sound output; the app cannot promise audibility in every device state. Volume-down key handling applies only while the LockdownActivity is visible and the app has kiosk authority.
- FCM can wake the app for high-priority commands, subject to Firebase delivery and Android power/app restrictions. The app acknowledges commands through `/api/phone/commands/ack`.
- SIM-change detection depends on Android/version/OEM telephony broadcasts and `READ_PHONE_STATE`. It is not a reliable signal on every device (eSIM, dual-SIM, vendor changes, or permissions may affect it).

## Backend integration gaps to close

This agent intentionally targets the routes currently in `server.mjs` without changing them. Two requested routes are **not present** in the existing backend:

- `POST /api/devices/:id/sim-alert` — `SimStateReceiver` attempts this route and logs a useful warning on failure; the current server returns 404, so the SIM alert is local-only until this authenticated, device-token-protected endpoint is added to the backend.
- `POST /api/devices/:id/location` — the current supported endpoint is `POST /api/phone/location` with `{ latitude, longitude, accuracy }`; the agent uses that existing route.

To make SIM events visible to the customer dashboard, add an authenticated route to the server that validates the phone JWT's device ID/version, records a timestamped `sim_tamper_alert` event, and applies owner-configured escalation policy. This Android project does not invent an unauthenticated endpoint or alter the web backend.

## Validation status

The Kotlin/Gradle project files are provided, but this workspace does not have Android Studio, the Android SDK, Gradle, or a Java toolchain installed. Run Android Studio Gradle Sync and build the debug APK on a configured development machine before installing or distributing it. Test Device Owner behavior on a dedicated test device; provisioning can remove existing device management and may require a wipe.
