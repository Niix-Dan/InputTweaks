package com.niixlabs.inputtweaks.api.trigger;

import com.niixlabs.inputtweaks.core.model.ResolvedBind;

public interface TriggerStrategy {
    TriggerMode mode();

    boolean handle(ResolvedBind bind, boolean pressed, long nowMs);

    default void resetState(String bindId) {}
}
