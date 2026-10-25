# Licence map

This repository contains original/independently implemented
Android-DJI-OM-Bridge code plus small attributed adaptations from permissively
licensed upstream implementation. Project-owned code is licensed under the
top-level MIT `LICENSE`. That project licence does not relicense third-party
components.

| Repository portion | Classification | Provenance/licence |
|---|---|---|
| `app/`, `om8-driver/`, Sony adapter, existing `bridge-core/` | Original or independently implemented project code, except where a file-level attribution says otherwise | Project-owned scope; top-level MIT `LICENSE` applies |
| `bridge-core/AssociatedDevice.java`, `AutoConnectPolicy.java`, `ReconnectBackoff.java`, and the Phase 3B lifecycle integration | Local Android integration and pure policy code informed by OpenGimbal's MIT lifecycle ideas; no upstream source file copied | OpenGimbal `6b7f403265d4ef96c9238ecaeed12bf5ce9f711c`, MIT; attribution retained in the central provenance documents |
| `bridge-core/CameraAction.java`, `ControllerTrigger.java`, `MappingDecision.java`, `MappingEngine.java` | Original local semantic model and resolver | No third-party source copied |
| `bridge-core/MappingStore.java` | Modified/partial adaptation of the OpenGimbal mapping-store boundary | ItzDFPlayer/OpenGimbal, commit `6b7f403265d4ef96c9238ecaeed12bf5ce9f711c`, MIT; copyright Denys (2026) |
| `bridge-core/MappingJsonCodec.java`, `app/AndroidMappingStore.java` | Original local versioned persistence implementation using the adapted boundary | Local implementation; no upstream code copied |
| `bridge-core` mapping tests | Original local tests, informed by OpenGimbal's pure mapping-test separation | Local tests |
| `docs/`, root provenance documents | Project documentation and required third-party attribution | Includes the OpenGimbal MIT notice |
| `tests/` | Project protocol vectors and fixtures | Generated/project test material unless individually marked |

## Licence classification summary

- Project-owned/original or independently implemented code: MIT under the
  top-level `LICENSE`.
- Incorporated MIT code: the attributed OpenGimbal mapping/lifecycle boundary;
  its copyright and MIT notice remain in `THIRD_PARTY_NOTICES.md`.
- Incorporated Apache-2.0 code: none identified or incorporated.
- Other licensed incorporated material: none identified or incorporated.
- Research/protocol-evidence-only repositories: documented as provenance only;
  their source licences are not inherited by this project.
- Proprietary/EULA material: DJI UXSDK/Mimo material was studied only and no
  proprietary source, binary, or runtime dependency is incorporated.

The OM8 FFF4 parser, GATT lifecycle, Sony Accessibility hot path, prepared-node
path, safety snapshot, and Sony re-arm implementation remain local production
code. No OpenGimbal HJ360 wire protocol, screenshot fallback, coordinate tap,
gesture fallback, or camera-control implementation was imported.

`GimbalProfile`, `GimbalProfileRegistry`, `ControllerGesturePolicy`, the
profile-scoped Android mapping storage, raw-diagnostic toggle, and Pixel/Samsung
evidence records are original local additions. They do not add third-party
runtime dependencies or executable upstream code.

For the complete candidate ledger, see `provenance/ADAPTATION_LEDGER.json` in
the separate research workspace. The research workspace is not a production
runtime dependency and is not required to build the application.

## Audited but not incorporated

The final DJI UXSDK audit added these research-only provenance entries:

| Repository | Commit | Classification |
|---|---|---|
| `dji-sdk/Mobile-UXSDK-Android` | `6a28754964a7517a0602dfda0bad09adc938489a` | DJI UX SDK EULA / MIT sample boundary; no production code copied |
| `dji-sdk/Mobile-UXSDK-Beta-Android` | `ed5ca45d118efbada6716f372b96dc5fe11e0620` | MIT repository; no production code copied because audited code is aircraft/camera UI/API code, not OM8 transport |

The exact source findings are in research report
`reports/27_DJI_UXSDK_FINAL_SOURCE_AUDIT.md`. These repositories are not
runtime dependencies of Android-DJI-OM-Bridge.
