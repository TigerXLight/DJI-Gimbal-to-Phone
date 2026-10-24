package com.androiddjibridge.app;

import android.app.Application;

import com.androiddjibridge.core.BridgeController;
import com.androiddjibridge.core.BridgeRuntime;
import com.androiddjibridge.core.CameraProfile;
import com.androiddjibridge.core.CameraProfileRegistry;
import com.androiddjibridge.sony.SonyVideoProCameraAdapter;
import com.androiddjibridge.sony.SonyVideoProProfile;

import java.util.Collections;

/** Installs the selected adapter without coupling the OM8 driver to Sony. */
public final class BridgeApplication extends Application {
    private BridgeController controller;

    @Override
    public void onCreate() {
        super.onCreate();
        CameraProfile sonyProfile = SonyVideoProProfile.builtIn();
        CameraProfileRegistry registry = new CameraProfileRegistry(
                Collections.singletonList(sonyProfile), sonyProfile.getStableId());
        controller = new BridgeController(
                registry, sonyProfile, new SonyVideoProCameraAdapter());
        BridgeRuntime.install(controller);
    }

    public BridgeController getController() {
        return controller;
    }
}
