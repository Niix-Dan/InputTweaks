package com.niixlabs.inputtweaks.platform;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public interface PlatformPaths {
    Path configDir();

    Path gameDir();

    default Path modConfigDir() {
        return configDir().resolve("inputtweaks");
    }

    default Path modpackRulesFile() {
        return modConfigDir().resolve("modpack_rules.json");
    }

    default Path localOverridesFile() {
        return modConfigDir().resolve("local_overrides.json");
    }

    default Path usageStatsFile() {
        return modConfigDir().resolve("usage_stats.json");
    }

    default Path activeProfileStateFile() {
        return modConfigDir().resolve("active_profile.txt");
    }

    default Path globalAppDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path base;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = (appData != null && !appData.isBlank())
                    ? Paths.get(appData)
                    : Paths.get(System.getProperty("user.home"), "AppData", "Roaming");
        } else if (os.contains("mac")) {
            base = Paths.get(System.getProperty("user.home"), "Library", "Application Support");
        } else {
            String xdg = System.getenv("XDG_CONFIG_HOME");
            base = (xdg != null && !xdg.isBlank())
                    ? Paths.get(xdg)
                    : Paths.get(System.getProperty("user.home"), ".config");
        }
        return base.resolve("InputTweaks");
    }

    default Path resolvedGlobalDir() {
        Path preferred = globalAppDataDir();
        try {
            java.nio.file.Files.createDirectories(preferred);
            if (java.nio.file.Files.isWritable(preferred)) {
                return preferred;
            }
        } catch (Exception ignored) {
        }
        return modConfigDir().resolve("global_fallback");
    }

    default Path globalProfilesDir() {
        return resolvedGlobalDir().resolve("profiles");
    }

    default Path globalRadialsFile() {
        return resolvedGlobalDir().resolve("radials.json");
    }
}
