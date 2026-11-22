package com.niixlabs.inputtweaks.ui.filter;

public enum FilterMode {
    ALL("inputtweaks.filter.all"),
    CONFLICTS("inputtweaks.filter.conflicts"),
    UNBOUND("inputtweaks.filter.unbound"),
    MODIFIED("inputtweaks.filter.modified"),
    UNUSED("inputtweaks.filter.unused"),
    HIDDEN("inputtweaks.filter.hidden");

    private final String translationKey;

    FilterMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public FilterMode next() {
        FilterMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
