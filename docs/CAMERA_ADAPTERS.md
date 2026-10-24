# Camera adapters

CameraAdapter is the generic output boundary. An adapter owns application-specific availability, preparation, event handling, state reporting, and release behavior.

## Sony Video Pro

The Phase 1 adapter targets exactly:

- package: jp.co.sony.mc.videopro;
- recording node: jp.co.sony.mc.videopro:id/recording;
- state node: jp.co.sony.mc.videopro:id/rec_status;
- duration node: jp.co.sony.mc.videopro:id/duration;
- class: android.widget.ImageButton.

The adapter delegates all node lookup, foreground checks, keyguard/interactive checks, prepared-node validity, Accessibility click, asynchronous state verification, and re-arm to VideoProKeyAccessibilityService. The adapter does not parse OM8 packets.

## Future generic profile boundary

A later profile engine may describe a package, record view ID, content-description patterns, optional state node, and supported actions. It must not make coordinate tapping the trusted default. Profile learning was intentionally outside Phase 1; Phase 2B now provides the scoped, confirmation-based Learn Camera discovery workflow described in `LEARN_CAMERA.md`.

