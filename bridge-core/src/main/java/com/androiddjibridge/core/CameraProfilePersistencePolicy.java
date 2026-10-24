package com.androiddjibridge.core;

import java.util.Set;

/** Rules preventing learned data from replacing built-in profiles. */
public final class CameraProfilePersistencePolicy {
    private CameraProfilePersistencePolicy() {
    }

    public static boolean canPersistLearned(
            CameraProfile profile,
            Set<String> builtInProfileIds,
            Set<String> builtInPackageNames) {
        return profile != null
                && profile.getOrigin() == CameraProfileOrigin.LEARNED
                && builtInProfileIds != null
                && builtInPackageNames != null
                && !builtInProfileIds.contains(profile.getStableId())
                && !builtInPackageNames.contains(profile.getPackageName());
    }
}
