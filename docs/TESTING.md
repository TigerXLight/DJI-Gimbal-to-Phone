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

## Phase 3A mapping tests

`MappingEngineSelfTest` covers the default Record press/release mapping, unknown
trigger rejection, deterministic JSON round-trip, malformed/future/unknown mapping
rejection, user remapping, unsupported-action fail-closed behavior, and the
distinction between discrete and continuous axis actions. `BridgeControllerSelfTest`
also exercises the semantic action boundary. The existing Sony profile test confirms
that the specialized adapter advertises `RECORD_TOGGLE` and continues to handle
release as a no-op.

The mapping lookup is pure/in-memory and must not add root, node, keyguard, or
foreground queries to the OM8 Record hot path.

## Exact debug-device commands

Run the following commands from the repository root:

~~~cmd
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
adb logcat -c
adb shell am start -n com.androiddjibridge/com.androiddjibridge.app.MainActivity
adb logcat -v time -s Om8BleListenerService:I VideoProKeyAccessibilityService:I Om8DiagnosticReceiver:I *:S
~~~

After the app opens, use **Find DJI Gimbal** once for the normal setup path.
The remembered association/presence path starts the connected-device service
where Android permits. Use **Reconnect now** in **Advanced / Diagnostics** only
for manual recovery; this initializes the existing OM8 driver without adding a
second GATT owner.

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

## Consumer acceptance procedure prepared

The executable interactive procedure is
`tools/final_consumer_acceptance.ps1`. It is intentionally prepared but not
executed in this preview because no Xperia device was attached. It installs the
debug APK, captures logcat, pauses for each required user/device action, records
the system-UI observations, and prints the timing-log extraction commands. It
does not send shell key events or Bluetooth commands.

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

## Phase 2B Learn Camera manual validation

1. Enable the bridge Accessibility service.
2. Open the app and choose **Learn Camera**.
3. Select the intended installed camera package; system/launcher packages are not
   offered by the normal list.
4. Start learning, switch to that camera, and deliberately tap its Record control.
5. Return to the bridge, review the captured package, resource ID/class/text,
   content description, actions, state flags, and validation result.
6. Explicitly confirm and save. The profile is stored privately as `LEARNED` and is
   not automatically selected.
7. Use **Test Selected Learned Profile** only with that profile selected and the
   target camera foreground. The test must resolve one fresh exact match; zero,
   multiple, wrong-package, unsafe, stale/changed, or unsupported matches fail
   closed without a click.

Learning captures only the Accessibility clicked source. If Android supplies no
source node, no candidate is created. A captured control does not discover or invent
`REC`/`Stby` state verification.

## Phase 2B self-tests

Run all Gradle tests:

~~~cmd
gradlew.bat test --no-daemon --console=plain
~~~

The Phase 2B additions cover explicit learning state/confirmation, package scope,
candidate validation, learned origin and persistence policy, deterministic profile
round-trip, generic exact-match/ambiguity decisions, and the existing Phase 1/2A
controller, parser, permission, and Sony key-safety tests.

## Phase 2B application-discovery validation

On the Xperia 1 IV, open **Learn Camera** and confirm the list includes, when the
applications are installed and package-visible:

- Photography Pro — `com.sonymobile.photopro`;
- Video Pro — `jp.co.sony.mc.videopro` (marked as a built-in profile where applicable).

The list may also contain other launchable or capture-intent applications. The source
indicator distinguishes `CAMERA INTENT`, `LAUNCHABLE`, and `BUILT-IN PROFILE`; none of
these labels means supported. Verify that the same package appears only once even if
it resolves multiple routes. System/preinstalled camera applications must remain
eligible; obvious infrastructure remains filtered.

The original failure state was Game enhancer, Play store, and Support only. That was
caused by the launchable-only query. The corrected build adds targeted `<queries>`
for launcher, video capture, and image capture and does not use `QUERY_ALL_PACKAGES`.
Do not proceed to Photography Pro control claims until the deliberate capture/review/
confirmation test succeeds.

## Phase 3A hardware regression — PASS

Test matrix: Sony Xperia 1 IV + DJI Osmo Mobile 8 + Sony Video Pro.

The required two-press test passed:

