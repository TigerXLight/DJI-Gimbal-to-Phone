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
10. Run the temporary Photography Pro CAMERA_BUTTON capability probe while
    Photography Pro is foreground and unlocked. Record whether the ordinary
    package-scoped broadcast produces exactly one photograph; `sendBroadcast()`
    returning normally is not sufficient evidence. Do not map OM8 input or create
    a Photography Pro profile from this experiment.
11. Keep automatic bridge startup/reconnection, remembered gimbal/profile, Bluetooth
    on/off recovery, reboot behavior, and optional CompanionDeviceManager evaluation
    as a later phase.

Do not start Insta360 work, broader camera compatibility claims, generic arbitrary
tapping, gimbal movement, or a public release from this baseline.

