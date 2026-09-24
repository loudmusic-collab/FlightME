# FlightME: build plan

> Status: **PROPOSAL, waiting for your OK.** No code has been written yet.
> "FlightME" is a placeholder name. Nothing here uses Flighty's name, logo, assets or distinctive UI.

This document covers:

1. Guiding principles
2. System architecture (overview diagram)
3. Android project structure
4. Firebase architecture: collections, security rules, Cloud Functions
5. The alert pipeline (the part that decides costs)
6. Cost control and query logging
7. Data sources and licensing checklist
8. Feature-by-feature technical approach
9. Build order, split into small testable steps
10. Things that will cost money (each one needs your approval)
11. Risks and unknowns
12. Questions for you

---

## 1. Guiding principles

| Principle | What it means in practice |
|---|---|
| **Mock first** | The app and the Cloud Functions both run on mock flight data until you decide to start paying for AeroAPI. The Firebase Emulator Suite runs Auth, Firestore and Functions locally for free. |
| **Server owns the data** | The app never calls AeroAPI and never holds an API key. It reads flight docs from Firestore that the server writes. |
| **One flight, one fetch** | If 50 users track BA283 on the same day, we hold one `flights/{id}` doc and one AeroAPI alert. Their 50 phones all read that one doc. |
| **Push, not poll** | AeroAPI alerts (webhooks) drive updates. We poll only on a fixed small schedule (e.g. T-24h and T-3h), and live position only while someone has the map open. |
| **Offline by default** | Room is the app's source of truth for what's on screen. Firestore syncs into Room, so flights stay visible in airplane mode. |
| **Count every query** | Every AeroAPI call goes through one wrapper that logs the endpoint, flight and reason, and checks a daily budget cap. |
| **Small steps** | Each step ends with something you can run on your phone and check. |

---

## 2. System architecture (overview)

```
┌──────────────────────── Android app (Kotlin / Compose) ────────────────────────┐
│  UI (Compose, M3) → ViewModels (MVVM, Hilt) → Repositories                      │
│                                                   │                            │
│                    ┌──────────────────────────────┼─────────────────────┐      │
│                    ▼                              ▼                     ▼      │
│              Room (offline cache)     Firestore listeners      Callable Functions│
│                                        (flights, my tracks)   (search, track,   │
│  FCM receiver → notifications / Live Updates                  live position)    │
│  Glance widgets, Wear OS (later) read from Room                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
                                    │  HTTPS / Firestore SDK
┌──────────────────────── Firebase / Google Cloud (europe-west2) ─────────────────┐
│ Auth · Firestore · FCM · Hosting (share pages) · Secret Manager (API keys)      │
│                                                                                  │
│ Cloud Functions (2nd gen, TypeScript):                                           │
│   callable:  searchFlights, trackFlight, untrackFlight, getLivePosition          │
│   http:      aeroapiWebhook, inboundEmail, sharePage, playRtdn                   │
│   triggers:  onFlightChanged → rules engine → FCM fan-out                        │
│   scheduled: refreshUpcoming, pollWeatherAndNas, resolveFutureFlights, rollups   │
│                                                                                  │
│   FlightProvider interface ── MockProvider (dev)  |  AeroApiProvider (prod)      │
│                                  └── every call goes through: logUsage + budgetGuard│
└──────────────────────────────────────────────────────────────────────────────────┘
          │ webhooks in / REST out                     │ free public feeds
   FlightAware AeroAPI                    aviationweather.gov · FAA NAS status · FAA NOTAM
```

---

## 3. Android project structure

**Recommendation: a light multi-module setup.** A strict "Now in Android" layout with around 20 modules is too much for a one-person project. One giant module gets messy once Wear OS and widgets need shared code. Proposed middle ground:

