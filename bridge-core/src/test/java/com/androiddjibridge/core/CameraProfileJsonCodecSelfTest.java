package com.androiddjibridge.core;

import java.util.Collections;

/** Pure validation and deterministic-serialization regression for camera profiles. */
public final class CameraProfileJsonCodecSelfTest {
    public static void main(String[] args) {
        CameraProfile profile = sampleProfile();
        String first = CameraProfileJsonCodec.toJson(profile);
        CameraProfile parsed = CameraProfileJsonCodec.fromJson(first);
        String second = CameraProfileJsonCodec.toJson(parsed);
        require(first.equals(second), "profile serialization must be deterministic");

        CameraProfileRegistry registry = new CameraProfileRegistry(
                Collections.singleton(profile), profile.getStableId());
        require(registry.findByStableId(profile.getStableId()).orElse(null) == profile,
                "stable ID lookup");
        require(registry.findByPackageName(profile.getPackageName()).orElse(null) == profile,
                "package lookup");
        require(registry.getDefaultProfile().orElse(null) == profile, "default profile");

        require(AccessibilityTargetMatcher.matchesRecordTarget(
                        profile,
                        profile.getPackageName(),
                        profile.getRecordButtonResourceId(),
                        profile.getRecordButtonClassName(),
                        "Record",
                        Collections.singleton(CameraAccessibilityAction.CLICK)),
                "matching accessibility target");
        require(!AccessibilityTargetMatcher.matchesRecordTarget(
                        profile,
                        "com.other.camera",
                        profile.getRecordButtonResourceId(),
                        profile.getRecordButtonClassName(),
                        "Record",
                        Collections.singleton(CameraAccessibilityAction.CLICK)),
                "wrong package must not match");

        expectFailure("missing required field", () -> CameraProfileJsonCodec.fromJson(
                first.replace("\"stableProfileId\":\"test-camera\",", "")));
        expectUnsupportedSchema(first);
        expectFailure("invalid package", () -> CameraProfileJsonCodec.fromJson(
                first.replace("\"packageName\":\"com.example.camera\"",
                        "\"packageName\":\"not a package\"")));
        expectFailure("invalid resource", () -> CameraProfileJsonCodec.fromJson(
                first.replace("com.example.camera:id/recording", "not-a-resource")));
        expectFailure("unknown capability", () -> CameraProfileJsonCodec.fromJson(
                first.replace("\"RECORD_TOGGLE\"", "\"SHELL\"")));
        expectFailure("unknown action", () -> CameraProfileJsonCodec.fromJson(
                first.replace("\"CLICK\"", "\"SHELL\"")));
        System.out.println("CameraProfileJsonCodecSelfTest: PASS");
    }

    private static CameraProfile sampleProfile() {
        return CameraProfile.builder()
                .stableId("test-camera")
                .displayName("Test camera")
                .packageName("com.example.camera")
                .recordButtonResourceId("com.example.camera:id/recording")
                .recordButtonClassName("android.widget.ImageButton")
                .allowedContentDescriptions(Collections.singleton("Record"))
                .stateResourceId("com.example.camera:id/rec_status")
                .recordingStateValues(Collections.singleton("REC"))
                .standbyStateValues(Collections.singleton("Stby"))
                .durationResourceId("com.example.camera:id/duration")
                .stateVerificationSupported(true)
                .requiredAccessibilityActions(
                        Collections.singleton(CameraAccessibilityAction.CLICK))
                .capabilities(Collections.singleton(CameraCapability.RECORD_TOGGLE))
                .origin(CameraProfileOrigin.IMPORTED)
                .validationStatus(CameraProfileValidationStatus.UNTESTED)
                .notes("test profile")
                .build();
    }

    private static void expectUnsupportedSchema(String json) {
        expectFailure("unsupported schema", () -> CameraProfileJsonCodec.fromJson(
                json.replace("\"schemaVersion\":1", "\"schemaVersion\":99")),
                CameraProfileJsonCodec.UnsupportedSchemaException.class);
    }

    private static void expectFailure(String label, Runnable action) {
        expectFailure(label, action, IllegalArgumentException.class);
    }

    private static void expectFailure(
            String label, Runnable action, Class<? extends RuntimeException> expectedType) {
        try {
            action.run();
            throw new AssertionError(label + " must fail");
        } catch (RuntimeException error) {
            if (!expectedType.isInstance(error)) {
                throw new AssertionError(label + " failed with wrong exception", error);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
