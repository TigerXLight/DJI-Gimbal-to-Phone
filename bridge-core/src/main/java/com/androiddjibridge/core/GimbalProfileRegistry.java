package com.androiddjibridge.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Deterministic registry for validated gimbal transport profiles. */
public final class GimbalProfileRegistry {
    private final List<GimbalProfile> profiles;
    private final Map<String, GimbalProfile> byId;

    public GimbalProfileRegistry(Collection<GimbalProfile> profiles) {
        if (profiles == null) {
            throw new IllegalArgumentException("profiles are required");
        }
        List<GimbalProfile> sorted = new ArrayList<>(profiles);
        sorted.sort((left, right) -> left.getStableId().compareTo(right.getStableId()));
        Map<String, GimbalProfile> values = new LinkedHashMap<>();
        for (GimbalProfile profile : sorted) {
            if (profile == null || values.put(profile.getStableId(), profile) != null) {
                throw new IllegalArgumentException("null or duplicate gimbal profile");
            }
        }
        this.profiles = Collections.unmodifiableList(sorted);
        this.byId = Collections.unmodifiableMap(values);
    }

    public Optional<GimbalProfile> findByStableId(String stableId) {
        return Optional.ofNullable(byId.get(stableId));
    }

    public List<GimbalProfile> getProfiles() { return profiles; }
}
