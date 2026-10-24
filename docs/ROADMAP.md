# Roadmap

## Phase 1 — COMPLETE / PASS

- Standalone repository and module boundaries.
- Extracted proven OM8 receive/recovery/parser core.
- Generic controller event and camera adapter contracts.
- Sony Video Pro reference adapter.
- Software regression/build surface and conservative documentation.
- Real Xperia 1 IV + OM8 + Sony Video Pro hardware regression.
- Complete-startup distinction documented: UI Start Bridge versus listener-only debug broadcast.

## Phase 2A — COMPLETE / FOUNDATION ONLY

- Immutable `CameraProfile` data model with explicit schema version, provenance, and
  validation status.
- Deterministic JSON serialization/parsing with safe rejection of malformed or
  unsupported profiles.
- Profile registry, metadata matcher, and bridge-level readiness state.
- Exactly one built-in, hardware-tested profile: Sony Video Pro.
- Sony metadata centralized in the profile while the proven Sony adapter/service
  execution path remains in place.

The interactive Learn Camera wizard is not implemented in Phase 2A. No additional
camera application is supported or hardware-tested by this phase.

## Later phases

1. Freeze/tag the Phase 1 generic bridge baseline.
2. Approve a separate Phase 2 specification.
3. Design and approve the interactive Learn Camera workflow.
4. Implement profile learning only after that approval.
5. Test a non-Sony camera application with a separately validated adapter.
6. Investigate additional DJI OM model drivers separately.
7. Evaluate CompanionDeviceManager for association/presence without replacing the proven GATT driver.
8. Research a secure cooperative camera API using intents, a bound service/AIDL, or a client SDK. Do not export unauthenticated recording control.

Insta360 synchronization is outside this repository and must not be added as a shortcut to camera-adapter work.

