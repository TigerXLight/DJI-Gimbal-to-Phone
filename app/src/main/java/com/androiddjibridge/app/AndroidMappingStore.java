package com.androiddjibridge.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.androiddjibridge.core.MappingJsonCodec;
import com.androiddjibridge.core.MappingStore;

/** App-private persistence boundary for the pure mapping store. */
public final class AndroidMappingStore {
    private static final String PREFERENCES = "controller_mapping";
    private static final String JSON_KEY = "mapping_json";

    private final SharedPreferences preferences;

    public AndroidMappingStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
    }

    public MappingStore load() {
        String json = preferences.getString(JSON_KEY, null);
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
        if (store == null) {
            throw new IllegalArgumentException("mapping store is required");
        }
        return preferences.edit()
                .putString(JSON_KEY, MappingJsonCodec.toJson(store))
                .commit();
    }

    public boolean reset() {
        return save(MappingStore.defaults());
    }
}
