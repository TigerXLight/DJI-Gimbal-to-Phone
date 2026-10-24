package com.androiddjibridge.app;

import android.app.Application;

import com.androiddjibridge.core.BridgeController;
import com.androiddjibridge.core.BridgeRuntime;
import com.androiddjibridge.core.CameraProfile;
import com.androiddjibridge.core.CameraProfileRegistry;
import com.androiddjibridge.core.CameraProfileOrigin;
import com.androiddjibridge.sony.GenericAccessibilityCameraAdapter;
import com.androiddjibridge.sony.SonyVideoProCameraAdapter;
import com.androiddjibridge.sony.SonyVideoProProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Installs the selected adapter without coupling the OM8 driver to Sony. */
public final class BridgeApplication extends Application {
    private BridgeController controller;
    private LearnedProfileStore learnedProfileStore;
    private CameraProfile sonyProfile;

    @Override
    public void onCreate() {
        super.onCreate();
        sonyProfile = SonyVideoProProfile.builtIn();
        Set<String> builtInIds = Collections.singleton(sonyProfile.getStableId());
        Set<String> builtInPackages = Collections.singleton(sonyProfile.getPackageName());
        learnedProfileStore = new LearnedProfileStore(this, builtInIds, builtInPackages);
        CameraProfileRegistry registry = buildRegistry();
        controller = new BridgeController(
                registry, sonyProfile, new SonyVideoProCameraAdapter());
        BridgeRuntime.install(controller);
    }

    public BridgeController getController() {
        return controller;
    }

    public List<CameraProfile> getProfiles() {
        return controller.getProfileRegistry().getProfiles();
    }

    public boolean selectProfile(String stableId) {
        CameraProfile profile = controller.getProfileRegistry()
                .findByStableId(stableId).orElse(null);
        if (profile == null || !controller.selectProfile(stableId)) {
            return false;
        }
        if (sonyProfile.getStableId().equals(profile.getStableId())
                && profile.getOrigin() == CameraProfileOrigin.BUILTIN) {
            controller.setSelectedAdapter(new SonyVideoProCameraAdapter());
        } else {
            controller.setSelectedAdapter(new GenericAccessibilityCameraAdapter(profile));
        }
        return true;
    }

    public boolean saveLearnedProfile(CameraProfile profile) {
        if (!learnedProfileStore.save(profile)) {
            return false;
        }
        controller.replaceProfileRegistry(buildRegistry());
        return true;
    }

    private CameraProfileRegistry buildRegistry() {
        List<CameraProfile> profiles = new ArrayList<>();
        profiles.add(sonyProfile);
        profiles.addAll(learnedProfileStore == null
                ? Collections.emptyList() : learnedProfileStore.loadProfiles());
        return new CameraProfileRegistry(profiles, sonyProfile.getStableId());
    }
}
