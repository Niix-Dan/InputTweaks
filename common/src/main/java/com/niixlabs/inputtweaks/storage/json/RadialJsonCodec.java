package com.niixlabs.inputtweaks.storage.json;

import com.niixlabs.inputtweaks.core.model.*;

import java.util.ArrayList;
import java.util.List;

final class RadialJsonCodec {
    private RadialJsonCodec() {
    }

    static RadialDto toDto(RadialMenuDefinition def) {
        RadialDto dto = new RadialDto();
        dto.id = def.id();
        dto.title = def.title();
        dto.defaultKey = def.defaultChord().primary().serialize();
        if (def.defaultChord().hasModifiers()) {
            dto.defaultModifiers = def.defaultChord().modifiers().stream().map(KeyCode::serialize).toList();
        }
        dto.activationMode = def.activationMode().name();
        dto.slots = def.slots().stream().map(s -> {
            RadialSlotDto sd = new RadialSlotDto();
            sd.label = s.label();
            sd.targetBindId = s.targetBindId();
            sd.icon = s.iconItemId();
            sd.color = String.format("#%06X", s.accentColor() & 0xFFFFFF);
            return sd;
        }).toList();
        return dto;
    }

    static RadialMenuDefinition fromDto(RadialDto dto, RadialScope scope) {
        if (dto == null || dto.id == null || dto.title == null) return null;

        KeyChord chord = KeyChord.UNBOUND;
        if (dto.defaultKey != null) {
            KeyCode primary = KeyCode.parse(dto.defaultKey);
            List<KeyCode> mods = dto.defaultModifiers == null
                    ? List.of()
                    : dto.defaultModifiers.stream().map(KeyCode::parse).toList();
            chord = KeyChord.of(primary, mods);
        }

        RadialActivationMode mode = RadialActivationMode.HOLD_RELEASE;
        if (dto.activationMode != null && !dto.activationMode.isBlank()) {
            try {
                mode = RadialActivationMode.valueOf(dto.activationMode);
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<RadialSlot> slots = new ArrayList<>();
        if (dto.slots != null) {
            for (RadialSlotDto s : dto.slots) {
                if (s == null || s.label == null) continue;
                int color = parseColor(s.color, RadialSlot.DEFAULT_COLOR);
                slots.add(new RadialSlot(s.label, s.targetBindId, s.icon, color));
            }
        }
        return new RadialMenuDefinition(dto.id, dto.title, chord, mode, scope, slots);
    }

    private static int parseColor(String hex, int fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            return 0xFF000000 | Integer.parseInt(clean, 16);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static final class RadialDto {
        String id;
        String title;
        String defaultKey;
        List<String> defaultModifiers;
        String activationMode;
        List<RadialSlotDto> slots;
    }

    static final class RadialSlotDto {
        String label;
        String targetBindId;
        String icon;
        String color;
    }
}
