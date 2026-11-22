package com.niixlabs.inputtweaks.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.niixlabs.inputtweaks.storage.UsageStatsRepository;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class JsonUsageStatsRepository implements UsageStatsRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {
    }.getType();

    private final Path statsFile;

    public JsonUsageStatsRepository(Path statsFile) {
        this.statsFile = statsFile;
    }

    @Override
    public Map<String, Long> loadUsageCounts() {
        if (!Files.exists(statsFile)) return Map.of();
        try {
            Map<String, Long> data = GSON.fromJson(Files.readString(statsFile), MAP_TYPE);
            return data != null ? data : Map.of();
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    @Override
    public void saveUsageCounts(Map<String, Long> counts) {
        try {
            Files.createDirectories(statsFile.getParent());
            Files.writeString(statsFile, GSON.toJson(new HashMap<>(counts), MAP_TYPE));
        } catch (IOException ignored) {
        }
    }
}
