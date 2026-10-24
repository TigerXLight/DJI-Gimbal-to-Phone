package com.androiddjibridge.core;

/**
 * Evidence-backed Pixel Camera selectors from video-boost-ao.
 *
 * These selectors describe video settings/mode controls, not a proven shutter
 * target. They are deliberately not exposed as a clickable built-in profile.
 */
public final class PixelCameraProfileEvidence {
    public static final String PACKAGE_NAME = "com.google.android.GoogleCamera";
    public static final String VIDEO_SETTINGS_ENTRY_ID =
            PACKAGE_NAME + ":id/options_entry_button";
    public static final String MODE_CHIP_TEXT_ID = PACKAGE_NAME + ":id/mode_chip_text";
    public static final CameraProfileValidationStatus STATUS =
            CameraProfileValidationStatus.IMPLEMENTED_HARDWARE_UNVERIFIED;

    private PixelCameraProfileEvidence() {
    }
}
