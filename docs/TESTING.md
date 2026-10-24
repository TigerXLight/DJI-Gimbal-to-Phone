# Testing and hardware regression

## Software commands

Run from the repository root:

~~~cmd
gradlew.bat test --no-daemon --console=plain
gradlew.bat assembleDebug --no-daemon --console=plain
~~~

The parser/edge and core-routing self-tests are kept as source-level regression classes. The Android Gradle test tasks must also remain green.

## Exact debug-device commands

~~~cmd
cd /d "T:\References\to go\Media\Android-DJI-OM-Bridge"
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
adb logcat -c
adb shell am start -n com.androiddjibridge/com.androiddjibridge.app.MainActivity
adb shell am broadcast -a com.androiddjibridge.OM8_LISTENER_START -n com.androiddjibridge/com.androiddjibridge.app.Om8DiagnosticReceiver
adb logcat -v time -s Om8BleListenerService:I VideoProKeyAccessibilityService:I Om8DiagnosticReceiver:I *:S
~~~

Short timing dump after a test:

~~~cmd
adb logcat -d -v time | findstr /i "OM8_REC_PRESS CONTROLLER_EVENT SONY_ACTION_CLICK SONY_REC_VERIFIED OM8_TIMING"
~~~

If adb is not on PATH, substitute the platform-tools adb.exe path for this repository.

## Two-to-three press hardware regression

Hardware status for this new repository is **PENDING USER TEST**. On the Xperia 1 IV:

1. Install the debug APK and grant Nearby Devices if requested.
2. Enable the bridge AccessibilityService.
3. Start the listener and wait for OM8 LISTENER READY.
4. Open Sony Video Pro and wait for its REC node to prepare.
5. Press OM8 Record once from Sony standby; expect one RECORD_DOWN, one Sony action, and SONY_REC_VERIFIED state=REC.
6. Wait for the UI to settle, press once more; expect Stby.
7. Optionally press a third time to confirm START again.
8. Go Home and press OM8; expect rejection and no Sony action.

Do not claim a hardware PASS from the build or unit tests. Record the phone, OM8 name, camera-app version, connection/recovery result, and any safety rejection when reporting the regression.

## Timing interpretation

Callback-entry to camera-action timing is application-side only. It excludes physical button travel/debounce, BLE transport before Android callback delivery, camera application/media-pipeline startup, and the first encoded frame/audio sample.

