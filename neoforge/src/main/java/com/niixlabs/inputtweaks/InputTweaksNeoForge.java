package com.niixlabs.inputtweaks;

import com.niixlabs.inputtweaks.bridge.BuiltinContexts;
import com.niixlabs.inputtweaks.platform.PlatformKeyBridge;
import com.niixlabs.inputtweaks.platform.PlatformPaths;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

import java.nio.file.Path;

@Mod(value = InputTweaks.MOD_ID, dist = Dist.CLIENT)
public final class InputTweaksNeoForge {
    public InputTweaksNeoForge() {
        PlatformPaths paths = new PlatformPaths() {
            @Override public Path configDir() { return FMLPaths.CONFIGDIR.get(); }
            @Override public Path gameDir() { return FMLPaths.GAMEDIR.get(); }
        };
        PlatformKeyBridge keyBridge = new PlatformKeyBridge() {
            @Override
            public String resolveDefaultContextId(KeyMapping mapping) {
                IKeyConflictContext ctx = mapping.getKeyConflictContext();
                if (ctx == KeyConflictContext.IN_GAME) return BuiltinContexts.IN_GAME;
                if (ctx == KeyConflictContext.GUI) return BuiltinContexts.GUI;
                return BuiltinContexts.UNIVERSAL;
            }
        };
        InputTweaks.init(paths, keyBridge);
    }
}
