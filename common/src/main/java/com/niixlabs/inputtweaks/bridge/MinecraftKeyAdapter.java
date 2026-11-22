package com.niixlabs.inputtweaks.bridge;

import com.niixlabs.inputtweaks.api.action.BindableAction;
import com.niixlabs.inputtweaks.core.model.KeyChord;
import com.niixlabs.inputtweaks.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;

public final class MinecraftKeyAdapter implements BindableAction {
    private final KeyMapping mapping;
    private final String modId;

    public MinecraftKeyAdapter(KeyMapping mapping, String modId) {
        this.mapping = mapping;
        this.modId = modId;
    }

    public KeyMapping raw() {
        return mapping;
    }

    @Override
    public String id() {
        return mapping.getName();
    }

    @Override
    public String translationKey() {
        return mapping.getName();
    }

    @Override
    public String defaultCategory() {
        return mapping.getCategory();
    }

    @Override
    public String modId() {
        return modId;
    }

    @Override
    public KeyChord defaultChord() {
        return KeyChord.of(InputKeyMapper.toKeyCode(mapping.getDefaultKey()));
    }

    @Override
    public KeyChord currentChord() {
        KeyMappingAccessor acc = (KeyMappingAccessor) mapping;
        return KeyChord.of(InputKeyMapper.toKeyCode(acc.inputtweaks$getKey()));
    }

    @Override
    public void applyChord(KeyChord chord) {
        mapping.setKey(InputKeyMapper.toMinecraftKey(chord.primary()));
    }

    @Override
    public void setPressed(boolean down) {
        mapping.setDown(down);
    }

    @Override
    public void triggerClick() {
        KeyMappingAccessor acc = (KeyMappingAccessor) mapping;
        acc.inputtweaks$setClickCount(acc.inputtweaks$getClickCount() + 1);
    }
}
