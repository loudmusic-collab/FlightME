# FlightME: notes for Claude Code

Android flight tracker (Kotlin, Compose, M3, Hilt, Room) with a Firebase backend (TypeScript Cloud Functions).
Read `PLAN.md` for the architecture and build order, and `DECISIONS.md` for choices already made. Don't reopen accepted decisions without a reason.

## Working with the owner
- The owner is a materials engineer, not a professional developer. Explain decisions briefly and in plain language.
- Work in **small steps** that match the numbered steps in PLAN.md §9. After each step, stop. Tell the owner exactly how to test it on their phone or emulator (a "✅ Test:" line), then wait.
- Ask lots of questions when something is unclear.
- **Always ask before anything that costs money:** paid API calls (AeroAPI, LLMs), a Firebase Blaze upgrade, domains, the Play developer account, or any new paid service.
- Add a row to `DECISIONS.md` for every significant choice.
- Keep FlightME's own branding. Never copy Flighty's name, logo, assets or distinctive UI.

## Environment
- Windows + PowerShell, with Android Studio. Use PowerShell-friendly commands (e.g. `.\gradlew.bat`, not `./gradlew`).
- Test devices:
  - Samsung Galaxy S21 Ultra (Android 15 / One UI 7, no Live Updates, so it tests the fallback notification)
  - Android 16 (API 36) emulator for Live Updates
- Firebase: project `flightme-dev` on the free Spark plan. Use the Emulator Suite for Auth, Firestore and Functions.

## Rules
- API keys are **never** in the app or in git. `google-services.json` is git-ignored.
- The app never calls AeroAPI directly. All flight data goes through Cloud Functions.
- Every provider call goes through `meteredCall` (usage logging + budget cap).
- Mock data first: `USE_MOCK_DATA` build flag in the app, `MockProvider` in functions.
- Brand direction (provisional): minimal, simple lines, thin-stroke icons, colour mainly for flight status.

## Commands (fill in as the project grows)
- Build debug APK: `.\gradlew.bat :app:assembleDebug`
- Unit tests: `.\gradlew.bat test`
- Firebase emulators: `firebase emulators:start`
