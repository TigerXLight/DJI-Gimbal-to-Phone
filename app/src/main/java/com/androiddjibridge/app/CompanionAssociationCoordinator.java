package com.androiddjibridge.app;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.androiddjibridge.core.AssociatedDevice;

import java.util.List;
import java.util.regex.Pattern;

/** Coordinates user-approved companion association and presence observation. */
public final class CompanionAssociationCoordinator {
    private static final String TAG = "Om8Companion";
    private static final Pattern OM8_NAME = Pattern.compile("OM8-.*");

    private CompanionAssociationCoordinator() {
    }

    public static boolean beginAssociation(Activity activity, int requestCode) {
        if (activity == null || Build.VERSION.SDK_INT < 26) {
            return false;
        }
        if (!hasCompanionFeature(activity)) {
            Log.i(TAG, "companion association unavailable: feature missing");
            return false;
        }
        CompanionDeviceManager manager = activity.getSystemService(
                CompanionDeviceManager.class);
        if (manager == null) {
            return false;
        }
        BluetoothLeDeviceFilter filter = new BluetoothLeDeviceFilter.Builder()
                .setNamePattern(OM8_NAME)
                .build();
        AssociationRequest request = new AssociationRequest.Builder()
                .addDeviceFilter(filter)
                .setSingleDevice(true)
                .setDisplayName("DJI OM8 gimbal")
                .build();
        try {
            manager.associate(request, new CompanionDeviceManager.Callback() {
                private boolean launched;

                @Override
                public void onDeviceFound(IntentSender intentSender) {
                    launchSelection(activity, requestCode, intentSender);
                }

                @Override
                public void onAssociationPending(IntentSender intentSender) {
                    launchSelection(activity, requestCode, intentSender);
                }

                private void launchSelection(
                        Activity owner, int code, IntentSender intentSender) {
                    if (launched || intentSender == null) {
                        return;
                    }
                    launched = true;
                    try {
                        owner.startIntentSenderForResult(
                                intentSender, code, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException error) {
                        Log.w(TAG, "association selection could not open", error);
                    }
                }

                @Override
                public void onAssociationCreated(AssociationInfo associationInfo) {
                    if (associationInfo == null) {
                        return;
                    }
                    String name = associationInfo.getDisplayName() == null
                            ? "OM8-associated"
                            : associationInfo.getDisplayName().toString();
                    saveObservation(activity, name, addressOf(associationInfo),
                            associationInfo.getId());
                    observePresence(activity);
                    AutoConnectServiceStarter.start(activity, "association-created-callback");
                }

                @Override
                public void onFailure(CharSequence error) {
                    Log.w(TAG, "companion association failed=" + error);
                }
            }, new Handler(Looper.getMainLooper()));
            return true;
        } catch (RuntimeException error) {
            Log.w(TAG, "companion association rejected", error);
            return false;
        }
    }

    public static boolean handleActivityResult(Context context, BluetoothDevice device) {
        if (context == null || device == null) {
            return false;
        }
        String name;
        String address;
        try {
            name = device.getName();
            address = device.getAddress();
        } catch (SecurityException error) {
            Log.w(TAG, "associated device identity unavailable", error);
            return false;
        }
        if (name == null || !OM8_NAME.matcher(name.trim()).matches()) {
            Log.w(TAG, "association result rejected: name is not OM8");
            return false;
        }
        saveObservation(context, name, address, AssociatedDevice.NO_ASSOCIATION_ID);
        observePresence(context);
        AutoConnectServiceStarter.start(context, "association-created");
        return true;
    }

    public static void observePresence(Context context) {
        if (context == null || Build.VERSION.SDK_INT < 31
                || !hasCompanionFeature(context)) {
            return;
        }
        CompanionAssociationStore store = new CompanionAssociationStore(context);
        AssociatedDevice device = store.load();
        if (device == null || device.getLastKnownAddress().isEmpty()) {
            return;
        }
        CompanionDeviceManager manager = context.getSystemService(
                CompanionDeviceManager.class);
        if (manager == null) {
            return;
        }
        try {
            manager.startObservingDevicePresence(device.getLastKnownAddress());
            Log.i(TAG, "presence observation started name=" + device.getDisplayName());
        } catch (RuntimeException error) {
            Log.w(TAG, "presence observation unavailable; manual Start Bridge remains valid",
                    error);
        }
    }

    public static void onPresence(Context context, String address, long associationId) {
        if (context == null) {
            return;
        }
        CompanionAssociationStore store = new CompanionAssociationStore(context);
        AssociatedDevice current = store.load();
        if (current != null) {
            store.save(current.withObservation(address, associationId));
        }
        AutoConnectServiceStarter.start(context, "companion-device-appeared");
    }

    public static void onDisappearance(Context context, String address) {
        Log.i(TAG, "associated OM8 presence disappeared address=" + address
                + "; existing GATT recovery remains responsible for cleanup/retry");
    }

    public static String status(Context context) {
        AssociatedDevice device = context == null
                ? null : new CompanionAssociationStore(context).load();
        return device == null
                ? "not associated"
                : "associated " + device.getDisplayName();
    }

    private static void saveObservation(
            Context context, String name, String address, long associationId) {
        if (name == null || !OM8_NAME.matcher(name.trim()).matches()) {
            name = "OM8-associated";
        }
        AssociatedDevice device = new AssociatedDevice(
                AssociatedDevice.CURRENT_SCHEMA_VERSION,
                name,
                address,
                associationId);
        new CompanionAssociationStore(context).save(device);
    }

    private static String addressOf(AssociationInfo associationInfo) {
        return associationInfo.getDeviceMacAddress() == null
                ? "" : associationInfo.getDeviceMacAddress().toString();
    }

    private static boolean hasCompanionFeature(Context context) {
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_COMPANION_DEVICE_SETUP);
    }
}
