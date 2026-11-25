package com.niixlabs.inputtweaks.api.action;

import com.niixlabs.inputtweaks.core.model.KeyChord;

public interface BindableAction {
    String id();
    String translationKey();
    String defaultCategory();
    String modId();

    KeyChord defaultChord();
    KeyChord currentChord();

    void applyChord(KeyChord chord);
    void setPressed(boolean down);
    void triggerClick();

    default boolean isVirtual() {
        return false;
    }
}
