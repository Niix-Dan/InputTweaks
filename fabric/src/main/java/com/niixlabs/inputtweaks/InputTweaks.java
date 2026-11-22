package com.niixlabs.inputtweaks;

import net.fabricmc.api.ModInitializer;

public class InputTweaks implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.LOG.info("Hello Fabric world!");
        InputTweaks.init();
    }
}
