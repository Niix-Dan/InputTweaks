package com.niixlabs.inputtweaks.bridge;

import com.niixlabs.inputtweaks.api.action.BindableAction;
import com.niixlabs.inputtweaks.core.model.KeyChord;
import com.niixlabs.inputtweaks.core.model.RadialMenuDefinition;
import com.niixlabs.inputtweaks.ui.overlay.RadialMenuScreen;
import net.minecraft.client.Minecraft;

public final class RadialActionAdapter implements BindableAction {
    private final RadialMenuDefinition definition;
    private KeyChord currentChord;

    public RadialActionAdapter(RadialMenuDefinition definition) {
        this.definition = definition;
        this.currentChord = definition.defaultChord();
    }

    public RadialMenuDefinition definition() {
        return definition;
    }

    @Override
    public String id() {
        return "inputtweaks.radial." + definition.id();
    }

    @Override
    public String translationKey() {
        return definition.title();
    }

    @Override
    public String defaultCategory() {
        return "key.categories.inputtweaks.radials";
    }

    @Override
    public String modId() {
        return "inputtweaks";
    }

    @Override
    public KeyChord defaultChord() {
        return definition.defaultChord();
    }

    @Override
    public KeyChord currentChord() {
        return currentChord;
    }

    @Override
    public void applyChord(KeyChord chord) {
        this.currentChord = chord;
    }

    @Override
    public void setPressed(boolean down) {
    }

    @Override
    public void triggerClick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new RadialMenuScreen(definition, currentChord, null));
        }
    }
}
