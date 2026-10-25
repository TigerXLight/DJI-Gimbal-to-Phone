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

The OM8 parser exposes the evidence-backed `04/57` joystick, M/trigger, switch,
side-wheel-press, and Record fields. The driver still routes only the Record
rising/falling edge into the active camera mapping; the other decoded fields
remain observational until their state-transition semantics and safe camera
mappings are independently validated. The Sony adapter handles only
`RECORD_TOGGLE`; release never toggles Sony. `CameraAction.SWITCH_LENS` is the
reserved semantic destination for the decoded side-wheel press, but no safe
Sony Accessibility node/action has been evidenced or implemented, so it is not
enabled. Side-wheel rotation is reserved for Sony zoom, remains unresolved,
and is not represented by a guessed field.

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
subscription. Phase 3B implements the initial CompanionDeviceManager presence
wake-up and connected-device foreground-service lifecycle; broader reboot/process
death behavior and future API-level refinements remain separate validation work.

## Phase 3B background gimbal lifecycle

Phase 3B separates system presence from the transport owner:

~~~text
user-approved Companion association
        -> CompanionDeviceService presence callback
        -> auto-start existing connectedDevice FGS
        -> Om8BleListenerService scan/GATT ownership
        -> FFF0/FFF4/CCCD validation
        -> Gimbal READY
        -> independent camera readiness
~~~

`CompanionAssociationStore` persists only the user-approved OM8 association
metadata in app-private preferences. Android's companion association is the
identity authority. A last-observed address is retained only for the legacy
API 31-35 presence-observation API; it is not hard-coded, permanent identity,
or a substitute for post-connect GATT validation. The user-facing association
filter selects `OM8-*` by name; the driver still proves FFF0/FFF4 before READY.

`Om8CompanionPresenceService` is system-bound and does not open GATT or consume
controller events. `AutoConnectServiceStarter` submits the explicit internal
auto-start action to the existing `Om8BleListenerService`. The service guards
`listenerRequested`, scan callbacks, and current GATT identity, so repeated
presence events cannot create concurrent owners. Existing bounded scan retry,
Bluetooth on/off recovery, notification watchdog, HIGH-priority request,
CCCD readiness, and stale-callback handling remain the transport lifecycle.

The default policy is association available -> connect, independent of the
camera foreground. A future battery-saving policy may gate connection on camera
activity, but it is not the default. Manual Start Bridge remains available as a
recovery/diagnostic path. Companion presence is best-effort on Android versions
where the legacy address-based observation API is available; absence of that
API does not weaken the direct GATT path.

## Broad integration boundary

The current implementation keeps independent registries and boundaries:

```text
GimbalProfileRegistry -> GimbalDriver -> ControllerEvent
                                      -> MappingEngine -> CameraAction
                                      -> CameraProfile/CameraAdapter
```

`GimbalProfile` contains only transport metadata. `CameraProfile` contains only
validated camera-application metadata. Neither contains executable commands.
Mappings are app-private, versioned, profile-scoped data. An adapter advertises
capabilities and fails closed when an action is unsupported.

The control model reserves single/double/long/hold and continuous-axis events,
but no unmeasured OM8 packet is interpreted. The proven Record edge remains
immediate; no double-press timeout is inserted into the default mapping.

The only hardware-tested camera remains Sony Video Pro. Pixel selectors are
source-evidenced but hardware-unverified, Samsung has no safe universal stock
camera contract in the evidence set, and Photography Pro's ordinary-app camera
button broadcast is tested negative. See `docs/UPSTREAM_INTEGRATION.md`.

## Consumer product lifecycle

The end-user flow is deliberately separate from the protocol-development
surface:

~~~text
Find DJI Gimbal
    -> user-approved CompanionDeviceManager association
    -> presence wake-up where Android permits
    -> connected-device foreground service
    -> existing OM8 scan/GATT/FFF4 owner
    -> Gimbal READY

foreground camera package
    -> profile registry lookup
    -> per-camera MappingStore
    -> specialized or conservative CameraAdapter
~~~

The association remembers the user-approved device concept, not a production
hardcoded MAC address. The service still performs dynamic `OM8-*` discovery and
proves FFF0/FFF4/CCCD readiness. **Disconnect** is an explicit user choice that
suppresses immediate recovery; **Forget DJI Gimbal** clears the association.

`VideoProKeyAccessibilityService` reports effective foreground package changes
to `BridgeRuntime` outside the Record callback. `BridgeApplication` selects the
matching configured profile and its mapping in memory. Unknown or unconfigured
foreground packages clear the adapter, so an OM8 event cannot control whichever
application happens to be visible.

The ordinary UI contains only connection setup, camera-app presets, and mapping
entry points. Raw FFF4 diagnostics, reconnect recovery, and learned-profile test
controls are inside the collapsed **Advanced / Diagnostics** section. The visible
Activity observes the service-published connection-status signal, so Bluetooth
off/on and reconnect transitions are reflected without recreating the screen. This
UI organization does not change the proven Record rising-edge, Sony prepared-node,
safety-snapshot, verification, or re-arm path.

### Record dispatch hot-path contract

The steady-state OM8 Record path is deliberately kept in memory:

`FFF4 callback -> control-only DUML parse -> rising-edge detection -> resident mapping/profile -> prepared Sony safety/action checks -> ACTION_CLICK`

The interval from callback entry (T0) through the start of `ACTION_CLICK` (T4)
must not perform SharedPreferences/DataStore reads or writes, mapping
serialization, UI redraw/broadcast work, human-readable diagnostic formatting,
or file I/O. Normal diagnostics are off by default; the control-only parser does
not retain a frame when diagnostics are off. Sony state verification, re-arm,
timing-log formatting, and UI/status updates happen after the click.

The GATT callback and the tested Sony bridge path currently run on the Android
main looper, so no worker-to-main scheduling hop is inserted in the hot path.
Prepared Accessibility state is validated for safety before the click; raw
`AccessibilityNodeInfo` objects are not treated as durable configuration and
are refreshed/re-armed according to the existing service lifecycle.

After the user has associated an OM8, the app declares `RECEIVE_BOOT_COMPLETED`
and uses an exported-false boot receiver to request the existing `connectedDevice`
foreground service. The receiver starts only when an association exists and the
user has not selected Disconnect. This is Android-compliant recovery, not hidden
background execution; Android may still defer delivery for a force-stopped app or
device policy, and Bluetooth/GATT readiness remains independently verified.

