package com.niixlabs.inputtweaks.bridge;

import com.mojang.blaze3d.platform.InputConstants;
import com.niixlabs.inputtweaks.core.model.InputType;
import com.niixlabs.inputtweaks.core.model.KeyChord;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class InputKeyMapper {
    private InputKeyMapper() {
    }

    public static KeyCode toKeyCode(InputConstants.Key mcKey) {
        if (mcKey == null || mcKey.equals(InputConstants.UNKNOWN)) {
            return KeyCode.UNKNOWN;
        }
        return mcKey.getType() == InputConstants.Type.MOUSE
                ? KeyCode.mouse(mcKey.getValue())
                : KeyCode.keyboard(mcKey.getValue());
    }

    public static InputConstants.Key toMinecraftKey(KeyCode keyCode) {
        if (keyCode == null || keyCode.isUnknown()) {
            return InputConstants.UNKNOWN;
        }
        return keyCode.type() == InputType.MOUSE
                ? InputConstants.Type.MOUSE.getOrCreate(keyCode.code())
                : InputConstants.Type.KEYSYM.getOrCreate(keyCode.code());
    }

    public static Component formatKey(KeyCode keyCode) {
        if (keyCode == null || keyCode.isUnknown()) {
            return InputConstants.UNKNOWN.getDisplayName();
        }
        return toMinecraftKey(keyCode).getDisplayName();
    }

    public static Component formatChord(KeyChord chord) {
        if (chord == null || chord.isUnbound()) {
            return InputConstants.UNKNOWN.getDisplayName();
        }
        MutableComponent result = Component.empty();
        for (KeyCode mod : chord.modifiers()) {
            result.append(formatKey(mod)).append(" + ");
        }
        result.append(formatKey(chord.primary()));
        return result;
    }
}
