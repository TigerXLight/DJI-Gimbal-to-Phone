package com.androiddjibridge.core;

public final class CameraBackendEvidenceSelfTest {
    public static void main(String[] args) {
        if (!PixelCameraProfileEvidence.PACKAGE_NAME.equals(
                "com.google.android.GoogleCamera")
                || PixelCameraProfileEvidence.STATUS
                != CameraProfileValidationStatus.IMPLEMENTED_HARDWARE_UNVERIFIED) {
            throw new AssertionError("Pixel evidence regression");
        }
        if (SamsungCameraProfileEvidence.STATUS
                != CameraProfileValidationStatus.UNSUPPORTED_NO_SAFE_PATH) {
            throw new AssertionError("Samsung safety status regression");
        }
    }
}
