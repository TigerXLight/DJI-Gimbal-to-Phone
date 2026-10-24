package com.androiddjibridge.core;

/** Small JVM regression for the transport-profile registry boundary. */
public final class GimbalProfileRegistrySelfTest {
    public static void main(String[] args) {
        GimbalProfile profile = new GimbalProfile(
                "dji.om8", "DJI Osmo Mobile 8", "fff0", "fff4", "fff5");
        GimbalProfileRegistry registry = new GimbalProfileRegistry(
                java.util.Collections.singletonList(profile));
        if (!registry.findByStableId("dji.om8").isPresent()) {
            throw new AssertionError("profile lookup failed");
        }
        if (registry.getProfiles().size() != 1) {
            throw new AssertionError("profile enumeration failed");
        }
    }
}
