# Decisions log

Short record of key choices and why. Newest at the bottom.
Status: **Proposed** (waiting for owner OK) → **Accepted** / **Rejected** / **Superseded by #N**.

| # | Date | Decision | Why | Status |
|---|---|---|---|---|
| 1 | 2026-09-24 | Working name "FlightME". Own branding. No Flighty name, logo, assets or distinctive UI. | Legal and differentiation. | Accepted (from brief) |
| 2 | 2026-09-24 | Mock data first, for both the app (`MockFlightDataSource`) and the server (`MockProvider`) | Test all UI and alert flows with zero API spend. | Proposed |
| 3 | 2026-09-24 | AeroAPI key and calls are server-side only, behind Cloud Functions | Keys never ship in the APK. Lets us cache and share results across users. | Accepted (from brief) |
| 4 | 2026-09-24 | One shared `flights/{id}` doc per flight instance, with user tracks as references | One fetch and one AeroAPI alert per flight, however many users track it. | Proposed |
| 5 | 2026-09-24 | Push (AeroAPI alerts → webhook) over polling. Small scheduled safety poll only. Live position only while the map is open, cached 60 s and shared. | Main lever on per-flight cost. | Proposed |
| 6 | 2026-09-24 | All provider calls go through `meteredCall` (usage logging + daily budget cap + kill switch) | Cost visibility from day one. Stops runaway spend. | Proposed |
| 7 | 2026-09-24 | Room is what the UI reads, and Firestore syncs into Room | Offline mode (in the air) comes for free. | Proposed |
| 8 | 2026-09-24 | FCM data messages (not notification messages). The app builds notifications. | One payload updates notifications, Live Updates, widgets and the cache. | Proposed |
| 9 | 2026-09-24 | Alert types defined as a data catalogue (`alertTypes.ts`) | Grows to 65+ types without 65 code paths. Free/Pro tier per type. | Proposed |
| 10 | 2026-09-24 | Cloud Functions 2nd gen in TypeScript, region europe-west2 | Best-supported Firebase path. UK users. | Proposed (Q5) |
| 11 | 2026-09-24 | Light multi-module Android layout (app + core/* + widget + wear) | Shared code for Wear and widgets without "Now in Android"-level complexity. | Proposed (Q6) |
| 12 | 2026-09-24 | MapLibre + OpenFreeMap tiles (OSM attribution shown) | Free, no API key, commercial use OK with attribution. | Proposed (Q7) |
| 13 | 2026-09-24 | OpenSky not used | Free data isn't licensed for commercial use. | Accepted (from brief) |
| 14 | 2026-09-24 | Email import via a per-user forwarding address, not the Gmail API | Gmail restricted scopes need a paid CASA assessment. | Accepted (from brief) |
| 15 | 2026-09-24 | TripIt via the user's private iCal feed, not the TripIt API | The partner API may not accept new apps. iCal needs no approval. | Proposed |
| 16 | 2026-09-24 | Entitlements decided server-side (Play Developer API + RTDN), and the app only reads them | Stops client-side tampering. One source of truth across devices. | Proposed |
| 17 | 2026-09-24 | Min SDK 26, target SDK 36. Live Updates on API 36+, ongoing-notification fallback below that. | Wide device coverage. Live Updates need Android 16. | Proposed (Q9) |
| 18 | 2026-09-24 | Stay on Firebase Spark + emulators until real data is needed. Blaze only after explicit OK, with budget alerts. | No surprise bills. | Proposed |
