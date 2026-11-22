package com.niixlabs.inputtweaks.storage;

import com.niixlabs.inputtweaks.core.model.ModpackConfig;

public interface ModpackRuleRepository {
    ModpackConfig loadRules();

    void saveRules(ModpackConfig config);
}
