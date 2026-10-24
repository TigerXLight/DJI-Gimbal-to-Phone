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
    BridgeController
    selected CameraAdapter
             |
             v
camera-adapters/sony-videopro
    Sony Video Pro Accessibility adapter
    prepared-node and safety-snapshot owner
~~~

The Android application module supplies the UI, application initialization, manifest, and debug-only receivers. BridgeApplication installs the selected adapter in the process-local BridgeRuntime registry. The OM8 service emits normalized events through that registry and never imports Sony classes.

The normal UI Start Bridge path is the complete bridge startup path. The explicit debug OM8_LISTENER_START broadcast is a driver/listener diagnostic path; it can establish Gimbal READY without proving that a selected camera adapter is available. A driver-only start may therefore report BRIDGE_EVENT_REJECTED reason=no-selected-camera-adapter-or-camera-service.

## Module boundaries

- bridge-core: Java-only generic interfaces and event routing.
- om8-driver: Android BLE service, OM8 DUML parser, rising-edge detector, recovery, watchdog, diagnostics, and Android permission decisions.
- camera-adapters/sony-videopro: Sony package/node constants, AccessibilityService, prepared REC node, local safety snapshot, post-click verification, and re-arm logic.
- app: Android entry point and user-facing start/stop/reconnect controls.

The service uses the API 26+ connectGatt overload with a main-looper handler, LE transport, and the existing PHY behavior. Lifecycle callbacks are protected by current-GATT identity/generation checks. Only the standard FFF4 CCCD write is performed; no DJI command data is written.

## Event contract

Phase 1 normalizes RECORD_DOWN and RECORD_UP. Future event types are reserved in the core enum but are not decoded or claimed as implemented. The Sony adapter handles only RECORD_DOWN; release never toggles Sony.

