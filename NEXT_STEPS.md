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
8. Hardware validation of the CompanionDeviceManager/background presence path is
   recorded as PASS; retain the direct GATT driver and continue monitoring real
   device recovery.

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
11. Phase 3B association/presence wake-up, power-cycle recovery, Gimbal READY,
    and the existing Sony START/STOP path are recorded as hardware PASS.

12. Phase 3A hardware regression is PASS: two OM8 presses produced Sony START/STOP,
    release no-op, REC/Stby verification, prepared-node/safety-snapshot use, and
    delayed re-arm. Preserve this result as the functional reference.
13. Add later camera-specific adapters only with selector/state evidence and an
    explicit validation tier. Keep `CAPTURE_PHOTO` semantic only until a safe
    ordinary-app backend is proven for a target camera.
14. Treat OM8 slider decoding, additional camera adapters, and controller-mapping UI
    as separate future work after Phase 3B.

15. Evaluate Android 36's newer association-based presence request API when the
    production compile SDK is intentionally raised; the current implementation
    uses the public API 31-35 address-observation path with a safe fallback.

Do not start Insta360 work, broader camera compatibility claims, generic arbitrary
tapping, gimbal movement, or a public release from this baseline.

16. Hardware-validate the Phase 3B association/presence lifecycle separately; do
    not treat OM8 LISTENER READY as complete camera readiness.
17. Hardware-test any future Pixel/Samsung/Photography Pro backend independently.
    Keep Pixel selector evidence hardware-unverified and the ordinary-app
    Photography Pro camera-button path tested negative.
18. The final DJI phase is closed for the current evidence. The parser exposes
    measured OM8 `04/57` fields, while only Record is mapped to Sony. Keep the
    Record edge immediate and do not add speculative transport writes.

19. Do not reopen OM8 side-wheel rotation research unless a new, higher-quality
    external protocol trace or documented public interface appears. Current
    status is `UNSUPPORTED_NO_SAFE_PATH`; side-wheel press remains decoded.

20. The next active hardware project is **Insta360 Mic Pro <-> Mobile
    synchronisation**. Start that work as a separate project and investigate
    both USB data/protocol behavior and Bluetooth/wireless behavior from the
    beginning.

21. Complete the consumer-preview acceptance checklist in `docs/TESTING.md` on
    the Xperia 1 IV + OM8. Record the device/system Bluetooth presentation,
    process/reboot behavior, and five bounded Sony START/STOP timing pairs.
22. If acceptance passes, choose and add the project's explicit open-source
    licence, audit tracked files for private evidence/paths/identifiers, and
    prepare the public GitHub release. Until then keep version
    `0.2.0-consumer-preview` and do not claim 1.0.

