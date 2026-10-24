# Camera profiles

## Purpose

`CameraProfile` is immutable, versioned data describing how a known Android camera
application exposes a recording control and, optionally, observable recording state.
It separates stable target metadata from adapter execution and safety logic.

It is not a permission to control an arbitrary foreground package, a script, an intent,
an ADB command, a coordinate tap, or a generic Accessibility action list.

## Phase 2A built-in profile

The registry contains exactly one built-in profile:

| Field | Value |
| --- | --- |
| Stable ID | `sony-videopro` |
| Display name | Sony Video Pro |
| Package | `jp.co.sony.mc.videopro` |
| Record resource | `jp.co.sony.mc.videopro:id/recording` |
| Record class | `android.widget.ImageButton` |
| Accepted descriptions | `Start video recording`, `Stop video recording` |
| State resource | `jp.co.sony.mc.videopro:id/rec_status` |
| Recording state | `REC` |
| Standby state | `Stby` |
| Duration resource | `jp.co.sony.mc.videopro:id/duration` |
| Capability | `RECORD_TOGGLE` |
| Origin | `BUILTIN` |
| Validation | `HARDWARE_TESTED` |

These identifiers come from the existing Sony hardware evidence. No additional camera
application is claimed or added by Phase 2A.

## JSON schema shape

The version-1 format uses deterministic field ordering and includes:

~~~json
{
  "schemaVersion": 1,
  "stableProfileId": "sony-videopro",
  "displayName": "Sony Video Pro",
  "packageName": "jp.co.sony.mc.videopro",
  "recordButtonResourceId": "jp.co.sony.mc.videopro:id/recording",
  "recordButtonClassName": "android.widget.ImageButton",
  "allowedContentDescriptions": ["Start video recording", "Stop video recording"],
  "requiredAccessibilityActions": ["CLICK"],
  "stateResourceId": "jp.co.sony.mc.videopro:id/rec_status",
  "recordingStateValues": ["REC"],
  "standbyStateValues": ["Stby"],
  "durationResourceId": "jp.co.sony.mc.videopro:id/duration",
  "stateVerificationSupported": true,
  "capabilities": ["RECORD_TOGGLE"],
  "origin": "BUILTIN",
  "validationStatus": "HARDWARE_TESTED",
  "notes": "..."
}
~~~

Optional parent/child constraints and content-description patterns are supported by
the data model. The parser rejects missing required fields, malformed package/resource
metadata, unknown fields, unknown capabilities/actions, malformed JSON, and schema
versions newer than the supported version. No broad storage permission or import UI is
present yet.

## Adapter boundary and safety

The Sony adapter remains the sole owner of Sony-specific operation. Before an action it
continues to enforce the selected Sony package, actual Sony foreground/window context,
interactive/keyguard safety, fresh/prepared node validity, required node predicates,
and stale-generation rejection. The post-click `rec_status` value remains the
authoritative REC/Stby verification signal. Profile data does not bypass these checks.

## Phase 2B learned profiles

The schema supports a user-learned record control. `recordButtonResourceId`,
`recordButtonClassName`, `recordButtonText`, and content-description metadata are
optional individually, but a profile must retain enough identity metadata to be
deterministically matchable. Learned profiles use `origin=LEARNED`, remain
`UNTESTED`/unverified, and use `RECORD_TOGGLE` without claiming state verification.

Learning is explicitly package-scoped and requires user confirmation. The persisted
JSON is written only to the app-private `files/learned_profiles` directory through
the existing deterministic codec. Built-in IDs/packages cannot be overwritten by a
learned profile; malformed files, duplicate identities, and unsupported schema
versions are rejected. Profiles are data only and cannot encode arbitrary executable
actions, intents, shell commands, or coordinate control.

The generic learned adapter resolves a fresh Accessibility tree for every deliberate
test action. It requires the exact selected package, interactive/unlocked device,
one unambiguous usable node, and `ACTION_CLICK`. It never uses the learned node as a
long-lived stale object. Sony Video Pro remains on the specialized adapter and is
the only camera application currently hardware-tested.

Application discovery is separate from profile compatibility. The Learn Camera list
combines launchable apps, `VIDEO_CAPTURE` resolvers, `IMAGE_CAPTURE` resolvers, and
the installed built-in profile package, then deduplicates by package. A listed
package is only eligible to attempt a scoped capture; it is not a support claim.
Preinstalled/system camera status is allowed and is never the sole rejection reason.
The built-in Sony Video Pro profile cannot be overwritten or masqueraded as a learned
profile.
