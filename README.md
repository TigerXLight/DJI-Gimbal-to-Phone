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
- Broader camera-profile compatibility beyond the scoped Phase 2B learning workflow.
- Universal camera-app support.
- CompanionDeviceManager replacement for the proven direct GATT driver.
- DJI Mimo process control.

See docs/ROADMAP.md and NEXT_STEPS.md.

## Phase 2A camera-profile foundation

Phase 2A adds a small, data-only camera-profile foundation. `CameraProfile` describes
validated camera-application metadata such as the package, recording target, state
nodes, supported capability, provenance, and schema version. `CameraProfileRegistry`
resolves the explicitly selected profile; its only built-in entry is Sony Video Pro.

Profiles are versioned JSON data. They are validated and deterministically serialized;
they cannot contain executable actions, intents, shell commands, or arbitrary
Accessibility operations. A profile is not permission to click an arbitrary app.

The trusted `SonyVideoProCameraAdapter` and
`VideoProKeyAccessibilityService` still own Sony-specific execution and safety logic,
including foreground/window checks, prepared-node and safety-snapshot fast paths,
post-click `rec_status` verification, and delayed re-arm. The profile supplies the
identifiers and capability metadata used by that adapter; it does not replace the
adapter or the proven Phase 1 path.

The architecture is designed to support multiple Android camera applications through
validated profiles/adapters. Sony Video Pro is currently the only hardware-tested
camera application.

The Phase 2B Learn Camera workflow is now implemented as a deliberately scoped
discovery workflow. The user explicitly selects an installed non-system application,
starts Learn Mode, taps that application's control, reviews the Accessibility
metadata, explicitly confirms it, and saves the resulting `LEARNED` profile in the
app-private `files/learned_profiles` directory. A learned profile is unverified and
does not claim recording-state discovery. Its optional test action uses a fresh,
exact metadata match and fails closed on wrong packages, unsafe device state, zero
matches, or ambiguous matches. It never uses coordinates, gestures, OCR, screenshots,
or arbitrary Accessibility actions. Sony continues to use its specialized adapter;
the generic learned adapter is not used for the built-in Sony profile.

The workflow is documented in docs/LEARN_CAMERA.md. It is not a universal camera-app
compatibility claim, and broader compatibility/lifecycle work remains future work.

