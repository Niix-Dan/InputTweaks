package com.niixlabs.inputtweaks.core.trigger;

import com.niixlabs.inputtweaks.api.trigger.TriggerMode;
import com.niixlabs.inputtweaks.api.trigger.TriggerStrategy;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

public final class PressTriggerStrategy implements TriggerStrategy {
    @Override
    public TriggerMode mode() {
        return TriggerMode.PRESS;
    }

    @Override
    public boolean handle(ResolvedBind bind, boolean pressed, long nowMs) {
        bind.action().setPressed(pressed);
        if (pressed) {
            bind.action().triggerClick();
            return true;
        }
        return false;
    }
}
