# Roadmap

## Phase 1 — current

- Standalone repository and module boundaries.
- Extracted proven OM8 receive/recovery/parser core.
- Generic controller event and camera adapter contracts.
- Sony Video Pro reference adapter.
- Software regression/build surface and conservative documentation.

## Later phases

1. Complete Xperia + OM8 + Sony Video Pro hardware regression on this repository.
2. Fix extraction regressions only if observed.
3. Freeze/tag the first generic bridge baseline.
4. Add a generic Accessibility camera-profile engine.
5. Add a profile-learning workflow.
6. Test a non-Sony camera application.
7. Investigate additional DJI OM model drivers separately.
8. Evaluate CompanionDeviceManager for association/presence without replacing the proven GATT driver.
9. Research a secure cooperative camera API using intents, a bound service/AIDL, or a client SDK. Do not export unauthenticated recording control.

Insta360 synchronization is outside this repository and must not be added as a shortcut to camera-adapter work.