```
FlightME/
├── app/                      # Phone app: navigation, DI setup, feature screens (by package)
│   └── src/main/java/.../
│       ├── feature/myflights/     # home list of tracked flights + trips
│       ├── feature/search/        # add flight by number / route / airline+date
│       ├── feature/flightdetail/  # status timeline, gates, weather, aircraft
│       ├── feature/map/           # live map (MapLibre)
│       ├── feature/passport/      # stats + route map
│       ├── feature/settings/      # account, units, theme, privacy
│       ├── feature/auth/
│       ├── notifications/         # FCM service, channels, Live Updates
│       └── navigation/
├── core/
│   ├── model/                # Pure Kotlin data classes (Flight, Airport, Trip…) – shared with Wear
│   ├── data/                 # Repositories + FlightDataSource interface (Mock / Firebase impls)
│   ├── database/             # Room entities, DAOs, migrations
│   ├── designsystem/         # FlightME theme, colours, type, icons, reusable components
│   └── bcbp/                 # (v1) Boarding-pass parser – pure Kotlin, heavily unit-tested
├── widget/                   # (v1) Glance widgets
├── wear/                     # (v1) Wear OS app, tile, complications
├── functions/                # Cloud Functions (TypeScript)
├── hosting/                  # Public share page (static + function-rendered)
├── firestore.rules
├── firestore.indexes.json
├── firebase.json             # Emulator config
├── gradle/libs.versions.toml # Version catalogue: one place for all library versions
├── PLAN.md
└── DECISIONS.md
```

**Key Android choices**

- **Min SDK 26** (Android 8), **target/compile SDK 36** (Android 16). Live Updates only turn on for API 36 and above. Older phones get a normal ongoing notification.
- **Single-activity Compose app** using Navigation Compose with type-safe routes.
- **Hilt** for dependency injection. A build flag (`USE_MOCK_DATA`) chooses `MockFlightDataSource` or `FirebaseFlightDataSource`.
- **Room is what the UI reads.** Firestore snapshot listeners write into Room, and the UI observes Room `Flow`s. That gives offline mode by design, not as an extra feature.
- **WorkManager** handles periodic housekeeping only (cache pruning, re-registering the FCM token, Passport recompute). It does not poll flight status.
- **MapLibre Android SDK** with a free tile source (see §7), wrapped in a Compose component.
- **Debug "time machine" screen** (debug builds only). It steps a mock flight through its lifecycle (scheduled → boarding → taxi → airborne → landed → at gate) so you can test the timeline, notifications and Live Updates in five minutes, not five hours.

---

## 4. Firebase architecture

### 4.1 Region & plan
- **Region:** `europe-west2` (London) for Functions and Firestore, since you and your first users are UK-based.
- **Plan:** Spark (free) is enough for Auth plus local emulators. **Deploying Cloud Functions and using Secret Manager needs the Blaze (pay-as-you-go) plan.** We'll stay on emulators until you approve Blaze, and we'll set a GCP budget alert when we switch.

### 4.2 Firestore collections

