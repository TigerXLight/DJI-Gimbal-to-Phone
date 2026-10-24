package com.androiddjibridge.core;

/** Pure controller-event to camera-action resolver. */
public final class MappingEngine {
    private volatile MappingStore store;

    public MappingEngine() {
        this(MappingStore.defaults());
    }

    public MappingEngine(MappingStore store) {
        setStore(store);
    }

    public MappingDecision resolve(ControllerEvent event) {
        ControllerTrigger trigger = ControllerTrigger.from(event);
        if (trigger == null) {
            return MappingDecision.unknown();
        }
        return MappingDecision.recognized(trigger, store.actionFor(trigger));
    }

    public MappingDecision map(ControllerEvent event) {
        return resolve(event);
    }

    public MappingStore getStore() {
        return store;
    }

    public void setStore(MappingStore replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("mapping store is required");
        }
        store = replacement;
    }
}
