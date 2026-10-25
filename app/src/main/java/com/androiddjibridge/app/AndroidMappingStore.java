package com.androiddjibridge.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.androiddjibridge.core.MappingJsonCodec;
import com.androiddjibridge.core.MappingStore;

/** App-private persistence boundary for the pure mapping store. */
public final class AndroidMappingStore {
    private static final String PREFERENCES = "controller_mapping";
    private static final String JSON_KEY = "mapping_json";
    private static final String PROFILE_KEY_PREFIX = "mapping_json.";

    private final SharedPreferences preferences;

    public AndroidMappingStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
    }

    public MappingStore load() {
        return loadForKey(JSON_KEY);
    }

    /** Loads the mapping for one selected camera profile without sharing profiles' settings. */
    public MappingStore load(String profileId) {
        if (profileId == null || profileId.trim().isEmpty()) {
            return load();
        }
        String key = PROFILE_KEY_PREFIX + safeProfileKey(profileId);
        if (!preferences.contains(key) && preferences.contains(JSON_KEY)) {
            // Migrate the pre-profile setting once. Future loads are then isolated
            // to this camera application's stable profile identity.
            MappingStore legacy = loadForKey(JSON_KEY);
            saveForKey(key, legacy);
            return legacy;
        }
        return loadForKey(key);
    }

    private MappingStore loadForKey(String key) {
        String json = preferences.getString(key, null);
        if (json == null) {
            return MappingStore.defaults();
        }
        try {
            return MappingJsonCodec.fromJson(json);
        } catch (IllegalArgumentException error) {
            // A malformed or future configuration fails closed to safe defaults.
            return MappingStore.defaults();
        }
    }

    public boolean save(MappingStore store) {
        return saveForKey(JSON_KEY, store);
    }

    /** Saves only the selected profile's mapping in app-private preferences. */
    public boolean save(String profileId, MappingStore store) {
        if (profileId == null || profileId.trim().isEmpty()) {
            return save(store);
        }
        return saveForKey(PROFILE_KEY_PREFIX + safeProfileKey(profileId), store);
    }

    private boolean saveForKey(String key, MappingStore store) {
        if (store == null) {
            throw new IllegalArgumentException("mapping store is required");
        }
        return preferences.edit()
                .putString(key, MappingJsonCodec.toJson(store))
                .commit();
    }

    public boolean reset() {
        return save(MappingStore.defaults());
    }

    public boolean reset(String profileId) {
        return save(profileId, MappingStore.defaults());
    }

    private String safeProfileKey(String profileId) {
        String value = profileId.trim();
        if (!value.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("invalid profile ID");
        }
        return value;
    }
}
