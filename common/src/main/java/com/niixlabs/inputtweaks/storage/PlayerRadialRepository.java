package com.niixlabs.inputtweaks.storage;

import com.niixlabs.inputtweaks.core.model.RadialMenuDefinition;

import java.util.List;

public interface PlayerRadialRepository {
    List<RadialMenuDefinition> loadPlayerRadials();

    void savePlayerRadials(List<RadialMenuDefinition> menus);
}
