package com.androiddjibridge.app;

import android.app.Application;

import com.androiddjibridge.core.BridgeController;
import com.androiddjibridge.core.BridgeRuntime;
import com.androiddjibridge.sony.SonyVideoProCameraAdapter;

/** Installs the selected adapter without coupling the OM8 driver to Sony. */
public final class BridgeApplication extends Application {
    private BridgeController controller;

    @Override
    public void onCreate() {
        super.onCreate();
        controller = new BridgeController(new SonyVideoProCameraAdapter());
        BridgeRuntime.install(controller);
    }

    public BridgeController getController() {
        return controller;
    }
}
