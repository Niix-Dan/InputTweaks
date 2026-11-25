package com.niixlabs.inputtweaks.mixin;

import com.niixlabs.inputtweaks.InputTweaks;
import com.niixlabs.inputtweaks.bridge.InputKeyMapper;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Shadow @Final private String name;

    @Inject(method = "getTranslatedKeyMessage", at = @At("HEAD"), cancellable = true)
    private void inputtweaks$formatChordDisplayName(CallbackInfoReturnable<Component> cir) {
        InputTweaks app = InputTweaks.get();
        if (app == null) return;
        ResolvedBind resolved = app.profileManager().getResolved(this.name);
        if (resolved != null && !resolved.hardDisabled() && resolved.chord().hasModifiers()) {
            cir.setReturnValue(InputKeyMapper.formatChordCompact(resolved.chord()));
        }
    }

    @Inject(method = "isUnbound", at = @At("HEAD"), cancellable = true)
    private void inputtweaks$checkChordBound(CallbackInfoReturnable<Boolean> cir) {
        InputTweaks app = InputTweaks.get();
        if (app == null) return;
        ResolvedBind resolved = app.profileManager().getResolved(this.name);
        if (resolved != null && !resolved.hardDisabled() && resolved.chord().hasModifiers()) {
            cir.setReturnValue(false);
        }
    }
}
