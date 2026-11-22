package com.niixlabs.inputtweaks.core.trigger;

import com.niixlabs.inputtweaks.api.trigger.TriggerMode;
import com.niixlabs.inputtweaks.api.trigger.TriggerStrategy;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

import java.util.HashMap;
import java.util.Map;

public final class ToggleTriggerStrategy implements TriggerStrategy {
    private final Map<String, Boolean> toggleState = new HashMap<>();

    @Override
    public TriggerMode mode() {
        return TriggerMode.TOGGLE;
    }

    @Override
    public boolean handle(ResolvedBind bind, boolean pressed, long nowMs) {
        if (!pressed) return false;
        boolean next = !toggleState.getOrDefault(bind.id(), false);
        toggleState.put(bind.id(), next);
        bind.action().setPressed(next);
        if (next) bind.action().triggerClick();
        return true;
    }

    @Override
    public void resetState(String bindId) {
        toggleState.remove(bindId);
    }
}
