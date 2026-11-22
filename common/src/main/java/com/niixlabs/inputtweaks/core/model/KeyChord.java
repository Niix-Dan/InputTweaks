package com.niixlabs.inputtweaks.core.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public record KeyChord(KeyCode primary, Set<KeyCode> modifiers) {
    public static final KeyChord UNBOUND = new KeyChord(KeyCode.UNKNOWN, Set.of());

    public KeyChord {
        primary = primary == null ? KeyCode.UNKNOWN : primary;
        if (primary.isUnknown() || modifiers == null || modifiers.isEmpty()) {
            modifiers = Set.of();
        } else {
            TreeSet<KeyCode> sorted = new TreeSet<>(modifiers);
            sorted.remove(primary);
            sorted.remove(KeyCode.UNKNOWN);
            modifiers = Collections.unmodifiableSet(sorted);
        }
    }

    public static KeyChord of(KeyCode primary) {
        return (primary == null || primary.isUnknown()) ? UNBOUND : new KeyChord(primary, Set.of());
    }

    public static KeyChord of(KeyCode primary, Collection<KeyCode> modifiers) {
        return (primary == null || primary.isUnknown()) ? UNBOUND : new KeyChord(primary, Set.copyOf(modifiers));
    }

    public boolean isUnbound() {
        return primary.isUnknown();
    }

    public boolean hasModifiers() {
        return !modifiers.isEmpty();
    }

    public boolean matches(KeyCode pressedKey, Set<KeyCode> heldKeys) {
        if (isUnbound() || !primary.equals(pressedKey)) {
            return false;
        }
        return heldKeys.containsAll(modifiers);
    }
}
