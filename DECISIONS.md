# Decisions log

Short record of key choices and why. Newest at the bottom.
Status: **Proposed** (waiting for owner OK) → **Accepted** / **Rejected** / **Superseded by #N**.

| # | Date | Decision | Why | Status |
|---|---|---|---|---|
| 1 | 2026-09-24 | Working name "FlightME". Own branding. No Flighty name, logo, assets or distinctive UI. | Legal and differentiation. | Accepted (from brief) |
| 2 | 2026-09-24 | Mock data first, for both the app (`MockFlightDataSource`) and the server (`MockProvider`) | Test all UI and alert flows with zero API spend. | Accepted 2026-09-25 |
| 3 | 2026-09-24 | AeroAPI key and calls are server-side only, behind Cloud Functions | Keys never ship in the APK. Lets us cache and share results across users. | Accepted (from brief) |
| 4 | 2026-09-24 | One shared `flights/{id}` doc per flight instance, with user tracks as references | One fetch and one AeroAPI alert per flight, however many users track it. | Accepted 2026-09-25 |
| 5 | 2026-09-24 | Push (AeroAPI alerts → webhook) over polling. Small scheduled safety poll only. Live position only while the map is open, cached 60 s and shared. | Main lever on per-flight cost. | Accepted 2026-09-25 |
| 6 | 2026-09-24 | All provider calls go through `meteredCall` (usage logging + daily budget cap + kill switch) | Cost visibility from day one. Stops runaway spend. | Accepted 2026-09-25 |
| 7 | 2026-09-24 | Room is what the UI reads, and Firestore syncs into Room | Offline mode (in the air) comes for free. | Accepted 2026-09-25 |
| 8 | 2026-09-24 | FCM data messages (not notification messages). The app builds notifications. | One payload updates notifications, Live Updates, widgets and the cache. | Accepted 2026-09-25 |
| 9 | 2026-09-24 | Alert types defined as a data catalogue (`alertTypes.ts`) | Grows to 65+ types without 65 code paths. Free/Pro tier per type. | Accepted 2026-09-25 |
| 10 | 2026-09-24 | Cloud Functions 2nd gen in TypeScript, region europe-west2 | Best-supported Firebase path. UK users. | Accepted 2026-09-25 |
| 11 | 2026-09-24 | Light multi-module Android layout (app + core/* + widget + wear) | Shared code for Wear and widgets without "Now in Android"-level complexity. | Accepted 2026-09-25 |
| 12 | 2026-09-24 | MapLibre + OpenFreeMap tiles (OSM attribution shown) | Free, no API key, commercial use OK with attribution. | Accepted 2026-09-25 |
| 13 | 2026-09-24 | OpenSky not used | Free data isn't licensed for commercial use. | Accepted (from brief) |
| 14 | 2026-09-24 | Email import via a per-user forwarding address, not the Gmail API | Gmail restricted scopes need a paid CASA assessment. | Accepted (from brief) |
| 15 | 2026-09-24 | TripIt via the user's private iCal feed, not the TripIt API | The partner API may not accept new apps. iCal needs no approval. | Accepted 2026-09-25 |
| 16 | 2026-09-24 | Entitlements decided server-side (Play Developer API + RTDN), and the app only reads them | Stops client-side tampering. One source of truth across devices. | Accepted 2026-09-25 |
| 17 | 2026-09-24 | Min SDK 26, target SDK 36. Live Updates on API 36+, ongoing-notification fallback below that. | Wide device coverage. Live Updates need Android 16. | Accepted 2026-09-25 |
| 18 | 2026-09-24 | Stay on Firebase Spark + emulators until real data is needed. Blaze only after explicit OK, with budget alerts. | No surprise bills. | Accepted 2026-09-25 |
| 19 | 2026-09-25 | Brand direction (provisional): minimal, simple line style. Thin-stroke icons, lots of whitespace, restrained colour used mainly for status (on time / delayed / cancelled). | Owner preference. The theme is kept in `core/designsystem` so it's easy to change later. | Provisional |
| 20 | 2026-09-25 | Trip pass is a one-off 7-day pass, not a weekly auto-renewing subscription | Users dislike forgotten weekly renewals. | Accepted |
| 21 | 2026-09-25 | New Firebase projects: `flightme-dev` now, `flightme-prod` later | Keeps test data and real users apart. | Accepted |
| 22 | 2026-09-25 | Development runs locally (Windows, PowerShell, Claude Code + Android Studio). First test phone: Samsung Galaxy S21 Ultra. | The Android SDK and emulators are only available locally. | Accepted |
| 23 | 2026-09-25 | Live Updates are tested on an Android 16 (API 36) emulator. The S21 Ultra tests the fallback ongoing notification. | S21-series phones stop at Android 15 / One UI 7, so they can't show Android 16 Live Updates. The Now Bar needs a newer Samsung. | Accepted |
| 24 | 2026-09-25 | Google Play developer account: create one only when we reach Play testing (v1) | No account yet. It costs a one-off fee, which needs owner approval at the time. New personal accounts must run a closed test (about 12 testers for 14 days) before production, so plan time for that. | Accepted |
