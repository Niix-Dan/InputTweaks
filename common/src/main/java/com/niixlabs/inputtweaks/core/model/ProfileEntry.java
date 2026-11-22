package com.niixlabs.inputtweaks.core.model;

import com.niixlabs.inputtweaks.api.trigger.TriggerMode;

public record ProfileEntry(
        KeyChord chord,
        Integer priority,
        ConflictPolicy policy,
        String contextId,
        TriggerMode triggerMode
) {
    public static ProfileEntry empty() {
        return new ProfileEntry(null, null, null, null, null);
    }

    public boolean isEmpty() {
        return chord == null && priority == null && policy == null && contextId == null && triggerMode == null;
    }

    public ProfileEntry withChord(KeyChord value) {
        return new ProfileEntry(value, priority, policy, contextId, triggerMode);
    }

    public ProfileEntry withPriority(int value) {
        return new ProfileEntry(chord, value, policy, contextId, triggerMode);
    }

    public ProfileEntry withPolicy(ConflictPolicy value) {
        return new ProfileEntry(chord, priority, value, contextId, triggerMode);
    }

    public ProfileEntry withContextId(String value) {
        return new ProfileEntry(chord, priority, policy, value, triggerMode);
    }

    public ProfileEntry withTriggerMode(TriggerMode value) {
        return new ProfileEntry(chord, priority, policy, contextId, value);
    }
}
