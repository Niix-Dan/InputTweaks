package com.niixlabs.inputtweaks.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.niixlabs.inputtweaks.core.model.RadialMenuDefinition;
import com.niixlabs.inputtweaks.core.model.RadialScope;
import com.niixlabs.inputtweaks.storage.PlayerRadialRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonPlayerRadialRepository implements PlayerRadialRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path radialsFile;

    public JsonPlayerRadialRepository(Path radialsFile) {
        this.radialsFile = radialsFile;
    }

    @Override
    public List<RadialMenuDefinition> loadPlayerRadials() {
        if (!Files.exists(radialsFile)) return List.of();
        try {
            RadialsFileDto dto = GSON.fromJson(Files.readString(radialsFile), RadialsFileDto.class);
            if (dto == null || dto.radialMenus == null) return List.of();

            List<RadialMenuDefinition> result = new ArrayList<>();
            for (RadialJsonCodec.RadialDto rd : dto.radialMenus) {
                RadialMenuDefinition def = RadialJsonCodec.fromDto(rd, RadialScope.PLAYER_GLOBAL);
                if (def != null) result.add(def);
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    @Override
    public void savePlayerRadials(List<RadialMenuDefinition> menus) {
        try {
            Files.createDirectories(radialsFile.getParent());
            RadialsFileDto dto = new RadialsFileDto();
            dto.schemaVersion = 1;
            dto.radialMenus = menus.stream().map(RadialJsonCodec::toDto).toList();
            Files.writeString(radialsFile, GSON.toJson(dto));
        } catch (IOException ignored) {
        }
    }

    private static final class RadialsFileDto {
        int schemaVersion;
        List<RadialJsonCodec.RadialDto> radialMenus;
    }
}
