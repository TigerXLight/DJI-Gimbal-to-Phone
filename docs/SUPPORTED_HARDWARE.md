# Supported hardware and software matrix

Only evidence-backed entries are listed as tested. Phase 1 hardware regression has passed for the entries marked tested.

Phase 2A adds profile architecture only; it does not add a new camera-app support
claim. Sony Video Pro remains the only hardware-tested camera application.

| Category | Item | Driver/adapter | Protocol or behavior verified | Hardware status |
| --- | --- | --- | --- | --- |
| Gimbal | DJI Osmo Mobile 8 / OM8 | om8-driver | Direct BLE, FFF0/FFF4 DUML Record status, CCCD setup, recovery | Tested — Phase 1 PASS |
| Phone | Sony Xperia 1 IV | Standard Android BLE + Accessibility APIs | Direct BLE and Sony Video Pro path | Tested — Phase 1 PASS |
| Camera app | Sony Video Pro jp.co.sony.mc.videopro | sony-videopro | Exact recording node, ACTION_CLICK, rec_status verification | Tested — Phase 1 PASS |
| Android release/version | Not recorded in the available Phase 1 source/frozen evidence | Android platform | The app build target SDK is not the handset Android release | Not claimed |

Not supported/verified claims:

- OM3/OM4/OM5/OM6/OM7 or other DJI models.
- Samsung, Pixel, Xiaomi, or other phone brands.
- Universal Android camera-app control.
- Insta360 Mic Pro or any audio recorder.

The normal UI Start Bridge control starts the complete bridge. The debug OM8_LISTENER_START broadcast is listener-only; OM8 LISTENER READY is Gimbal READY and does not alone prove camera-adapter availability.

The built-in Sony profile records the already verified package/resource metadata. The
profile's `HARDWARE_TESTED` status applies to the Sony/Xperia/OM8 path documented here,
not to universal Android or camera-app compatibility.

