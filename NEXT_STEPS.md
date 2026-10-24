# Next steps

1. Keep the normal UI Start Bridge path as the documented complete-startup path; treat the debug OM8 broadcast as listener-only.
2. Phase 2A camera-profile foundation is implemented: one built-in Sony profile,
   versioned data validation, registry lookup, and bridge readiness state.
3. Use the Phase 2B Learn Camera workflow only for deliberate discovery and review;
   do not treat a learned profile as hardware support without evidence.
4. Test a learned non-Sony camera app only after explicit user review and separate
   hardware evidence. Recording-state verification remains a separate question.
5. Expand compatibility claims only after evidence and a dedicated adapter review.
6. Consider later learning/profile lifecycle work, including stronger persistence
   policy and adapter validation, without adding arbitrary control fallbacks.
7. Investigate additional DJI OM model drivers separately.
8. Evaluate CompanionDeviceManager/background presence while retaining the direct GATT driver.

9. Re-test the corrected Learn Camera selector on the Xperia 1 IV and confirm that
   Photography Pro (`com.sonymobile.photopro`) and Video Pro
   (`jp.co.sony.mc.videopro`) are listed. Then perform the first deliberate
   Photography Pro candidate capture; do not call it supported until that hardware
   test passes.
10. Investigate why the ordinary-app Photography Pro CAMERA_BUTTON probe is
    `TESTED_NEGATIVE` even though shell `KEYCODE_CAMERA` captured a photograph.
    Capture a physical/shell/ordinary-broadcast A/B trace if device tooling is
    available. Do not map OM8 input or create a Photography Pro profile from
    this result.
11. Keep automatic bridge startup/reconnection, remembered gimbal/profile, Bluetooth
    on/off recovery, reboot behavior, and optional CompanionDeviceManager evaluation
    as a later phase.

12. Hardware-regress Phase 3A with exactly two OM8 presses: Sony START then STOP.
    Confirm one action per Record rising edge, release no-op, REC/Stby verification,
    prepared-node/safety-snapshot use, and delayed re-arm. Do not call this a
    Phase 3A hardware PASS until that test is recorded.
13. Add later camera-specific adapters only with selector/state evidence and an
    explicit validation tier. Keep `CAPTURE_PHOTO` semantic only until a safe
    ordinary-app backend is proven for a target camera.
14. Treat background auto-connect, CompanionDeviceManager, OM8 slider decoding,
    and additional camera adapters as separate future phases.

Do not start Insta360 work, broader camera compatibility claims, generic arbitrary
tapping, gimbal movement, or a public release from this baseline.

