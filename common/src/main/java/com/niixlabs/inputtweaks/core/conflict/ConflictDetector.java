package com.niixlabs.inputtweaks.core.conflict;

import com.niixlabs.inputtweaks.api.context.InputContext;
import com.niixlabs.inputtweaks.core.context.ContextRegistry;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

import java.util.*;

public final class ConflictDetector {
    private final ContextRegistry contextRegistry;
    private final Map<KeyCode, List<ResolvedBind>> byPrimaryKey = new HashMap<>();

    public ConflictDetector(ContextRegistry contextRegistry) {
        this.contextRegistry = contextRegistry;
    }

    public void rebuildIndex(Collection<ResolvedBind> binds) {
        byPrimaryKey.clear();
        for (ResolvedBind bind : binds) {
            if (bind.hardDisabled() || bind.chord().isUnbound()) continue;
            byPrimaryKey.computeIfAbsent(bind.chord().primary(), k -> new ArrayList<>()).add(bind);
        }
    }

    public List<ResolvedBind> getBindsForKey(KeyCode key) {
        return byPrimaryKey.getOrDefault(key, List.of());
    }

    public List<ResolvedBind> getRealConflicts(ResolvedBind target) {
        if (target.hardDisabled() || target.chord().isUnbound()) {
            return List.of();
        }
        List<ResolvedBind> sameKey = byPrimaryKey.getOrDefault(target.chord().primary(), List.of());
        if (sameKey.size() <= 1) return List.of();

        InputContext targetCtx = contextRegistry.getOrDefault(target.contextId());
        List<ResolvedBind> conflicts = new ArrayList<>();

        for (ResolvedBind other : sameKey) {
            if (other.id().equals(target.id())) continue;
            if (!other.chord().modifiers().equals(target.chord().modifiers())) continue;
            if (other.triggerMode() != target.triggerMode()
                    && ((other.triggerMode().name().equals("TAP") && target.triggerMode().name().equals("HOLD"))
                    || (other.triggerMode().name().equals("HOLD") && target.triggerMode().name().equals("TAP")))) {
                continue;
            }

            InputContext otherCtx = contextRegistry.getOrDefault(other.contextId());
            if (targetCtx.conflictsWith(otherCtx) && otherCtx.conflictsWith(targetCtx)) {
                conflicts.add(other);
            }
        }
        return conflicts;
    }

    public boolean hasRealConflict(ResolvedBind bind) {
        return !getRealConflicts(bind).isEmpty();
    }

    public List<KeyCode> getConflictingKeys() {
        List<KeyCode> result = new ArrayList<>();
        for (KeyCode key : byPrimaryKey.keySet()) {
            if (getKeyStatus(key) == KeyHeatStatus.CONFLICT) {
                result.add(key);
            }
        }
        return result;
    }

    public KeyHeatStatus getKeyStatus(KeyCode key) {
        List<ResolvedBind> binds = byPrimaryKey.get(key);
        if (binds == null || binds.isEmpty()) return KeyHeatStatus.FREE;
        if (binds.size() == 1) return KeyHeatStatus.BOUND;

        for (ResolvedBind bind : binds) {
            if (hasRealConflict(bind)) return KeyHeatStatus.CONFLICT;
        }
        return KeyHeatStatus.MULTI_CONTEXT;
    }
}
