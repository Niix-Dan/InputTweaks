package com.niixlabs.inputtweaks.api.context;

public interface InputContext {
    String id();
    String translationKey();
    String shortLabel();

    boolean isActive();
    boolean conflictsWith(InputContext other);
}
