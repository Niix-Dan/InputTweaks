package com.niixlabs.inputtweaks;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(InputTweaks.MOD_ID)
public final class InputTweaksForge {
    public InputTweaksForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            InputTweaksForgeClient.init();
        }
    }
}