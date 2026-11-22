package com.niixlabs.inputtweaks.api.trigger;

public enum TriggerMode {
    PRESS("inputtweaks.trigger.press", "STD"),
    TAP("inputtweaks.trigger.tap", "TAP"),
    HOLD("inputtweaks.trigger.hold", "HOLD"),
    DOUBLE_TAP("inputtweaks.trigger.double_tap", "2X"),
    TOGGLE("inputtweaks.trigger.toggle", "TOG");

    private final String translationKey;
    private final String shortLabel;

    TriggerMode(String translationKey, String shortLabel) {
        this.translationKey = translationKey;
        this.shortLabel = shortLabel;
    }

    public String translationKey() {
        return translationKey;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public TriggerMode next() {
        TriggerMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public TriggerMode previous() {
        TriggerMode[] all = values();
        return all[(ordinal() - 1 + all.length) % all.length];
    }
}
