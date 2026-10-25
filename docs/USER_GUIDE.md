# DJI Gimbal Bridge user guide

## First use

1. Install the app and grant Nearby Devices/notification permission if Android
   requests it.
2. Tap **Enable Camera Control** and enable the bridge Accessibility service in
   Android Settings. This is required for camera-app control; the bridge does
   not inject coordinates, gestures, shell keys, or ADB commands.
3. Tap **Find DJI Gimbal** and select the nearby `OM8-*` device. The selected
   device is remembered without hardcoding its address.
4. In **Camera Apps**, press **Mappings** for the camera app you intend to use.
   The built-in Sony Video Pro profile is labelled `BUILTIN / HARDWARE_TESTED`.
5. For another eligible camera app, use **+ Add camera app** and learn one
   Accessibility-visible Record control only if the app exposes a stable,
   clickable node. Review and confirm the candidate before saving it.

## Normal use

Turn on the remembered gimbal and open the configured camera application. The
foreground camera package selects its own profile automatically. The bridge
maintains the actual direct BLE GATT connection and subscribes to the verified
FFF4 notification characteristic; Android Bluetooth pairing alone is not a
substitute for that connection.

The Sony Video Pro default is:

- OM8 Record press: Record / Stop;
- OM8 Record release: no action;
- side-wheel press: decoded, but lens switching is unavailable until a safe
  semantic Sony Accessibility action is evidenced;
- side-wheel rotation: Zoom is shown as unavailable because its safe external
  encoding remains unresolved;
- M button, trigger, Switch, and joystick: no default Sony camera action.

DJI Mimo is not required for normal use and the bridge never force-stops it. If
another process owns the OM8 connection, the bridge reports a connection/retry
state and recovers when the device becomes available.

## Connection controls

**Disconnect** stops the active GATT owner and suppresses immediate automatic
reconnection while preserving the remembered device. **Forget DJI Gimbal**
clears the saved association. **Reconnect now** is intentionally under
**Advanced / Diagnostics** for recovery; it is not part of normal daily use.

The foreground notification says **DJI Gimbal Bridge** and shows the OM8
connection state while the connected-device service is active. Android system
Bluetooth screens may or may not expose an active app-owned GATT connection as
a normal bonded/connected device; the bridge notification is the authoritative
user-facing status when the system UI does not surface it.

## Diagnostics

Open **Advanced / Diagnostics** only when troubleshooting. **OM8 Raw
Diagnostics** is off by default. When enabled it persists across service
recreation and logs complete inbound FFF4 bytes plus parsed metadata; it does
not change the Record parser or Sony hot path. Disable it after capture.

## Support boundaries

The app is a validated foundation, not universal camera compatibility. Support
levels are recorded in `docs/OM8_CONTROL_SUPPORT.md` and distinguish hardware
tested, upstream-evidenced, hardware-unverified, learned/user-validated,
tested-negative, and unsupported/no-safe-path results. No production FFF5
application-data writes are performed.
