package com.niixlabs.inputtweaks.storage;

import java.util.Map;

public interface UsageStatsRepository {
    Map<String, Long> loadUsageCounts();

    void saveUsageCounts(Map<String, Long> counts);
}
