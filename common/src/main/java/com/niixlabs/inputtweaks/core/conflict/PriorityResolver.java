package com.niixlabs.inputtweaks.core.conflict;

import com.niixlabs.inputtweaks.core.model.ResolvedBind;

import java.util.Comparator;
import java.util.List;

public final class PriorityResolver {
    private static final Comparator<ResolvedBind> ORDER = Comparator
            .comparingInt((ResolvedBind b) -> b.chord().modifiers().size()).reversed()
            .thenComparing(Comparator.comparingInt(ResolvedBind::priority).reversed())
            .thenComparing(ResolvedBind::id);

    public List<ResolvedBind> sort(List<ResolvedBind> candidates) {
        return candidates.stream().sorted(ORDER).toList();
    }
}
