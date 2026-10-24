# Minimal OM8 vectors

These are structural regression vectors, not a claim that the final two bytes are captured CRC values.

~~~text
normal:
55 15 04 A9 27 02 B1 03 40 04 57 00 00 00 00 01 00 00 00 00 00

record down:
55 15 04 A9 27 02 B2 03 40 04 57 00 00 00 00 01 00 08 00 00 00
~~~

The driver validates the routing/length structure and derives Record state from payload byte 6 bit 0x08. It does not send these vectors to the gimbal.

