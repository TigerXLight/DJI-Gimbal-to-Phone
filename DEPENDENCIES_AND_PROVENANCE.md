# Dependencies and provenance

## Project licence

Project-owned/original and independently implemented production code is licensed
under the MIT License in the repository-root `LICENSE`. This does not relicense
the separate attributed OpenGimbal portion or any future file-specific component.

No Apache-2.0 source is incorporated. No GPL/copyleft or no-licence expressive
source is incorporated. Research-only repositories contribute protocol and
interoperability evidence only. DJI proprietary/EULA material was not copied
into production.

## Phase 3A direct/partial adaptation

| Upstream repository | Commit | Upstream path/class/method | Local target | Adaptation | Licence | Validation |
|---|---|---|---|---|---|---|
| [ItzDFPlayer/OpenGimbal](https://github.com/ItzDFPlayer/OpenGimbal) | `6b7f403265d4ef96c9238ecaeed12bf5ce9f711c` | `app/src/main/java/com/itzdfplayer/opengimbal/mapping/GimbalMapping.kt`: `GimbalTrigger`, `GimbalAction`; `MappingStore.kt`: mapping persistence boundary | `bridge-core/MappingStore.java`, `MappingEngine.java`, `ControllerTrigger.java`, `CameraAction.java` | MODIFIED/PARTIAL | MIT; Copyright (c) 2026 Denys | Pure JVM tests pass; Sony hardware regression remains the reference |

Local modifications are substantive: controller-neutral names replace
OpenGimbal's HJ360-specific names, `RECORD_PRESS` defaults to
`RECORD_TOGGLE`, `RECORD_RELEASE` defaults to `NONE`, continuous axis capacity
is represented without inventing OM8 packets, and persistence is strict,
versioned JSON with fail-closed defaults. The implementation does not reuse
OpenGimbal BLE code or camera screenshot/gesture fallbacks.

## Phase 3B lifecycle adaptation

| Upstream repository | Commit | Upstream path/class/method | Local target | Adaptation | Licence | Validation |
|---|---|---|---|---|---|---|
| [ItzDFPlayer/OpenGimbal](https://github.com/ItzDFPlayer/OpenGimbal) | `6b7f403265d4ef96c9238ecaeed12bf5ce9f711c` | `app/src/main/java/com/itzdfplayer/opengimbal/gimbal/AutoConnectPolicy.kt`: `AutoConnectPolicy.shouldConnect`; `GimbalManager.kt`: remembered-device/reconnect lifecycle | `bridge-core/AutoConnectPolicy.java`, `bridge-core/ReconnectBackoff.java`, `bridge-core/AssociatedDevice.java`, `app/CompanionAssociationCoordinator.java`, `app/AutoConnectServiceStarter.java`, `om8-driver/Om8BleListenerService.java` | FUNCTIONAL_REIMPLEMENTATION / MODIFIED | MIT; Copyright (c) 2026 Denys | JVM policy tests added; OM8 transport and Sony hardware path remain local and unchanged |

The local implementation uses Android CompanionDeviceManager/CompanionDeviceService
for user-approved presence wake-up and the existing connected-device foreground
GATT service for the actual FFF0/FFF4 session. It deliberately does not import
OpenGimbal's HJ360 protocol, UUIDs, screenshot fallback, coordinate control, or
gesture fallback. The local retry schedule is bounded and preserves the existing
OM8 recovery state machine.

## Current foundation additions

| Local path | Origin | Provenance/licence | Validation |
|---|---|---|---|
| `bridge-core/GimbalProfile.java`, `GimbalProfileRegistry.java`, `om8-driver/Om8GimbalProfile.java` | Original data boundary; informed by protocol comparison | No upstream source copied; OM8 UUID values come from project hardware evidence | JVM registry test; no new GATT writes |
| `bridge-core/ControllerGesturePolicy.java`, added event/trigger enum capacity | Original pure model, informed by OpenGimbal `TriggerDecoder` | OpenGimbal MIT is a design donor; no source copied | JVM policy test; OM8 parser unchanged |
| `app/AndroidMappingStore.java`, `MainActivity.java` mapping/diagnostic controls | Local Android integration | No upstream source copied | Gradle compile; profile-scoped JSON remains strict |
| `bridge-core/PixelCameraProfileEvidence.java` | Local evidence record from `video-boost-ao` `Selectors.kt` | MIT, commit `0205b761dca8b033b755ac18421f25ad1ef2d426`; no service copied | Hardware unverified; no record action enabled |
| `bridge-core/SamsungCameraProfileEvidence.java` | Local safety classification | SamsungCamera-Research has no licence; no code copied | Fail-closed status only |

## Existing research-only references

The research workspace retains the broader upstream catalogue and exact commits
in its manifest. They are not new production dependencies in Phase 3A:

- `video-boost-ao` MIT: selector/label/state ideas remain a future Pixel adapter.
- `osmo-mobile-ble` MIT: model-specific protocol reference; not used by OM8.
- `CameraRemote`, `volume-button-mapper`, `SamsungCamera-Research`, and other
  no-license projects: research-reference only.
- `OpenCamera`, `osmobile`, and `Volume-Power-App`: GPL/behavioral research only;
  no source was incorporated.

See the synchronized research ledger for every repository, author, commit,
licence, proposed target, and reuse classification in the separate research
workspace at `provenance/ADAPTATION_LEDGER.json`. The research workspace is
not required to build or run this repository.

## Final DJI UXSDK audit

| Repository | Exact commit | Audited source | Local target | Adaptation | Licence | Validation |
|---|---|---|---|---|---|---|
| `dji-sdk/Mobile-UXSDK-Android` | `6a28754964a7517a0602dfda0bad09adc938489a` | `sample/app/.../CompleteWidgetActivity.java`, `CustomizedWidgetsActivity.java`, `LICENSE.txt` | none | RESEARCH_ONLY | DJI UX SDK EULA; sample MIT | No OM8 wheel origin; no source copied |
| `dji-sdk/Mobile-UXSDK-Beta-Android` | `ed5ca45d118efbada6716f372b96dc5fe11e0620` | `ManualFocusModel.java`, `ManualFocusWidget.java`, `FocusModeWidgetModel.java`, `FPVInteractionWidgetModel.java`, `LICENSE.txt` | none | RESEARCH_ONLY | MIT | Focus/touch SDK command paths only; no OM8 wheel origin; no source copied |

The complete audit, including URL, local path, branch, source methods, and the
Mavic 2 Zoom versus Osmo Mobile distinction, is in
`Android-DJI-OM-Bridge-Research/reports/27_DJI_UXSDK_FINAL_SOURCE_AUDIT.md`.
