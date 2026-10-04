# VRBiliBili client

Vendored from https://github.com/bggRGjQaUbCoE/PiliPlus at
`c102a6115c7ac040f6a0c6a1653944b81b82dcb4`. Original source and GPL license
are retained. This directory is the actual Flutter/Android application.

Quest uses a distinct application ID (`io.github.vrbilibili.quest`) and a
Flutter large-window layout. Ordinary playback stays in the original media-kit player.
The current implementation also provides an explicit full-screen cinema entry using
Meta Spatial SDK and Media3. The earlier free-placement/multi-window direction stays
out of scope. Authentication and user data stay with the existing client.
See [third-party sources and license boundaries](../../docs/THIRD-PARTY-NOTICES.md).

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

Android uses AGP 8.11.1, Kotlin 2.2.21, Gradle 8.14.5 and Java 17.
The cinema declares Meta Spatial SDK core/toolkit/vr 0.14.0 and AndroidX Media3
ExoPlayer 1.5.1. Their cached binaries and local official samples are not vendored
into Git. Source publication does not establish combined-APK redistribution compatibility.
