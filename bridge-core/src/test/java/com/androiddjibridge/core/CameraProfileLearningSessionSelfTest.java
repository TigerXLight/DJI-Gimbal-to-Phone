package com.androiddjibridge.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Pure learning-state, confirmation, and persistence-policy regression. */
public final class CameraProfileLearningSessionSelfTest {
    public static void main(String[] args) {
        CameraProfileLearningSession session = new CameraProfileLearningSession();
        require(!session.start(null), "learning requires explicit package");
        require(!session.start("not a package"), "invalid package rejected");
        require(session.start("com.example.camera"), "valid package starts learning");

        CameraProfileCandidate wrongPackage = candidate("com.other.camera", true);
        require(!session.captureCandidate(wrongPackage), "wrong package rejected");

        CameraProfileCandidate disabled = candidate("com.example.camera", false);
        require(!session.captureCandidate(disabled), "disabled target rejected");

        CameraProfileCandidate valid = candidate("com.example.camera", true);
        require(session.captureCandidate(valid), "valid target captured");
        require(!session.isConfirmed(),
                "candidate cannot be persisted before confirmation");
        try {
            session.createConfirmedProfile("Example Camera");
            throw new AssertionError("profile creation must require confirmation");
        } catch (IllegalStateException expected) {
            // Expected fail-closed behavior.
        }
        require(session.confirmCandidate(), "explicit confirmation succeeds");
        CameraProfile learned = session.createConfirmedProfile("Example Camera");
        require(learned.getOrigin() == CameraProfileOrigin.LEARNED,
                "learned origin preserved");
        require(learned.getValidationStatus() == CameraProfileValidationStatus.UNTESTED,
                "learned profile is not hardware tested");
        require(!learned.isStateVerificationSupported(),
                "state verification is not invented");
        require(learned.getStableId().startsWith("learned.com.example.camera."),
                "stable ID is deterministic and coordinate-free");

        Set<String> builtInIds = Collections.singleton("sony-videopro");
        Set<String> builtInPackages = Collections.singleton("jp.co.sony.mc.videopro");
        require(CameraProfilePersistencePolicy.canPersistLearned(
                        learned, builtInIds, builtInPackages),
                "ordinary learned profile may be persisted");
        require(!CameraProfilePersistencePolicy.canPersistLearned(
                        CameraProfile.builder()
                                .stableId("sony-videopro")
                                .displayName("Fake")
                                .packageName("com.example.camera2")
                                .recordButtonClassName("android.widget.ImageButton")
                                .requiredAccessibilityActions(
                                        Collections.singleton(CameraAccessibilityAction.CLICK))
                                .capabilities(Collections.singleton(CameraCapability.RECORD_TOGGLE))
                                .origin(CameraProfileOrigin.LEARNED)
                                .validationStatus(CameraProfileValidationStatus.UNTESTED)
                                .build(),
                        builtInIds, builtInPackages),
                "learned profile cannot overwrite built-in ID");
        require(!CameraProfilePersistencePolicy.canPersistLearned(
                        candidate("jp.co.sony.mc.videopro", true)
                                .toLearnedProfile("Sony clone"),
                        builtInIds, builtInPackages),
                "learned profile cannot replace built-in package");
        CameraProfile noResourceProfile = CameraProfile.builder()
                .stableId("learned.com.example.text")
                .displayName("Text camera")
                .packageName("com.example.text")
                .recordButtonClassName("android.widget.ImageButton")
                .allowedContentDescriptions(Collections.singleton("Record"))
                .requiredAccessibilityActions(
                        Collections.singleton(CameraAccessibilityAction.CLICK))
                .capabilities(Collections.singleton(CameraCapability.RECORD_TOGGLE))
                .origin(CameraProfileOrigin.LEARNED)
                .validationStatus(CameraProfileValidationStatus.UNTESTED)
                .build();
        require(CameraProfileJsonCodec.toJson(noResourceProfile).contains(
                        "\"recordButtonResourceId\":null"),
                "optional resource ID serializes as null");
        require(CameraProfileJsonCodec.fromJson(
                        CameraProfileJsonCodec.toJson(noResourceProfile)).getRecordButtonResourceId()
                        == null,
                "optional resource ID round-trips");
        System.out.println("CameraProfileLearningSessionSelfTest: PASS");
    }

    private static CameraProfileCandidate candidate(String packageName, boolean enabled) {
        EnumSet<CameraAccessibilityAction> actions =
                EnumSet.of(CameraAccessibilityAction.CLICK);
        return CameraProfileCandidate.builder()
                .packageName(packageName)
                .resourceId(packageName + ":id/record")
                .className("android.widget.ImageButton")
                .contentDescription("Record")
                .supportedActions(actions)
                .clickable(true)
                .enabled(enabled)
                .visibleToUser(true)
                .windowId(3)
                .build();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
