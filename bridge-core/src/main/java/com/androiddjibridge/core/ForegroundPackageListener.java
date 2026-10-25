package com.androiddjibridge.core;

/** Receives foreground application changes outside the controller hot path. */
public interface ForegroundPackageListener {
    void onForegroundPackageChanged(String packageName);
}
