# BUILD_REPORT.md — Brainrot Rebuild

**Project:** Native Compose/Room/Glance rebuild of the BrainRot reels-blocker app
(`com.brainrot.android` v12.2.494, versionCode 494), produced purely from static
analysis (see [brainrot-static-analysis](https://github.com/swathigampa354-ship-it/brainrot-static-analysis)).

## Build result

| Item | Value |
|---|---|
| Repo/commit | `swathigampa354-ship-it/brainrot-rebuild` @ `78c17a8` |
| CI run (green) | https://github.com/swathigampa354-ship-it/brainrot-rebuild/actions |
| Build path | GitHub Actions — JDK 17 Temurin, Gradle 8.10.2, assembleDebug |
| APK | `app-debug.apk`, 64,319,369 bytes |
| Public download | https://github.com/swathigampa354-ship-it/brainrot-rebuild/releases/download/build-78c17a8f1329031adc81f82d92a63dd502ed0ccb/app-debug.apk |
| Release tag | `build-78c17a8f1329031adc81f82d92a63dd502ed0ccb` |
| Local mirror | `~/Downloads/brainrot-rebuild-app-debug.apk` (sha256 verified) |

Build identity: `com.brainrot.rebuild`, versionName 1.0.0, versionCode 1,
minSdk 28 (Glance requires 8+; accessibility API needs 9+), targetSdk 36,
Debug (unminified) build.

## CI iteration log (honest)

- Run 1 (`707cbba`) — FAILED, 44 Kotlin errors (missing shared UI file, `Increments`
  package mismatch, suspend call sites, Glance ColorProvider type, signature slips).
  Fixed in `b9e1a7e`.
- Run 2 (`b9e1a7e`) — FAILED, 15 errors (coroutine scoping in `Increments`, Glance
  `updateAll`, R import ambiguity, scaffold call-sites). Fixed in `83004ef`.
- Run 3 (`83004ef`) — FAILED, 2 errors (duplicate `R` imports in widget). Fixed in
  `78c17a8`.
- Run 4 (`78c17a8`) — **SUCCESS**, debug APK produced and released.

## Feature parity status

- Reels accessibility watcher (`ReelsAccessibilityService`), threshold store
  (reels limit / open limit / block toggle / overlay warnings / usage-fallback /
  watch-listed packages) — ✅ implemented, behavior mirrors the decompiled design.
- FallbackUsageStatsService (usage-stats for when accessibility is off) — ✅ implemented
  with a 60-second poller; needs `PACKAGE_USAGE_STATS` special grant.
- Block overlay after daily reels limit (`BlockOverlayActivity`) — ✅ enabled by the
  watcher once the counter exceeds the configured limit.
- 15-minute widget refresh via WorkManager + `CounterRefreshWorker` +
  `updateAll` — ✅ implemented.
- Home-screen counter widgets (reels + opens) — ✅ basic Glance widget.
- Room schema (daily counters, app-split package breakdowns, rolling event log) +
  the DataStore thresholds equivalent, all in a Room DB — ✅.
- WorkManager (daily pruning, sync trigger, widget refresh) — ✅.
- Suspend-call-safe `Increments` object — ✅.

## Gaps (honest)

- **No pixel clone** — UI is behavior-first, not a screenshot copy.
- **Advanced widgets** — only one Glance widget (reels + opens);
  the original ships multiple variants (`AppOpenCounterWidgetReceiver`,
  `ReelsCounterWidgetExpandedReceiver`, etc.).
- **Floating bubbles** — only the overlay + widget are present; the
  exact floating bubble service (in the decompiled codebase this is
  a Windows-blind `AppOpenFloatingService`/`ReelsCounterFloatingService`) is a
  research target (smali evidence only), not committed here.
- **Cloud sync / RevenueCat / Firebase / Segment / Pairip** — excluded, as with
  swipewipe; the network layer goes to a user-configured base URL only
  (see threshold settings).

## What to build next (parity roadmap)

1. Floating bubbles (`AppOpenFloatingService` equivalent) via WindowManager overlay.
2. Expanded widget variants.
3. Full `${endpoint}` /RevenueCat billing surface (needs user credentials anyway).
4. Usage parity for `feature_duel`, `feature_invite` (needs server).
5. Persistence migration mapping Table aliases and `MigrationKey`/`MigrationStatus`
   bookkeeping.
