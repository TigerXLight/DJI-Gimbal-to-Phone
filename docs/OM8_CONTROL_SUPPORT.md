# OM8 control support matrix

This matrix records the current product evidence boundary. The Sony Video Pro
Record path was hardware-tested historically, but the consumer-preview UI and
lifecycle changes still require the final acceptance sequence before a public
release or `1.0.0` claim.

This matrix separates passive protocol decoding from a camera action actually
being safe and hardware-validated. A decoded controller state is not by itself
permission to click a camera control.

| OM8 input/state | Verified representation | Production status | Camera-action status |
|---|---|---|---|
| Record | FFF4 `04/57`, payload byte 6 bit `0x08`; false-to-true edge | `HARDWARE_TESTED` | Sony Video Pro `RECORD_TOGGLE`, hardware-tested |
| Joystick | FFF4 `04/57`, bytes 0--1 signed LE X and bytes 2--3 signed LE Y | `HARDWARE_TESTED` for passive field decoding | No default camera mapping; no motor/action implementation |
| M button | FFF4 `04/57`, payload byte 4 observed as `0x03` | `HARDWARE_TESTED` for passive field decoding | No default camera mapping |
| Rear trigger | FFF4 `04/57`, payload byte 4 observed as `0x11`, `0x21`, `0x61`, `0x71` | `HARDWARE_TESTED` for observed raw states | Exact `0x61`/`0x71` semantics remain intentionally unresolved; no default camera mapping |
| Switch | FFF4 `04/57`, payload byte 5 `0x20` single / `0x40` double | `HARDWARE_TESTED` for passive field decoding | No default camera mapping |
| Side-wheel press | FFF4 `04/57`, payload byte 6 bit `0x01` | `HARDWARE_TESTED` for passive field decoding | Reserved intended Sony mapping: `SWITCH_LENS`; not enabled because no safe/evidenced Sony semantic lens-switch action is currently implemented |
| Side-wheel rotation | No reproducible field in correctly timed passive FFF4, FFF3, custom notify, or device-matched Mimo HCI/ATT evidence | `UNSUPPORTED_NO_SAFE_PATH` | Not decoded; no guessed telemetry or FFF5 replay |

The parser exposes the verified raw fields for diagnostics and future
state-machine work. It does not dispatch non-Record camera actions, and the
default mapping remains `RECORD_PRESS -> RECORD_TOGGLE` with
`RECORD_RELEASE -> NONE`. Joystick consumers must use a deadzone and state
transitions rather than exact observed magnitudes.

## Evidence boundary

The OM8 side-wheel rotation physically works in DJI Mimo, but its safe external
source remains unresolved. The completed evidence includes:

- repeated passive PC FFF4/FFF3 captures;
- a Mimo-owned HCI/ATT census;
- the custom `15f1e603-a277-43fc-a484-dd39ef8a9100` notification experiment;
- USB-C port reconnaissance with no Windows USB data interface observed; and
- final source audits of DJI Mobile UXSDK Android and Beta Android.

No safe FFF5 activation/replay sequence was established. The bridge therefore
does not send application-data FFF5 writes.

For the Sony Video Pro use case, the decoded side-wheel press is reserved for
future camera/lens switching, but it is not currently routed to Sony. The
specialized Sony adapter intentionally advertises and executes only
`RECORD_TOGGLE`; no safe Sony Accessibility node/action for lens switching has
been evidenced. Side-wheel rotation remains reserved for Sony zoom and is
`UNSUPPORTED_NO_SAFE_PATH`.

## Support taxonomy

- `HARDWARE_TESTED`: the stated device/application path was exercised on real
  hardware, or the passive representation was directly reproduced in a
  controlled capture as stated.
- `IMPLEMENTED_UPSTREAM_EVIDENCED`: implementation exists from credible
  upstream evidence but has not been validated on the target hardware here.
- `IMPLEMENTED_HARDWARE_UNVERIFIED`: a defensive implementation exists but
  target hardware validation is still required.
- `EXPECTED_COMPATIBLE`: a bounded compatibility expectation, not support.
- `LEARNED_USER_VALIDATED`: a user-created profile passed its deliberate test;
  it is not a built-in universal claim.
- `TESTED_NEGATIVE`: the named backend or experiment failed under the stated
  conditions.
- `UNSUPPORTED_NO_SAFE_PATH`: no safe, evidenced production path is available.

