# OM8 protocol and driver boundary

The driver is receive-only in Phase 1.

## GATT

~~~text
service:       0000fff0-0000-1000-8000-00805f9b34fb
notification:  0000fff4-0000-1000-8000-00805f9b34fb
CCCD:          00002902-0000-1000-8000-00805f9b34fb
~~~

Discovery selects a device whose discovered advertisement/device name begins with OM8-. The observed address is not a hardcoded identity and is not included in this repository.

## DUML status validation

The parser requires the 0x55 marker, a present non-truncated declared length, sufficient payload for payload[6], sender 0x27, receiver 0x02, flags 0x40, command set 0x04, and command ID 0x57.

~~~java
boolean recordDown = (payload[6] & 0x08) != 0;
~~~

Only false-to-true creates RECORD_DOWN. True-to-false creates RECORD_UP for diagnostics/adapter observation and never creates a second recording action. Repeated heartbeat states are ignored for trigger purposes.

## Connection lifecycle

The driver preserves direct scan/GATT operation, main-looper callbacks on API 26+, stale-GATT rejection, FFF0/FFF4/CCCD readiness, controlled scan retry/backoff, notification watchdog recovery, Bluetooth off/on recovery, and one CONNECTION_PRIORITY_HIGH request per genuine new GATT connection. The API boolean is not evidence of a changed connection interval; HCI evidence is required.

No FFF5 command or movement/control packet is sent.

