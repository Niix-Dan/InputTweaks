package com.niixlabs.inputtweaks.core.trigger;

import com.niixlabs.inputtweaks.api.trigger.TriggerMode;
import com.niixlabs.inputtweaks.api.trigger.TriggerStrategy;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

import java.util.HashMap;
import java.util.Map;

public final class TapTriggerStrategy implements TriggerStrategy {
    private static final long MAX_TAP_DURATION_MS = 240L;
    private final Map<String, Long> pressStart = new HashMap<>();

    @Override
    public TriggerMode mode() {
        return TriggerMode.TAP;
    }

    @Override
    public boolean handle(ResolvedBind bind, boolean pressed, long nowMs) {
        if (pressed) {
            pressStart.put(bind.id(), nowMs);
            return false;
        }
        Long started = pressStart.remove(bind.id());
        if (started != null && (nowMs - started) <= MAX_TAP_DURATION_MS) {
            bind.action().setPressed(true);
            bind.action().triggerClick();
            bind.action().setPressed(false);
            return true;
        }
        return false;
    }

    @Override
    public void resetState(String bindId) {
        pressStart.remove(bindId);
    }
}
