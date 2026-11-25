package com.niixlabs.inputtweaks.ui.capture;

import com.niixlabs.inputtweaks.core.model.InputType;
import com.niixlabs.inputtweaks.core.model.KeyChord;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ChordCaptureSession {
    public static final int MAX_CHORD_KEYS = 4;

    private boolean active = false;
    private final LinkedHashSet<KeyCode> pressedOrder = new LinkedHashSet<>();

    public void start() {
        active = true;
        pressedOrder.clear();
    }

    public void cancel() {
        active = false;
        pressedOrder.clear();
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasKeysPressed() {
        return !pressedOrder.isEmpty();
    }

    public List<KeyCode> orderedKeys() {
        return List.copyOf(pressedOrder);
    }

    public Optional<KeyChord> onPress(KeyCode key) {
        if (!active || key == null || key.isUnknown()) {
            return Optional.empty();
        }
        if (key.type() == InputType.KEYBOARD && key.code() == GLFW.GLFW_KEY_ESCAPE) {
            if (pressedOrder.isEmpty()) {
                active = false;
                return Optional.of(KeyChord.UNBOUND);
            }
            cancel();
            return Optional.empty();
        }
        if (pressedOrder.contains(key)) {
            return Optional.empty();
        }

        pressedOrder.add(key);
        if (pressedOrder.size() >= MAX_CHORD_KEYS) {
            return Optional.of(commitAndClose());
        }
        return Optional.empty();
    }

    public Optional<KeyChord> onRelease(KeyCode key) {
        if (!active || key == null || !pressedOrder.contains(key)) {
            return Optional.empty();
        }
        return Optional.of(commitAndClose());
    }

    private KeyChord commitAndClose() {
        List<KeyCode> list = new ArrayList<>(pressedOrder);
        active = false;
        pressedOrder.clear();

        KeyCode primary = list.get(list.size() - 1);
        if (list.size() == 1) {
            return KeyChord.of(primary);
        }
        Set<KeyCode> modifiers = new LinkedHashSet<>(list.subList(0, list.size() - 1));
        return KeyChord.of(primary, modifiers);
    }
}
