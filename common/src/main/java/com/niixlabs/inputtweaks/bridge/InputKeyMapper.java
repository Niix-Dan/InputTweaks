package com.niixlabs.inputtweaks.bridge;

import com.mojang.blaze3d.platform.InputConstants;
import com.niixlabs.inputtweaks.core.model.InputType;
import com.niixlabs.inputtweaks.core.model.KeyChord;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class InputKeyMapper {
    private InputKeyMapper() {}

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

    public static Component formatKeyCompact(KeyCode keyCode) {
        if (keyCode == null || keyCode.isUnknown()) {
            return InputConstants.UNKNOWN.getDisplayName();
        }
        if (keyCode.type() == InputType.MOUSE) {
            return switch (keyCode.code()) {
                case 0 -> Component.literal("LMB");
                case 1 -> Component.literal("RMB");
                case 2 -> Component.literal("MMB");
                default -> Component.literal("M" + (keyCode.code() + 1));
            };
        }
        return switch (keyCode.code()) {
            case GLFW.GLFW_KEY_LEFT_CONTROL -> Component.literal("Ctrl");
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> Component.literal("RCtrl");
            case GLFW.GLFW_KEY_LEFT_SHIFT -> Component.literal("Shift");
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> Component.literal("RShift");
            case GLFW.GLFW_KEY_LEFT_ALT -> Component.literal("Alt");
            case GLFW.GLFW_KEY_RIGHT_ALT -> Component.literal("RAlt");
            case GLFW.GLFW_KEY_SPACE -> Component.literal("Space");
            default -> formatKey(keyCode);
        };
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

    public static Component formatChordCompact(KeyChord chord) {
        if (chord == null || chord.isUnbound()) {
            return InputConstants.UNKNOWN.getDisplayName();
        }
        if (!chord.hasModifiers()) {
            return formatKey(chord.primary());
        }
        MutableComponent result = Component.empty();
        for (KeyCode mod : chord.modifiers()) {
            result.append(formatKeyCompact(mod)).append(" + ");
        }
        result.append(formatKeyCompact(chord.primary()));
        return result;
    }

    public static Component formatCapturePreview(List<KeyCode> orderedKeys) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < orderedKeys.size(); i++) {
            if (i > 0) result.append(" + ");
            result.append(formatKeyCompact(orderedKeys.get(i)));
        }
        result.append(" + ...");
        return result;
    }
}