```
users/{uid}
    displayName, homeAirport, units, theme, createdAt
    entitlement: { tier: free|pro|trip|family, expiresAt, source, trialFlightId }   ← server-write only
    emailAlias: "k7f3q2@in.flightme.app"                                           ← server-write only
  /devices/{installId}        fcmToken, platform, appVersion, liveUpdatesSupported, updatedAt
  /tracks/{trackId}           flightId, tripId?, source (search|bcbp|email|calendar|tripit|friend),
                              alertPrefs {…overrides}, role (self|friend), addedAt, archived
  /trips/{tripId}             name, trackIds[], autoDetected, connectionScores (v2)
  /stats/summary              Passport aggregates (server-computed)
  /following/{friendUid}      (v1) who I follow; permissions granted by the friend

flights/{flightId}            ← SHARED, server-write only. One doc per flight instance.
    ident (BAW283), identIata (BA283), operator, flightNumber
    origin, destination {icao, iata, tz}, scheduled/estimated/actual
      × {gateOut, runwayOff, runwayOn, gateIn}
    status (scheduled|boarding|departed|enroute|landed|arrived|cancelled|diverted)
    terminal/gate origin & destination, baggageClaim
    aircraftType, registration, inboundFlightId
    delayReasons [ {code, text, source} ]
    lastProviderFetchAt, providerAlertId, subscriberCount
    dataFreshness { status: ts, position: ts }
  /events/{eventId}           Change log: {type, field, old, new, at, source}. Feeds the timeline,
                              alerts, and later the ML training data.
  /positions/latest           Last live position (lat, lon, alt, gs, heading, at). Shared cache.
  /track/full                 Flown track (compressed polyline), written once after landing.

flightKeys/{ident_yyyymmdd_origin}   → { flightId }   Lookup/dedup before a provider id exists.

airports/{icao}               Static data (name, tz, lat/lon, country) + conditions:
    metar, taf, nasStatus, updatedAt                                   ← server-write only
aircraft/{registration}       type, built year, operator, history (as available)

shares/{shareToken}           flightId, ownerUid, createdAt, expiresAt, revoked   (v1)
emailAliases/{alias}          uid                                                 (v1)

apiUsage/{yyyy-mm-dd}         totals by endpoint + estimated cost               ← server only
  /flights/{flightId}         per-flight counters: {endpoint: count, cost}
config/runtime                dailyBudgetUsd, killSwitch, mockMode, featureFlags ← server only
```

**Why a shared `flights` collection?** Cost. Tracking is a reference (`users/{uid}/tracks`) to a flight that exists once. The server keeps `subscriberCount` so it knows when to create or delete the AeroAPI alert.

**The "future flight" problem.** AeroAPI only gives a flight its unique `fa_flight_id` a few days before departure. For a flight booked months ahead we create a provisional doc keyed `ident_date_origin`. A scheduled job (`resolveFutureFlights`) swaps it for the real id when it appears. That is one cheap query per flight, run close to the date.

### 4.3 Security rules (summary)
- `users/{uid}/**`: read/write only by that user. The `entitlement` and `emailAlias` fields are rejected if the client tries to write them.
- `flights/**`, `airports/**`, `aircraft/**`: read by any signed-in user, write by server only. Flight status isn't personal data. Which flights *you* track is private, because that lives under `users/{uid}`.
- `shares`, `apiUsage`, `config`, `emailAliases`: no client access. The share page reads them through a function.
- **App Check** (Play Integrity) on all callable functions, so only our genuine app can trigger API spend.

### 4.4 Cloud Functions (TypeScript, 2nd gen)

| Function | Type | What it does | AeroAPI cost |
|---|---|---|---|
| `searchFlights` | callable | Checks the Firestore cache first (fresh within N min). On a miss, one provider query. Upserts `flights` docs. | 0–1 queries |
| `trackFlight` | callable | Creates the user's track and bumps `subscriberCount`. On the first subscriber, registers **one** AeroAPI alert for the flight. | 0–1 alert setups |
| `untrackFlight` | callable | Decrements the count. At zero, deletes the alert. | 0 |
| `aeroapiWebhook` | https | Receives alert pushes and verifies them. Diffs against the stored doc, writes changed fields, and adds `events`. | 0 (push) |
| `onFlightChanged` | Firestore trigger on `flights/{id}` | Runs the **rules engine** (§5) and fans out FCM to subscribers. | 0 |
| `getLivePosition` | callable | Returns `positions/latest` if it's under ~60 s old. Otherwise makes one provider call and updates it. The app only calls this while the map is open. | ≤1/min/flight, shared |
| `fetchTrackOnLanding` | internal | Fetches the full track once after arrival, for the Passport map. | 1 |
| `refreshUpcoming` | scheduled (15 min) | Safety net: re-fetches flights that are T-24h/T-3h, or whose last update is suspiciously old. | small, capped |
| `pollAirportConditions` | scheduled (10–30 min) | METAR/TAF and FAA NAS status for airports that have active flights. Free feeds. | 0 |
| `resolveFutureFlights` | scheduled (daily) | Swaps provisional flight docs for real ones. | 1 per future flight |
| `rollupUsage` | scheduled (daily) | Aggregates `apiUsage` and emails or logs a report. | 0 |
| `inboundEmail` (v1) | https | Parses forwarded booking emails with an LLM, then adds tracks. | 0 + LLM |
| `playRtdn` / `verifyPurchase` (v1) | Pub/Sub / callable | Google Play subscription events, then writes `entitlement`. | 0 |
| `sharePage` (v1) | https (Hosting rewrite) | Renders the public live-flight page. | 0 (reads cache) |

