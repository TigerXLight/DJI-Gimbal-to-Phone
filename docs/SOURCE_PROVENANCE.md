# Source provenance

## Project licence boundary

The repository-root `LICENSE` is MIT and applies to project-owned/original and
independently implemented production code. It does not relicense the attributed
OpenGimbal material; its MIT copyright/notice is retained in
`THIRD_PARTY_NOTICES.md`. No Apache-2.0, GPL, no-licence expressive source, or
DJI proprietary/EULA source is incorporated.

## Final DJI UXSDK source audit

The final bounded audit cloned and inspected these repositories without copying
source into production:

- `dji-sdk/Mobile-UXSDK-Android`, `master`, commit
  `6a28754964a7517a0602dfda0bad09adc938489a`; DJI UX SDK EULA with MIT sample
  code boundary.
- `dji-sdk/Mobile-UXSDK-Beta-Android`, `master`, commit
  `ed5ca45d118efbada6716f372b96dc5fe11e0620`; MIT.

The Beta source contains lens-scoped focus-ring, focus-mode, and touch-focus
calls through DJI `CameraKey`/`KeyManager` APIs. Neither repository contains an
OM8/Osmo Mobile physical side-wheel origin or raw OM8 BLE/DUML input path. The
full source/method audit is
`Android-DJI-OM-Bridge-Research/reports/27_DJI_UXSDK_FINAL_SOURCE_AUDIT.md`.

Source project: a separate private Android project, not included in this
repository.

Canonical historical freeze: the immutable OM8/Sony reference snapshot held
outside this repository.

Extraction date: 2026-10-24.

Frozen source files used:

- SOURCE_SNAPSHOT/app/src/main/java/com/xperiamicprosync/ble/Om8BleListenerService.java
- SOURCE_SNAPSHOT/app/src/main/java/com/xperiamicprosync/ble/Om8DumlStatusParser.java
- SOURCE_SNAPSHOT/app/src/main/java/com/xperiamicprosync/ble/Om8RecButtonEdgeDetector.java
- SOURCE_SNAPSHOT/app/src/main/java/com/xperiamicprosync/ble/Om8SonyTriggerTiming.java
- SOURCE_SNAPSHOT/app/src/main/java/com/xperiamicprosync/accessibility/VideoProKeyAccessibilityService.java
- SOURCE_SNAPSHOT/app/src/main/java/com/xperiamicprosync/accessibility/VideoProKeyDecision.java
- SOURCE_SNAPSHOT/app/src/main/java/com/xperiamicprosync/diagnostics/Om8DiagnosticReceiver.java
- SOURCE_SNAPSHOT/app/src/main/java/com/xperiamicprosync/diagnostics/SonyRecDiagnosticReceiver.java
- SOURCE_SNAPSHOT/app/src/main/java/com/xperiamicprosync/logging/DiagnosticLogger.java

The extraction intentionally excludes all Mic Pro source, Mic Pro project-memory files, private HCI/bugreport archives, personal media, and unrelated private logs. The observed OM8 address is not copied into the implementation.

## Phase 3A upstream adaptation

The mapping boundary partially adapts the MIT-licensed mapping model from
ItzDFPlayer/OpenGimbal at commit `6b7f403265d4ef96c9238ecaeed12bf5ce9f711c`.
The local adaptation is limited to generic semantic triggers/actions and strict
app-private JSON persistence. It does not copy HJ360 BLE protocol code or camera
fallback behavior. Copyright (c) 2026 Denys and the MIT notice are preserved in
`THIRD_PARTY_NOTICES.md`; the detailed ledger is in
`DEPENDENCIES_AND_PROVENANCE.md` and the research workspace adaptation ledger.

## Phase 3B upstream adaptation

Phase 3B uses the OpenGimbal MIT research as an adaptation donor for the pure
auto-connect policy/retry boundary (`AutoConnectPolicy`, `ReconnectBackoff`). The
Android Companion Device implementation and OM8 service integration are local
code. No HJ360 wire protocol, screenshot/coordinate fallback, or OpenGimbal
transport UUID was copied. See `DEPENDENCIES_AND_PROVENANCE.md` and the research
`ADAPTATION_LEDGER.json` for the exact upstream commit and adaptation record.

## Current integration additions

The production tree adds local `GimbalProfile` transport metadata, profile-scoped
mapping persistence, a pure gesture-policy boundary, and an explicit raw OM8
diagnostic toggle. These are local additions and do not alter the frozen OM8/Sony
implementation path. `PixelCameraProfileEvidence` records only the exact
selectors reported by `video-boost-ao` (MIT, commit
`0205b761dca8b033b755ac18421f25ad1ef2d426`); no source service or privileged
ADB/Shizuku behavior was incorporated. Samsung is represented only by a local
fail-closed research status.

