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
- Phase 3A mapping foundation: semantic `CameraAction`, normalized
  `ControllerTrigger`, pure `MappingEngine`, strict versioned mapping JSON, and
  app-private mapping persistence. The default is Record press -> Record toggle;
  Record release remains a no-op.

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

Phase 3A does not change the OM8 parser, GATT lifecycle, Sony prepared-node or
safety-snapshot path, or Sony verification/re-arm behavior. `BridgeController`
now resolves a `ControllerEvent` to a `CameraAction` before invoking an adapter;
Sony accepts only `RECORD_TOGGLE` and still owns the complete specialized action
path. Continuous-axis action capacity is modelled only; no unmeasured OM8 slider
packet is decoded.

## Phase 3A hardware regression — PASS

Tested on Sony Xperia 1 IV + DJI Osmo Mobile 8 + Sony Video Pro. The two-press
regression confirmed one mapped action per Record rising edge, no release duplicate,
prepared-node and safety-snapshot fast paths, main-looper direct dispatch, REC/Stby
verification, and delayed re-arm.

| Press | Callback -> detection | Callback -> Sony click | Bridge request -> Sony click | Verified | Re-arm |
| --- | ---: | ---: | ---: | --- | --- |
| START, sequence 1 | 0.335052 ms | 2.489375 ms | 0.265834 ms | REC | attempt 1 |
| STOP, sequence 2 | 0.048646 ms | 0.151719 ms | 0.035364 ms | Stby | attempt 4 |

Both presses used the prepared-node and safety-snapshot fast paths, with
`preClickRecStatusQuery=false`, `gattCallbackOnMainLooper=true`,
`sonyBridgeAlreadyOnMainLooper=true`, and `mainDispatchSkipped=true`.

Phase 2A hardware regression is PASS on Sony Xperia 1 IV + DJI Osmo Mobile 8 + Sony
Video Pro. The tested START sample was callback-to-detection 0.108854 ms,
callback-to-click 4.994531 ms, bridge-request-to-click 0.354948 ms, verified REC,
with release duplicate protection and re-arm attempt 1. The tested STOP sample was
callback-to-detection 0.065208 ms, callback-to-click 0.252031 ms,
bridge-request-to-click 0.104427 ms, verified Stby, with re-arm attempt 4. The
4.994531 ms START value is recorded as an observed transient sample, not an
architecture regression.

## Phase 2B selector correction

The first real-device Learn Camera test exposed an application-discovery defect: the
selector showed only Game enhancer, Play store, and Support even though Photography
Pro was installed and Android reported `com.sonymobile.photopro/.MultiWindowVideoActivity`
for `VIDEO_CAPTURE` and `.MultiWindowActivity` for `IMAGE_CAPTURE`. The cause was the
old selector's single `ACTION_MAIN`/`CATEGORY_LAUNCHER` query, which did not enumerate
camera-only capture resolvers.

The selector now merges launchable, video-capture, image-capture, and built-in-profile
package candidates, deduplicates by package, and filters only explicit infrastructure.
System/preinstalled status is not a rejection rule. Targeted manifest package
visibility is used; `QUERY_ALL_PACKAGES` is not requested. The built-in Sony Video Pro
profile remains protected from learned-profile replacement. This fix has not yet
claimed that Photography Pro control works; the next action is a real-device selector
and Learn Camera capture retest.

## Photography Pro capability probe

The Xperia evidence shows that Photography Pro (`com.sonymobile.photopro`) has
no static `CAMERA_BUTTON` receiver, but registers a live dynamic
`android.intent.action.CAMERA_BUTTON` receiver while running. The debug build
therefore contains a temporary, DUMP-protected probe only. It is package-scoped
to Photography Pro and sends exactly one public-API `ACTION_CAMERA_BUTTON`
DOWN/UP pair carrying `EXTRA_KEY_EVENT`/`KEYCODE_CAMERA`, after the existing
AccessibilityService verifies the target foreground package, interactive
screen, and unlocked keyguard. It is not connected to OM8 input, the profile
schema, or the Sony Video Pro path, and it does not use shell/input injection,
hidden APIs, coordinates, or retries.

The probe's `sendBroadcast()` returned normally but the real-device test
captured no photograph. The ordinary-app CAMERA_BUTTON backend is therefore
`TESTED_NEGATIVE`. In contrast, `adb shell input keyevent KEYCODE_CAMERA`
captured a photograph; that is a privileged shell/input path and is not an
ordinary-app capability. Photography Pro itself remains an open investigation,
not an unsupported-app conclusion. No permanent profile or OM8 mapping was
added.

## Future normal-use direction

The later product direction is setup once, remembered gimbal/profile, automatic
bridge startup/reconnection where Android permits, and direct opening/use of the
selected camera application. Pairing and bridge GATT operation remain separate:
pairing alone does not subscribe the bridge to controller notifications. Automatic
background/foreground-service lifecycle, Bluetooth recovery, reboot behavior, and
Phase 3B now implements the first CompanionDeviceManager/CompanionDeviceService
wake-up path and retains the existing connected-device foreground GATT service.
Broader reboot/process-death behavior remains later lifecycle validation.

