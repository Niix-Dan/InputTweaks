package com.niixlabs.inputtweaks;

import com.niixlabs.inputtweaks.bridge.BuiltinContexts;
import com.niixlabs.inputtweaks.platform.PlatformKeyBridge;
import com.niixlabs.inputtweaks.platform.PlatformPaths;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.loading.FMLPaths;

import com.niixlabs.inputtweaks.ui.overlay.DebugInputOverlay;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;

import java.nio.file.Path;

final class InputTweaksForgeClient {
    private InputTweaksForgeClient() {}

    static void init() {
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

        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
            DebugInputOverlay.render(event.getGuiGraphics());
        });
    }
}