package com.niixlabs.inputtweaks.core.layer;

import com.niixlabs.inputtweaks.core.model.BindOrigin;
import com.niixlabs.inputtweaks.core.model.ModpackConfig;
import com.niixlabs.inputtweaks.core.model.ModpackRule;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

public final class ModpackConfigLayer implements ConfigLayer {
    private final ModpackConfig config;

    public ModpackConfigLayer(ModpackConfig config) {
        this.config = config;
    }

    @Override
    public BindOrigin origin() {
        return BindOrigin.MODPACK;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void apply(String bindId, ResolvedBind.Builder builder) {
        if (config == null) return;
        ModpackRule rule = config.rules().get(bindId);
        if (rule == null) return;

        builder.applyHidden(rule.hidden());
        builder.applyHardDisabled(rule.hardDisabled());
        if (rule.defaultChord() != null) {
            builder.applyChord(rule.defaultChord(), BindOrigin.MODPACK);
        }
        if (rule.categoryOverride() != null && !rule.categoryOverride().isBlank()) {
            builder.applyCategory(rule.categoryOverride());
        }
        builder.applyLocked(rule.locked());
    }
}