- START, `om8PressSequence=1`: prepared-node direct action, safety-snapshot fast
  path, `mainDispatchSkipped=true`, `preClickRecStatusQuery=false`, verified REC,
  re-arm attempt 1.
- STOP, `om8PressSequence=2`: prepared-node direct action, safety-snapshot fast
  path, `mainDispatchSkipped=true`, `preClickRecStatusQuery=false`, verified Stby,
  re-arm attempt 4 after transient `enabled` failures on attempts 1–3.
- Both Record releases produced no duplicate camera action.

| Press | Notification -> detection | Detection -> click | Notification -> click | Bridge request -> click |
| --- | ---: | ---: | ---: | ---: |
| START | 0.335052 ms | 2.154323 ms | 2.489375 ms | 0.265834 ms |
| STOP | 0.048646 ms | 0.103073 ms | 0.151719 ms | 0.035364 ms |

This closes Phase 3A as **HARDWARE PASS**. Phase 3B background lifecycle testing is
separate and must not be inferred from this result.

## Phase 3B lifecycle validation

### Software tests

`AutoConnectPolicySelfTest` covers no-association refusal, Bluetooth-off refusal,
associated-device start permission, duplicate presence suppression while a driver
is active, association metadata retention, and bounded retry delays. Existing
parser, mapping, profile, controller, permission, and Sony tests remain required.

### Setup and hardware test

1. Install the debug APK and grant Nearby Devices/notification permissions if
   requested.
2. Enable the bridge AccessibilityService.
3. Open the bridge and choose **Associate OM8 for Automatic Connection**.
4. Select the intended `OM8-*` device in Android's system association UI.
5. Confirm the UI reports the association, then leave the bridge UI/Home.
6. Power the OM8 off, wait for the current GATT to disappear, then power it on.
7. Filter logs for `Om8Companion`, `Om8AutoConnect`, and
   `Om8BleListenerService`. The expected path is presence callback -> automatic
   foreground-service start -> scan -> GATT -> FFF0/FFF4 -> CCCD -> `OM8 LISTENER READY`.
8. Open Sony Video Pro and press OM8 Record once for START/REC, then once for
   STOP/Stby. Confirm release does not create a second action.

If the system lacks the companion setup feature, or an Android version cannot
observe the stored address, use **Start Bridge** as the documented manual
fallback. A Bluetooth association does not itself establish GATT or FFF4
notifications. Do not force-stop DJI Mimo; contention remains a safe retry case.
8. Go Home and press OM8; expect rejection and no Sony action.

Use the UI Start Bridge path for this complete-bridge procedure. The debug broadcast is listener-only and is useful for OM8 driver diagnostics, not for proving that the camera adapter is selected.

## Timing interpretation

Callback-entry to camera-action timing is application-side only. It excludes physical button travel/debounce, BLE transport before Android callback delivery, camera application/media-pipeline startup, and the first encoded frame/audio sample.

## Temporary Photography Pro CAMERA_BUTTON capability probe

The debug build contains a narrow, DUMP-protected diagnostic receiver for the
installed Sony Photography Pro package (`com.sonymobile.photopro`):

```cmd
adb shell am broadcast -a com.androiddjibridge.TEST_PHOTOPRO_CAMERA_BUTTON_PROBE -n com.androiddjibridge/com.androiddjibridge.app.PhotographyProCameraButtonProbeReceiver
```

Run it only while Photography Pro is the actual foreground application, the
screen is interactive, and the device is unlocked. The AccessibilityService
checks those conditions and fails closed otherwise. On success it sends one
package-scoped `android.intent.action.CAMERA_BUTTON` DOWN/UP pair with
`Intent.EXTRA_KEY_EVENT` and `KEYCODE_CAMERA` through normal public
`Context.sendBroadcast()`. It never uses `adb`, shell execution, input
injection, hidden APIs, coordinates, gestures, or retries. This is not mapped
to OM8 input and does not create a permanent Photography Pro profile.

Inspect the result with:

```cmd
adb logcat -v time -s VideoProKeyAccessibilityService:I *:S
```

Normal return from `sendBroadcast()` is not proof of camera behavior. The real
Xperia result for this probe was no photograph, so this ordinary-app backend is
`TESTED_NEGATIVE`. `adb shell input keyevent KEYCODE_CAMERA` did capture a
photograph, but that is a privileged shell/input path and must not be treated as
ordinary-app support. See
[`PHOTOPRO_CAMERA_BUTTON_PROBE.md`](PHOTOPRO_CAMERA_BUTTON_PROBE.md) for the
receiver/permission evidence and full interpretation.

