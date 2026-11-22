package com.niixlabs.inputtweaks.core.layer;

import com.niixlabs.inputtweaks.api.action.BindableAction;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

import java.util.Comparator;
import java.util.List;

public final class LayerMerger {
    public ResolvedBind merge(BindableAction action, String defaultContextId, List<ConfigLayer> layers) {
        ResolvedBind.Builder builder = new ResolvedBind.Builder(action, defaultContextId);
        layers.stream()
                .sorted(Comparator.comparingInt(ConfigLayer::order))
                .forEach(layer -> layer.apply(action.id(), builder));
        return builder.build();
    }
}
