package com.niixlabs.inputtweaks.core.layer;

import com.niixlabs.inputtweaks.core.model.BindOrigin;
import com.niixlabs.inputtweaks.core.model.ProfileData;
import com.niixlabs.inputtweaks.core.model.ProfileEntry;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

public final class ProfileConfigLayer implements ConfigLayer {
    private final BindOrigin origin;
    private final int order;
    private final ProfileData profile;

    public ProfileConfigLayer(BindOrigin origin, int order, ProfileData profile) {
        this.origin = origin;
        this.order = order;
        this.profile = profile;
    }

    @Override
    public BindOrigin origin() {
        return origin;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public void apply(String bindId, ResolvedBind.Builder builder) {
        if (profile == null) return;
        ProfileEntry entry = profile.entries().get(bindId);
        if (entry == null) return;

        if (entry.chord() != null) builder.applyChord(entry.chord(), origin);
        if (entry.priority() != null) builder.applyPriority(entry.priority());
        if (entry.policy() != null) builder.applyPolicy(entry.policy());
        if (entry.contextId() != null) builder.applyContextId(entry.contextId());
        if (entry.triggerMode() != null) builder.applyTriggerMode(entry.triggerMode());
    }
}