Inside `functions/`, a single `FlightProvider` interface has two implementations:
- `MockProvider`: plays back scripted scenarios (on-time, delayed, gate change, cancelled, diverted, missed connection) and sends fake webhooks to the emulator.
- `AeroApiProvider`: the real thing. Every method goes through `meteredCall(endpoint, flightId, reason)`, which logs usage and enforces the budget cap.

---

## 5. The alert pipeline

```
 AeroAPI alert webhook ─┐
 scheduled safety poll ─┼─► normalise to our Flight model ─► diff vs Firestore doc
 FAA NAS / METAR poll ──┘                                         │
                                                                   ▼
                                        write changed fields + events/{…}  (one transaction)
                                                                   │
                                          Firestore trigger: onFlightChanged
                                                                   ▼
                                  RULES ENGINE: for each changed field / event
                                    • match alert types (catalogue in alertTypes.ts)
                                    • dedupe (same alert within X min → skip)
                                    • suppress noise (e.g. ±2 min estimate wiggles)
                                                                   ▼
                          for each subscriber (users with a track on this flight):
                            • check tier (free vs pro alert types, trial flight)
                            • check user's alert prefs + quiet hours
                            • build message (plain language, user's units/timezone)
                                                                   ▼
                     FCM: high-priority DATA message → app builds the notification
                          (so the same payload also updates Live Update / widget / Room)
```

- **Alert catalogue as data.** `alertTypes.ts` lists every type with its id, trigger condition, tier (free/pro), default on/off, and message template. That makes "65+ alert types" a growing list, not 65 pieces of code. The MVP ships about 10: *delayed, gate change, terminal change, departed, landed, arrived at gate, cancelled, diverted, baggage claim, schedule change*.
- **Data messages, not notification messages.** The app decides how to show each one: a normal notification, an update to the Live Update progress, or both. It also caches the new state into Room.
- **Idempotency.** Webhooks can arrive twice or out of order. Each event carries the provider timestamp, and older updates are ignored.

---

## 6. Cost control and query logging (from day one)

1. **`meteredCall` wrapper.** It is the only code path to AeroAPI. For each call it logs `{endpoint, flightId, reason, uid?, costEstimate}` to Cloud Logging (structured) and increments `apiUsage/{date}` and `apiUsage/{date}/flights/{flightId}`.
2. **Budget guard.** It reads `config/runtime.dailyBudgetUsd`. When today's estimate passes the cap, non-essential calls (live position, refresh polls) fall back to cached data, and you get an alert email. **Kill switch:** `config/runtime.killSwitch = true` stops all provider calls.
3. **Caching TTLs** (tunable in config):
   - search results: 10 min
   - flight status outside T-6h: 30 min
   - flight status inside T-6h: rely on push + a 5 min floor
   - live position: 60 s, shared
   - aircraft info: 30 days
   - airport static data: bundled in the app plus refreshed monthly
4. **GCP budget alerts** at 50/90/100% of the monthly budget you choose.
5. **Cost-per-flight dashboard.** A simple admin screen or report showing `apiUsage` divided by tracked flights, so we can check the $0.10–0.50/flight target against real numbers.

