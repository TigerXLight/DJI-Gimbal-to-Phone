# GitHub release preparation

This document prepares the repository for public GitHub release. It does not
publish the repository or claim hardware acceptance.

## Suggested repository metadata

- Repository name: `Android-DJI-OM-Bridge`
- Installed app name: `DJI Gimbal Pairing`
- Short description: `A fail-closed Android bridge for DJI OM gimbals and configured camera-app controls.`
- Suggested topics: `android`, `bluetooth-le`, `ble`, `dji`, `osmo-mobile`, `gimbal`, `camera-control`, `accessibility-service`
- About text: `Unofficial Android companion for selected DJI OM gimbals; Sony Video Pro Record is the only hardware-tested camera path in the current preview.`

The About text must not imply universal DJI, Android camera, zoom, or side-wheel
support. Side-wheel press is decoded but not mapped to Sony lens switching;
side-wheel rotation is `UNSUPPORTED_NO_SAFE_PATH`.

## Current preview release preparation

Suggested title: `DJI Gimbal Pairing 0.2.0 Consumer Preview`

Suggested preview notes:

- Consumer-first Find DJI Gimbal and remembered association flow.
- Automatic/presence-assisted reconnect where Android permits it.
- Per-camera-app presets and foreground package selection.
- Sony Video Pro Record START/STOP remains the only hardware-tested camera
  action.
- OM8 passive Record decoding and raw diagnostics remain available.
- Side-wheel rotation/Zoom is unavailable; no speculative FFF5 replay is used.
- Current version remains `0.2.0-consumer-preview` (versionCode `2`); this is not
  `1.0.0`.

The current APK is prepared for offline inspection and the primary freeze. The
project-owned code licence is MIT; publication uses a sanitized mirror so the
canonical development email is not exposed.

## Unused 1.0 template

Release title: `DJI Gimbal Pairing 1.0.0`

Use only after the final acceptance procedure passes, the project licence is
selected, the public history/archive is sanitized, and the README/support
matrix accurately reflects the tested device and camera scope. The unresolved
side-wheel rotation does not block 1.0 by itself if it remains clearly marked
unavailable and no speculative implementation is advertised.

## Publication checklist

1. Confirm the MIT `LICENSE` and third-party notices are present.
2. Publish a sanitized Git history rather than exposing the canonical personal
   author email.
3. Re-run `docs/PUBLIC_RELEASE_SANITISATION.md` checks from a clean clone.
4. Run `powershell -ExecutionPolicy Bypass -File .\final_consumer_acceptance.ps1`
   on the Xperia/OM8 setup, passing `-AdbPath` if adb is not on PATH.
5. Update the preview/final freeze and manifest only after acceptance.
6. Push the sanitized mirror to `TigerXLight/DJI-Gimbal-to-Phone`, verify
   remote HEAD/tree equality, then create the matching release.
