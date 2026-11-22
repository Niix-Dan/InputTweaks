package com.niixlabs.inputtweaks.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ModpackConfig(
        Map<String, ModpackRule> rules,
        List<RadialMenuDefinition> radialMenus
) {
    public ModpackConfig {
        rules = new LinkedHashMap<>(rules == null ? Map.of() : rules);
        radialMenus = new ArrayList<>(radialMenus == null ? List.of() : radialMenus);
    }

    public static ModpackConfig empty() {
        return new ModpackConfig(new LinkedHashMap<>(), new ArrayList<>());
    }
}
