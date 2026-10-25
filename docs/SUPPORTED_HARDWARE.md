# Supported hardware and software matrix

Only evidence-backed entries are listed as tested. The final DJI phase preserves
the real-device regression and distinguishes passive input decoding from camera
action support.

The current installed build is `0.2.0-consumer-preview` / versionCode `2`.
Historical OM8/Sony hardware validation is preserved, but consumer acceptance
of the productized UI, notification, system Bluetooth presentation, recovery,
and repeated timing sequence is still pending.

Phase 2A adds profile architecture only; it does not add a new camera-app support
claim. Sony Video Pro remains the only hardware-tested camera application.

| Category | Item | Driver/adapter | Protocol or behavior verified | Hardware status |
| --- | --- | --- | --- | --- |
| Gimbal | DJI Osmo Mobile 8 / OM8 | om8-driver | Direct BLE, FFF0/FFF4 DUML status, CCCD setup, HIGH request, recovery, automatic lifecycle driver path | `HARDWARE_TESTED` — Phase 3A and Phase 3B PASS |
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

Phase 3B background association and automatic GATT wake-up are hardware-tested
PASS on the target Xperia/OM8 setup. The driver still owns the actual GATT
connection and FFF4 subscription; Android association/pairing alone is not
camera control.

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

## OM8 control matrix

See [`OM8_CONTROL_SUPPORT.md`](OM8_CONTROL_SUPPORT.md) for the detailed matrix.
Record, joystick, M, trigger raw states, switch, and side-wheel press have
evidence-backed passive `04/57` representations. Only Record is mapped to the
hardware-tested Sony Video Pro action. Side-wheel **rotation** is explicitly
`UNSUPPORTED_NO_SAFE_PATH` because no safe passive or Mimo-derived encoding was
established.

## Consumer-preview status

The installed app name is **DJI Gimbal Bridge**. The consumer-preview UI
supports remembered `OM8-*` association, automatic/presence-assisted reconnect,
per-camera profile rows, per-profile mappings, and a collapsed diagnostic area.
These UI changes are not themselves a new hardware validation claim. The
current product version remains `0.2.0-consumer-preview` until the complete
acceptance sequence in `docs/TESTING.md` is run on the Xperia 1 IV + OM8 setup.

