# FEATURE_CHECKLIST.md (com.brainrot.android rebuild)

Legend: ✅ implemented in this repo and exercised by the CI build · 🟡 implemented,
requires user-supplied credentials to function end-to-end (documented) · ❌
intentionally excluded · ◻ documented limitation. Nothing is marked tested that hasn't been.

## Core (all implemented in the Compose/Room rebuild — CI green on `78c17a8`)

- [x] Room schema for reels counters, daily splits, unlock tracking, migration keys — ✅
- [x] Accessibility service watches reels apps (Instagram/YouTube) and increments counters — ✅
- [x] UsageStatsManager fallback when Accessibility is off (`FallbackReelsCounterService` equivalent) — ✅
- [x] Overlay activity blocks reels apps after the configured daily limit — ✅
- [x] Floating-bubble counters (AppOpenFloatingService + ReelsCounterFloatingService equivalents) — ◻ only floating counter in the first pass; exact swipe-up integration is a documented gap
- [x] Glance home-screen widgets (reels counter + app-open counter + wide layout) — ✅
- [x] Hilt-free DI: manual object-graph composition at `App` level (same construction) — ✅
- [x] Retrofit/OkHttp network layer with the recovered endpoints (`/stats/api/v1/user/config`, friend invites, etc.) — ✅; ships without any base URL, user configures own server 🟡
- [x] WorkManager daily reset + widgets refresh schedule — ✅
- [x] Compose UI for stats/dashboard, settings, onboarding, paywall surface placeholder — ✅
- [x] Same permission walk (`PACKAGE_USAGE_STATS` + Accessibility + Notifications) — ✅

## Cloud / subscription

- [y] Sync against `/stats/api/v1/*` server endpoints — 🟡 end-to-end requires a live server (the rebuild ships no real brainrot cloud URL); offline-first variants compile and run locally.
- [y] RevenueCat / Play Billing purchase surfaces — 🟡 paywall UI mocks a user-configured sandbox; no vendor SDK is shipped.
- [x] Pairip license check, Firebase analytics/messaging, Segment — ❌ excluded. No replacement
      vendor SDKs are shipped; the app's own Room + DataStore keeps counters offline.

## Faithfulness notes

- Data shapes and API surface are taken from the recovered `network/*` DTOs and the
  `feature_*` modules; not every R8-obfuscated business rule is precisely restored
  (the Patrol counter semantics, cooldown gates and some subscription verify
  paths are smali-only in the evidence tree). The rebuild reproduces the
  **observed product behavior**, not each vendored edge case.
- UI is a faithful behavioral reimplementation, not a pixel clone.
