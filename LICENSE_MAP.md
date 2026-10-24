# Licence map

This repository contains original Android-DJI-OM-Bridge code plus a small
Phase 3A mapping boundary adapted from permissively licensed upstream design.
No upstream repository was copied wholesale into the production tree.

| Repository portion | Classification | Provenance/licence |
|---|---|---|
| `app/`, `om8-driver/`, Sony adapter, existing `bridge-core/` | Original project implementation, except where a file-level attribution says otherwise | Android-DJI-OM-Bridge authorship; project licence policy applies |
| `bridge-core/AssociatedDevice.java`, `AutoConnectPolicy.java`, `ReconnectBackoff.java`, and the Phase 3B lifecycle integration | Local Android integration and pure policy code informed by OpenGimbal's MIT lifecycle ideas; no upstream source file copied | OpenGimbal `6b7f403265d4ef96c9238ecaeed12bf5ce9f711c`, MIT; attribution retained in the central provenance documents |
| `bridge-core/CameraAction.java`, `ControllerTrigger.java`, `MappingDecision.java`, `MappingEngine.java` | Original local semantic model and resolver | No third-party source copied |
| `bridge-core/MappingStore.java` | Modified/partial adaptation of the OpenGimbal mapping-store boundary | ItzDFPlayer/OpenGimbal, commit `6b7f403265d4ef96c9238ecaeed12bf5ce9f711c`, MIT; copyright Denys (2026) |
| `bridge-core/MappingJsonCodec.java`, `app/AndroidMappingStore.java` | Original local versioned persistence implementation using the adapted boundary | Local implementation; no upstream code copied |
| `bridge-core` mapping tests | Original local tests, informed by OpenGimbal's pure mapping-test separation | Local tests |
| `docs/`, root provenance documents | Project documentation and required third-party attribution | Includes the OpenGimbal MIT notice |
| `tests/` | Project protocol vectors and fixtures | Generated/project test material unless individually marked |

The OM8 FFF4 parser, GATT lifecycle, Sony Accessibility hot path, prepared-node
path, safety snapshot, and Sony re-arm implementation remain local production
code. No OpenGimbal HJ360 wire protocol, screenshot fallback, coordinate tap,
gesture fallback, or camera-control implementation was imported.

For the complete candidate ledger, see
`T:\References\to go\Media\tools\Android-DJI-OM-Bridge-Research\provenance\ADAPTATION_LEDGER.json`.
