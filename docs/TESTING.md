# Testing and hardware regression

## Software commands

Run from the repository root:

~~~cmd
gradlew.bat test --no-daemon --console=plain
gradlew.bat assembleDebug --no-daemon --console=plain
~~~

The parser/edge and core-routing self-tests are kept as source-level regression classes. The Android Gradle test tasks must also remain green.

## Phase 2A profile tests

The following source self-tests cover the profile foundation in addition to the
existing Phase 1 tests:

~~~cmd
java -cp "bridge-core\build\classes\java\main;bridge-core\build\classes\java\test" com.androiddjibridge.core.CameraProfileJsonCodecSelfTest
java -cp "bridge-core\build\classes\java\main;bridge-core\build\classes\java\test" com.androiddjibridge.core.BridgeControllerSelfTest
java -cp "camera-adapters\sony-videopro\build\intermediates\javac\debug\compileDebugJavaWithJavac\classes;camera-adapters\sony-videopro\build\intermediates\javac\debugUnitTest\compileDebugUnitTestJavaWithJavac\classes" com.androiddjibridge.sony.SonyVideoProProfileSelfTest
~~~

They verify Sony metadata and registry lookup, valid JSON round-trip and deterministic
serialization, malformed/unsupported profile rejection, safe action/capability
handling, target matching, and the rule that bridge readiness requires a ready gimbal,
selected profile, and available matching adapter. The existing OM8 parser, foreground
service, key-decision, and controller-routing self-tests remain required.

Profile JSON is data only. Tests must not add arbitrary intents, shell commands,
coordinates, gestures, or unvalidated package control.

## Exact debug-device commands

~~~cmd
cd /d "T:\References\to go\Media\tools\Android-DJI-OM-Bridge"
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
adb logcat -c
adb shell am start -n com.androiddjibridge/com.androiddjibridge.app.MainActivity
adb logcat -v time -s Om8BleListenerService:I VideoProKeyAccessibilityService:I Om8DiagnosticReceiver:I *:S
~~~

After the app opens, use Start Bridge in the UI for the complete bridge startup. This initializes the normal selected-camera path and starts the OM8 driver.

The optional debug command below starts the OM8 listener/driver only:

~~~cmd
adb shell am broadcast -a com.androiddjibridge.OM8_LISTENER_START -n com.androiddjibridge/com.androiddjibridge.app.Om8DiagnosticReceiver
~~~

OM8 LISTENER READY means Gimbal READY only. The debug broadcast must not be treated as proof of Bridge READY / camera adapter available; it may produce BRIDGE_EVENT_REJECTED reason=no-selected-camera-adapter-or-camera-service. This diagnostic distinction is intentional and no startup behavior is changed by this documentation closeout.

Short timing dump after a test:

~~~cmd
adb logcat -d -v time | findstr /i "OM8_REC_PRESS CONTROLLER_EVENT SONY_ACTION_CLICK SONY_REC_VERIFIED OM8_TIMING"
~~~

If adb is not on PATH, substitute the platform-tools adb.exe path for this repository.

## Phase 1 hardware regression — PASS

Tested matrix:

- Gimbal: DJI Osmo Mobile 8 — tested.
- Phone: Sony Xperia 1 IV — tested.
- Camera: Sony Video Pro, package jp.co.sony.mc.videopro — tested.
- Android release/version: not recorded in the available Phase 1 source/frozen evidence; no device-version claim is made.

Verified:

- Direct BLE connection, FFF0/FFF4 discovery, CCCD enable, and successful HIGH-priority request submission.
- Physical RECORD_DOWN/RECORD_UP, with release producing no duplicate camera action.
- Generic controller-event routing through the Sony adapter.
- Non-Sony foreground rejection with OM8_SONY_TRIGGER_REJECTED reason=foreground-package-not-video-pro.
- START -> REC and STOP -> Stby.
- Prepared-node/safety-snapshot paths and mainThreadQueueNanos=0 on successful tested actions.
- STOP re-arm succeeded on attempt 4.

Representative samples:

| Action | Callback -> detection | Callback -> Sony click | Bridge request -> Sony click | Verified state |
| --- | ---: | ---: | ---: | --- |
| START | 0.064896 ms | 0.366666 ms | 0.224166 ms | REC |
| STOP | 0.045677 ms | 1.522708 ms | 0.117448 ms | Stby |

The 1.522708 ms STOP value is recorded as an observed sample/outlier only; it is not treated as a permanent regression.

## Reproduction procedure

Hardware status for this new repository is **HARDWARE REGRESSION: PASS**. On the Xperia 1 IV:

1. Install the debug APK and grant Nearby Devices if requested.
2. Enable the bridge AccessibilityService.
3. Start the listener and wait for OM8 LISTENER READY.
4. Open Sony Video Pro and wait for its REC node to prepare.
5. Press OM8 Record once from Sony standby; expect one RECORD_DOWN, one Sony action, and SONY_REC_VERIFIED state=REC.
6. Wait for the UI to settle, press once more; expect Stby.
7. Optionally press a third time to confirm START again.
8. Go Home and press OM8; expect rejection and no Sony action.

Use the UI Start Bridge path for this complete-bridge procedure. The debug broadcast is listener-only and is useful for OM8 driver diagnostics, not for proving that the camera adapter is selected.

## Timing interpretation

Callback-entry to camera-action timing is application-side only. It excludes physical button travel/debounce, BLE transport before Android callback delivery, camera application/media-pipeline startup, and the first encoded frame/audio sample.

