package com.niixlabs.inputtweaks.core.model;

public enum RadialScope {
    PLAYER_GLOBAL("inputtweaks.radial.scope.player"),
    MODPACK_DEFAULT("inputtweaks.radial.scope.modpack");

    private final String translationKey;

    RadialScope(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public RadialScope toggle() {
        return this == PLAYER_GLOBAL ? MODPACK_DEFAULT : PLAYER_GLOBAL;
    }
}
