# Security and safety

- The app never force-stops, inspects, or automates DJI Mimo.
- Debug receivers exist only in the debug manifest and require android.permission.DUMP.
- No unauthenticated production receiver or public recording-control API is added.
- The OM8 driver does not write DJI command data.
- One rising Record edge produces at most one normalized event.
- Release and repeated heartbeat frames do not duplicate actions.
- Stale GATT callbacks and GATT objects are rejected.
- Sony action is rejected unless the exact package is safely foreground, the device is interactive/unlocked, and the prepared or freshly validated target node is usable.
- No coordinates, gestures, synthetic key events, private camera APIs, or automatic application launch are used.
- The Sony prepared-node and safety-snapshot paths remain invalidated by relevant foreground/window/lifecycle/screen conditions; harmless timer updates do not continuously invalidate them.
- No Insta360 code or synchronization control is present.

Before any public release, review exported components, release-manifest behavior, privacy, and licensing again.

