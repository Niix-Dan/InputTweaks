package com.niixlabs.inputtweaks.core.history;

public record EditCommand(String description, Runnable execute, Runnable undo) {
}
