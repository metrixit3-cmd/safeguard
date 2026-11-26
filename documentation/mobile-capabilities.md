# Phone companion capabilities

## Implemented in the browser companion (`/phone`)

- Account sign-in to link or update a browser-specific device record.
- Browser `navigator.geolocation.watchPosition()` immediately after sign-in and user permission.
- Location reports every three seconds while the page is open and the browser permits activity.
- Theft Mode polling every 2.5 seconds while connected.
- In-page alert overlay, vibration when supported, and an oscillator siren after an explicit audio-enabling gesture.

GPS requires HTTPS (except browser localhost contexts). Mobile browsers may throttle or suspend pages and location callbacks in the background. Audio playback requires an initial user interaction and may be muted or limited by system settings. A signal can be delayed or missed when the phone is offline, the page closes, or the browser suspends it. The companion cannot lock the OS, prevent shutdown/reset/uninstall, or persist through a force stop.

## Not implemented

There is no React Native app, Android service/Kotlin module, iOS/Swift app, push notification/APNs/FCM integration, SIM/subscription detector, Android Enterprise integration, Apple MDM integration, or native remote action service. Those require platform-native implementations, declared permissions, enrollment, vendor configuration and physical device validation.
