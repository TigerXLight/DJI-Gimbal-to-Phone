# Project state

## Implemented

- Standalone Gradle Android repository with app, bridge-core, om8-driver, and camera-adapters:sony-videopro modules.
- Generic GimbalDriver, ControllerEvent, CameraAdapter, and BridgeController boundaries.
- Direct OM8 scan/GATT connection using the proven FFF0/FFF4 receive path.
- DUML validation and Record rising/falling-edge normalization.
- Main-looper GATT callback path, stale-GATT checks, retry/backoff, watchdog, CCCD readiness, Bluetooth recovery, and one HIGH-priority request per new GATT connection.
- Sony Video Pro prepared-node/safety-snapshot Accessibility adapter extracted from the frozen reference.
- Debug-only protected ADB start/stop and Sony diagnostic receivers.

## Software status

Build and Gradle unit-test tasks passed during extraction. Pure source self-tests are present for core routing, OM8 parser/edge behavior, foreground-service permission decisions, and Sony key-safety decisions.

## Hardware status

**PENDING USER TEST for this new repository.** The same hardware path is proven in the frozen private project on Sony Xperia 1 IV + OM8 + Sony Video Pro, but this repository must be installed and regression-tested independently.

## Claims and limitations

- Supported/proven source device: OM8 only.
- Current evidenced phone: Xperia 1 IV only.
- Current adapter: Sony Video Pro only.
- No universal DJI, Android-phone, or camera-app support is claimed.
- No Insta360 implementation is present.

## Source boundary

Extraction source is FROZEN_REFERENCE\OM8_SONY_2026-10-24. The private project and frozen reference remain separate and unchanged.

