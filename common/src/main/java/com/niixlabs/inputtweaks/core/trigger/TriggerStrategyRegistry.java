package com.niixlabs.inputtweaks.core.trigger;

import com.niixlabs.inputtweaks.api.trigger.TriggerMode;
import com.niixlabs.inputtweaks.api.trigger.TriggerStrategy;

import java.util.EnumMap;
import java.util.Map;

public final class TriggerStrategyRegistry {
    private final Map<TriggerMode, TriggerStrategy> strategies = new EnumMap<>(TriggerMode.class);

    public TriggerStrategyRegistry() {
        register(new PressTriggerStrategy());
        register(new TapTriggerStrategy());
        register(new HoldTriggerStrategy());
        register(new DoubleTapTriggerStrategy());
        register(new ToggleTriggerStrategy());
    }

    public void register(TriggerStrategy strategy) {
        strategies.put(strategy.mode(), strategy);
    }

    public TriggerStrategy get(TriggerMode mode) {
        return strategies.getOrDefault(mode, strategies.get(TriggerMode.PRESS));
    }

    public void resetAllForBind(String bindId) {
        strategies.values().forEach(s -> s.resetState(bindId));
    }
}
