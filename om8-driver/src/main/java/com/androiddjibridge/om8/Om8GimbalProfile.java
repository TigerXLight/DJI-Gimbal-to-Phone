package com.androiddjibridge.om8;

import com.androiddjibridge.core.GimbalProfile;

/** The hardware-proven OM8 transport profile; it does not add any GATT behavior. */
public final class Om8GimbalProfile {
    public static final String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static final String NOTIFICATION_UUID = "0000fff4-0000-1000-8000-00805f9b34fb";
    public static final String WRITE_UUID = "0000fff5-0000-1000-8000-00805f9b34fb";

    private Om8GimbalProfile() {
    }

    public static GimbalProfile proven() {
        return new GimbalProfile("dji.om8", "DJI Osmo Mobile 8", SERVICE_UUID,
                NOTIFICATION_UUID, WRITE_UUID);
    }
}
