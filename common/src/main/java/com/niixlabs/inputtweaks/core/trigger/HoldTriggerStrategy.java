package com.niixlabs.inputtweaks.core.trigger;

import com.niixlabs.inputtweaks.api.trigger.TriggerMode;
import com.niixlabs.inputtweaks.api.trigger.TriggerStrategy;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

import java.util.HashMap;
import java.util.Map;

public final class HoldTriggerStrategy implements TriggerStrategy {
    private static final long MIN_HOLD_DURATION_MS = 260L;
    private final Map<String, Long> pressStart = new HashMap<>();

    @Override
    public TriggerMode mode() {
        return TriggerMode.HOLD;
    }

    @Override
    public boolean handle(ResolvedBind bind, boolean pressed, long nowMs) {
        if (pressed) {
            pressStart.putIfAbsent(bind.id(), nowMs);
            return false;
        }
        Long started = pressStart.remove(bind.id());
        bind.action().setPressed(false);
        if (started != null && (nowMs - started) >= MIN_HOLD_DURATION_MS) {
            bind.action().triggerClick();
            return true;
        }
        return false;
    }

    @Override
    public void resetState(String bindId) {
        pressStart.remove(bindId);
    }
}