**Estimated AeroAPI calls per tracked flight (rough, to verify):** 1 search (often cached) + 1 alert registration + ~6–12 pushed alerts + ~1 safety poll + 1 track fetch on landing + live positions only while the map is open (shared and capped). **I won't quote dollar prices from memory.** AeroAPI pricing and alert billing change, so step 3.1 of the build is to read the current price sheet with you and fill in a real per-flight estimate before spending anything.

---

## 7. Data sources and licensing checklist

We check the terms of each source before we write code against it. The results get recorded in DECISIONS.md.

| Source | Use | Cost | Licence status / to verify |
|---|---|---|---|
| **FlightAware AeroAPI (Standard)** | Status, alerts, positions, tracks, aircraft | Paid, per query. I believe Standard has a monthly minimum; **verify** | **Verify with FlightAware:** redistribution to consumer app users, caching/retention limits (matters for Passport history and ML training on stored data), attribution requirements, public share pages. |
| aviationweather.gov Data API | METAR/TAF worldwide | Free | US government. Generally public domain; check usage/rate policy. |
| FAA NAS Status | US ground stops, GDPs, delays | Free | Public. Check rate policy. |
| FAA NOTAM API (NMS) | NOTAMs | Free, registration needed | Check terms. The FAA is migrating NOTAM systems, so confirm the current endpoint. |
| **OurAirports** | Airport names, coords, codes | Free | Public domain. Bundle a trimmed copy in the app. |
| Airline names/codes | Display | Free (OpenFlights or similar) | Check the licence (OpenFlights is ODbL, which needs attribution). |
| **Airline logos** | Display | ⚠️ Trademarks | Don't scrape. Use coloured text badges for MVP; decide later. |
| Map tiles: **OpenFreeMap** (or MapTiler / self-hosted Protomaps) | Map base layer | Free / free tier | OpenStreetMap data, so the ODbL attribution must be shown on the map. |
| OpenSky Network | — | — | **Not used.** Free data isn't licensed for commercial use. |
| Minimum Connection Times | Connection Assistant | Official data (OAG/IATA) is paid | v2 question. Start with our own table from airport-published MCTs plus rules. |
| TripIt | Import | — | Their API partner programme may be closed to new apps. **The iCal feed route is safer.** |
| LLM for email parsing | v1 | Pay per use | Candidates: Claude Haiku 4.5 or Gemini Flash (via Firebase AI Logic). Check the data-processing terms, since booking emails contain personal data. |

---

## 8. Feature-by-feature technical approach

### MVP
- **Auth:** Firebase Auth with Google Sign-In (Credential Manager) plus anonymous start. The user can try the app, then link to Google so nothing is lost.
- **Search/add:** by flight number + date, route + date, or airline + date. Results come from `searchFlights`. The user taps to track.
- **Status timeline:** four milestones (gate out, takeoff, landing, gate in), each showing scheduled, estimated and actual times with colour for early/late. Taxi-out and taxi-in estimates come from the provider or a per-airport average. Also shows terminal, gate and baggage.
- **Live map:** great-circle planned route, flown track, plane icon rotated to heading, altitude and speed readout. It calls `getLivePosition` every 60 s **only while the screen is visible** (lifecycle-aware).
- **Basic alerts:** the ~10 types in §5, sent via FCM.
- **Live Updates:** `Notification.ProgressStyle` with `requestPromotedOngoing` on Android 16. Progress segments cover to-gate → boarding → in-air → landed, with a countdown to landing. We expect it to show in the Samsung Now Bar on One UI 8+; **verify on a real Samsung**. Older Android gets a standard ongoing notification.
- **Basic Passport:** counts of flights, distance (great-circle), hours, airports, airlines and aircraft types, plus a route map. Computed from completed tracks.
- **Offline:** Room holds flights, events, the last position and the track. The Live Update countdown runs on-device from the estimated landing time, so it keeps ticking in airplane mode.

