package com.niixlabs.inputtweaks.core.model;

import java.util.List;

public record RadialMenuDefinition(
        String id,
        String title,
        KeyChord defaultChord,
        RadialActivationMode activationMode,
        RadialScope scope,
        List<RadialSlot> slots
) {
    public RadialMenuDefinition {
        id = (id == null || id.isBlank()) ? "wheel" : id;
        title = (title == null || title.isBlank()) ? "Radial Menu" : title;
        defaultChord = defaultChord == null ? KeyChord.UNBOUND : defaultChord;
        activationMode = activationMode == null ? RadialActivationMode.HOLD_RELEASE : activationMode;
        scope = scope == null ? RadialScope.PLAYER_GLOBAL : scope;
        slots = slots == null ? List.of() : List.copyOf(slots);
    }

    public RadialMenuDefinition(String id, String title, KeyChord defaultChord, List<RadialSlot> slots) {
        this(id, title, defaultChord, RadialActivationMode.HOLD_RELEASE, RadialScope.MODPACK_DEFAULT, slots);
    }

    public RadialMenuDefinition withTitle(String value) {
        return new RadialMenuDefinition(id, value, defaultChord, activationMode, scope, slots);
    }

    public RadialMenuDefinition withDefaultChord(KeyChord value) {
        return new RadialMenuDefinition(id, title, value, activationMode, scope, slots);
    }

    public RadialMenuDefinition withActivationMode(RadialActivationMode value) {
        return new RadialMenuDefinition(id, title, defaultChord, value, scope, slots);
    }

    public RadialMenuDefinition withScope(RadialScope value) {
        return new RadialMenuDefinition(id, title, defaultChord, activationMode, value, slots);
    }

    public RadialMenuDefinition withSlots(List<RadialSlot> value) {
        return new RadialMenuDefinition(id, title, defaultChord, activationMode, scope, value);
    }
}
