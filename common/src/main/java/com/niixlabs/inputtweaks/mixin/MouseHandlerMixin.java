package com.niixlabs.inputtweaks.mixin;

import com.niixlabs.inputtweaks.InputTweaks;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void inputtweaks$onMousePress(long window, int button, int action, int modifiers, CallbackInfo ci) {
        InputTweaks app = InputTweaks.get();
        if (app == null || window != this.minecraft.getWindow().getWindow()) return;

        app.profileManager().ensureLoaded();
        boolean pressed = action == GLFW.GLFW_PRESS;
        boolean screenOpen = minecraft.screen != null;

        if (app.dispatcher().handleInput(KeyCode.mouse(button), pressed, screenOpen)) {
            ci.cancel();
        }
    }
}
