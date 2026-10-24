package com.androiddjibridge.core;

import java.util.Arrays;
import java.util.List;

/** Pure discovery merge/filter regression for Learn Camera selection. */
public final class CameraApplicationDiscoverySelfTest {
    public static void main(String[] args) {
        List<CameraApplicationDiscovery.Descriptor> descriptors = Arrays.asList(
                new CameraApplicationDiscovery.Descriptor(
                        "com.sonymobile.photopro", "Photography Pro",
                        CameraApplicationDiscovery.Source.VIDEO_CAPTURE, true),
                new CameraApplicationDiscovery.Descriptor(
                        "com.sonymobile.photopro", "Photography Pro",
                        CameraApplicationDiscovery.Source.IMAGE_CAPTURE, true),
                new CameraApplicationDiscovery.Descriptor(
                        "com.sonymobile.photopro", "Photography Pro",
                        CameraApplicationDiscovery.Source.LAUNCHABLE, true),
                new CameraApplicationDiscovery.Descriptor(
                        "jp.co.sony.mc.videopro", "Video Pro",
                        CameraApplicationDiscovery.Source.BUILTIN_PROFILE, true),
                new CameraApplicationDiscovery.Descriptor(
                        "com.example.camera", "Example Camera",
                        CameraApplicationDiscovery.Source.VIDEO_CAPTURE, false),
                new CameraApplicationDiscovery.Descriptor(
                        "com.android.systemui", "System UI",
                        CameraApplicationDiscovery.Source.LAUNCHABLE, true),
                new CameraApplicationDiscovery.Descriptor(
                        "com.example.bridge", "Bridge",
                        CameraApplicationDiscovery.Source.LAUNCHABLE, false));

        List<CameraApplicationDiscovery.Candidate> candidates =
                CameraApplicationDiscovery.merge(descriptors, "com.example.bridge");
        require(candidates.size() == 3, "deduplicate and filter candidates");

        CameraApplicationDiscovery.Candidate photography = find(
                candidates, "com.sonymobile.photopro");
        require(photography != null, "system/preinstalled camera is eligible");
        require(photography.isSystemApplication(), "system status is retained as metadata");
        require(photography.hasSource(CameraApplicationDiscovery.Source.VIDEO_CAPTURE),
                "video capture source retained");
        require(photography.hasSource(CameraApplicationDiscovery.Source.IMAGE_CAPTURE),
                "image capture source retained");
        require(photography.getSources().size() == 3,
                "duplicate package has one merged candidate");

        CameraApplicationDiscovery.Candidate videoPro = find(
                candidates, "jp.co.sony.mc.videopro");
        require(videoPro != null && videoPro.hasSource(
                        CameraApplicationDiscovery.Source.BUILTIN_PROFILE),
                "built-in Sony Video Pro remains discoverable");
        require(find(candidates, "com.android.systemui") == null,
                "obvious infrastructure is excluded");
        System.out.println("CameraApplicationDiscoverySelfTest: PASS");
    }

    private static CameraApplicationDiscovery.Candidate find(
            List<CameraApplicationDiscovery.Candidate> candidates,
            String packageName) {
        for (CameraApplicationDiscovery.Candidate candidate : candidates) {
            if (packageName.equals(candidate.getPackageName())) {
                return candidate;
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
