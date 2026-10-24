package com.androiddjibridge.core;

import java.util.Set;

/** Pure metadata matcher used for deterministic profile tests and future adapters. */
public final class AccessibilityTargetMatcher {
    private AccessibilityTargetMatcher() {
    }

    public static boolean matchesRecordTarget(
            CameraProfile profile,
            String packageName,
            String resourceId,
            String className,
            String contentDescription,
            Set<CameraAccessibilityAction> availableActions) {
        if (profile == null
                || !profile.getPackageName().equals(packageName)
                || !profile.getRecordButtonResourceId().equals(resourceId)
                || !profile.getRecordButtonClassName().equals(className)
                || !profile.isAllowedContentDescription(contentDescription)
                || availableActions == null) {
            return false;
        }
        return availableActions.containsAll(profile.getRequiredAccessibilityActions());
    }
}
