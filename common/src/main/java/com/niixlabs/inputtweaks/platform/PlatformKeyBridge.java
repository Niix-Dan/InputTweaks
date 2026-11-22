package com.niixlabs.inputtweaks.platform;

import net.minecraft.client.KeyMapping;

public interface PlatformKeyBridge {
    default String resolveDefaultContextId(KeyMapping mapping) {
        return "inputtweaks:universal";
    }

    default String resolveModId(KeyMapping mapping) {
        String name = mapping.getName();
        if (name == null || name.isBlank()) return "minecraft";

        String[] parts = name.split("\\.");
        if (parts.length >= 3 && "key".equals(parts[0])) {
            return parts[1];
        }
        if (parts.length >= 2 && !"key".equals(parts[0])) {
            return parts[0];
        }
        return "minecraft";
    }
}
