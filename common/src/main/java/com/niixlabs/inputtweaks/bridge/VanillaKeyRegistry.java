package com.niixlabs.inputtweaks.bridge;

import com.niixlabs.inputtweaks.api.action.BindableAction;
import com.niixlabs.inputtweaks.core.model.RadialMenuDefinition;
import com.niixlabs.inputtweaks.platform.PlatformKeyBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class VanillaKeyRegistry {
    private final PlatformKeyBridge keyBridge;
    private Supplier<List<RadialMenuDefinition>> radialSupplier = List::of;

    public VanillaKeyRegistry(PlatformKeyBridge keyBridge) {
        this.keyBridge = keyBridge;
    }

    public void setRadialSupplier(Supplier<List<RadialMenuDefinition>> radialSupplier) {
        this.radialSupplier = radialSupplier;
    }

    public List<BindableAction> collectAll() {
        Minecraft mc = Minecraft.getInstance();
        List<BindableAction> list = new ArrayList<>();
        if (mc != null && mc.options != null) {
            for (KeyMapping mapping : mc.options.keyMappings) {
                list.add(new MinecraftKeyAdapter(mapping, keyBridge.resolveModId(mapping)));
            }
        }
        for (RadialMenuDefinition radial : radialSupplier.get()) {
            list.add(new RadialActionAdapter(radial));
        }
        return list;
    }

    public String resolveDefaultContext(BindableAction action) {
        if (action instanceof MinecraftKeyAdapter adapter) {
            return keyBridge.resolveDefaultContextId(adapter.raw());
        }
        if (action instanceof RadialActionAdapter) {
            return BuiltinContexts.IN_GAME;
        }
        return BuiltinContexts.UNIVERSAL;
    }

    public void syncVanillaMappings() {
        KeyMapping.resetMapping();
    }
}
