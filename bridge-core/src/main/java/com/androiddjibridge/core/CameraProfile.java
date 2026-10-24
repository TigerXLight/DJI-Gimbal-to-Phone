package com.androiddjibridge.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Immutable camera-application metadata. It contains data, not executable actions. */
public final class CameraProfile {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Pattern PROFILE_ID_PATTERN =
            Pattern.compile("^[a-z0-9][a-z0-9._-]*$");
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$");
    private static final Pattern RESOURCE_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9_.]+:id/[A-Za-z0-9_]+$");
    private static final Pattern CLASS_NAME_PATTERN =
            Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+$");

    private final int schemaVersion;
    private final String stableId;
    private final String displayName;
    private final String packageName;
    private final String recordButtonResourceId;
    private final String recordButtonClassName;
    private final String recordButtonText;
    private final List<String> allowedContentDescriptions;
    private final List<String> allowedContentDescriptionPatterns;
    private final String parentResourceId;
    private final String parentClassName;
    private final List<String> requiredChildResourceIds;
    private final Set<CameraAccessibilityAction> requiredAccessibilityActions;
    private final String stateResourceId;
    private final List<String> recordingStateValues;
    private final List<String> standbyStateValues;
    private final String durationResourceId;
    private final boolean stateVerificationSupported;
    private final Set<CameraCapability> capabilities;
    private final CameraProfileOrigin origin;
    private final CameraProfileValidationStatus validationStatus;
    private final String notes;

    private CameraProfile(Builder builder) {
        schemaVersion = builder.schemaVersion;
        stableId = requireProfileId(builder.stableId);
        displayName = requireText(builder.displayName, "displayName");
        packageName = requirePackage(builder.packageName);
        recordButtonResourceId = optionalResourceId(
                builder.recordButtonResourceId, "recordButtonResourceId");
        recordButtonClassName = optionalClassName(
                builder.recordButtonClassName, "recordButtonClassName");
        recordButtonText = optionalText(builder.recordButtonText, "recordButtonText");
        allowedContentDescriptions = normalizeTextList(
                builder.allowedContentDescriptions, "allowedContentDescriptions");
        allowedContentDescriptionPatterns = normalizeTextList(
                builder.allowedContentDescriptionPatterns,
                "allowedContentDescriptionPatterns");
        parentResourceId = optionalResourceId(builder.parentResourceId, "parentResourceId");
        parentClassName = optionalClassName(builder.parentClassName, "parentClassName");
        requiredChildResourceIds = normalizeResourceList(
                builder.requiredChildResourceIds, "requiredChildResourceIds");
        requiredAccessibilityActions = normalizeActions(
                builder.requiredAccessibilityActions);
        stateResourceId = optionalResourceId(builder.stateResourceId, "stateResourceId");
        recordingStateValues = normalizeTextList(
                builder.recordingStateValues, "recordingStateValues");
        standbyStateValues = normalizeTextList(
                builder.standbyStateValues, "standbyStateValues");
        durationResourceId = optionalResourceId(builder.durationResourceId, "durationResourceId");
        stateVerificationSupported = builder.stateVerificationSupported;
        capabilities = normalizeCapabilities(builder.capabilities);
        origin = requireEnum(builder.origin, "origin");
        validationStatus = requireEnum(builder.validationStatus, "validationStatus");
        notes = builder.notes == null ? "" : builder.notes.trim();
        if (notes.length() > 2048) {
            throw new IllegalArgumentException("notes is too long");
        }
        if (stateVerificationSupported
                && stateResourceId == null
                && recordingStateValues.isEmpty()
                && standbyStateValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "state verification requires a state resource or known state values");
        }
        if (recordButtonResourceId == null
                && recordButtonClassName == null
                && recordButtonText == null
                && allowedContentDescriptions.isEmpty()
                && allowedContentDescriptionPatterns.isEmpty()) {
            throw new IllegalArgumentException(
                    "record control requires at least one identity field");
        }
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported profile schema version "
                    + schemaVersion);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getStableId() {
        return stableId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getRecordButtonResourceId() {
        return recordButtonResourceId;
    }

    public String getRecordButtonClassName() {
        return recordButtonClassName;
    }

    public String getRecordButtonText() {
        return recordButtonText;
    }

    public List<String> getAllowedContentDescriptions() {
        return allowedContentDescriptions;
    }

    public List<String> getAllowedContentDescriptionPatterns() {
        return allowedContentDescriptionPatterns;
    }

    public String getParentResourceId() {
        return parentResourceId;
    }

    public String getParentClassName() {
        return parentClassName;
    }

    public List<String> getRequiredChildResourceIds() {
        return requiredChildResourceIds;
    }

    public Set<CameraAccessibilityAction> getRequiredAccessibilityActions() {
        return requiredAccessibilityActions;
    }

    public String getStateResourceId() {
        return stateResourceId;
    }

    public List<String> getRecordingStateValues() {
        return recordingStateValues;
    }

    public List<String> getStandbyStateValues() {
        return standbyStateValues;
    }

    public String getDurationResourceId() {
        return durationResourceId;
    }

    public boolean isStateVerificationSupported() {
        return stateVerificationSupported;
    }

    public Set<CameraCapability> getCapabilities() {
        return capabilities;
    }

    public CameraProfileOrigin getOrigin() {
        return origin;
    }

    public CameraProfileValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public String getNotes() {
        return notes;
    }

    public boolean supports(CameraCapability capability) {
        return capability != null && capabilities.contains(capability);
    }

    public boolean requiresAction(CameraAccessibilityAction action) {
        return action != null && requiredAccessibilityActions.contains(action);
    }

    public boolean isAllowedContentDescription(String value) {
        if (value == null) {
            return false;
        }
        if (allowedContentDescriptions.contains(value)) {
            return true;
        }
        for (String pattern : allowedContentDescriptionPatterns) {
            if (globMatches(pattern, value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean globMatches(String pattern, String value) {
        if (pattern.indexOf('*') < 0) {
            return pattern.equals(value);
        }
        String[] pieces = pattern.split("\\*", -1);
        int position = 0;
        boolean first = true;
        for (int i = 0; i < pieces.length; i++) {
            String piece = pieces[i];
            if (first) {
                if (!value.startsWith(piece)) {
                    return false;
                }
                position = piece.length();
                first = false;
            } else if (i == pieces.length - 1) {
                return value.substring(position).endsWith(piece);
            } else {
                int found = value.indexOf(piece, position);
                if (found < 0) {
                    return false;
                }
                position = found + piece.length();
            }
        }
        return true;
    }

    public static final class Builder {
        private int schemaVersion = CURRENT_SCHEMA_VERSION;
        private String stableId;
        private String displayName;
        private String packageName;
        private String recordButtonResourceId;
        private String recordButtonClassName;
        private String recordButtonText;
        private Collection<String> allowedContentDescriptions = Collections.emptyList();
        private Collection<String> allowedContentDescriptionPatterns = Collections.emptyList();
        private String parentResourceId;
        private String parentClassName;
        private Collection<String> requiredChildResourceIds = Collections.emptyList();
        private Collection<CameraAccessibilityAction> requiredAccessibilityActions =
                Collections.emptyList();
        private String stateResourceId;
        private Collection<String> recordingStateValues = Collections.emptyList();
        private Collection<String> standbyStateValues = Collections.emptyList();
        private String durationResourceId;
        private boolean stateVerificationSupported;
        private Collection<CameraCapability> capabilities = Collections.emptyList();
        private CameraProfileOrigin origin;
        private CameraProfileValidationStatus validationStatus;
        private String notes;

        public Builder schemaVersion(int value) { schemaVersion = value; return this; }
        public Builder stableId(String value) { stableId = value; return this; }
        public Builder displayName(String value) { displayName = value; return this; }
        public Builder packageName(String value) { packageName = value; return this; }
        public Builder recordButtonResourceId(String value) {
            recordButtonResourceId = value; return this;
        }
        public Builder recordButtonClassName(String value) {
            recordButtonClassName = value; return this;
        }
        public Builder recordButtonText(String value) {
            recordButtonText = value; return this;
        }
        public Builder allowedContentDescriptions(Collection<String> value) {
            allowedContentDescriptions = value; return this;
        }
        public Builder allowedContentDescriptionPatterns(Collection<String> value) {
            allowedContentDescriptionPatterns = value; return this;
        }
        public Builder parentResourceId(String value) { parentResourceId = value; return this; }
        public Builder parentClassName(String value) { parentClassName = value; return this; }
        public Builder requiredChildResourceIds(Collection<String> value) {
            requiredChildResourceIds = value; return this;
        }
        public Builder requiredAccessibilityActions(
                Collection<CameraAccessibilityAction> value) {
            requiredAccessibilityActions = value; return this;
        }
        public Builder stateResourceId(String value) { stateResourceId = value; return this; }
        public Builder recordingStateValues(Collection<String> value) {
            recordingStateValues = value; return this;
        }
        public Builder standbyStateValues(Collection<String> value) {
            standbyStateValues = value; return this;
        }
        public Builder durationResourceId(String value) { durationResourceId = value; return this; }
        public Builder stateVerificationSupported(boolean value) {
            stateVerificationSupported = value; return this;
        }
        public Builder capabilities(Collection<CameraCapability> value) {
            capabilities = value; return this;
        }
        public Builder origin(CameraProfileOrigin value) { origin = value; return this; }
        public Builder validationStatus(CameraProfileValidationStatus value) {
            validationStatus = value; return this;
        }
        public Builder notes(String value) { notes = value; return this; }
        public CameraProfile build() { return new CameraProfile(this); }
    }

    private static String requireProfileId(String value) {
        String text = requireText(value, "stableId");
        if (!PROFILE_ID_PATTERN.matcher(text).matches()) {
            throw new IllegalArgumentException("invalid stableId");
        }
        return text;
    }

    private static String requirePackage(String value) {
        String text = requireText(value, "packageName");
        if (!PACKAGE_PATTERN.matcher(text).matches()) {
            throw new IllegalArgumentException("invalid packageName");
        }
        return text;
    }

    private static String requireResourceId(String value, String field) {
        String text = requireText(value, field);
        if (!RESOURCE_ID_PATTERN.matcher(text).matches()) {
            throw new IllegalArgumentException("invalid " + field);
        }
        return text;
    }

    private static String optionalResourceId(String value, String field) {
        return value == null || value.trim().isEmpty() ? null : requireResourceId(value, field);
    }

    private static String requireClassName(String value, String field) {
        String text = requireText(value, field);
        if (!CLASS_NAME_PATTERN.matcher(text).matches()) {
            throw new IllegalArgumentException("invalid " + field);
        }
        return text;
    }

    private static String optionalClassName(String value, String field) {
        return value == null || value.trim().isEmpty() ? null : requireClassName(value, field);
    }

    private static String optionalText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field + " contains a line break");
        }
        return text;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static <T extends Enum<T>> T requireEnum(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static List<String> normalizeTextList(
            Collection<String> values, String field) {
        TreeSet<String> sorted = new TreeSet<>();
        if (values != null) {
            for (String value : values) {
                String text = requireText(value, field);
                if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
                    throw new IllegalArgumentException(field + " contains a line break");
                }
                sorted.add(text);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(sorted));
    }

    private static List<String> normalizeResourceList(
            Collection<String> values, String field) {
        TreeSet<String> sorted = new TreeSet<>();
        if (values != null) {
            for (String value : values) {
                sorted.add(requireResourceId(value, field));
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(sorted));
    }

    private static Set<CameraAccessibilityAction> normalizeActions(
            Collection<CameraAccessibilityAction> values) {
        EnumSet<CameraAccessibilityAction> result =
                EnumSet.noneOf(CameraAccessibilityAction.class);
        if (values != null) {
            for (CameraAccessibilityAction value : values) {
                if (value == null) {
                    throw new IllegalArgumentException("required action cannot be null");
                }
                result.add(value);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("at least one Accessibility action is required");
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<CameraCapability> normalizeCapabilities(
            Collection<CameraCapability> values) {
        EnumSet<CameraCapability> result = EnumSet.noneOf(CameraCapability.class);
        if (values != null) {
            for (CameraCapability value : values) {
                if (value == null) {
                    throw new IllegalArgumentException("capability cannot be null");
                }
                result.add(value);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("at least one capability is required");
        }
        return Collections.unmodifiableSet(result);
    }
}
