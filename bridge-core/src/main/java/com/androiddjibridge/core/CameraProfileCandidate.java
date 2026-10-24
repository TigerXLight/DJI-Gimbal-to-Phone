package com.androiddjibridge.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable metadata captured from one deliberate Learn Camera click. */
public final class CameraProfileCandidate {
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$");

    private final String packageName;
    private final String resourceId;
    private final String className;
    private final String text;
    private final String contentDescription;
    private final String parentResourceId;
    private final String parentClassName;
    private final Set<CameraAccessibilityAction> supportedActions;
    private final boolean clickable;
    private final boolean enabled;
    private final boolean visibleToUser;
    private final int windowId;
    private final int sourceEventType;

    private CameraProfileCandidate(Builder builder) {
        packageName = required(builder.packageName, "packageName");
        if (!PACKAGE_PATTERN.matcher(packageName).matches()) {
            throw new IllegalArgumentException("invalid packageName");
        }
        resourceId = optional(builder.resourceId);
        className = optional(builder.className);
        text = optional(builder.text);
        contentDescription = optional(builder.contentDescription);
        parentResourceId = optional(builder.parentResourceId);
        parentClassName = optional(builder.parentClassName);
        EnumSet<CameraAccessibilityAction> actions =
                EnumSet.noneOf(CameraAccessibilityAction.class);
        if (builder.supportedActions != null) {
            actions.addAll(builder.supportedActions);
        }
        supportedActions = Collections.unmodifiableSet(actions);
        clickable = builder.clickable;
        enabled = builder.enabled;
        visibleToUser = builder.visibleToUser;
        windowId = builder.windowId;
        sourceEventType = builder.sourceEventType;
        if (!hasIdentity()) {
            throw new IllegalArgumentException("candidate has no stable control identity");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getPackageName() { return packageName; }
    public String getResourceId() { return resourceId; }
    public String getClassName() { return className; }
    public String getText() { return text; }
    public String getContentDescription() { return contentDescription; }
    public String getParentResourceId() { return parentResourceId; }
    public String getParentClassName() { return parentClassName; }
    public Set<CameraAccessibilityAction> getSupportedActions() { return supportedActions; }
    public boolean isClickable() { return clickable; }
    public boolean isEnabled() { return enabled; }
    public boolean isVisibleToUser() { return visibleToUser; }
    public int getWindowId() { return windowId; }
    public int getSourceEventType() { return sourceEventType; }

    public boolean isStructurallyUsable() {
        return clickable && enabled && visibleToUser
                && supportedActions.contains(CameraAccessibilityAction.CLICK)
                && hasIdentity();
    }

    public CameraProfile toLearnedProfile(String displayName) {
        if (!isStructurallyUsable()) {
            throw new IllegalStateException("candidate is not a usable clickable target");
        }
        CameraProfile.Builder profile = CameraProfile.builder()
                .stableId(deterministicProfileId())
                .displayName(displayName == null || displayName.trim().isEmpty()
                        ? packageName : displayName.trim())
                .packageName(packageName)
                .recordButtonResourceId(resourceId)
                .recordButtonClassName(className)
                .recordButtonText(text)
                .parentResourceId(parentResourceId)
                .parentClassName(parentClassName)
                .requiredAccessibilityActions(
                        Collections.singleton(CameraAccessibilityAction.CLICK))
                .capabilities(Collections.singleton(CameraCapability.RECORD_TOGGLE))
                .origin(CameraProfileOrigin.LEARNED)
                .validationStatus(CameraProfileValidationStatus.UNTESTED)
                .stateVerificationSupported(false)
                .notes("Captured by Learn Camera; control and state are unverified.");
        if (contentDescription != null) {
            profile.allowedContentDescriptions(Collections.singleton(contentDescription));
        }
        return profile.build();
    }

    public String deterministicProfileId() {
        String suffix;
        if (resourceId != null && resourceId.contains(":id/")) {
            suffix = resourceId.substring(resourceId.indexOf(":id/") + 4);
        } else {
            suffix = "control-" + shortHash(packageName + "|" + className + "|"
                    + text + "|" + contentDescription + "|" + parentResourceId);
        }
        suffix = suffix.toLowerCase(Locale.US).replaceAll("[^a-z0-9._-]", "_");
        return "learned." + packageName.toLowerCase(Locale.US) + "." + suffix;
    }

    private boolean hasIdentity() {
        return resourceId != null || className != null || text != null
                || contentDescription != null;
    }

    private static String required(String value, String field) {
        String text = optional(value);
        if (text == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return text;
    }

    private static String optional(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                result.append(String.format(Locale.US, "%02x", digest[i] & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }

    public static final class Builder {
        private String packageName;
        private String resourceId;
        private String className;
        private String text;
        private String contentDescription;
        private String parentResourceId;
        private String parentClassName;
        private Set<CameraAccessibilityAction> supportedActions;
        private boolean clickable;
        private boolean enabled;
        private boolean visibleToUser;
        private int windowId = -1;
        private int sourceEventType;

        public Builder packageName(String value) { packageName = value; return this; }
        public Builder resourceId(String value) { resourceId = value; return this; }
        public Builder className(String value) { className = value; return this; }
        public Builder text(String value) { text = value; return this; }
        public Builder contentDescription(String value) {
            contentDescription = value; return this;
        }
        public Builder parentResourceId(String value) {
            parentResourceId = value; return this;
        }
        public Builder parentClassName(String value) { parentClassName = value; return this; }
        public Builder supportedActions(Set<CameraAccessibilityAction> value) {
            supportedActions = value; return this;
        }
        public Builder clickable(boolean value) { clickable = value; return this; }
        public Builder enabled(boolean value) { enabled = value; return this; }
        public Builder visibleToUser(boolean value) { visibleToUser = value; return this; }
        public Builder windowId(int value) { windowId = value; return this; }
        public Builder sourceEventType(int value) { sourceEventType = value; return this; }
        public CameraProfileCandidate build() { return new CameraProfileCandidate(this); }
    }
}
