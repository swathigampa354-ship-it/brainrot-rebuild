# Brainrot Rebuild

A faithful, from-scratch native Android rebuild of the **BrainRot** reels/shorts
self-control app (`com.brainrot.android` v12.2.494), produced from pure static
analysis — see [brainrot-static-analysis](https://github.com/swathigampa354-ship-it/brainrot-static-analysis)
(355 decompiled classes, manifest, redacted resources, manuals).

**No original code baked; no proprietary SDKs bound.** Room schema, network
endpoints and behavior come from the decompiled sources; UI is a representative
behavior clone (not pixel-perfect).

## What it does

- **Reels accessibility watcher** — counts reels as user scrolls in
  Instagram/YouTube/TikTok/Reddit.
- **Usage-stats fallback** — counter when a11y is off.
- **Block overlay** — when the configured daily reels limit is reached
  (`BlockOverlayActivity`, launched by the watcher).
- **Floating counter** (the sway of the app's floating bubbles, GL widget) — in
  progress; the current build ships the solid home-screen **Glance counter widget**.
- **Stats** — today's counters (reels / opens / unlocks / blocks) + per-package splits.
- **Settings** — daily limits, blocking + overlay toggle, usage fallback switch,
  watch list of packages (editable), cloud sync range (user's own server —
  `/stats/api/v1/user/config` and `/sync` DTOs from the original).

## Requires

Kotlin 2.0.21, Compose, Room, Retrofit, WorkManager, Glance. Compose v2024.09.03.
`gradle assembleDebug` — APK lands at `app/build/outputs/apk/debug/`.

## CI (`.github/workflows/build.yml`) shared as with swipewipe-rebuild

`build-<sha>` tags with public APK assets on each push.

## Known limitations

1. **No Firebase/RevenueCat** — those clouds are excluded; the paywall surface is a
   showcase stub, not connected.
2. **Cloud endpoints are user-supplied** — a base URL configured in Settings controls
   whether the app syncs (`user/config`, `user/sync`).
3. **Accessibility service** must be enabled by hand (Settings → Accessibility).
4. **Usage stats** requires the `PACKAGE_USAGE_STATS` special permission — guided flow
   in-app.
5. **Floating bubble** (`SYSTEM_ALERT_WINDOW`) — only overlay/bubble is present;
   advanced (per-app "swipe up") bubble navigation is a documented gap.

## License

MIT — see `LICENSE`.
