# PROJECT_RESEARCH.md — com.brainrot.android v12.2.494

Derived entirely from static analysis (see HANDOFF.md + evidence repo 
`swathigampa354-ship-it/brainrot-static-analysis`). No proprietary code copied —
this is a behavioral reimplementation.

## What the app is

"BrainRot" — a **short-form-video (Reels/Shorts) usage tracker and blocker**:

1. **Count** reels via Accessibility service reading the Instagram/YouTube
   app interface, plus a usage-stats fallback (`feature_fallback_counter`) for
   times when Accessibility is disabled.
2. **Block / warn** — full-screen overlay on top of the reels (`BlockReelsOverlayActivity`)
   and floating bubbles (`AppOpenFloatingService`, `ReelsCounterFloatingService`) that
   counter narrative "this is your Nth reel today".
3. **Stats** — per-day, per-app break-down of Indie vs. Brainrot-class usage
   (`core/domain/model/{ReelsScrollEvent, PhoneUnlockDaily, DailyReelsAppSplit,
   AppOpenDailySplit, AppInstallationHistory}`) plus `feature_reels_counter` for friends and
   `feature_duel`, `feature_invite` social surfaces.
4. **Home-screen widgets** (Glance, wide+small modes) + a floating widget — all show live
   counters.
5. **Subscription** — Pro tier gates features; server-side RevenueCat wrapper
   (`com.revenuecat.purchases`, 2,080 smali classes) + Play Billing proxies + product
   services (pal payment method).
6. **Companion sync** — a cloud API (`/stats/api/v1/…`) for device-level thresholds
   (`/stats/api/v1/user/config`), restores, friend stats, 1-time-payments.
7. **Hilt** module-graph dependency injection, Room (`BrainRotRoomDatabase` +
   `UserPrefDatabase`, com.couchbase.lite as secondary storage), Retrofit/OkHttp
   network layer, Firebase messaging for push, Segment for product analytics, ExoPlayer
   for short media, Glance for widgets, Pairip license-check for anti-tamper.

## Component map (from AndroidManifest)

| Type | Component | Purpose |
|---|---|---|
| Activity | `com.brainrot.android.MainActivity` | app launcher + Compose UI host |
| Activity | `com.brainrot.android.BlockReelsOverlayActivity` | overlay over reels |
| Activity | `com.brainrot.android.PermissionStepsOverlayActivity` | onboarding permission steps |
| Activity | `com.brainrot.android.RatingPromptOverlayActivity` | rating prompt |
| Service | `com.brainrot.android.services.ReelsAccessibilityService` | Accessibility reels reader |
| Service | `com.brainrot.android.feature_fallback_counter.FallbackReelsCounterService` | usage-stats fallback counter |
| Service | `com.brainrot.android.floating_bubble.*` | floating widget bubbles |
| Service | `com.brainrot.android.services.BrainRotFirebaseMessagingService` | push messages |
| Receiver | `com.brainrot.android.core.receiver.DateChangedReceiver` | daily counters reset |
| Receiver | `com.brainrot.android.feature_widget.…` | Glance widgets |
| Provider | `ai.regainapp.payments.sharing.PaymentsFileProvider` | payments sharing |
| Widget | `feature_widget` | widget providers |

## Data model (Room) recovered from `core.data.data_source` + `feature_reels_counter.data.data_source`

Entities **not all** de-closed statically in this pass, but the observable
runs of the UI/nets layer confirm at least:
`MigrationKey, MigrationStatus, PhoneUnlockDaily, ReelsScrollEvent, AppOpenDailySplit,
DailyReelsAppSplit, AppInstallationHistory, UserBlockingConfig` in the brainrot net layer,
plus `ReelWeekEventDb`, `AppReelsStats`, `DailyReelsStats`, `Friends/OneFriend`,
`BrainPalPaymentMethod`, `SubscriptionState` in feature_* layers.

## Known gaps / deliberate exclusions (mirror of FEATURE_CHECKLIST reasoning)

- **RevenueCat + Play Billing** — no shipping of vendor SDKs in any rebuild; the rebuild
  feature-presents a similar set of "type open" Paywall UI but requires the **user's
  own RevenueCat/Play key** or runs locked to the free tier. Documented as 🟡.
- **Firebase** (messaging, crashlytics, analytics) and **Segment** — excluded.
- **Pairip LicenseCheck** — anti-tamper only; excluded.
- **Regain-shared payments provider** (`ai.regainapp.*`) — third-party glue code, excluded.
- Reels detection relies on **the platform Accessibility service (android.permission.
  BIND_ACCESSIBILITY_SERVICE)** and **PACKAGE_USAGE_STATS** — rebuilt as first-class
  features with the same user-visible semantics (counter visible every reel with
  floating bubble + overlay).

## Build target

Same as previous `swipewipe-rebuild` model: Kotlin, Compose, Room, Retrofit/OkHttp
raw stack, WorkManager, accessibility + glance widgets, all real but with the
management surfaces compiled. Rebuild language is Kotlin 2.0.21, minSdk 26 (Glance
needs 8+ and accessibility insists on it), target 36, CI-reproducible.

## Deliverables and build checklist

1. App skeleton (application, MainActivity, base UI, DI or manual-graph equivalent).
2. Reels AccessibilityService with underlying counting logic.
3. Fallback detail (UsageStatsManager-based; needs `PACKAGE_USAGE_STATS` special
   permission flow).
4. Overlay block + floating bubbles with actual counters from the same DB.
5. Glance widgets (Reels counter + app-open counter).
6. Companion `StatsScreen` (Room-backed; mirrors `DailyReelsAppSplit` +
   `PhoneUnlockDaily`), SettingsScreen for thresholds, paywall stub.
7. Sync layer matching the decompiled `BrainRotApi` + `network/*` DTOs
   (`/stats/api/v1/user/config` etc.), addressing remote configuration, friend
   invites and duels possibly via an own test server OR an offline shim (documented).
8. CI (GitHub Actions, build-debug, artifact + release) — same shape as the
   swipewipe-rebuild workflow.
9. BUILD_REPORT doc here; keep `FEATURE_CHECKLIST.md` marked consistently.
