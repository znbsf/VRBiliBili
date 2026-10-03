# VRBiliBili client

Vendored from https://github.com/bggRGjQaUbCoE/PiliPlus at
`c102a6115c7ac040f6a0c6a1653944b81b82dcb4`. Original source and GPL license
are retained. This directory is the actual Flutter/Android application.

Quest additions use a distinct application ID (`io.github.vrbilibili.quest`),
and an in-process native Meta Spatial SDK activity. PiliPlus owns discovery,
accounts and playback resolution; the spatial activity owns the active decoder
while visible. Signed media URLs stay in memory and are never put in Intent
extras, preferences, diagnostics or source control. Returning transfers position
and leaves playback paused. No website wrapping is involved.

The project requires Flutter 3.47.5 with the upstream framework patches from
`lib/scripts/` in an isolated SDK. Do not run upstream `patch.ps1` blindly: it
changes global Git identity and resets its Flutter checkout.

Pinned local dependency overrides:
- `vendor/material_ui`: pub.dev material_ui 1.4.0 plus the nine matching upstream
  material patches (modal_barrier_material, navigation_drawer, popup_menu, fab,
  text_field, scaffold, refresh_indicator, tabs, bottom_sheet_android).
- `vendor/webview`: `flutter_inappwebview_android` from the upstream-pinned fork
  commit `0bfa46dfff87f0d9e9d5e13cbd5c4a7c7310f8c9`; a short local path avoids
  Windows checkout truncation. Existing dependency licenses remain in each vendor directory.
- media-kit stays locked to upstream fork commit
  `73771ec38176be2d984a3049c28177bce23b54a0`.

Android uses AGP 8.11.1, Kotlin 2.2.21, Gradle 8.14.5 and Java 17. Meta Spatial
SDK runtime 0.14.0 and Media3 1.5.1 are application dependencies. The Meta Gradle
code-generation plugin is unnecessary because this app registers runtime panels.
