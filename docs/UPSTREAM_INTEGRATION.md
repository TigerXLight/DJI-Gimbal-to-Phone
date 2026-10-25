# Upstream integration status

This document records what has been incorporated into the production bridge after
the upstream research pass. It is an implementation record, not a compatibility
claim.

## Integrated now

- OpenGimbal (MIT, commit `6b7f403265d4ef96c9238ecaeed12bf5ce9f711c`) informed and
  partially donated the controller-neutral mapping boundary, immutable mapping
  defaults, bounded lifecycle policy, and gesture-model separation. The local
  OM8 FFF4 parser and Sony path remain original/model-specific production code.
- OpenGimbal's HJ360 protocol, screenshot shutter, coordinate fallback, and generic
  gesture fallback were not imported.
- The production mapping store is now profile-scoped in app-private preferences.
  `RECORD_PRESS -> RECORD_TOGGLE` remains the safe default and `RECORD_RELEASE`
  remains a no-op.
- A data-only `GimbalProfile`/registry separates transport metadata from the
  driver. OM8's FFF0/FFF4/FFF5 UUID metadata is descriptive only; this release
  does not write FFF5 or add movement commands.
- Raw OM8 packet logging is an explicit UI-controlled diagnostic mode. Normal
  heartbeat processing remains quiet.

## Evidence-backed but not enabled as universal control

`video-boost-ao` (MIT) provides Pixel Camera selectors for the settings/mode
workflow. The production core records those selectors as
`IMPLEMENTED_HARDWARE_UNVERIFIED` evidence, but does not pretend that a settings
button is a safe shutter target. Pixel Record/Photo control remains fail-closed
until a target-specific record control is evidenced.

Samsung stock camera has no sufficiently stable ordinary-app contract in the
research set. It remains `UNSUPPORTED_NO_SAFE_PATH` for an unlearned profile;
learned profiles can still be user-validated through the strict exact-match path.

Photography Pro's ordinary-app `ACTION_CAMERA_BUTTON` probe is `TESTED_NEGATIVE`.
The privileged shell key path is not a production capability.

## Deliberately excluded

No GPL/unknown-license source was copied into production. No ADB, root, Shizuku,
coordinate tap, gesture fallback, arbitrary intent, arbitrary GATT write, or
Insta360 code was added. The full provenance ledger is maintained in the sibling
research workspace and mirrored by the repository provenance documents.

## Final DJI UXSDK audit and OM8 boundary

`dji-sdk/Mobile-UXSDK-Android` (`6a28754964a7517a0602dfda0bad09adc938489a`)
and `dji-sdk/Mobile-UXSDK-Beta-Android`
(`ed5ca45d118efbada6716f372b96dc5fe11e0620`) were audited in full enough for
the bounded closeout. The Beta repository shows camera-command/UI paths for
focus ring, focus mode, and touch focus; neither repository shows an OM8
physical side-wheel input origin or raw OM8 BLE/DUML transport. Neither is a
production dependency. The side-wheel rotation status is therefore
`UNSUPPORTED_NO_SAFE_PATH`; side-wheel press is separately decoded from the
verified FFF4 `04/57` field.

## Scope boundary

The current release establishes the reusable architecture and safe controls UI;
it does not claim that all camera apps, Pixel Camera, Samsung Camera, or Sony
Photography Pro are controllable. Hardware validation remains per profile and per
backend.
