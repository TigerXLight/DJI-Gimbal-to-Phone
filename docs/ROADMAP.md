# Roadmap

## Phase 1 — COMPLETE / PASS

- Standalone repository and module boundaries.
- Extracted proven OM8 receive/recovery/parser core.
- Generic controller event and camera adapter contracts.
- Sony Video Pro reference adapter.
- Software regression/build surface and conservative documentation.
- Real Xperia 1 IV + OM8 + Sony Video Pro hardware regression.
- Complete-startup distinction documented: UI Start Bridge versus listener-only debug broadcast.

## Phase 2A — COMPLETE / FOUNDATION ONLY

- Immutable `CameraProfile` data model with explicit schema version, provenance, and
  validation status.
- Deterministic JSON serialization/parsing with safe rejection of malformed or
  unsupported profiles.
- Profile registry, metadata matcher, and bridge-level readiness state.
- Exactly one built-in, hardware-tested profile: Sony Video Pro.
- Sony metadata centralized in the profile while the proven Sony adapter/service
  execution path remains in place.

No additional camera application is supported or hardware-tested by this phase.

## Phase 2B — IMPLEMENTED / DISCOVERY ONLY

- Explicit installed-package selection and visibly scoped Learn Mode.
- `TYPE_VIEW_CLICKED` source capture restricted to the selected package.
- Candidate review and explicit confirmation before app-private persistence.
- Learned-profile validation, deterministic JSON persistence, and conservative
  generic exact-match test action.
- No learned recording-state verification, arbitrary fallback, coordinate/gesture
  control, OCR, screenshot recognition, or broader compatibility claim.

The built-in Sony profile continues through its specialized adapter. Sony Video Pro
is still the only camera application currently hardware-tested.

The Phase 2B selector correction now discovers camera applications through merged
launchable/video-capture/image-capture routes and targeted package visibility. The
Xperia 1 IV Photography Pro discovery issue is recorded; Photography Pro remains an
unverified learning candidate, not a support claim.

## Later phases

1. Freeze/tag the Phase 1 generic bridge baseline.
2. Test a learned non-Sony camera application only with explicit review and separate
   hardware evidence.
3. Investigate additional DJI OM model drivers separately.
4. Evaluate CompanionDeviceManager for association/presence without replacing the proven GATT driver.
5. Research a secure cooperative camera API using intents, a bound service/AIDL, or a client SDK. Do not export unauthenticated recording control.

Insta360 synchronization is outside this repository and must not be added as a shortcut to camera-adapter work.

## Future normal-use lifecycle (not implemented)

The desired later experience is setup once, remembered/associated gimbal and profile,
automatic bridge startup/reconnection, and direct use of the selected camera app.
This requires careful Android background/foreground-service lifecycle handling,
Bluetooth on/off and reboot recovery, and possibly CompanionDeviceManager evaluation.
Bluetooth pairing alone does not provide camera control; bridge GATT connection and
notification subscription remain necessary.

