package com.niixlabs.inputtweaks.mixin;

import com.niixlabs.inputtweaks.InputTweaks;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void inputtweaks$onKeyPress(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        InputTweaks app = InputTweaks.get();
        if (app == null || action == GLFW.GLFW_REPEAT) return;

        app.profileManager().ensureLoaded();
        boolean pressed = action == GLFW.GLFW_PRESS;
        boolean textActive = minecraft.screen != null;

        if (app.dispatcher().handleInput(KeyCode.keyboard(key), pressed, textActive)) {
            ci.cancel();
        }
    }
}
