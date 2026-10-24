# Project state

## Implemented

- Standalone Gradle Android repository with app, bridge-core, om8-driver, and camera-adapters:sony-videopro modules.
- Generic GimbalDriver, ControllerEvent, CameraAdapter, and BridgeController boundaries.
- Direct OM8 scan/GATT connection using the proven FFF0/FFF4 receive path.
- DUML validation and Record rising/falling-edge normalization.
- Main-looper GATT callback path, stale-GATT checks, retry/backoff, watchdog, CCCD readiness, Bluetooth recovery, and one HIGH-priority request per new GATT connection.
- Sony Video Pro prepared-node/safety-snapshot Accessibility adapter extracted from the frozen reference.
- Debug-only protected ADB start/stop and Sony diagnostic receivers.
- Phase 2A camera-profile foundation: immutable `CameraProfile`, registry, versioned
  JSON codec/validation, deterministic metadata matcher, and bridge readiness model.
- Exactly one built-in profile: Sony Video Pro, selected by default by the app.

## Software status

**BUILD/UNIT TEST: PASS.** Gradle test tasks and the Phase 1 source self-tests pass. Tests cover core routing, OM8 parser/edge behavior, foreground-service permission decisions, and Sony key-safety decisions.

## Hardware status

**HARDWARE REGRESSION: PASS.** Tested on Sony Xperia 1 IV + DJI Osmo Mobile 8 + Sony Video Pro.

Verified in the new repository:

- Direct OM8 BLE, FFF0/FFF4 discovery, CCCD enable, and HIGH-priority request submission.
- Physical Record down/up events with release duplicate protection.
- Generic event routing through BridgeController and SonyVideoProCameraAdapter.
- Sony foreground rejection, START -> REC, and STOP -> Stby.
- Prepared-node and safety-snapshot paths, with successful actions reporting mainThreadQueueNanos=0.
- STOP delayed re-arm, successful on attempt 4 in the tested cycle.

The Android release/version of the test handset was not recorded in the available source/frozen evidence. Build target SDK 35 must not be interpreted as the handset Android version.

The debug broadcast com.androiddjibridge.OM8_LISTENER_START starts the OM8 driver/listener diagnostic path only. OM8 LISTENER READY means Gimbal READY, not necessarily Bridge READY. The normal UI Start Bridge path is the complete startup path with the selected Sony adapter available.

Phase 2A does not add the Learn Camera wizard or a generic replacement for the Sony
adapter. The profile contains camera metadata; the adapter remains responsible for
Sony-specific Accessibility execution and safety.

Phase 2B safe Learn Camera workflow is implemented: explicit package selection,
scoped clicked-source capture, review/confirmation, app-private learned-profile
persistence, and a conservative generic learned-profile test adapter.

Phase 2B does not replace the Sony adapter. The built-in Sony profile continues
through `SonyVideoProCameraAdapter` and `VideoProKeyAccessibilityService`, including
prepared-node, safety-snapshot, verification, and delayed re-arm behavior. Learned
profiles are `LEARNED`/unverified, require explicit confirmation before persistence,
and do not imply that recording-state verification was learned. The generic adapter
is a conservative test path only.

Phase 2A hardware regression is PASS on Sony Xperia 1 IV + DJI Osmo Mobile 8 + Sony
Video Pro. The tested START sample was callback-to-detection 0.108854 ms,
callback-to-click 4.994531 ms, bridge-request-to-click 0.354948 ms, verified REC,
with release duplicate protection and re-arm attempt 1. The tested STOP sample was
callback-to-detection 0.065208 ms, callback-to-click 0.252031 ms,
bridge-request-to-click 0.104427 ms, verified Stby, with re-arm attempt 4. The
4.994531 ms START value is recorded as an observed transient sample, not an
architecture regression.

## Claims and limitations

- Supported/proven source device: OM8 only.
- Current evidenced phone: Xperia 1 IV only.
- Current adapter: Sony Video Pro only.
- No universal DJI, Android-phone, or camera-app support is claimed.
- No Insta360 implementation is present.

## Source boundary

Extraction source is FROZEN_REFERENCE\OM8_SONY_2026-10-24. The private project and frozen reference remain separate and unchanged.

