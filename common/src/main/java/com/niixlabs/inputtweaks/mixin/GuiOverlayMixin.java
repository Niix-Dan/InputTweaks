package com.niixlabs.inputtweaks.mixin;

import com.niixlabs.inputtweaks.ui.overlay.DebugInputOverlay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void inputtweaks$renderDebugHud(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        DebugInputOverlay.render(graphics);
    }
}
