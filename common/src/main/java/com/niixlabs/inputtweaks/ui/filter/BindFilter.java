package com.niixlabs.inputtweaks.ui.filter;

import com.niixlabs.inputtweaks.bridge.InputKeyMapper;
import com.niixlabs.inputtweaks.core.analytics.UsageTracker;
import com.niixlabs.inputtweaks.core.conflict.ConflictDetector;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;
import net.minecraft.client.resources.language.I18n;

import java.util.Locale;

public final class BindFilter {
    private final ConflictDetector conflictDetector;
    private final UsageTracker usageTracker;

    public BindFilter(ConflictDetector conflictDetector, UsageTracker usageTracker) {
        this.conflictDetector = conflictDetector;
        this.usageTracker = usageTracker;
    }

    public boolean matches(
            ResolvedBind bind,
            String rawQuery,
            FilterMode mode,
            KeyCode keyFilter,
            boolean packDevMode
    ) {
        if (bind.hidden() && !packDevMode && mode != FilterMode.HIDDEN) {
            return false;
        }
        if (keyFilter != null && !bind.chord().primary().equals(keyFilter)) {
            return false;
        }

        boolean modeOk = switch (mode) {
            case ALL -> true;
            case CONFLICTS -> conflictDetector.hasRealConflict(bind);
            case UNBOUND -> bind.chord().isUnbound();
            case MODIFIED -> bind.isModified();
            case UNUSED -> !bind.chord().isUnbound() && usageTracker.isUnused(bind.id());
            case HIDDEN -> bind.hidden() || bind.hardDisabled();
        };
        if (!modeOk) return false;

        if (rawQuery == null || rawQuery.isBlank()) return true;

        String q = rawQuery.trim().toLowerCase(Locale.ROOT);
        if (q.startsWith("@")) {
            String mod = q.substring(1);
            return bind.action().modId().toLowerCase(Locale.ROOT).contains(mod);
        }
        if (q.equals("#conflict") || q.equals("#conflicts")) {
            return conflictDetector.hasRealConflict(bind);
        }
        if (q.equals("#unbound")) {
            return bind.chord().isUnbound();
        }
        if (q.equals("#modified")) {
            return bind.isModified();
        }
        if (q.equals("#unused") || q.equals("#dead")) {
            return !bind.chord().isUnbound() && usageTracker.isUnused(bind.id());
        }
        if (q.equals("#hidden")) {
            return bind.hidden() || bind.hardDisabled();
        }

        String displayName = I18n.get(bind.action().translationKey()).toLowerCase(Locale.ROOT);
        String categoryName = I18n.get(bind.category()).toLowerCase(Locale.ROOT);
        String keyName = InputKeyMapper.formatChord(bind.chord()).getString().toLowerCase(Locale.ROOT);

        return displayName.contains(q)
                || categoryName.contains(q)
                || keyName.contains(q)
                || bind.action().modId().toLowerCase(Locale.ROOT).contains(q)
                || bind.id().toLowerCase(Locale.ROOT).contains(q);
    }
}
