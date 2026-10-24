# Dependencies and provenance

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
licence, proposed target, and reuse classification:

`T:\References\to go\Media\tools\Android-DJI-OM-Bridge-Research\provenance\ADAPTATION_LEDGER.json`
