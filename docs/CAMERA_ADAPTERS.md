# Camera adapters

CameraAdapter is the generic semantic output boundary. An adapter owns
application-specific availability, preparation, supported `CameraAction` values,
execution, state reporting, and release behavior. The adapter does not know which
physical control caused the action.

`supports(CameraAction)` must return false for actions the adapter cannot execute;
`execute(CameraAction, ControllerEvent)` must fail closed for unsupported actions.
The legacy event method remains only for adapter-level compatibility tests. Normal
bridge routing goes through `MappingEngine` and `CameraAction`.

## Sony Video Pro

The Phase 1 adapter targets exactly:

- package: jp.co.sony.mc.videopro;
- recording node: jp.co.sony.mc.videopro:id/recording;
- state node: jp.co.sony.mc.videopro:id/rec_status;
- duration node: jp.co.sony.mc.videopro:id/duration;
- class: android.widget.ImageButton.

The adapter delegates all node lookup, foreground checks, keyguard/interactive checks, prepared-node validity, Accessibility click, asynchronous state verification, and re-arm to VideoProKeyAccessibilityService. The adapter does not parse OM8 packets.

Sony advertises only `RECORD_TOGGLE`. `ControllerTrigger.RECORD_PRESS` resolves to
that action by default and `RECORD_RELEASE` resolves to no action, preserving the
hardware-tested no-duplicate-release behavior. No pre-click state lookup or new
Accessibility query was added by Phase 3A.

## Future generic profile boundary

A later profile engine may describe a package, record view ID, content-description patterns, optional state node, and supported actions. `CameraAction` already contains semantic capacity for photo, focus, zoom, lens, and continuous-axis operations, but no additional camera adapter is implemented by Phase 3A. It must not make coordinate tapping the trusted default. Profile learning was intentionally outside Phase 1; Phase 2B now provides the scoped, confirmation-based Learn Camera discovery workflow described in `LEARN_CAMERA.md`.

