# Android DJI OM Bridge

Android DJI OM Bridge is an unofficial Phase 1 foundation for receiving a supported DJI OM controller event over direct BLE and routing it to a selected camera adapter.

## Disclaimer

Unofficial project.
Not affiliated with or endorsed by DJI.
DJI and Osmo are trademarks of their respective owner.

License: not yet selected. Repository should not be publicly released until licensing is decided.

## Phase 1 status

The extracted driver and Sony Video Pro adapter are buildable. The OM8 parser, Record rising-edge behavior, recovery state machine, and Sony prepared Accessibility action are carried over from the frozen reference. Real-device regression from this new repository is still **PENDING USER TEST**.

Supported/proven source device:

- DJI Osmo Mobile 8 / OM8.

First camera adapter:

- Sony Video Pro, package jp.co.sony.mc.videopro.

Currently evidenced phone:

- Sony Xperia 1 IV.

These entries are not universal compatibility claims. Other DJI OM models, Android phone brands, and camera apps require separate evidence.

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

