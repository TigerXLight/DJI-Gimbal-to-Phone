package com.androiddjibridge.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.androiddjibridge.core.AssociatedDevice;

/** App-private persistence for the user-approved Android companion association. */
public final class CompanionAssociationStore {
    private static final String PREFERENCES = "om8_companion_association";
    private static final String NAME = "display_name";
    private static final String ADDRESS = "last_known_address";
    private static final String ASSOCIATION_ID = "association_id";

    private final SharedPreferences preferences;

    public CompanionAssociationStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
    }

    public AssociatedDevice load() {
        String name = preferences.getString(NAME, null);
        if (name == null) {
            return null;
        }
        try {
            return new AssociatedDevice(
                    AssociatedDevice.CURRENT_SCHEMA_VERSION,
                    name,
                    preferences.getString(ADDRESS, ""),
                    preferences.getLong(
                            ASSOCIATION_ID, AssociatedDevice.NO_ASSOCIATION_ID));
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    public boolean save(AssociatedDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("device is required");
        }
        return preferences.edit()
                .putString(NAME, device.getDisplayName())
                .putString(ADDRESS, device.getLastKnownAddress())
                .putLong(ASSOCIATION_ID, device.getAssociationId())
                .commit();
    }

    public boolean clear() {
        return preferences.edit().clear().commit();
    }
}
