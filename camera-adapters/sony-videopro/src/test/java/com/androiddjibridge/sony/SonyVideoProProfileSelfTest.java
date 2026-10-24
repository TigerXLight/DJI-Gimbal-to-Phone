package com.androiddjibridge.sony;

import com.androiddjibridge.core.CameraAccessibilityAction;
import com.androiddjibridge.core.CameraCapability;
import com.androiddjibridge.core.CameraProfile;
import com.androiddjibridge.core.CameraProfileOrigin;
import com.androiddjibridge.core.CameraProfileRegistry;
import com.androiddjibridge.core.CameraProfileValidationStatus;

import java.util.Collections;

/** Regression for the sole built-in, hardware-tested camera profile. */
public final class SonyVideoProProfileSelfTest {
    public static void main(String[] args) {
        CameraProfile profile = SonyVideoProProfile.builtIn();
        require("sony-videopro".equals(profile.getStableId()), "stable profile ID");
        require("Sony Video Pro".equals(profile.getDisplayName()), "display name");
        require(SonyVideoProProfile.PACKAGE_NAME.equals(profile.getPackageName()), "package");
        require(SonyVideoProProfile.RECORDING_RESOURCE_ID.equals(
                        profile.getRecordButtonResourceId()), "record resource");
        require(SonyVideoProProfile.REC_STATUS_RESOURCE_ID.equals(
                        profile.getStateResourceId()), "status resource");
        require(SonyVideoProProfile.DURATION_RESOURCE_ID.equals(
                        profile.getDurationResourceId()), "duration resource");
        require("android.widget.ImageButton".equals(profile.getRecordButtonClassName()),
                "record class");
        require(profile.isAllowedContentDescription("Start video recording"),
                "start description");
        require(profile.isAllowedContentDescription("Stop video recording"),
                "stop description");
        require(profile.requiresAction(CameraAccessibilityAction.CLICK), "click action");
        require(profile.supports(CameraCapability.RECORD_TOGGLE), "record capability");
        require(profile.getOrigin() == CameraProfileOrigin.BUILTIN, "origin");
        require(profile.getValidationStatus() == CameraProfileValidationStatus.HARDWARE_TESTED,
                "validation status");
        require(profile.isStateVerificationSupported(), "state verification");

        CameraProfileRegistry registry = new CameraProfileRegistry(
                Collections.singleton(profile), profile.getStableId());
        require(registry.getProfiles().size() == 1, "exactly one built-in profile");
        require(registry.findByStableId(profile.getStableId()).orElse(null) == profile,
                "ID registry lookup");
        require(registry.findByPackageName(profile.getPackageName()).orElse(null) == profile,
                "package registry lookup");
        require(new SonyVideoProCameraAdapter().profile().getStableId()
                        .equals(profile.getStableId()),
                "adapter/profile identity");
        System.out.println("SonyVideoProProfileSelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
