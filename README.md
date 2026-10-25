# DJI Gimbal Pairing

Unofficial Android companion for using supported DJI Osmo Mobile physical controls with supported camera apps.

**Current release:** `v0.2.0-consumer-preview`

## Download and install

1. Open the [Releases](https://github.com/TigerXLight/DJI-Gimbal-to-Phone/releases) page.
2. Open the latest **DJI Gimbal Pairing** release.
3. Download the APK named:
   `DJI-Gimbal-Pairing-v0.2.0-consumer-preview.apk`
4. On Android, allow installation from the browser/file manager if prompted.
5. Open **DJI Gimbal Pairing**.
6. Enable the requested Android Accessibility service for camera control.

The APK is attached directly to the GitHub Release; no ZIP extraction is required.

## First-time setup

1. Turn on the DJI gimbal.
2. Open **DJI Gimbal Pairing**.
3. Tap **Find DJI Gimbal**.
4. Select the nearby compatible DJI gimbal once.
5. Add/select a camera app and review its mappings.

The app remembers the gimbal for later reconnects.

## Everyday use

1. Turn on the remembered gimbal.
2. Open a configured camera app.
3. Use the supported physical controls.

For normal use you do **not** need:

- DJI Mimo
- a PC
- ADB
- manual Bluetooth Settings pairing
- a routine “Start Bridge” action

## Hardware-tested setup

The currently verified setup is:

- **DJI Osmo Mobile 8 / OM8**
- **Sony Xperia 1 IV**
- **Sony Video Pro**

Verified physical control:

- OM8 **Record** button → Sony Video Pro **Record / Stop**

The bridge also supports per-camera-app mappings and automatic foreground-camera profile selection.

Other DJI models, Android phones, and camera apps are not automatically claimed as hardware-tested.

## Known limitations

- OM8 side-wheel **rotation / zoom**: `UNSUPPORTED_NO_SAFE_PATH`
- OM8 side-wheel **press** is decoded, but Sony lens switching is not yet safely implemented.
- Additional camera apps need a supported/safe adapter or mapping path.
- Additional DJI models and Android phone brands require separate hardware verification.

## Latency

The optimized steady-state Record path is very low latency.

Bounded 5 START + 5 STOP hardware profile:

- START median: about **0.336 ms**
- STOP median: about **0.341 ms**
- 9/10 samples were below **1 ms**
- one START sample was about **1.378 ms**

These measurements are **FFF4 callback → Android Accessibility ACTION_CLICK**, not physical-button-to-media-start time. This is not a guaranteed sub-1 ms maximum.

## Safety / implementation boundaries

The production bridge:

- uses the direct DJI BLE notification path
- does not hardcode the test-unit MAC address
- does not require ADB at runtime
- does not use coordinate taps, OCR, or MediaProjection for camera control
- does not perform speculative FFF5 application writes
- fails closed when a camera adapter/mapping is not safely available

## Build from source

Windows:

```cmd
gradlew.bat test --no-daemon --console=plain
gradlew.bat assembleDebug --no-daemon --console=plain
```

Debug APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Documentation

More detailed engineering and interoperability documentation is available in:

- `docs/ARCHITECTURE.md`
- `docs/USER_GUIDE.md`
- `docs/SUPPORTED_HARDWARE.md`
- `docs/OM8_PROTOCOL.md`
- `docs/OM8_CONTROL_SUPPORT.md`
- `docs/TESTING.md`
- `docs/SOURCE_PROVENANCE.md`
- `LICENSE_MAP.md`
- `THIRD_PARTY_NOTICES.md`

## Licence

Project-owned code is released under the **MIT License**.

The project includes an MIT-attributed OpenGimbal-derived mapping boundary; required attribution is preserved separately. See `LICENSE_MAP.md`, `DEPENDENCIES_AND_PROVENANCE.md`, and `THIRD_PARTY_NOTICES.md`.

## Disclaimer

Unofficial project. Not affiliated with or endorsed by DJI.

DJI and Osmo are trademarks of their respective owner.
