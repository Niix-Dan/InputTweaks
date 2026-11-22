package com.niixlabs.inputtweaks.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.niixlabs.inputtweaks.api.trigger.TriggerMode;
import com.niixlabs.inputtweaks.core.model.*;
import com.niixlabs.inputtweaks.storage.ProfileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class JsonProfileRepository implements ProfileRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path globalDir;
    private final Path localFile;
    private final Path activeStateFile;

    public JsonProfileRepository(Path globalDir, Path localFile, Path activeStateFile) {
        this.globalDir = globalDir;
        this.localFile = localFile;
        this.activeStateFile = activeStateFile;
    }

    private static EntryDto toDto(ProfileEntry e) {
        EntryDto dto = new EntryDto();
        if (e.chord() != null) {
            dto.key = e.chord().primary().serialize();
            if (e.chord().hasModifiers()) {
                dto.modifiers = e.chord().modifiers().stream().map(KeyCode::serialize).toList();
            }
        }
        dto.priority = e.priority();
        dto.policy = e.policy() != null ? e.policy().name() : null;
        dto.context = e.contextId();
        dto.trigger = e.triggerMode() != null ? e.triggerMode().name() : null;
        return dto;
    }

    private static ProfileEntry fromDto(EntryDto dto) {
        KeyChord chord = null;
        if (dto.key != null) {
            KeyCode primary = KeyCode.parse(dto.key);
            List<KeyCode> mods = dto.modifiers == null
                    ? List.of()
                    : dto.modifiers.stream().map(KeyCode::parse).toList();
            chord = KeyChord.of(primary, mods);
        }
        ConflictPolicy policy = parseEnum(ConflictPolicy.class, dto.policy);
        TriggerMode trigger = parseEnum(TriggerMode.class, dto.trigger);
        return new ProfileEntry(chord, dto.priority, policy, dto.context, trigger);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public List<ProfileData> loadGlobalProfiles() {
        List<ProfileData> result = new ArrayList<>();
        if (!Files.isDirectory(globalDir)) return result;

        try (Stream<Path> files = Files.list(globalDir)) {
            files.filter(p -> p.toString().endsWith(".json")).sorted().forEach(path -> {
                ProfileData loaded = readProfileFile(path);
                if (loaded != null) result.add(loaded);
            });
        } catch (IOException ignored) {
        }
        return result;
    }

    @Override
    public void saveGlobalProfile(ProfileData profile) {
        writeProfileFile(globalDir.resolve(sanitizeFilename(profile.name()) + ".json"), profile);
    }

    @Override
    public void deleteGlobalProfile(String profileName) {
        try {
            Files.deleteIfExists(globalDir.resolve(sanitizeFilename(profileName) + ".json"));
        } catch (IOException ignored) {
        }
    }

    @Override
    public ProfileData loadLocalOverrides() {
        ProfileData loaded = readProfileFile(localFile);
        return loaded != null ? loaded : ProfileData.empty("local");
    }

    @Override
    public void saveLocalOverrides(ProfileData overrides) {
        writeProfileFile(localFile, overrides);
    }

    @Override
    public String loadActiveProfileName() {
        if (!Files.exists(activeStateFile)) return null;
        try {
            return Files.readString(activeStateFile).trim();
        } catch (IOException ignored) {
            return null;
        }
    }

    @Override
    public void saveActiveProfileName(String profileName) {
        try {
            Files.createDirectories(activeStateFile.getParent());
            Files.writeString(activeStateFile, profileName);
        } catch (IOException ignored) {
        }
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private ProfileData readProfileFile(Path path) {
        if (!Files.exists(path)) return null;
        try {
            ProfileDto dto = GSON.fromJson(Files.readString(path), ProfileDto.class);
            if (dto == null || dto.name == null) return null;

            Map<String, ProfileEntry> entries = new LinkedHashMap<>();
            if (dto.entries != null) {
                dto.entries.forEach((id, e) -> entries.put(id, fromDto(e)));
            }
            return new ProfileData(dto.name, entries);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeProfileFile(Path path, ProfileData profile) {
        try {
            Files.createDirectories(path.getParent());
            ProfileDto dto = new ProfileDto();
            dto.schemaVersion = 1;
            dto.name = profile.name();
            dto.entries = new LinkedHashMap<>();
            profile.entries().forEach((id, e) -> {
                if (!e.isEmpty()) dto.entries.put(id, toDto(e));
            });
            Files.writeString(path, GSON.toJson(dto));
        } catch (IOException ignored) {
        }
    }

    private static final class ProfileDto {
        int schemaVersion;
        String name;
        Map<String, EntryDto> entries;
    }

    private static final class EntryDto {
        String key;
        List<String> modifiers;
        Integer priority;
        String policy;
        String context;
        String trigger;
    }
}
