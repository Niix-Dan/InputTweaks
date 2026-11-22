package com.niixlabs.inputtweaks.core.model;

public record ModpackRule(
        boolean hidden,
        boolean hardDisabled,
        boolean locked,
        KeyChord defaultChord,
        String categoryOverride
) {
    public static ModpackRule none() {
        return new ModpackRule(false, false, false, null, null);
    }

    public boolean isEmpty() {
        return !hidden && !hardDisabled && !locked && defaultChord == null
                && (categoryOverride == null || categoryOverride.isBlank());
    }

    public ModpackRule withHidden(boolean value) {
        return new ModpackRule(value, hardDisabled, locked, defaultChord, categoryOverride);
    }

    public ModpackRule withHardDisabled(boolean value) {
        return new ModpackRule(hidden, value, locked, defaultChord, categoryOverride);
    }

    public ModpackRule withLocked(boolean value) {
        return new ModpackRule(hidden, hardDisabled, value, defaultChord, categoryOverride);
    }

    public ModpackRule withDefaultChord(KeyChord value) {
        return new ModpackRule(hidden, hardDisabled, locked, value, categoryOverride);
    }

    public ModpackRule withCategoryOverride(String value) {
        return new ModpackRule(hidden, hardDisabled, locked, defaultChord, value);
    }
}
