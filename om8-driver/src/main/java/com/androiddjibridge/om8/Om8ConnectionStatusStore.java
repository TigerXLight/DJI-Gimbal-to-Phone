package com.androiddjibridge.om8;

import android.content.Context;
import android.content.SharedPreferences;

/** Small process-independent status snapshot used by the consumer UI/notification. */
public final class Om8ConnectionStatusStore {
    public static final String ACTION_STATUS_CHANGED =
            "com.androiddjibridge.OM8_CONNECTION_STATUS_CHANGED";
    private static final String PREFERENCES = "om8_connection_status";
    private static final String STATE = "state";
    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_ADDRESS = "device_address";

    private Om8ConnectionStatusStore() {
    }

    public static void publish(Context context, String state, String name, String address) {
        if (context == null) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(STATE, state == null ? "DISCONNECTED" : state)
                .putString(DEVICE_NAME, name == null ? "" : name)
                .putString(DEVICE_ADDRESS, address == null ? "" : address)
                .apply();
        // The service remains the source of truth. This explicit in-app signal lets
        // a visible Activity refresh without creating another GATT owner or polling BLE.
        applicationContext.sendBroadcast(new android.content.Intent(ACTION_STATUS_CHANGED)
                .setPackage(applicationContext.getPackageName()));
    }

    public static Snapshot read(Context context) {
        if (context == null) {
            return new Snapshot("DISCONNECTED", "", "");
        }
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return new Snapshot(
                preferences.getString(STATE, "DISCONNECTED"),
                preferences.getString(DEVICE_NAME, ""),
                preferences.getString(DEVICE_ADDRESS, ""));
    }

    public static final class Snapshot {
        private final String state;
        private final String deviceName;
        private final String deviceAddress;

        private Snapshot(String state, String deviceName, String deviceAddress) {
            this.state = state;
            this.deviceName = deviceName;
            this.deviceAddress = deviceAddress;
        }

        public String getState() { return state; }
        public String getDeviceName() { return deviceName; }
        public String getDeviceAddress() { return deviceAddress; }

        public boolean isConnected() { return "READY".equals(state); }
    }
}