### v1
- **Boarding pass scan:** CameraX + ML Kit barcode scanning (PDF417, Aztec, QR). A pure-Kotlin **BCBP parser** in `core/bcbp` handles flight, date (Julian day, so we infer the year), from/to, seat and PNR. It runs fully offline. Unit tests use synthetic sample strings; we won't commit real passes.
- **Calendar import:** CalendarProvider with the `READ_CALENDAR` permission. A regex spots flight numbers and airport codes in event titles, then asks the user to confirm. We also export tracked flights to the calendar via an insert intent (no write permission needed).
- **Email import:** each user gets a unique forwarding address. Inbound mail goes through **Cloudflare Email Routing + Email Worker** (free) or SendGrid Inbound Parse, which POSTs to `inboundEmail`. There, the LLM extracts flight JSON, we validate it against our schema, and we create tracks. Email bodies are deleted after parsing.
- **TripIt:** the user pastes their private iCal feed URL, and a scheduled function reads it.
- **Friends + share links:** follow requests with consent, stored under each user. Share link = `https://<domain>/f/{token}`, which Firebase Hosting rewrites to `sharePage`. It renders the cached flight doc and auto-refreshes, with no app needed.
- **Widgets (Glance):** "next flight" and "flight in progress" sizes, reading Room.
- **Wear OS:** a tile showing the next flight, a complication showing time to departure or landing, and a small app. Data comes from the phone via the Data Layer.
- **Play Billing + paywall:** Billing Library, with purchases verified **server-side** (Play Developer API) and real-time developer notifications via Pub/Sub → `entitlement`. The app only reads its entitlement and never decides it. Free trial = the first tracked flight gets the Pro flag.

