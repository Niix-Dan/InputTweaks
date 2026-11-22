package com.niixlabs.inputtweaks.core.model;

public enum ConflictPolicy {
    PASS_THROUGH("inputtweaks.policy.pass_through", "PASS"),
    CONSUME("inputtweaks.policy.consume", "STOP");

    private final String translationKey;
    private final String shortLabel;

    ConflictPolicy(String translationKey, String shortLabel) {
        this.translationKey = translationKey;
        this.shortLabel = shortLabel;
    }

    public String translationKey() {
        return translationKey;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public ConflictPolicy toggle() {
        return this == CONSUME ? PASS_THROUGH : CONSUME;
    }
}
