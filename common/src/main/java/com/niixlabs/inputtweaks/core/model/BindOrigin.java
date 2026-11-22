package com.niixlabs.inputtweaks.core.model;

public enum BindOrigin {
    VANILLA("inputtweaks.origin.vanilla", 0xFFAAAAAA),
    MODPACK("inputtweaks.origin.modpack", 0xFFE6A23C),
    GLOBAL_PROFILE("inputtweaks.origin.global", 0xFF409EFF),
    LOCAL_OVERRIDE("inputtweaks.origin.local", 0xFF67C23A);

    private final String translationKey;
    private final int badgeColor;

    BindOrigin(String translationKey, int badgeColor) {
        this.translationKey = translationKey;
        this.badgeColor = badgeColor;
    }

    public String translationKey() {
        return translationKey;
    }

    public int badgeColor() {
        return badgeColor;
    }
}