### v2
- **Delay prediction (rules first):** inbound aircraft ETA + minimum turnaround for the aircraft type vs. scheduled departure, plus active FAA GDP/ground stops, plus severe METAR. Output is a risk level (Low/Med/High) with plain-language reasons. Every prediction is stored with its later outcome, which becomes the ML training set.
- **Gate prediction:** most frequent gate for this flight number from our own `events` history.
- **Connection Assistant:** a score from 0–100 based on layover time vs. MCT for the airport and route type (domestic/international), terminal change, whether immigration/security re-screen is needed (rules per airport and country pair), inbound delay risk, and time of day. Live "connection at risk" alerts fire when the inbound estimate crosses thresholds.
- **Airport trends:** hourly aggregates of departure/arrival delays from flights we already track.
- **Morning Of:** a scheduled function at the local morning of travel. It combines leave-by time (user's home airport + a buffer setting), weather and risk level.
- **Gemini integration:** the current route is Android **App Functions**. Verify at build time, as this API is evolving.

---

## 9. Build order: small testable steps

Each step ends with **✅ Test:**, which is what you check on your phone.

### Phase 0: Setup (no cost)
- **0.1** Repo skeleton, `.gitignore`, PLAN.md, DECISIONS.md. ✅ You've read and approved this plan.
- **0.2** Install and check: Android Studio (latest stable), JDK 17, a Pixel emulator on API 36, Node 22, Firebase CLI. ✅ `firebase --version` works, and an empty emulator boots.
- **0.3** Create the Firebase project (Spark/free) and register the Android app. You'll download `google-services.json` yourself; it stays out of git. ✅ Project visible in the Firebase console.

### Phase 1: MVP on mock data (no cost)
- **1.1** Android scaffold: Gradle version catalogue, Compose, M3 theme, Hilt, and an empty nav graph with bottom tabs (My Flights / Passport / Settings). ✅ App launches with three tabs, and dark mode follows the system.
- **1.2** `core/model` + `MockFlightDataSource` with about 8 scripted flights. The My Flights list shows them. ✅ List shows on-time, delayed and cancelled cards.
- **1.3** Flight detail screen with the status timeline, gates, terminal and baggage. ✅ Delayed flight shows red estimated times.
- **1.4** Room cache + repository (UI reads Room). ✅ Airplane mode → flights still there after an app restart.
- **1.5** Debug time machine: step a mock flight through its lifecycle. ✅ The timeline updates as you step.
- **1.6** Search / add flight screen (mock search). ✅ Search "FM123", add it, and it appears in the list.
- **1.7** Live map with MapLibre + free tiles, and a mock position moving along the route. ✅ Plane moves; the map shows OSM attribution.
- **1.8** Local notifications + Live Updates driven by the time machine. ✅ On an Android 16 emulator, a progress notification advances. On Samsung, check the Now Bar.
- **1.9** Basic Passport stats + route map from mock history. ✅ Numbers add up for the mock data.
- **1.10** Settings: units (km/mi), 12/24h, theme. ✅ Changing units updates the map and Passport.

### Phase 2: Firebase on emulators (no cost)
- **2.1** Firebase Emulator Suite config + Auth (anonymous → Google link). ✅ Sign in; the user appears in the emulator UI.
- **2.2** Firestore collections + security rules + **rules unit tests**. ✅ Tests prove user A can't read user B's tracks.
- **2.3** `functions/` project with `FlightProvider` + `MockProvider`, `searchFlights` and `trackFlight`. ✅ The app (flag switched to Firebase) adds a flight through the emulator.
- **2.4** `meteredCall` + `apiUsage` logging (counts mock calls as if real). ✅ Usage doc increments per search, and cached repeats don't increment.
- **2.5** Mock webhook → diff → `events` → rules engine → FCM. ✅ Trigger "gate change" in the mock and get a push on the phone.
- **2.6** Room ⇄ Firestore sync, with Live Updates driven by FCM. ✅ Mock flight progresses from the server and the phone follows along.

### Phase 3: Real data (⚠️ costs money, needs your approval at each step)
- **3.1** Review AeroAPI pricing/terms together and write a cost model into DECISIONS.md. *(no spend)*
- **3.2** 💷 Upgrade Firebase to Blaze + set budget alerts; put the AeroAPI key in Secret Manager.
- **3.3** 💷 Sign up for AeroAPI. Implement `AeroApiProvider` against a **tiny daily cap** (e.g. $1/day) and test with 2–3 real flights.
- **3.4** Register real AeroAPI alerts → deployed `aeroapiWebhook`. ✅ A real flight's gate change arrives as a push.
- **3.5** Airport conditions from aviationweather.gov + FAA NAS (free). ✅ Weather on the detail screen matches the airport's current METAR.
- **3.6** Cost review: actual $/flight vs target. Tune TTLs.

### Phase 4: v1 (each feature is its own mini-plan when we get there)
Boarding pass scan → calendar import/export → email import (💷 domain + LLM) → TripIt iCal → Friends → share pages → widgets → Wear OS → Play Billing + paywall (💷 Play developer account) → closed testing track.

### Phase 5: v2
Rules-based delay prediction → gate prediction → Connection Assistant → airport trends → Morning Of → Gemini/App Functions → ML model (once we have months of stored outcomes).

---

## 10. Things that will cost money (I'll ask before each)

| Item | When | Notes |
|---|---|---|
| Firebase Blaze plan | Phase 3.2 | Pay-as-you-go. Free quotas still apply, so at low usage the bill is often very small. Budget alerts go on first. |
| FlightAware AeroAPI | Phase 3.3 | Main running cost. Verify the monthly minimum and per-query/alert prices. |
| Google Play developer account | Before any Play testing | One-off registration fee. You may already have one from your previous app. |
| Domain name | v1 (email import, share links) | Needed for `@in.<domain>` forwarding and nice share URLs. |
| LLM API | v1 email import | Small per email. Could also use Firebase AI Logic (Gemini). |
| Inbound email service | v1 | Cloudflare Email Routing is free. Alternatives are paid. |
| Map tiles | Only if we outgrow the free options | OpenFreeMap is free. MapTiler has a free tier. |
| MCT dataset | v2, optional | Official MCT data is expensive. We'll start with our own. |

---

## 11. Risks and unknowns

1. **AeroAPI terms for a consumer app.** Retention and redistribution rules could limit Passport history, share pages and ML training on stored data. *Mitigation: ask FlightAware sales before 3.3.*
2. **Future-flight coverage.** Flights far out have no provider id yet. *Handled by provisional docs + `resolveFutureFlights`.*
3. **Samsung Now Bar** behaviour on One UI versions before 8 may need Samsung partner approval. *Test on a real device.*
4. **FCM delivery timing.** High-priority data messages can be deprioritised if the app never shows a notification for them. *We always show or update a notification.*
5. **Aircraft age/history.** Not all of this is in AeroAPI. The FAA registry covers N-registered aircraft for free, and other countries vary. *May be partial at launch.*
6. **Minimum connection times.** No free authoritative global source. *Connection Assistant v2 starts with rules + curated data for top airports.*
7. **Play policy.** The calendar permission needs a justification, account deletion must be in-app and on the web, and the Data Safety form must be filled in.
8. **UK GDPR.** Flight plans + friends = personal data. We need a privacy policy, data export/delete, and must minimise what we keep from emails.

---

## 12. Questions for you

(Short answers are fine. I've given my recommendation for each.)

**Setup and workflow**
1. Will you run Claude Code **locally** alongside Android Studio, or keep using this cloud session? This container has no Android SDK, so here I can write and compile-check some code but you'll run the app on your machine. *(Rec: local for Android work, cloud is fine for functions/planning.)*
2. What phone(s) do you test on: model and Android version? Is there a Samsung for Now Bar testing? A Wear OS watch?
3. Do you still have your Google Play developer account from the previous app?
4. Is your previous Firebase project reusable, or should we make a fresh `flightme-dev` + later `flightme-prod`? *(Rec: fresh, two projects.)*

**Architecture choices**
5. Cloud Functions language: **TypeScript** (best Firebase support, most examples) or Python? *(Rec: TypeScript. I'll keep it readable.)*
6. Module layout: OK with the "light multi-module" structure in §3? *(Alternative: one module to start, split later.)*
7. Maps: happy with **MapLibre + OpenFreeMap tiles** (free, needs an OSM attribution line)?
8. Sign-in: Google only, plus anonymous "try first"? Or also email/password?
9. Min SDK 26 (Android 8) OK? That covers nearly all active devices.

**Product**
10. Units/timezone default: show times in **airport local time** (with an option for home time)? *(Rec: local time, as airlines do.)*
11. Trip pass £1.49/week: a **weekly auto-renewing subscription** or a **one-off 7-day pass**? *(Rec: one-off. Users hate forgotten weekly renewals.)*
12. Family plan: OK to run our **own** family system (the owner buys, then invites up to 4 by link), since Play's family sharing doesn't cleanly cover this?
13. Which ~10 alerts matter most to you for MVP? My list is in §5.
14. Any visual direction for the brand: colours, mood, apps you like the look of (non-Flighty)? I'll draft a design system in `core/designsystem`.
15. Do you have a real name in mind, or should I propose some names later (with a quick trademark sanity check)?

**Data and money**
16. Have you already talked to FlightAware or signed up for AeroAPI? Do you know your intended tier's current pricing?
17. What monthly budget cap should the kill switch use during development? *(Rec: $1/day while testing real data.)*
18. For email parsing later: preference between Claude (Haiku) and Gemini (via Firebase)?

---

*Next step after your OK: Phase 0.1–0.3, then Phase 1.1 (scaffold). I'll pause after each step for you to test.*
