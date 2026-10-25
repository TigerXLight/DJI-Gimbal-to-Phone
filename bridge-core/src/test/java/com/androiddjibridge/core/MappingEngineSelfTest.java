package com.androiddjibridge.core;

import java.util.Collections;

/** Pure mapping, persistence, fail-closed, and continuous-input regression. */
public final class MappingEngineSelfTest {
    public static void main(String[] args) {
        MappingEngine engine = new MappingEngine();
        ControllerEvent press = event(ControllerEvent.Type.RECORD_DOWN);
        ControllerEvent release = event(ControllerEvent.Type.RECORD_UP);

        require(engine.resolve(press).getAction() == CameraAction.RECORD_TOGGLE,
                "default record press mapping");
        require(engine.resolve(release).getAction() == CameraAction.NONE,
                "record release is a no-op");
        require(!engine.resolve(new ControllerEvent(
                        null, "test", 3L, 1L, 2L, true, 1L))
                        .isRecognizedTrigger(),
                "unknown trigger is rejected");

        MappingStore remapped = engine.getStore()
                .withMapping(ControllerTrigger.RECORD_PRESS, CameraAction.CAPTURE_PHOTO);
        engine.setStore(remapped);
        require(engine.resolve(press).getAction() == CameraAction.CAPTURE_PHOTO,
                "user mapping changes action");
        String serialized = MappingJsonCodec.toJson(remapped);
        require(serialized.equals(MappingJsonCodec.toJson(MappingJsonCodec.fromJson(serialized))),
                "mapping JSON round trip is deterministic");
        MappingStore reloaded = MappingJsonCodec.fromJson(serialized);
        require(reloaded.actionFor(ControllerTrigger.RECORD_PRESS) == CameraAction.CAPTURE_PHOTO,
                "explicit saved action survives reload");
        require(MappingStore.defaults().actionFor(ControllerTrigger.RECORD_PRESS)
                        == CameraAction.RECORD_TOGGLE,
                "Sony-safe default remains record toggle");

        require(engine.getStore().withMapping(
                        ControllerTrigger.SLIDER_AXIS, CameraAction.ZOOM_AXIS)
                        .actionFor(ControllerTrigger.SLIDER_AXIS)
                == CameraAction.ZOOM_AXIS, "continuous mapping remains distinct");
        require(CameraAction.ZOOM_AXIS.isContinuous(), "axis action is continuous");
        expectFailure(() -> MappingJsonCodec.fromJson(
                "{\"schemaVersion\":1,\"mappings\":{\"UNKNOWN\":\"RECORD_TOGGLE\"}}"),
                "unknown trigger is rejected by codec");
        expectFailure(() -> MappingJsonCodec.fromJson(
                "{\"schemaVersion\":99,\"mappings\":{}}"),
                "future schema is rejected");
        expectFailure(() -> MappingJsonCodec.fromJson("not-json"),
                "malformed mapping falls closed");

        CameraProfile profile = CameraProfile.builder()
                .stableId("test-camera")
                .displayName("Test camera")
                .packageName("com.example.camera")
                .recordButtonClassName("android.widget.ImageButton")
                .requiredAccessibilityActions(
                        Collections.singleton(CameraAccessibilityAction.CLICK))
                .capabilities(Collections.singleton(CameraCapability.RECORD_TOGGLE))
                .origin(CameraProfileOrigin.IMPORTED)
                .validationStatus(CameraProfileValidationStatus.UNTESTED)
                .build();
        BridgeController unsupported = new BridgeController(
                new CameraProfileRegistry(Collections.singleton(profile), profile.getStableId()),
                profile,
                new MappingEngine(MappingStore.defaults()
                        .withMapping(ControllerTrigger.RECORD_PRESS, CameraAction.CAPTURE_PHOTO)),
                new UnsupportedAdapter(profile));
        require(!unsupported.onControllerEvent(press),
                "unsupported action fails closed");

        System.out.println("MappingEngineSelfTest: PASS");
    }

    private static ControllerEvent event(ControllerEvent.Type type) {
        return new ControllerEvent(type, "test", 1L, 10L, 11L, true, 7L);
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static final class UnsupportedAdapter implements CameraAdapter {
        private final CameraProfile profile;

        private UnsupportedAdapter(CameraProfile profile) {
            this.profile = profile;
        }

        @Override public String id() { return "unsupported"; }
        @Override public String displayName() { return "Unsupported"; }
        @Override public CameraProfile profile() { return profile; }
        @Override public boolean isAvailable() { return true; }
        @Override public void prepare() { }
        @Override public boolean supports(CameraAction action) { return false; }
        @Override public boolean handleControllerEvent(ControllerEvent event) { return false; }
        @Override public CameraAdapterState getState() { return CameraAdapterState.READY; }
        @Override public void release() { }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
