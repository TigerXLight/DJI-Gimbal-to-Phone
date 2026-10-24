package com.androiddjibridge.sony;

import com.androiddjibridge.core.CameraAccessibilityAction;
import com.androiddjibridge.core.CameraCapability;
import com.androiddjibridge.core.CameraProfile;
import com.androiddjibridge.core.CameraProfileOrigin;
import com.androiddjibridge.core.CameraProfileValidationStatus;

import java.util.Arrays;

/** The only built-in camera profile, backed by the Phase 1 hardware evidence. */
public final class SonyVideoProProfile {
    public static final String PROFILE_ID = "sony-videopro";
    public static final String PACKAGE_NAME = "jp.co.sony.mc.videopro";
    public static final String RECORDING_RESOURCE_ID = PACKAGE_NAME + ":id/recording";
    public static final String REC_STATUS_RESOURCE_ID = PACKAGE_NAME + ":id/rec_status";
    public static final String DURATION_RESOURCE_ID = PACKAGE_NAME + ":id/duration";
    public static final String RECORDING_CLASS_NAME = "android.widget.ImageButton";
    public static final String START_DESCRIPTION = "Start video recording";
    public static final String STOP_DESCRIPTION = "Stop video recording";

    private static final CameraProfile BUILT_IN = CameraProfile.builder()
            .stableId(PROFILE_ID)
            .displayName("Sony Video Pro")
            .packageName(PACKAGE_NAME)
            .recordButtonResourceId(RECORDING_RESOURCE_ID)
            .recordButtonClassName(RECORDING_CLASS_NAME)
            .allowedContentDescriptions(Arrays.asList(START_DESCRIPTION, STOP_DESCRIPTION))
            .requiredAccessibilityActions(
                    Arrays.asList(CameraAccessibilityAction.CLICK))
            .stateResourceId(REC_STATUS_RESOURCE_ID)
            .recordingStateValues(Arrays.asList("REC"))
            .standbyStateValues(Arrays.asList("Stby"))
            .durationResourceId(DURATION_RESOURCE_ID)
            .stateVerificationSupported(true)
            .capabilities(Arrays.asList(CameraCapability.RECORD_TOGGLE))
            .origin(CameraProfileOrigin.BUILTIN)
            .validationStatus(CameraProfileValidationStatus.HARDWARE_TESTED)
            .notes("Phase 1 hardware-tested on Sony Xperia 1 IV.")
            .build();

    private SonyVideoProProfile() {
    }

    public static CameraProfile builtIn() {
        return BUILT_IN;
    }
}
