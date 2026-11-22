package com.niixlabs.inputtweaks.core.analytics;

import com.niixlabs.inputtweaks.storage.UsageStatsRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class UsageTracker {
    private final UsageStatsRepository repository;
    private final Map<String, Long> counts = new HashMap<>();
    private boolean dirty = false;

    public UsageTracker(UsageStatsRepository repository) {
        this.repository = repository;
    }

    public void load() {
        counts.clear();
        counts.putAll(repository.loadUsageCounts());
        dirty = false;
    }

    public void recordUse(String bindId) {
        counts.merge(bindId, 1L, Long::sum);
        dirty = true;
    }

    public long getUsageCount(String bindId) {
        return counts.getOrDefault(bindId, 0L);
    }

    public boolean isUnused(String bindId) {
        return getUsageCount(bindId) == 0L;
    }

    public void flushIfDirty() {
        if (!dirty) return;
        repository.saveUsageCounts(Collections.unmodifiableMap(counts));
        dirty = false;
    }
}
