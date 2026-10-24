# Roadmap

## Phase 1 — COMPLETE / PASS

- Standalone repository and module boundaries.
- Extracted proven OM8 receive/recovery/parser core.
- Generic controller event and camera adapter contracts.
- Sony Video Pro reference adapter.
- Software regression/build surface and conservative documentation.
- Real Xperia 1 IV + OM8 + Sony Video Pro hardware regression.
- Complete-startup distinction documented: UI Start Bridge versus listener-only debug broadcast.

## Later phases

1. Freeze/tag the Phase 1 generic bridge baseline.
2. Approve a separate Phase 2 specification.
3. Add a generic Accessibility camera-profile engine.
4. Add a profile-learning workflow.
5. Test a non-Sony camera application.
6. Investigate additional DJI OM model drivers separately.
7. Evaluate CompanionDeviceManager for association/presence without replacing the proven GATT driver.
8. Research a secure cooperative camera API using intents, a bound service/AIDL, or a client SDK. Do not export unauthenticated recording control.

Insta360 synchronization is outside this repository and must not be added as a shortcut to camera-adapter work.

