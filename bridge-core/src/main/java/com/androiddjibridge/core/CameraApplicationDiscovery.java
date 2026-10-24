package com.androiddjibridge.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure merge/filter rules for the package candidates shown by Learn Camera. */
public final class CameraApplicationDiscovery {
    private CameraApplicationDiscovery() {
    }

    public enum Source {
        LAUNCHABLE,
        VIDEO_CAPTURE,
        IMAGE_CAPTURE,
        BUILTIN_PROFILE
    }

    public static final class Descriptor {
        private final String packageName;
        private final String displayName;
        private final Source source;
        private final boolean systemApplication;

        public Descriptor(
                String packageName,
                String displayName,
                Source source,
                boolean systemApplication) {
            this.packageName = packageName;
            this.displayName = displayName;
            this.source = source;
            this.systemApplication = systemApplication;
        }

        public String getPackageName() { return packageName; }
        public String getDisplayName() { return displayName; }
        public Source getSource() { return source; }
        public boolean isSystemApplication() { return systemApplication; }
    }

    public static final class Candidate {
        private final String packageName;
        private final String displayName;
        private final EnumSet<Source> sources;
        private final boolean systemApplication;

        private Candidate(
                String packageName,
                String displayName,
                EnumSet<Source> sources,
                boolean systemApplication) {
            this.packageName = packageName;
            this.displayName = displayName;
            this.sources = EnumSet.copyOf(sources);
            this.systemApplication = systemApplication;
        }

        public String getPackageName() { return packageName; }
        public String getDisplayName() { return displayName; }
        public EnumSet<Source> getSources() { return EnumSet.copyOf(sources); }
        public boolean isSystemApplication() { return systemApplication; }
        public boolean hasSource(Source source) { return sources.contains(source); }
    }

    public static List<Candidate> merge(
            Collection<Descriptor> descriptors,
            String ownPackageName) {
        Map<String, MutableCandidate> byPackage = new LinkedHashMap<>();
        if (descriptors != null) {
            for (Descriptor descriptor : descriptors) {
                if (descriptor == null || descriptor.getSource() == null) {
                    continue;
                }
                String packageName = normalized(descriptor.getPackageName());
                if (packageName == null
                        || packageName.equals(normalized(ownPackageName))
                        || isObviousInfrastructurePackage(packageName)) {
                    continue;
                }
                String displayName = normalized(descriptor.getDisplayName());
                MutableCandidate candidate = byPackage.get(packageName);
                if (candidate == null) {
                    candidate = new MutableCandidate(packageName,
                            displayName == null ? packageName : displayName);
                    byPackage.put(packageName, candidate);
                } else if (candidate.displayName.equals(candidate.packageName)
                        && displayName != null) {
                    candidate.displayName = displayName;
                }
                candidate.sources.add(descriptor.getSource());
                candidate.systemApplication |= descriptor.isSystemApplication();
            }
        }
        List<Candidate> result = new ArrayList<>();
        for (MutableCandidate candidate : byPackage.values()) {
            result.add(new Candidate(candidate.packageName, candidate.displayName,
                    candidate.sources, candidate.systemApplication));
        }
        result.sort(Comparator
                .comparing((Candidate value) -> value.getDisplayName().toLowerCase(Locale.US))
                .thenComparing(Candidate::getPackageName));
        return Collections.unmodifiableList(result);
    }

    /** System/preinstalled status is deliberately not part of the rejection rule. */
    public static boolean isObviousInfrastructurePackage(String packageName) {
        String value = normalized(packageName);
        return value != null
                && (value.equals("com.android.systemui")
                || value.equals("com.android.settings")
                || value.startsWith("com.android.launcher")
                || value.startsWith("com.sonymobile.launcher")
                || value.contains("inputmethod"));
    }

    private static String normalized(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static final class MutableCandidate {
        private final String packageName;
        private String displayName;
        private final EnumSet<Source> sources = EnumSet.noneOf(Source.class);
        private boolean systemApplication;

        private MutableCandidate(String packageName, String displayName) {
            this.packageName = packageName;
            this.displayName = displayName;
        }
    }
}
