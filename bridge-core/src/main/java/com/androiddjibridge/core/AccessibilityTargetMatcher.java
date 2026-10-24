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
        return matchesRecordTarget(profile, packageName, resourceId, className, null,
                contentDescription, null, null, availableActions);
    }

    public static boolean matchesRecordTarget(
            CameraProfile profile,
            String packageName,
            String resourceId,
            String className,
            String text,
            String contentDescription,
            String parentResourceId,
            String parentClassName,
            Set<CameraAccessibilityAction> availableActions) {
        if (profile == null
                || !profile.getPackageName().equals(packageName)
                || availableActions == null) {
            return false;
        }
        if (profile.getRecordButtonResourceId() != null
                && !profile.getRecordButtonResourceId().equals(resourceId)) {
            return false;
        }
        if (profile.getRecordButtonClassName() != null
                && !profile.getRecordButtonClassName().equals(className)) {
            return false;
        }
        if (profile.getRecordButtonText() != null
                && !profile.getRecordButtonText().equals(text)) {
            return false;
        }
        if (!profile.getAllowedContentDescriptions().isEmpty()
                || !profile.getAllowedContentDescriptionPatterns().isEmpty()) {
            if (!profile.isAllowedContentDescription(contentDescription)) {
                return false;
            }
        }
        if (profile.getParentResourceId() != null
                && !profile.getParentResourceId().equals(parentResourceId)) {
            return false;
        }
        if (profile.getParentClassName() != null
                && !profile.getParentClassName().equals(parentClassName)) {
            return false;
        }
        return availableActions.containsAll(profile.getRequiredAccessibilityActions());
    }
}
