package com.niixlabs.inputtweaks.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.niixlabs.inputtweaks.core.model.*;
import com.niixlabs.inputtweaks.storage.ModpackRuleRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonModpackRuleRepository implements ModpackRuleRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path rulesFile;

    public JsonModpackRuleRepository(Path rulesFile) {
        this.rulesFile = rulesFile;
    }

    @Override
    public ModpackConfig loadRules() {
        if (!Files.exists(rulesFile)) return ModpackConfig.empty();
        try {
            ConfigDto dto = GSON.fromJson(Files.readString(rulesFile), ConfigDto.class);
            if (dto == null) return ModpackConfig.empty();

            Map<String, ModpackRule> rules = new LinkedHashMap<>();
            if (dto.rules != null) {
                dto.rules.forEach((id, r) -> {
                    KeyChord chord = null;
                    if (r.defaultKey != null) {
                        KeyCode primary = KeyCode.parse(r.defaultKey);
                        List<KeyCode> mods = r.defaultModifiers == null
                                ? List.of()
                                : r.defaultModifiers.stream().map(KeyCode::parse).toList();
                        chord = KeyChord.of(primary, mods);
                    }
                    rules.put(id, new ModpackRule(r.hidden, r.hardDisabled, r.locked, chord, r.categoryOverride));
                });
            }

            List<RadialMenuDefinition> radials = new ArrayList<>();
            if (dto.radialMenus != null) {
                for (RadialJsonCodec.RadialDto rd : dto.radialMenus) {
                    RadialMenuDefinition def = RadialJsonCodec.fromDto(rd, RadialScope.MODPACK_DEFAULT);
                    if (def != null) radials.add(def);
                }
            }
            return new ModpackConfig(rules, radials);
        } catch (Exception ignored) {
            return ModpackConfig.empty();
        }
    }

    @Override
    public void saveRules(ModpackConfig config) {
        try {
            Files.createDirectories(rulesFile.getParent());
            ConfigDto dto = new ConfigDto();
            dto.schemaVersion = 1;
            dto.rules = new LinkedHashMap<>();
            config.rules().forEach((id, rule) -> {
                RuleDto r = new RuleDto();
                r.hidden = rule.hidden();
                r.hardDisabled = rule.hardDisabled();
                r.locked = rule.locked();
                if (rule.defaultChord() != null) {
                    r.defaultKey = rule.defaultChord().primary().serialize();
                    if (rule.defaultChord().hasModifiers()) {
                        r.defaultModifiers = rule.defaultChord().modifiers().stream().map(KeyCode::serialize).toList();
                    }
                }
                r.categoryOverride = rule.categoryOverride();
                dto.rules.put(id, r);
            });

            dto.radialMenus = config.radialMenus().stream()
                    .map(RadialJsonCodec::toDto)
                    .toList();

            Files.writeString(rulesFile, GSON.toJson(dto));
        } catch (IOException ignored) {
        }
    }

    private static final class ConfigDto {
        int schemaVersion;
        Map<String, RuleDto> rules;
        List<RadialJsonCodec.RadialDto> radialMenus;
    }

    private static final class RuleDto {
        boolean hidden;
        boolean hardDisabled;
        boolean locked;
        String defaultKey;
        List<String> defaultModifiers;
        String categoryOverride;
    }
}
