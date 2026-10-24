# Learn Camera

Phase 2B provides a deliberately narrow way to create a candidate camera profile
from Accessibility metadata exposed by an installed Android camera application.
It does not provide universal camera compatibility and it does not turn arbitrary
Accessibility events into remote control.

## Workflow

1. Open Android-DJI-OM-Bridge and choose **Learn Camera**.
2. Select the intended installed application. The normal list excludes the bridge,
   System UI, Settings, launcher, and input-method packages.
3. Start Learn Mode. The selected package becomes the strict capture scope.
4. Switch to that application and deliberately tap its Record control once.
5. Return to the bridge and review the candidate metadata.
6. Explicitly confirm the candidate, then save it locally.

The app captures only a `TYPE_VIEW_CLICKED` Accessibility source whose package is
exactly the selected package. Events from another application, launcher, System UI,
Settings, notifications, or keyboard are rejected. If Android provides no source
node for the user tap, no candidate is created; the workflow does not guess from a
random event or use coordinates.

## Captured data and validation

The review shows the application name/package, resource ID when available, class,
content description, text, parent metadata, supported actions, clickable/enabled/
visible flags, and whether the candidate is structurally usable. The generated
profile has `origin=LEARNED`, an unverified validation status, and capability
`RECORD_TOGGLE`. It does not invent a recording-state resource or `REC`/`Stby`
values. A learned control and learned state verification are separate things.

Profiles are deterministic JSON data and are stored in the app-private
`files/learned_profiles` directory. Built-in profiles cannot be overwritten, learned
profiles cannot claim `BUILTIN`, malformed/unsupported/duplicate profiles are
rejected, and no broad storage permission is used.

## Testing a learned profile

The deliberate **Test Selected Learned Profile** action requires the selected
profile, exact foreground package, interactive/unlocked device, fresh active
Accessibility root, and exactly one node matching the learned metadata and
`ACTION_CLICK`. Zero matches, ambiguous matches, wrong package, stale/changed UI,
unsupported constraints, and unsafe device state fail closed. No coordinate taps,
gestures, OCR, screenshots, MediaProjection, synthetic keys, arbitrary actions, or
fallback to another control are used.

The built-in Sony Video Pro profile continues through its specialized adapter and
is not routed through this generic learned test path. Sony Video Pro is the only
camera application currently hardware-tested.

## Application discovery

The target list merges three Android-safe discovery routes:

- launchable applications (`ACTION_MAIN` + `CATEGORY_LAUNCHER);
- packages resolving `android.media.action.VIDEO_CAPTURE`;
- packages resolving `android.media.action.IMAGE_CAPTURE`.

Candidates are deduplicated by package and displayed with their label, package name,
and a small source indicator. A built-in profile package can also be included when
the installed package is visible. System/preinstalled status is not treated as
unsafe; OEM cameras are legitimate learning targets. Only obvious infrastructure
(System UI, Settings, launchers, and input methods) is excluded.

The original Xperia selector defect queried only the launchable route, so it showed
Game enhancer, Play store, and Support while omitting installed Photography Pro.
Android evidence confirmed Photography Pro's video and image capture activities.
The corrected implementation adds targeted manifest `<queries>` entries rather than
requesting `QUERY_ALL_PACKAGES`. The post-fix selector still requires real-device
validation; listing Photography Pro means eligible to attempt learning, not that its
control works.

The future normal-use direction is setup once followed by opening the selected camera
directly while the bridge reconnects where Android permits. Pairing alone is not
enough: the bridge must still manage its BLE GATT connection and notification
subscription. Automatic startup and background lifecycle work are not part of Phase
2B.
