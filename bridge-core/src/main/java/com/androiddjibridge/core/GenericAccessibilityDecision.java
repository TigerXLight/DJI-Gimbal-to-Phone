package com.androiddjibridge.core;

/** Fail-closed decision rules for a learned profile test action. */
public final class GenericAccessibilityDecision {
    private GenericAccessibilityDecision() {
    }

    public static boolean canAttemptClick(
            String configuredPackage,
            String activePackage,
            boolean interactive,
            boolean keyguardLocked,
            int exactMatchCount) {
        return configuredPackage != null
                && configuredPackage.equals(activePackage)
                && interactive
                && !keyguardLocked
                && exactMatchCount == 1;
    }
}
