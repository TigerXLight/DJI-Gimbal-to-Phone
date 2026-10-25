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
Xperia 1 IV Photography Pro discovery issue is recorded. The ordinary-app
CAMERA_BUTTON probe is now `TESTED_NEGATIVE`; Photography Pro remains an open
backend investigation and is not a support claim.

## Phase 3A — COMPLETE / HARDWARE PASS

- Pure `ControllerEvent -> MappingEngine -> CameraAction -> CameraAdapter`
  foundation.
- Default `RECORD_PRESS -> RECORD_TOGGLE` and release no-op behavior.
- Strict deterministic versioned mapping persistence in app-private preferences.
- Semantic camera-action capacity for photo, focus, zoom, lens, and continuous axes.
- Sony remains on its specialized adapter; no Sony hot-path Accessibility logic was
  moved into the generic mapping layer.
- MIT OpenGimbal mapping-boundary adaptation with attribution and notices.

The two-press Xperia 1 IV + OM8 + Sony Video Pro regression passed. START verified
REC and re-armed on attempt 1; STOP verified Stby and re-armed on attempt 4; release
produced no duplicate action. Background lifecycle is recorded separately in
Phase 3B; OM8 rotation decoding, FFF5 writes, and new camera adapters remain
outside the proven scope.

## Phase 3B — COMPLETE / HARDWARE PASS

- Remembered Android companion association for an OM8 name/service identity.
- Companion Device presence callbacks as the background wake-up signal.
- Existing connected-device foreground GATT service as the actual FFF0/FFF4 owner.
- Bounded reconnect/recovery remains in the OM8 driver, with duplicate-operation guards.
- Camera readiness remains separate from Gimbal READY.
- Real Xperia 1 IV + OM8 power-cycle/automatic-recovery and Sony START/STOP regression passed.

## Later phases

1. Freeze/tag the Phase 1 generic bridge baseline.
2. Test a learned non-Sony camera application only with explicit review and separate
   hardware evidence.
3. Investigate additional DJI OM model drivers separately.
4. Evaluate CompanionDeviceManager for association/presence without replacing the proven GATT driver.
5. Research a secure cooperative camera API using intents, a bound service/AIDL, or a client SDK. Do not export unauthenticated recording control.

Insta360 synchronization is outside this repository and must not be added as a shortcut to camera-adapter work. It is the next separate project after DJI closeout.

## Future normal-use lifecycle refinements

The desired later experience is setup once, remembered/associated gimbal and profile,
automatic bridge startup/reconnection, and direct use of the selected camera app.
The initial supported path is implemented with Android association/presence
wake-up plus the existing connected-device foreground service. Further reboot,
process-death, and API-level refinements remain separate validation work.
Bluetooth pairing alone does not provide camera control; bridge GATT connection
and notification subscription remain necessary.

## Broad research integration status

The current foundation incorporates the useful permissive-license architecture
from the upstream audit without importing incompatible protocol or unsafe camera
fallbacks. Mappings are profile-scoped and editable; raw OM8 diagnostics are
explicitly toggled. Pixel selectors are retained as hardware-unverified evidence,
Samsung stock control remains fail-closed, and Photography Pro's ordinary-app
camera-button broadcast is tested negative. Future work is hardware-backed
adapter-by-adapter validation, not a universal compatibility claim.

## Consumer productization — IN PROGRESS

- User-facing name is **DJI Gimbal Bridge**.
- Main screen is organized around **Find DJI Gimbal**, remembered connection
  status, camera-app rows, per-app **Mappings**, and a collapsed **Advanced /
  Diagnostics** area.
- Companion association and the existing connected-device foreground service
  remain the automatic connection path; **Disconnect** and **Forget DJI
  Gimbal** are distinct actions.
- Foreground camera package changes select the matching configured profile and
  mapping automatically; unknown packages fail closed.
- The current build is `0.2.0-consumer-preview`, not 1.0. The Xperia/OM8
  product acceptance sequence, timing summary, release licence selection, and
  public-file sanitization are still required before a public release claim.
- Sony Video Pro remains the only hardware-tested camera application. Side-wheel
  rotation remains `UNSUPPORTED_NO_SAFE_PATH`, while side-wheel press is decoded
  but not routed to an unsafe lens-switch fallback.

