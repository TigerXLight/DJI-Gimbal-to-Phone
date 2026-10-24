# Photography Pro CAMERA_BUTTON probe

This is a temporary capability experiment for the installed Sony Photography
Pro package `com.sonymobile.photopro`. It does not add a camera profile, map an
OM8 event, or claim Photography Pro support.

## What the probe does

The debug build exposes the protected action:

```text
com.androiddjibridge.TEST_PHOTOPRO_CAMERA_BUTTON_PROBE
```

The debug-only receiver is protected by `android.permission.DUMP`. When the
AccessibilityService is connected, it queues one probe on its service looper.
The service first requires all of the following:

- effective foreground package is exactly `com.sonymobile.photopro`;
- the device is interactive;
- the keyguard is not locked.

If any check fails, the probe logs a rejection and sends no broadcast. If all
checks pass, it constructs exactly one package-scoped
`Intent.ACTION_CAMERA_BUTTON` DOWN/UP pair. Each Intent carries
`Intent.EXTRA_KEY_EVENT` with `KeyEvent.KEYCODE_CAMERA`. The code uses only the
ordinary public `Context.sendBroadcast()` API. There is no shell execution,
ADB invocation, input injection, hidden API, root, coordinate tap, gesture, or
retry path.

The outbound Intent is restricted with:

```java
setPackage("com.sonymobile.photopro")
```

`sendBroadcast()` returning normally only means Android accepted the send call;
it does not prove that Photography Pro acted on it. On the tested Xperia, the
probe returned normally but captured no photograph, so this ordinary-app
CAMERA_BUTTON backend is `TESTED_NEGATIVE`.

## Debug test

With the debug APK installed, the AccessibilityService enabled, and
Photography Pro already foreground and unlocked:

```cmd
adb shell am broadcast -a com.androiddjibridge.TEST_PHOTOPRO_CAMERA_BUTTON_PROBE -n com.androiddjibridge/com.androiddjibridge.app.PhotographyProCameraButtonProbeReceiver
adb logcat -v time -s VideoProKeyAccessibilityService:I *:S
```

Run the command once. Expected diagnostics include:

```text
PHOTOPRO_CAMERA_BUTTON_PROBE begin
PHOTOPRO_CAMERA_BUTTON_PROBE send attempt ... keyAction=ACTION_DOWN
PHOTOPRO_CAMERA_BUTTON_PROBE send returned normally ... keyAction=ACTION_DOWN
PHOTOPRO_CAMERA_BUTTON_PROBE send attempt ... keyAction=ACTION_UP
PHOTOPRO_CAMERA_BUTTON_PROBE send returned normally ... keyAction=ACTION_UP
PHOTOPRO_CAMERA_BUTTON_PROBE complete
```

The same command while another app, the launcher, a locked screen, or a
non-interactive screen is active must log a rejection and must not send either
event.

## Device evidence before this probe

On the tested Xperia, `cmd package query-receivers --brief -a
android.intent.action.CAMERA_BUTTON` returned no static receiver. While
Photography Pro was running, `dumpsys activity broadcasts` showed its process
with a live dynamic filter containing `android.intent.action.CAMERA_BUTTON`.
The inspected Photography Pro manifest did not declare a static CAMERA_BUTTON
receiver. The available runtime dump did not show a required sender permission
on that live filter. These observations do not prove that an ordinary app can
deliver a usable event; the probe and physical one-photo test are required.

For comparison, `adb shell input keyevent KEYCODE_CAMERA` while Photography Pro
was foreground did capture a photograph. That is a privileged shell/input path,
not evidence that this ordinary-app broadcast can inject a camera key. The
distinction remains unresolved; future work should compare physical shutter,
shell input, and ordinary broadcast traces, including KeyEvent metadata and any
framework/Sony intermediary. No production mapping is justified by the shell
result.

The current research environment did not have `adb` available for a new
APK/runtime receiver inspection, so no stronger sender-permission or UID claim
is made here.
