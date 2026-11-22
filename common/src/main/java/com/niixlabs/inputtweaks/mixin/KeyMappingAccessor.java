package com.niixlabs.inputtweaks.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("key")
    InputConstants.Key inputtweaks$getKey();

    @Accessor("clickCount")
    int inputtweaks$getClickCount();

    @Accessor("clickCount")
    void inputtweaks$setClickCount(int count);
}