## Claims and limitations

- Supported/proven source device: OM8 only.
- Current evidenced phone: Xperia 1 IV only.
- Current adapter: Sony Video Pro only.
- No universal DJI, Android-phone, or camera-app support is claimed.
- No Insta360 implementation is present.
- Phase 3B implements initial background auto-start/presence wake-up. Slider
  reverse engineering, Pixel/Samsung adapters, and Photography Pro control remain
  out of scope.

## Source boundary

Extraction source is FROZEN_REFERENCE\OM8_SONY_2026-10-24. The private project and frozen reference remain separate and unchanged.

Phase 3A provenance is recorded in `LICENSE_MAP.md`,
`DEPENDENCIES_AND_PROVENANCE.md`, and `THIRD_PARTY_NOTICES.md`. The only direct
upstream adaptation in this phase is the MIT OpenGimbal mapping boundary; the
OM8 wire path remains local and hardware-proven.

## Phase 3B background lifecycle

Phase 3B adds a user-approved Android Companion Device association and a
system-bound presence callback. Presence starts the existing connected-device
foreground GATT service through an explicit internal action; it does not open a
second GATT connection or replace the proven OM8 driver. The association filter
uses the `OM8-*` name family, while FFF0/FFF4 discovery, CCCD enablement,
connection-priority request, and `OM8 LISTENER READY` remain the actual transport
readiness checks.

The association record is stored in app-private preferences. The system
association is authoritative; a last-observed Bluetooth address is only a
legacy presence-observation hint and is never hard-coded or accepted as proof
of device identity. The default policy connects when the associated gimbal is
available, even before a camera is foreground. Existing bounded retries,
Bluetooth on/off handling, watchdog, stale-GATT guards, and manual Start Bridge
remain available.

Phase 3B hardware status is **PASS**. The requested association, background
wake-up, power-cycle, Gimbal READY, and Sony START/STOP regression completed on
the target hardware. The raw-diagnostics UI/capture fix is independent of that
transport result.

## Final DJI phase closeout

The OM8 passive decoder exposes the verified `04/57` fields: signed LE joystick
X/Y, M state `0x03`, observed rear-trigger states `0x11`, `0x21`, `0x61`, and
`0x71`, switch single/double `0x20`/`0x40`, side-wheel press bit `0x01`, and
Record bit `0x08`. Joystick interpretation is deadzone/state-transition based;
the parser does not claim more precise semantics for `0x61`/`0x71`. Only the
Record rising edge is connected to the Sony action path.

Side-wheel rotation is **UNSUPPORTED_NO_SAFE_PATH** for production decoding. It
works in DJI Mimo, but repeated passive PC captures, Mimo-owned HCI/ATT channel
analysis, corrected custom-notify timing, USB-C enumeration, and the final DJI
UXSDK source audit found no safe external encoding or justified FFF5 activation
sequence. Side-wheel press remains decoded and is not conflated with rotation.

The final UXSDK audit is recorded in the research workspace as
`reports/27_DJI_UXSDK_FINAL_SOURCE_AUDIT.md`. The UXSDK repositories provide
camera-command/UI evidence such as lens-scoped focus-ring and touch-focus SDK
calls, but no physical OM8 wheel origin. They are not production dependencies.

The post-Phase-3B integration foundation keeps mappings profile-scoped and adds
the explicit Edit Control Mappings and OM8 Raw Diagnostics controls. The
normalized event model reserves press/hold/multi-press and continuous-axis forms
without decoding any unmeasured OM8 packet. A data-only GimbalProfile registry
describes OM8 transport metadata; it does not add FFF5 writes.

Pixel selector evidence is recorded as IMPLEMENTED_HARDWARE_UNVERIFIED and is not
yet a safe record-control adapter. Samsung stock camera remains
UNSUPPORTED_NO_SAFE_PATH absent a stable ordinary-app contract. Photography Pro's
ordinary-app ACTION_CAMERA_BUTTON probe is TESTED_NEGATIVE. See
docs/UPSTREAM_INTEGRATION.md.

## Consumer productization preview

The installed application name is now **DJI Gimbal Bridge**. The main screen
offers a user-facing **Find DJI Gimbal** flow, remembered connection status,
separate **Disconnect** and **Forget DJI Gimbal** actions, per-camera profile
rows with **Mappings**, and a collapsed **Advanced / Diagnostics** section.
Raw protocol terms are not shown on the ordinary setup surface.

Foreground package changes from the existing Accessibility service select the
matching configured camera profile and its app-private mapping automatically.
Unknown/unconfigured packages clear the selected adapter and fail closed. The
existing Sony Video Pro specialized adapter remains the only adapter capable of
the hardware-tested Record action.

The current productization build is `0.2.0-consumer-preview`, versionCode `2`.
Automated tests/build are passing, but the full product-level Xperia 1 IV + OM8
acceptance matrix has not yet been rerun on this UI/lifecycle build. The release
gate also still requires an explicit project licence and public-file
sanitization; no 1.0 or public GitHub claim is made yet.

