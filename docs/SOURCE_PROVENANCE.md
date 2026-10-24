# Source provenance

Source project:

T:\References\to go\Media\tools\Insta360 send receive app

Canonical freeze:

FROZEN_REFERENCE\OM8_SONY_2026-10-24

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

