package com.niixlabs.inputtweaks.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record ProfileData(String name, Map<String, ProfileEntry> entries) {
    public ProfileData {
        entries = new LinkedHashMap<>(entries == null ? Map.of() : entries);
    }

    public static ProfileData empty(String name) {
        return new ProfileData(name, new LinkedHashMap<>());
    }
}