## Mapping and diagnostic controls

The app's **Edit Control Mappings** screen stores only normalized trigger names
and semantic camera actions, separately for each selected profile. Unsupported
actions fail closed in the adapter. The default Record rising edge remains
immediate `RECORD_TOGGLE`, and Record release remains a no-op.

**OM8 Raw Diagnostics** is an explicit toggle for the current listener. It is off
by default, so idle FFF4 heartbeats do not flood Logcat. When enabled it records
raw frames for diagnosis; it does not change parsing, readiness, or camera action
behavior. The control is under the scrollable **Advanced / Diagnostics** section
of the main screen and shows `OM8 Raw Diagnostics: ON` or `OFF`. The preference
is stored in app-private `SharedPreferences` and is restored by the listener
service after recreation.

For the controlled physical-control capture:

1. Open Android-DJI-OM-Bridge and scroll to **Advanced / Diagnostics**.
2. Press **Enable OM8 Raw Diagnostics**. Confirm the visible state changes to
   `ON`.
3. Start the bridge normally, wait for `OM8 LISTENER READY`, and capture the
   required OM8 controls while watching:

```cmd
adb logcat -v monotonic -s Om8BleListenerService:I *:S
```

Raw mode emits one complete line per received FFF4 notification before parsing:

```text
OM8_RAW_FFF4 timestampNanos=... characteristic=0000fff4-... length=... value=55 ...
```

Valid packets also emit `OM8_PARSED_FFF4` with frame length, sender, receiver,
flags, command set, command ID, and complete payload. Rejected packets retain the
raw line and include the parser reason. The notification bytes are not truncated.

## Final DJI closeout evidence

The final decoder evidence is summarized in `OM8_CONTROL_SUPPORT.md`. The
passive `04/57` fields for joystick X/Y, M/trigger state, switch,
side-wheel press, and Record are parser-tested. Only Record has a mapped
camera action and real Sony Video Pro hardware regression. Side-wheel rotation
is not a production test target because its external encoding remains
unresolved and is classified `UNSUPPORTED_NO_SAFE_PATH`.

The final bounded DJI UXSDK audit is recorded in the research workspace as
`reports/27_DJI_UXSDK_FINAL_SOURCE_AUDIT.md`; it found camera-command/UI APIs
but no physical OM8 wheel origin. No additional wheel experiment is required
for this DJI closeout.

4. After capture, press **Disable OM8 Raw Diagnostics** and confirm the state is
   `OFF`. Normal mode suppresses routine heartbeat and expected parser-rejection
   spam while retaining connection, readiness, release/press, and unusual error
   messages.

## Consumer-preview acceptance checklist

The productization build remains `0.2.0-consumer-preview` (versionCode `2`). The
following product-level checks are still required before any 1.0/publication decision:

1. With no saved association, open **DJI Gimbal Bridge**, press **Find DJI
   Gimbal**, select the nearby `OM8-*` device, and confirm the association is
   retained.
2. Enable **Camera Control** once, leave the bridge UI, turn the remembered OM8
   off and on, and confirm the connected-device service reaches Gimbal READY
   without a routine Start Bridge press.
3. Open Sony Video Pro directly and verify one physical Record press starts REC,
   one later press stops at Stby, and releases do not create duplicate actions.
4. Confirm the foreground Sony package selects the Sony preset automatically;
   unknown/unconfigured packages must clear the camera adapter and fail closed.
5. Exercise **Disconnect** and **Forget DJI Gimbal** separately. Disconnect must
   suppress immediate auto-reconnect without erasing the remembered association;
   Forget must clear it.
6. Test OM8 power-cycle, Bluetooth off/on, process/service recreation, and phone
   reboot without clearing app data. The app now requests the existing
   `connectedDevice` service from an Android `BOOT_COMPLETED` receiver when an
   association exists and Disconnect is not active. Record whether the tested OS
   delivers that recovery; force-stopped apps and device policy may still prevent it.
