# Verification

Run `npm run check` for syntax validation of the Express server and browser scripts.

## Manual checks for local development

- Confirm `GET /api/health` reports MongoDB connection status.
- Create an account, verify its trial dates, sign out, and sign back in.
- Sign in on `/phone` with a test account, confirm the device is automatically created, and sign in again to verify the same browser record is updated rather than duplicated.
- Grant location permission and verify the latest report, timestamp and accuracy on the dashboard map.
- Toggle Theft Mode and verify the paired open companion receives it and creates an alert after sound is enabled.
- Test owner isolation with two accounts; try the first account's device ID while authenticated as the second.
- Test offline phone, denied GPS, suspended tab, unavailable map tiles, expired device token and missing MongoDB.
- Inspect mobile layout, keyboard navigation, screen-reader labels and reduced motion.

## Automated suite status

No automated browser, API integration, security or mobile device test suite is included yet. A successful syntax check does not verify MongoDB transactions, UI behavior, external tile availability, BOLA prevention, or phone permission behavior.
