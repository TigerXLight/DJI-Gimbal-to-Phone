package com.androiddjibridge.sony;

import java.util.List;

/** Regression for the fixed, fail-closed Photography Pro capability probe. */
public final class PhotographyProCameraButtonProbeDecisionSelfTest {
    public static void main(String[] args) {
        require("com.sonymobile.photopro".equals(
                        PhotographyProCameraButtonProbeDecision.PACKAGE_NAME),
                "exact Photography Pro package");
        require(PhotographyProCameraButtonProbeDecision.KEYCODE_CAMERA == 27,
                "KEYCODE_CAMERA");

        List<PhotographyProCameraButtonProbeDecision.KeyEventSpec> sequence =
                PhotographyProCameraButtonProbeDecision.createSequence(
                        "com.sonymobile.photopro", true, false);
        require(sequence.size() == 2, "one DOWN/UP pair");
        require(sequence.get(0).getAction()
                        == PhotographyProCameraButtonProbeDecision.ACTION_DOWN,
                "first event is DOWN");
        require(sequence.get(1).getAction()
                        == PhotographyProCameraButtonProbeDecision.ACTION_UP,
                "second event is UP");
        require(sequence.get(0).getKeyCode()
                        == PhotographyProCameraButtonProbeDecision.KEYCODE_CAMERA,
                "DOWN key code");
        require(sequence.get(1).getKeyCode()
                        == PhotographyProCameraButtonProbeDecision.KEYCODE_CAMERA,
                "UP key code");
        require(PhotographyProCameraButtonProbeDecision.isSafeToProbe(
                        "com.sonymobile.photopro", true, false),
                "target foreground and safe device accepted");
        require(!PhotographyProCameraButtonProbeDecision.isSafeToProbe(
                        "com.androiddjibridge", true, false),
                "wrong foreground rejected");
        require(!PhotographyProCameraButtonProbeDecision.isSafeToProbe(
                        "com.sonymobile.photopro", false, false),
                "non-interactive device rejected");
        require(!PhotographyProCameraButtonProbeDecision.isSafeToProbe(
                        "com.sonymobile.photopro", true, true),
                "keyguard rejected");
        require(PhotographyProCameraButtonProbeDecision.createSequence(
                        "com.other.camera", true, false).isEmpty(),
                "wrong package produces no sequence");
        require(PhotographyProCameraButtonProbeDecision.createSequence(
                        "com.sonymobile.photopro", true, true).isEmpty(),
                "locked device produces no sequence");
        System.out.println("PhotographyProCameraButtonProbeDecisionSelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