7. Inspect Android Bluetooth Settings and Quick Settings while the bridge owns
   the GATT connection. Record whether the OM8 is surfaced as connected/bonded;
   do not force bonding. The app notification must say **DJI Gimbal Bridge** and
   show a useful OM8 connection status if system UI is silent.
8. Run five bounded START/STOP pairs and retain the timing lines. Report
   notification callback -> edge detection, callback -> ACTION_CLICK, click ->
   authoritative REC/Stby verification, and total callback -> verification for
    START and STOP separately, with min/median/max. These are application-side
    timings, not physical-button-to-media-start latency.

The mapping editor opens on the stored value for the selected camera app. Sony
Video Pro's default is `Record Press -> Record toggle`; explicit saves are stored
under the profile stable ID and must survive Activity/process recreation. The
acceptance script records `PASS`, `FAIL`, `N/A`, or `OBSERVATION` per physical
sample and waits for the requested ADB serial to return as `device` after reboot
before attempting to launch the Activity.

The final acceptance result must be labelled **PASS**, **FAIL**, or
**NOT RUN**. A passing unit suite and APK build do not substitute for this
product-level device run. The unresolved OM8 side-wheel rotation does not block
1.0 if it remains visibly unavailable and is not advertised as supported.

## 2026-10-25 bounded Record hot-path profile

After the consumer lifecycle fixes, a quiet, bounded Xperia 1 IV run measured
five START and five STOP presses without raw diagnostics enabled. All ten
presses produced the expected authoritative Sony state sequence:

`REC, Stby, REC, Stby, REC, Stby, REC, Stby, REC, Stby`.

The primary metric is callback entry (T0) to the start of the prepared Sony
`ACTION_CLICK` call (T4). START samples in milliseconds were
`0.328645, 0.336250, 1.377708, 0.335000, 0.469583` (min `0.328645`,
median `0.336250`, max `1.377708`). STOP samples were
`0.341042, 0.571511, 0.339167, 0.405417, 0.322761` (min `0.322761`,
median `0.341042`, max `0.571511`). Nine of ten samples were below 1 ms;
the single 1.377708 ms START sample is retained as an outlier rather than
discarded.

The quiet hot path uses an in-memory control-only parser with no retained frame
when diagnostics are off. It performs no storage, UI, serialization, or file
work before the click. The post-UI sanity pair again verified REC then Stby,
with prepared-node and safety-snapshot fast paths and no duplicate release
action. These are callback-to-click measurements, not physical travel or
camera media-pipeline latency.

## 2026-10-25 targeted consumer retest

On Xperia 1 IV `QV770840CQ` with OM8 `OM8-15QPML`, the targeted checks A--G
passed on the consumer-preview build. The Sony default was
`Record Press -> Record toggle`; the profile mapping survived navigation,
process recreation, and reboot; Sony Video Pro START/STOP verified `REC` and
`Stby`; the visible Bluetooth state updated live; power-cycle and Bluetooth
off/on recovery reconnected without Start Bridge; and non-destructive process
recreation preserved the active Sony adapter. Test D was recorded `N/A` because
no second camera application was configured.

The first process-recreation attempt used `adb shell am force-stop`, which the
Xperia treated as revoking the user-enabled Accessibility service. It produced
the expected fail-closed `no-selected-camera-adapter-or-camera-service` result
and was not treated as an app-runtime regression. The acceptance harness now
uses `adb shell am kill` for process recreation and retains explicit per-sample
PASS/FAIL/N/A/OBSERVATION notes. The post-reboot service check showed
Accessibility still enabled/bound and the OM8 service reconnecting without
opening the bridge Activity. One clean post-reboot timing sample measured
notification-to-ACTION_CLICK at approximately 2.955 ms START and 2.481 ms
STOP, with authoritative state verification at approximately 290 ms and 272 ms
after the click respectively. These are application-side timings, not physical
button-to-media-start latency.

The long-form reboot/repeated-five-pair section was intentionally stopped after
the targeted checks passed to avoid redundant hardware wear. The preview remains
pre-1.0 and unpublished.

## Public-release gate

Before any GitHub publication, audit tracked files for personal paths, device
addresses, raw bugreports/HCI captures, credentials, private project-memory
material, and build/IDE caches. The production repository must contain no
Insta360 implementation or private evidence archive. The project licence is
still intentionally undecided in this preview; do not publish until a licence
is explicitly selected and added as `LICENSE`.

