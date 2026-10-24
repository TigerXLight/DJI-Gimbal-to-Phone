package com.androiddjibridge.core;

/** Immutable description of a gimbal transport family. It contains no driver code. */
public final class GimbalProfile {
    private final String stableId;
    private final String displayName;
    private final String serviceUuid;
    private final String notificationCharacteristicUuid;
    private final String writeCharacteristicUuid;

    public GimbalProfile(String stableId, String displayName, String serviceUuid,
                         String notificationCharacteristicUuid,
                         String writeCharacteristicUuid) {
        this.stableId = require(stableId, "stableId");
        this.displayName = require(displayName, "displayName");
        this.serviceUuid = require(serviceUuid, "serviceUuid");
        this.notificationCharacteristicUuid = require(
                notificationCharacteristicUuid, "notificationCharacteristicUuid");
        this.writeCharacteristicUuid = writeCharacteristicUuid == null
                ? "" : writeCharacteristicUuid.trim();
    }

    public String getStableId() { return stableId; }
    public String getDisplayName() { return displayName; }
    public String getServiceUuid() { return serviceUuid; }
    public String getNotificationCharacteristicUuid() {
        return notificationCharacteristicUuid;
    }
    public String getWriteCharacteristicUuid() { return writeCharacteristicUuid; }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
