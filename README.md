# Android DJI OM Bridge

Android DJI OM Bridge is an unofficial Phase 1 foundation for receiving a supported DJI OM controller event over direct BLE and routing it to a selected camera adapter.

## Disclaimer

Unofficial project.
Not affiliated with or endorsed by DJI.
DJI and Osmo are trademarks of their respective owner.

License: not yet selected. Repository should not be publicly released until licensing is decided.

## Phase 1 status

**BUILD/UNIT TEST: PASS**

**HARDWARE REGRESSION: PASS** on Sony Xperia 1 IV + DJI Osmo Mobile 8 + Sony Video Pro.

The extracted driver and Sony Video Pro adapter passed the real-device regression. The OM8 parser, Record rising-edge behavior, recovery state machine, and Sony prepared Accessibility action are carried over from the frozen reference.

Supported/proven source device:

- DJI Osmo Mobile 8 / OM8.

First camera adapter:

- Sony Video Pro, package jp.co.sony.mc.videopro.

Currently evidenced phone:

- Sony Xperia 1 IV.

These entries are not universal compatibility claims. Other DJI OM models, Android phone brands, and camera apps require separate evidence.

## Phase 1 hardware validation

The new repository was tested on the real setup above:

- Direct OM8 BLE connection worked.
- FFF0/FFF4 discovery and FFF4 CCCD notification enable worked.
- CONNECTION_PRIORITY_HIGH submitted successfully once for the connection.
- Physical RECORD_DOWN and RECORD_UP were received.
- Release did not produce a duplicate Sony action.
- Generic ControllerEvent -> BridgeController -> SonyVideoProCameraAdapter routing worked.
- A press outside Sony Video Pro was rejected with OM8_SONY_TRIGGER_REJECTED reason=foreground-package-not-video-pro.
- Sony START verified REC; Sony STOP verified Stby.
- Prepared-node and safety-snapshot fast paths survived extraction.
- Successful actions reported mainThreadQueueNanos=0.
- STOP re-arm succeeded on attempt 4.

Representative measured samples:

| Action | Callback -> detection | Callback -> Sony click | Bridge request -> Sony click | Verified state |
| --- | ---: | ---: | ---: | --- |
| START | 0.064896 ms | 0.366666 ms | 0.224166 ms | REC |
| STOP | 0.045677 ms | 1.522708 ms | 0.117448 ms | Stby |

The 1.522708 ms STOP value is one observed sample/outlier, not a claimed permanent regression.

The Android release/version of the test handset was not recorded in the available Phase 1 source/frozen evidence. The project build target SDK is not a claim about the handset's Android release.

## What it does

~~~text
OM8 direct BLE FFF4 notification
    -> validated DUML status
    -> normalized ControllerEvent.RECORD_DOWN
    -> selected Sony Video Pro adapter
    -> AccessibilityService safety checks
    -> Sony recording ACTION_CLICK
~~~

The OM8 driver has no knowledge of Sony, AccessibilityNodeInfo, Xperia, or Insta360. The Sony adapter has no OM8 packet parser.

DJI Mimo is not required for normal bridge operation after the bridge obtains its own GATT connection. Mimo remains useful for DJI settings, activation where applicable, firmware updates, and maintenance. Mimo can take ownership of the gimbal; close or release it before using the bridge. This application never force-stops or automates Mimo.

## Build and install

From this repository:

~~~cmd
gradlew.bat test --no-daemon --console=plain
gradlew.bat assembleDebug --no-daemon --console=plain
~~~

Install the debug APK with an adb available on PATH:

~~~cmd
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
~~~

Launch the UI:

~~~cmd
adb shell am start -n com.androiddjibridge/com.androiddjibridge.app.MainActivity
~~~

The debug build also has protected, explicit diagnostic receivers. See docs/TESTING.md for exact bridge-start and Logcat commands.

## Startup modes

Use the app UI's **Start Bridge** control for normal operation. It starts the complete bridge with the selected Sony Video Pro adapter available.

The debug broadcast com.androiddjibridge.OM8_LISTENER_START is an OM8-driver/listener diagnostic control. It can start scanning and reach OM8 LISTENER READY, but that is only Gimbal READY; it does not by itself guarantee that the complete camera bridge is initialized. A driver-only start may therefore log BRIDGE_EVENT_REJECTED reason=no-selected-camera-adapter-or-camera-service. Use the UI path for the complete bridge until startup ownership is intentionally redesigned.

## Accessibility requirement

The Sony adapter requires the user to enable the bridge AccessibilityService. It validates the exact Sony foreground package, interactive/keyguard state, active window, prepared REC node, generations, and node predicates before using ACTION_CLICK. rec_status remains the authoritative post-click verification signal. No coordinate tap, synthetic camera key, automatic relaunch, or private Sony API is used.

## Out of scope

- Insta360 Mic Pro control or synchronization.
- Gimbal movement, zoom, focus, or arbitrary DJI DUML writes.
- Generic camera-profile learning.
- Universal camera-app support.
- CompanionDeviceManager replacement for the proven direct GATT driver.
- DJI Mimo process control.

See docs/ROADMAP.md and NEXT_STEPS.md.

