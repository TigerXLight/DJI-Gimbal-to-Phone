# Architecture

~~~text
om8-driver
    BLE discovery/connection/recovery
    FFF0/FFF4 notification setup
    DUML validation and Record edge detection
             |
             v
bridge-core
    ControllerEvent
    MappingEngine / MappingStore
    CameraAction
    BridgeController
    CameraProfileRegistry / selected CameraProfile
    BridgeReadiness
    selected CameraAdapter
             |
             v
camera-adapters/sony-videopro
    Sony Video Pro Accessibility adapter
    prepared-node and safety-snapshot owner
~~~

The Android application module supplies the UI, application initialization, manifest, and debug-only receivers. BridgeApplication installs the selected adapter in the process-local BridgeRuntime registry. The OM8 service emits normalized events through that registry and never imports Sony classes.

The normal UI Start Bridge path is the complete bridge startup path. The explicit debug OM8_LISTENER_START broadcast is a driver/listener diagnostic path; it can establish Gimbal READY without proving that a selected camera adapter is available. A driver-only start may therefore report BRIDGE_EVENT_REJECTED reason=no-selected-camera-adapter-or-camera-service.

## Camera profiles and readiness

`CameraProfile` is immutable, versioned data describing a camera application. It can
contain the expected Android package, recording resource/class metadata, accepted
content descriptions, optional state/duration resources, supported capabilities,
provenance, and validation status. `CameraProfileRegistry` indexes explicitly
registered profiles by stable ID and package. Phase 2A registers exactly one built-in
profile: Sony Video Pro (`jp.co.sony.mc.videopro`).

The profile is not an executable configuration. Versioned JSON parsing rejects missing
or malformed required fields, unsupported schema versions, unknown capabilities/actions,
and arbitrary commands. The deterministic `AccessibilityTargetMatcher` is a metadata
matching aid; it is not an exported click interface.

The selected profile and adapter remain separate. The Sony adapter retains the proven
prepared-node lifecycle, safety snapshot, foreground/keyguard/window checks,
`ACTION_CLICK`, authoritative `rec_status` verification, and delayed re-arm. The
profile centralizes the stable Sony identifiers without replacing that execution path.

Complete bridge readiness is distinct from driver readiness:

~~~text
GIMBAL_READY + CAMERA_PROFILE_SELECTED + CAMERA_SERVICE_AVAILABLE = BRIDGE_READY
~~~

`OM8 LISTENER READY` therefore means the OM8 driver is ready. The normal UI Start
Bridge path selects Sony and starts the complete bridge; the debug listener broadcast
remains driver/listener-only.

## Phase 3A mapping boundary

The production event path is now:

~~~text
GimbalDriver
    -> ControllerEvent
    -> MappingEngine / MappingStore
    -> CameraAction
    -> selected CameraAdapter
~~~

`ControllerEvent` remains the normalized transport-neutral event, including the
existing `RECORD_DOWN`/`RECORD_UP` pair. `ControllerTrigger` translates it into
semantic mapping keys. `MappingEngine` performs an in-memory lookup and returns a
`MappingDecision`; it performs no Android or Accessibility work. The default
`RECORD_PRESS -> RECORD_TOGGLE` mapping preserves the Phase 1 behavior, while
`RECORD_RELEASE -> NONE` prevents a duplicate action.

`CameraAdapter.supports(CameraAction)` and `execute(CameraAction, ControllerEvent)`
are the semantic output contract. Sony implements only `RECORD_TOGGLE` and routes
that action into the existing specialized adapter/service path. Unsupported
actions fail closed. `CameraAdapter.handleControllerEvent` remains as a narrow
adapter-level compatibility entry point for existing regression tests; the bridge
controller uses the action contract.

`MappingStore` is immutable and defaults every unconfigured trigger to `NONE`.
`MappingJsonCodec` is strict, deterministic, schema-versioned data parsing, and
`AndroidMappingStore` stores it only in app-private `SharedPreferences`. No
mapping can contain an intent, shell command, package/resource target, or arbitrary
Accessibility action. Axis actions are model capacity only; no unmeasured OM8
slider packet is interpreted.

## Module boundaries

- bridge-core: Java-only generic interfaces and event routing.
- om8-driver: Android BLE service, OM8 DUML parser, rising-edge detector, recovery, watchdog, diagnostics, and Android permission decisions.
- camera-adapters/sony-videopro: Sony package/node constants, AccessibilityService, prepared REC node, local safety snapshot, post-click verification, and re-arm logic.
- app: Android entry point and user-facing start/stop/reconnect controls.

Phase 3A provenance for the mapping boundary is documented in the repository-root
licence/provenance files. Its partial structural donor is the MIT-licensed
OpenGimbal mapping model; HJ360 BLE code and camera fallback behavior are not used.

The service uses the API 26+ connectGatt overload with a main-looper handler, LE transport, and the existing PHY behavior. Lifecycle callbacks are protected by current-GATT identity/generation checks. Only the standard FFF4 CCCD write is performed; no DJI command data is written.

## Event contract

Phase 1 normalizes RECORD_DOWN and RECORD_UP. Future event types are reserved in the core enum but are not decoded or claimed as implemented. The Sony adapter handles only RECORD_DOWN; release never toggles Sony.

## Phase 2B Learn Camera boundary

Learn Mode is a discovery path, not a generic remote-control API. The user explicitly
selects one installed target package, starts the visibly distinct mode, and taps a
control in that target. The Accessibility service captures a `TYPE_VIEW_CLICKED`
source only when its package exactly matches the selected package. The candidate is
reviewed and explicitly confirmed before a `LEARNED` profile is created and saved.

The resulting profile is data: it contains validated package and Accessibility
metadata, supported capability `RECORD_TOGGLE`, and no executable commands. It does
not contain coordinates, gestures, OCR output, shell commands, or arbitrary action
lists. A later test action resolves a fresh active tree in the selected package,
requires one exact usable match and `ACTION_CLICK`, and fails closed for zero or
ambiguous matches. Screen/keyguard safety is required. Sony remains on its
specialized adapter and proven prepared-node/safety-snapshot path.

Learned profiles are loaded from app-private storage and remain distinct from the
built-in Sony profile. A learned control and a recording-state verifier are separate:
the workflow does not invent `REC`/`Stby` nodes or claim state verification merely
because a control was captured.

## Application discovery and future normal use

Learn Camera discovery is a package-selection aid, not an authority to control an
arbitrary foreground application. The app merges launchable, `VIDEO_CAPTURE`, and
`IMAGE_CAPTURE` resolver results plus installed built-in profile packages. Targeted
manifest `<queries>` entries provide visibility; `QUERY_ALL_PACKAGES` is not used.
Candidates are deduplicated by package, system/preinstalled camera status is allowed,
and only explicit infrastructure filters are applied before the user selects a
package. Package-scoped Learn Mode and the generic adapter's exact safety checks
remain the enforcement boundary.

The eventual product direction is setup/configuration once, a remembered gimbal and
selected profile, automatic bridge service/reconnection where Android permits, and
direct use of the selected camera application. Pairing is not equivalent to bridge
control: the bridge must establish/manage its own GATT connection and notification
subscription. Background/foreground-service lifecycle, Bluetooth on/off recovery,
reboot behavior, and optional CompanionDeviceManager evaluation are future work.

