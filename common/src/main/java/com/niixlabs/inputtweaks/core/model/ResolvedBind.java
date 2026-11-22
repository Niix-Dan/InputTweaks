package com.niixlabs.inputtweaks.core.model;

import com.niixlabs.inputtweaks.api.action.BindableAction;
import com.niixlabs.inputtweaks.api.trigger.TriggerMode;

public record ResolvedBind(
        BindableAction action,
        KeyChord chord,
        BindOrigin origin,
        int priority,
        ConflictPolicy policy,
        String contextId,
        TriggerMode triggerMode,
        String category,
        boolean hidden,
        boolean hardDisabled,
        boolean locked
) {
    public String id() {
        return action.id();
    }

    public boolean isModified() {
        return !chord.equals(action.defaultChord())
                || priority != 0
                || policy != ConflictPolicy.PASS_THROUGH
                || triggerMode != TriggerMode.PRESS;
    }

    public boolean requiresCustomDispatch() {
        return hardDisabled
                || chord.hasModifiers()
                || priority != 0
                || policy == ConflictPolicy.CONSUME
                || triggerMode != TriggerMode.PRESS
                || !"inputtweaks:universal".equals(contextId);
    }

    public static final class Builder {
        private final BindableAction action;
        private KeyChord chord;
        private BindOrigin origin = BindOrigin.VANILLA;
        private int priority = 0;
        private ConflictPolicy policy = ConflictPolicy.PASS_THROUGH;
        private String contextId;
        private TriggerMode triggerMode = TriggerMode.PRESS;
        private String category;
        private boolean hidden;
        private boolean hardDisabled;
        private boolean locked;

        public Builder(BindableAction action, String defaultContextId) {
            this.action = action;
            this.chord = action.defaultChord();
            this.contextId = defaultContextId;
            this.category = action.defaultCategory();
        }

        public void applyChord(KeyChord chord, BindOrigin origin) {
            if (locked && origin != BindOrigin.MODPACK) return;
            this.chord = chord;
            this.origin = origin;
        }

        public void applyPriority(int priority) {
            if (!locked) this.priority = priority;
        }

        public void applyPolicy(ConflictPolicy policy) {
            if (!locked) this.policy = policy;
        }

        public void applyContextId(String contextId) {
            if (!locked) this.contextId = contextId;
        }

        public void applyTriggerMode(TriggerMode triggerMode) {
            if (!locked) this.triggerMode = triggerMode;
        }

        public void applyCategory(String category) {
            this.category = category;
        }

        public void applyHidden(boolean hidden) {
            this.hidden = hidden;
        }

        public void applyHardDisabled(boolean hardDisabled) {
            this.hardDisabled = hardDisabled;
        }

        public void applyLocked(boolean locked) {
            this.locked = locked;
        }

        public ResolvedBind build() {
            return new ResolvedBind(
                    action, chord, origin, priority, policy,
                    contextId, triggerMode, category,
                    hidden, hardDisabled, locked
            );
        }
    }
}
