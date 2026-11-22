package com.niixlabs.inputtweaks.core.context;

import com.niixlabs.inputtweaks.api.context.InputContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContextRegistry {
    private final Map<String, InputContext> contexts = new LinkedHashMap<>();

    public void register(InputContext context) {
        contexts.put(context.id(), context);
    }

    public InputContext getOrDefault(String id) {
        InputContext ctx = contexts.get(id);
        if (ctx != null) return ctx;
        return contexts.values().iterator().next();
    }

    public List<InputContext> all() {
        return List.copyOf(contexts.values());
    }

    public String nextId(String currentId) {
        List<String> ids = new ArrayList<>(contexts.keySet());
        if (ids.isEmpty()) return currentId;
        int idx = ids.indexOf(currentId);
        return ids.get((idx + 1) % ids.size());
    }

    public String previousId(String currentId) {
        List<String> ids = new ArrayList<>(contexts.keySet());
        if (ids.isEmpty()) return currentId;
        int idx = ids.indexOf(currentId);
        if (idx < 0) idx = 0;
        return ids.get((idx - 1 + ids.size()) % ids.size());
    }
}
