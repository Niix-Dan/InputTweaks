package com.niixlabs.inputtweaks.core.model;

public record DebugInputEvent(
        long timestampMs,
        KeyCode key,
        String bindId,
        String modId,
        String contextId,
        String resultTag
) {
}
