package com.niixlabs.inputtweaks.core.conflict;

public enum KeyHeatStatus {
    FREE(0xFF1F4E2E),
    BOUND(0xFF234D7B),
    MULTI_CONTEXT(0xFF8C6D1F),
    CONFLICT(0xFF912020);

    private final int fillColor;

    KeyHeatStatus(int fillColor) {
        this.fillColor = fillColor;
    }

    public int fillColor() {
        return fillColor;
    }
}
