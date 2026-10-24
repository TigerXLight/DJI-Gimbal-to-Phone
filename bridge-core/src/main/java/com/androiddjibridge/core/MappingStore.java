package com.androiddjibridge.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable, data-only mapping configuration.
 *
 * The structure follows the useful OpenGimbal mapping boundary from
 * ItzDFPlayer/OpenGimbal, commit 6b7f403265d4ef96c9238ecaeed12bf5ce9f711c,
 * MIT License, while intentionally excluding its HJ360 protocol and UI
 * fallback behavior. Local changes add the normalized OM8 trigger vocabulary,
 * schema versioning, and fail-closed NONE defaults.
 */
public final class MappingStore {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final int schemaVersion;
    private final EnumMap<ControllerTrigger, CameraAction> mappings;

    private MappingStore(int schemaVersion, Map<ControllerTrigger, CameraAction> source) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported mapping schema " + schemaVersion);
        }
        if (source == null) {
            throw new IllegalArgumentException("mappings are required");
        }
        mappings = new EnumMap<>(ControllerTrigger.class);
        for (ControllerTrigger trigger : ControllerTrigger.values()) {
            mappings.put(trigger, CameraAction.NONE);
        }
        for (Map.Entry<ControllerTrigger, CameraAction> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("mapping keys and values are required");
            }
            mappings.put(entry.getKey(), entry.getValue());
        }
        this.schemaVersion = schemaVersion;
    }

    public static MappingStore defaults() {
        EnumMap<ControllerTrigger, CameraAction> defaults =
                new EnumMap<>(ControllerTrigger.class);
        defaults.put(ControllerTrigger.RECORD_PRESS, CameraAction.RECORD_TOGGLE);
        defaults.put(ControllerTrigger.RECORD_RELEASE, CameraAction.NONE);
        return new MappingStore(CURRENT_SCHEMA_VERSION, defaults);
    }

    public static MappingStore fromMap(
            int schemaVersion, Map<ControllerTrigger, CameraAction> mappings) {
        return new MappingStore(schemaVersion, mappings);
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public CameraAction actionFor(ControllerTrigger trigger) {
        if (trigger == null) {
            return CameraAction.NONE;
        }
        CameraAction action = mappings.get(trigger);
        return action == null ? CameraAction.NONE : action;
    }

    public Map<ControllerTrigger, CameraAction> asMap() {
        return Collections.unmodifiableMap(new EnumMap<>(mappings));
    }

    public MappingStore withMapping(ControllerTrigger trigger, CameraAction action) {
        if (trigger == null || action == null) {
            throw new IllegalArgumentException("trigger and action are required");
        }
        EnumMap<ControllerTrigger, CameraAction> replacement = new EnumMap<>(mappings);
        replacement.put(trigger, action);
        return new MappingStore(schemaVersion, replacement);
    }

    public MappingStore reset() {
        return defaults();
    }
}
