package com.androiddjibridge.core;

/**
 * App-level record of an Android companion association.
 *
 * The association is the identity authority. The optional address is only a
 * last-observed value needed by legacy Android presence APIs and is never used
 * as a permanent OM8 identity or as a replacement for post-connect GATT
 * validation.
 */
public final class AssociatedDevice {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final long NO_ASSOCIATION_ID = -1L;

    private final int schemaVersion;
    private final String displayName;
    private final String lastKnownAddress;
    private final long associationId;

    public AssociatedDevice(
            int schemaVersion,
            String displayName,
            String lastKnownAddress,
            long associationId) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported association schema " + schemaVersion);
        }
        if (displayName == null || !displayName.trim().toUpperCase().startsWith("OM8-")) {
            throw new IllegalArgumentException("associated device must have an OM8 name");
        }
        if (associationId < NO_ASSOCIATION_ID) {
            throw new IllegalArgumentException("association id is invalid");
        }
        this.schemaVersion = schemaVersion;
        this.displayName = displayName.trim();
        this.lastKnownAddress = lastKnownAddress == null ? "" : lastKnownAddress.trim();
        this.associationId = associationId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLastKnownAddress() {
        return lastKnownAddress;
    }

    public long getAssociationId() {
        return associationId;
    }

    public AssociatedDevice withObservation(String address, long id) {
        long replacementId = id <= NO_ASSOCIATION_ID ? associationId : id;
        String replacementAddress = address == null || address.trim().isEmpty()
                ? lastKnownAddress : address;
        return new AssociatedDevice(
                schemaVersion,
                displayName,
                replacementAddress,
                replacementId);
    }
}
