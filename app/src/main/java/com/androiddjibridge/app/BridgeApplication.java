package com.androiddjibridge.app;

import android.app.Application;
import android.content.SharedPreferences;

import com.androiddjibridge.core.BridgeController;
import com.androiddjibridge.core.BridgeRuntime;
import com.androiddjibridge.core.CameraProfile;
import com.androiddjibridge.core.CameraProfileRegistry;
import com.androiddjibridge.core.CameraProfileOrigin;
import com.androiddjibridge.core.CameraAction;
import com.androiddjibridge.core.ControllerTrigger;
import com.androiddjibridge.core.MappingEngine;
import com.androiddjibridge.core.MappingStore;
import com.androiddjibridge.sony.GenericAccessibilityCameraAdapter;
import com.androiddjibridge.sony.SonyVideoProCameraAdapter;
import com.androiddjibridge.sony.SonyVideoProProfile;
import com.androiddjibridge.om8.Om8ConnectionPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Installs the selected adapter without coupling the OM8 driver to Sony. */
public final class BridgeApplication extends Application {
    private static final String PREFERENCES = "bridge_consumer_settings";
    private static final String SELECTED_PROFILE = "selected_profile";
    private BridgeController controller;
    private LearnedProfileStore learnedProfileStore;
    private AndroidMappingStore mappingStore;
    private MappingEngine mappingEngine;
    private CameraProfile sonyProfile;

    @Override
    public void onCreate() {
        super.onCreate();
        sonyProfile = SonyVideoProProfile.builtIn();
        Set<String> builtInIds = Collections.singleton(sonyProfile.getStableId());
        Set<String> builtInPackages = Collections.singleton(sonyProfile.getPackageName());
        learnedProfileStore = new LearnedProfileStore(this, builtInIds, builtInPackages);
        mappingStore = new AndroidMappingStore(this);
        CameraProfileRegistry registry = buildRegistry();
        CameraProfile initialProfile = registry.findByStableId(
                        selectedProfileIdFromPreferences())
                .orElse(registry.getDefaultProfile().orElse(sonyProfile));
        mappingEngine = new MappingEngine(mappingStore.load(initialProfile.getStableId()));
        controller = new BridgeController(
                registry, initialProfile, mappingEngine, adapterFor(initialProfile));
        BridgeRuntime.install(controller);
        BridgeRuntime.installForegroundPackageListener(this::selectProfileForForegroundPackage);
        // Presence observation is a system-managed wake-up path. It does not
        // itself own GATT or imply that the camera adapter is ready.
        CompanionAssociationCoordinator.observePresence(this);
        if (new CompanionAssociationStore(this).load() != null
                && !Om8ConnectionPreferences.isManualDisconnect(this)) {
            AutoConnectServiceStarter.start(this, "application-created");
        }
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
        return profile != null && selectProfileInternal(profile, true);
    }

    /** Selects the configured preset for the foreground camera, or fails closed. */
    public void selectProfileForForegroundPackage(String packageName) {
        CameraProfile profile = controller.getProfileRegistry()
                .findByPackageName(packageName).orElse(null);
        if (profile == null) {
            controller.clearSelectedProfile();
            controller.setSelectedAdapter(null);
            return;
        }
        selectProfileInternal(profile, false);
    }

    public boolean saveLearnedProfile(CameraProfile profile) {
        if (!learnedProfileStore.save(profile)) {
            return false;
        }
        controller.replaceProfileRegistry(buildRegistry());
        return true;
    }

    public MappingStore getMappingStore() {
        return mappingEngine.getStore();
    }

    /** Loads a mapping without depending on whichever profile is foreground right now. */
    public MappingStore getMappingStore(String profileId) {
        return mappingStore.load(profileId);
    }

    public boolean setMapping(ControllerTrigger trigger, CameraAction action) {
        return setMapping(selectedProfileId(), trigger, action);
    }

    /** Saves a mapping against the camera profile being edited, not transient foreground state. */
    public boolean setMapping(String profileId, ControllerTrigger trigger, CameraAction action) {
        MappingStore current = mappingStore.load(profileId);
        MappingStore replacement = current.withMapping(trigger, action);
        if (!mappingStore.save(profileId, replacement)) {
            return false;
        }
        if (isSelectedProfile(profileId)) {
            mappingEngine.setStore(replacement);
        }
        return true;
    }

    public boolean resetMappings() {
        return resetMappings(selectedProfileId());
    }

    /** Resets only the camera profile whose mapping page is being edited. */
    public boolean resetMappings(String profileId) {
        MappingStore replacement = mappingStore.load(profileId).reset();
        if (!mappingStore.save(profileId, replacement)) {
            return false;
        }
        if (isSelectedProfile(profileId)) {
            mappingEngine.setStore(replacement);
        }
        return true;
    }

    private boolean isSelectedProfile(String profileId) {
        CameraProfile selected = controller.getSelectedProfile();
        return selected != null && selected.getStableId().equals(profileId);
    }

    private String selectedProfileId() {
        CameraProfile profile = controller.getSelectedProfile();
        return profile == null ? "default" : profile.getStableId();
    }

    private boolean selectProfileInternal(CameraProfile profile, boolean persist) {
        if (profile == null || !controller.selectProfile(profile.getStableId())) {
            return false;
        }
        mappingEngine.setStore(mappingStore.load(profile.getStableId()));
        controller.setSelectedAdapter(adapterFor(profile));
        if (persist) {
            getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                    .putString(SELECTED_PROFILE, profile.getStableId())
                    .apply();
        }
        return true;
    }

    private com.androiddjibridge.core.CameraAdapter adapterFor(CameraProfile profile) {
        if (profile != null
                && sonyProfile.getStableId().equals(profile.getStableId())
                && profile.getOrigin() == CameraProfileOrigin.BUILTIN) {
            return new SonyVideoProCameraAdapter();
        }
        return profile == null ? null : new GenericAccessibilityCameraAdapter(profile);
    }

    private String selectedProfileIdFromPreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        return preferences.getString(SELECTED_PROFILE, sonyProfile.getStableId());
    }

    private CameraProfileRegistry buildRegistry() {
        List<CameraProfile> profiles = new ArrayList<>();
        profiles.add(sonyProfile);
        profiles.addAll(learnedProfileStore == null
                ? Collections.emptyList() : learnedProfileStore.loadProfiles());
        return new CameraProfileRegistry(profiles, sonyProfile.getStableId());
    }
}
