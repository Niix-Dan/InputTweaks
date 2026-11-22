package com.niixlabs.inputtweaks;

import com.niixlabs.inputtweaks.platform.PlatformKeyBridge;
import com.niixlabs.inputtweaks.platform.PlatformPaths;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class InputTweaksFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PlatformPaths paths = new PlatformPaths() {
            @Override public Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
            @Override public Path gameDir() { return FabricLoader.getInstance().getGameDir(); }
        };
        PlatformKeyBridge keyBridge = new PlatformKeyBridge() {};
        InputTweaks.init(paths, keyBridge);
    }
}