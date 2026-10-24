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
it does not prove that Photography Pro acted on it. The hardware result is
whether one new photograph is captured.

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

The installed APK was inspected only for the narrow receiver/permission
question. No unrelated Sony internals were reverse-engineered or modified.
