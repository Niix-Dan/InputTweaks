package com.niixlabs.inputtweaks.core.trigger;

import com.niixlabs.inputtweaks.api.trigger.TriggerMode;
import com.niixlabs.inputtweaks.api.trigger.TriggerStrategy;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

import java.util.HashMap;
import java.util.Map;

public final class DoubleTapTriggerStrategy implements TriggerStrategy {
    private static final long DOUBLE_TAP_WINDOW_MS = 320L;
    private final Map<String, Long> lastTap = new HashMap<>();

    @Override
    public TriggerMode mode() {
        return TriggerMode.DOUBLE_TAP;
    }

    @Override
    public boolean handle(ResolvedBind bind, boolean pressed, long nowMs) {
        if (!pressed) {
            bind.action().setPressed(false);
            return false;
        }
        Long prev = lastTap.get(bind.id());
        if (prev != null && (nowMs - prev) <= DOUBLE_TAP_WINDOW_MS) {
            lastTap.remove(bind.id());
            bind.action().setPressed(true);
            bind.action().triggerClick();
            return true;
        }
        lastTap.put(bind.id(), nowMs);
        return false;
    }

    @Override
    public void resetState(String bindId) {
        lastTap.remove(bindId);
    }
}
