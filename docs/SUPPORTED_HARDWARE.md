# Supported hardware and software matrix

Only evidence-backed entries are listed as tested. “Source verified” is not the same as a completed new-repository hardware regression.

| Category | Item | Driver/adapter | Protocol or behavior verified | Hardware status |
| --- | --- | --- | --- | --- |
| Gimbal | DJI Osmo Mobile 8 / OM8 | om8-driver | FFF0/FFF4 DUML Record status and recovery | Proven in frozen reference; new-repository regression pending |
| Phone | Sony Xperia 1 IV | Standard Android BLE + Accessibility APIs | Direct BLE and Sony Video Pro path | Proven in frozen reference; new-repository regression pending |
| Camera app | Sony Video Pro jp.co.sony.mc.videopro | sony-videopro | Exact recording node, ACTION_CLICK, rec_status verification | Proven in frozen reference; new-repository regression pending |

Not supported/verified claims:

- OM3/OM4/OM5/OM6/OM7 or other DJI models.
- Samsung, Pixel, Xiaomi, or other phone brands.
- Universal Android camera-app control.
- Insta360 Mic Pro or any audio recorder.

