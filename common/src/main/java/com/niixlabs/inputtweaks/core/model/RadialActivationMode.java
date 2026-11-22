package com.niixlabs.inputtweaks.core.model;

public enum RadialActivationMode {
    HOLD_RELEASE("inputtweaks.radial.mode.hold", "Hold & Release"),
    CLICK("inputtweaks.radial.mode.click", "Toggle & Click");

    private final String translationKey;
    private final String fallbackLabel;

    RadialActivationMode(String translationKey, String fallbackLabel) {
        this.translationKey = translationKey;
        this.fallbackLabel = fallbackLabel;
    }

    public String translationKey() {
        return translationKey;
    }

    public String fallbackLabel() {
        return fallbackLabel;
    }

    public RadialActivationMode toggle() {
        return this == HOLD_RELEASE ? CLICK : HOLD_RELEASE;
    }
}
