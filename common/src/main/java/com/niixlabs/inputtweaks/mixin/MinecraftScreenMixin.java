package com.niixlabs.inputtweaks.mixin;

import com.niixlabs.inputtweaks.InputTweaks;
import com.niixlabs.inputtweaks.ui.screen.InputTweaksScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftScreenMixin {
    @Shadow
    public Screen screen;
    @Shadow
    @Final
    public Options options;

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen inputtweaks$replaceKeyBindsScreen(Screen next) {
        if (InputTweaks.get() != null && next instanceof KeyBindsScreen) {
            return new InputTweaksScreen(this.screen, this.options);
        }
        return next;
    }
}
