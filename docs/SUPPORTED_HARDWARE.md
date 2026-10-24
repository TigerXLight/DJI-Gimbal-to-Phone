# Supported hardware and software matrix

Only evidence-backed entries are listed as tested. Phase 3A hardware regression has
passed for the entries marked tested.

Phase 2A adds profile architecture only; it does not add a new camera-app support
claim. Sony Video Pro remains the only hardware-tested camera application.

| Category | Item | Driver/adapter | Protocol or behavior verified | Hardware status |
| --- | --- | --- | --- | --- |
| Gimbal | DJI Osmo Mobile 8 / OM8 | om8-driver | Direct BLE, FFF0/FFF4 DUML Record status, CCCD setup, recovery, automatic lifecycle driver path | Tested — Phase 3A PASS; Phase 3B background lifecycle pending |
| Phone | Sony Xperia 1 IV | Standard Android BLE + Accessibility APIs | Direct BLE and Sony Video Pro path | Tested — Phase 3A PASS |
| Camera app | Sony Video Pro jp.co.sony.mc.videopro | sony-videopro | Exact recording node, ACTION_CLICK, rec_status verification | Tested — Phase 3A PASS |
| Android release/version | Not recorded in the available Phase 1 source/frozen evidence | Android platform | The app build target SDK is not the handset Android release | Not claimed |

Not supported/verified claims:

- OM3/OM4/OM5/OM6/OM7 or other DJI models.
- Samsung, Pixel, Xiaomi, or other phone brands.
- Universal Android camera-app control.
- Insta360 Mic Pro or any audio recorder.

The normal UI Start Bridge control remains a manual/diagnostic complete-bridge path.
Phase 3B adds an association/presence-driven automatic start path; the debug
OM8_LISTENER_START broadcast remains listener-only. OM8 LISTENER READY is Gimbal
READY and does not alone prove camera-adapter availability.

The built-in Sony profile records the already verified package/resource metadata. The
profile's `HARDWARE_TESTED` status applies to the Sony/Xperia/OM8 path documented here,
not to universal Android or camera-app compatibility.

Phase 3B background association and automatic GATT wake-up are implemented but
remain hardware-validation pending. The Phase 3A two-press Sony regression is a
separate PASS and does not prove background wake-up after process recreation or a
power cycle.

Learned profiles are discovery candidates only. `LEARNED`/unverified does not change
this matrix and does not constitute support for the target camera application. A
separate real-device test is required before any learned profile can be described as
hardware-tested.

## Evidence tiers outside the tested matrix

- Pixel Camera (`com.google.android.GoogleCamera`):
  `IMPLEMENTED_HARDWARE_UNVERIFIED` selector evidence only; no safe record action
  is enabled.
- Samsung stock camera (`com.sec.android.app.camera` historical identifier):
  `UNSUPPORTED_NO_SAFE_PATH` for an unlearned built-in backend; learned profiles
  remain possible with explicit user validation.
- Sony Photography Pro: ordinary-app `ACTION_CAMERA_BUTTON` broadcast is
  `TESTED_NEGATIVE`; privileged shell key injection is not a production backend.

