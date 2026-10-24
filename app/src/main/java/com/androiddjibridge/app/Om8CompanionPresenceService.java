package com.androiddjibridge.app;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceService;
import android.util.Log;

/** System-bound presence callback; it does not own or replace the OM8 GATT session. */
public final class Om8CompanionPresenceService extends CompanionDeviceService {
    private static final String TAG = "Om8Companion";

    @Override
    public void onDeviceAppeared(String address) {
        Log.i(TAG, "associated OM8 appeared address=" + address);
        CompanionAssociationCoordinator.onPresence(this, address, -1L);
    }

    @Override
    public void onDeviceDisappeared(String address) {
        CompanionAssociationCoordinator.onDisappearance(this, address);
    }

    @Override
    public void onDeviceAppeared(AssociationInfo associationInfo) {
        if (associationInfo == null) {
            return;
        }
        Log.i(TAG, "associated OM8 appeared associationId=" + associationInfo.getId());
        CompanionAssociationCoordinator.onPresence(
                this,
                associationInfo.getDeviceMacAddress() == null
                        ? null : associationInfo.getDeviceMacAddress().toString(),
                associationInfo.getId());
    }

    @Override
    public void onDeviceDisappeared(AssociationInfo associationInfo) {
        Log.i(TAG, "associated OM8 disappeared associationId="
                + (associationInfo == null ? -1 : associationInfo.getId()));
    }
}
