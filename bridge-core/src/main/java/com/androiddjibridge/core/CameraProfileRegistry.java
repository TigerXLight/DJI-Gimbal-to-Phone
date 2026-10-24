package com.androiddjibridge.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable registry of explicitly supplied built-in or imported profiles. */
public final class CameraProfileRegistry {
    private final List<CameraProfile> profiles;
    private final Map<String, CameraProfile> byId;
    private final Map<String, CameraProfile> byPackage;
    private final String defaultProfileId;

    public CameraProfileRegistry(Collection<CameraProfile> profiles, String defaultProfileId) {
        if (profiles == null) {
            throw new IllegalArgumentException("profiles are required");
        }
        List<CameraProfile> sorted = new ArrayList<>(profiles);
        sorted.sort(Comparator.comparing(CameraProfile::getStableId));
        Map<String, CameraProfile> ids = new LinkedHashMap<>();
        Map<String, CameraProfile> packages = new LinkedHashMap<>();
        for (CameraProfile profile : sorted) {
            if (profile == null) {
                throw new IllegalArgumentException("profile cannot be null");
            }
            if (ids.put(profile.getStableId(), profile) != null) {
                throw new IllegalArgumentException("duplicate profile ID "
                        + profile.getStableId());
            }
            if (packages.put(profile.getPackageName(), profile) != null) {
                throw new IllegalArgumentException("duplicate package "
                        + profile.getPackageName());
            }
        }
        if (defaultProfileId != null && !ids.containsKey(defaultProfileId)) {
            throw new IllegalArgumentException("default profile is not registered");
        }
        this.profiles = Collections.unmodifiableList(sorted);
        this.byId = Collections.unmodifiableMap(ids);
        this.byPackage = Collections.unmodifiableMap(packages);
        this.defaultProfileId = defaultProfileId;
    }

    public static CameraProfileRegistry empty() {
        return new CameraProfileRegistry(Collections.emptyList(), null);
    }

    public List<CameraProfile> getProfiles() {
        return profiles;
    }

    public Optional<CameraProfile> findByStableId(String stableId) {
        return Optional.ofNullable(byId.get(stableId));
    }

    public Optional<CameraProfile> findByPackageName(String packageName) {
        return Optional.ofNullable(byPackage.get(packageName));
    }

    public Optional<CameraProfile> getDefaultProfile() {
        return Optional.ofNullable(byId.get(defaultProfileId));
    }

    public boolean contains(CameraProfile profile) {
        return profile != null && byId.get(profile.getStableId()) == profile;
    }
}
