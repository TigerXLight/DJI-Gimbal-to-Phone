package com.androiddjibridge.sony;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure safety and sequence rules for the temporary Photography Pro probe.
 *
 * <p>This is a capability experiment only. It deliberately describes one
 * fixed, package-scoped CAMERA_BUTTON DOWN/UP pair and cannot encode an
 * arbitrary package, key code, or executable action.</p>
 */
public final class PhotographyProCameraButtonProbeDecision {
    public static final String PACKAGE_NAME = "com.sonymobile.photopro";
    public static final int KEYCODE_CAMERA = 27;
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;

    private PhotographyProCameraButtonProbeDecision() {
    }

    public static boolean isSafeToProbe(
            String effectiveForegroundPackage,
            boolean interactive,
            boolean keyguardLocked) {
        return interactive
                && !keyguardLocked
                && PACKAGE_NAME.equals(normalize(effectiveForegroundPackage));
    }

    public static List<KeyEventSpec> createSequence(
            String effectiveForegroundPackage,
            boolean interactive,
            boolean keyguardLocked) {
        if (!isSafeToProbe(effectiveForegroundPackage, interactive, keyguardLocked)) {
            return Collections.emptyList();
        }
        return Arrays.asList(
                new KeyEventSpec(ACTION_DOWN, KEYCODE_CAMERA),
                new KeyEventSpec(ACTION_UP, KEYCODE_CAMERA));
    }

    public static final class KeyEventSpec {
        private final int action;
        private final int keyCode;

        private KeyEventSpec(int action, int keyCode) {
            this.action = action;
            this.keyCode = keyCode;
        }

        public int getAction() {
            return action;
        }

        public int getKeyCode() {
            return keyCode;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
