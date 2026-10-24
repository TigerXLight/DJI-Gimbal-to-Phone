# Contributing future device drivers

Do not assume DJI OM models share one protocol. Add a new driver only after collecting device-specific advertisement, GATT, notification, parser, and hardware evidence.

Each driver should:

1. expose the generic GimbalDriver lifecycle and capability boundary;
2. emit normalized ControllerEvent values;
3. keep packet parsing and transport ownership inside the driver module;
4. avoid camera-package, Accessibility, Xperia, and recorder imports;
5. add parser, edge, recovery, and hardware procedures before claiming support;
6. document unknown controls as UNIMPLEMENTED / NOT YET DECODED rather than guessing.

Movement, zoom, focus, and arbitrary command writes are not part of the Phase 1 OM8 driver.

