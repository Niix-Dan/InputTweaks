package com.niixlabs.inputtweaks.core.model;

public record RadialSlot(
        String label,
        String targetBindId,
        String iconItemId,
        int accentColor
) {
    public static final String DEFAULT_ICON = "minecraft:compass";
    public static final int DEFAULT_COLOR = 0xFF409EFF;

    public RadialSlot {
        label = (label == null || label.isBlank()) ? "Action" : label;
        targetBindId = targetBindId == null ? "" : targetBindId;
        iconItemId = (iconItemId == null || iconItemId.isBlank()) ? DEFAULT_ICON : iconItemId;
        accentColor = accentColor == 0 ? DEFAULT_COLOR : (0xFF000000 | accentColor);
    }

    public RadialSlot(String label, String targetBindId) {
        this(label, targetBindId, DEFAULT_ICON, DEFAULT_COLOR);
    }

    public RadialSlot withLabel(String value) {
        return new RadialSlot(value, targetBindId, iconItemId, accentColor);
    }

    public RadialSlot withTargetBindId(String value) {
        return new RadialSlot(label, value, iconItemId, accentColor);
    }

    public RadialSlot withIconItemId(String value) {
        return new RadialSlot(label, targetBindId, value, accentColor);
    }

    public RadialSlot withAccentColor(int value) {
        return new RadialSlot(label, targetBindId, iconItemId, value);
    }
}
