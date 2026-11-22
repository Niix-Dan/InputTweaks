package com.niixlabs.inputtweaks.core.dispatch;

import com.niixlabs.inputtweaks.api.context.InputContext;
import com.niixlabs.inputtweaks.api.trigger.TriggerStrategy;
import com.niixlabs.inputtweaks.core.analytics.UsageTracker;
import com.niixlabs.inputtweaks.core.conflict.PriorityResolver;
import com.niixlabs.inputtweaks.core.context.ContextRegistry;
import com.niixlabs.inputtweaks.core.model.ConflictPolicy;
import com.niixlabs.inputtweaks.core.model.DebugInputEvent;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;
import com.niixlabs.inputtweaks.core.trigger.TriggerStrategyRegistry;

import java.util.*;

public final class InputDispatcher {
    private static final int MAX_DEBUG_LOG = 8;

    private final ContextRegistry contextRegistry;
    private final PriorityResolver priorityResolver;
    private final TriggerStrategyRegistry triggerStrategies;
    private final UsageTracker usageTracker;

    private final Map<KeyCode, List<ResolvedBind>> indexedByKey = new HashMap<>();
    private final Set<KeyCode> heldKeys = new HashSet<>();
    private final Deque<DebugInputEvent> recentDebugEvents = new ArrayDeque<>();
    private boolean debugOverlayEnabled = false;

    public InputDispatcher(
            ContextRegistry contextRegistry,
            PriorityResolver priorityResolver,
            TriggerStrategyRegistry triggerStrategies,
            UsageTracker usageTracker
    ) {
        this.contextRegistry = contextRegistry;
        this.priorityResolver = priorityResolver;
        this.triggerStrategies = triggerStrategies;
        this.usageTracker = usageTracker;
    }

    public void rebuildIndex(Collection<ResolvedBind> binds) {
        indexedByKey.clear();
        for (ResolvedBind bind : binds) {
            if (bind.chord().isUnbound()) continue;
            indexedByKey.computeIfAbsent(bind.chord().primary(), k -> new ArrayList<>()).add(bind);
        }
        indexedByKey.replaceAll((k, list) -> priorityResolver.sort(list));
    }

    public boolean handleInput(KeyCode key, boolean pressed, boolean textInputActive) {
        if (pressed) {
            heldKeys.add(key);
        } else {
            heldKeys.remove(key);
        }

        if (textInputActive) return false;

        List<ResolvedBind> candidates = indexedByKey.get(key);
        if (candidates == null || candidates.isEmpty()) return false;

        long now = System.currentTimeMillis();
        boolean hasManagedBehavior = false;

        for (ResolvedBind bind : candidates) {
            if (bind.requiresCustomDispatch()) {
                hasManagedBehavior = true;
                break;
            }
        }

        if (!hasManagedBehavior) {
            if (pressed) {
                for (ResolvedBind bind : candidates) {
                    usageTracker.recordUse(bind.id());
                    recordDebug(now, key, bind, "VANILLA");
                }
            }
            return false;
        }

        boolean handledAny = false;
        for (ResolvedBind bind : candidates) {
            if (bind.hardDisabled()) {
                if (pressed) recordDebug(now, key, bind, "BLOCKED");
                handledAny = true;
                continue;
            }

            if (pressed && !bind.chord().matches(key, heldKeys)) {
                continue;
            }

            InputContext ctx = contextRegistry.getOrDefault(bind.contextId());
            if (!ctx.isActive()) {
                continue;
            }

            TriggerStrategy strategy = triggerStrategies.get(bind.triggerMode());
            boolean fired = strategy.handle(bind, pressed, now);
            handledAny = true;

            if (fired) {
                usageTracker.recordUse(bind.id());
                String tag = bind.policy() == ConflictPolicy.CONSUME ? "CONSUMED" : "FIRED";
                recordDebug(now, key, bind, tag);
            }

            if (bind.policy() == ConflictPolicy.CONSUME) {
                break;
            }
        }
        return handledAny;
    }

    private void recordDebug(long now, KeyCode key, ResolvedBind bind, String resultTag) {
        if (!debugOverlayEnabled) return;
        recentDebugEvents.addFirst(new DebugInputEvent(
                now, key, bind.id(), bind.action().modId(), bind.contextId(), resultTag
        ));
        while (recentDebugEvents.size() > MAX_DEBUG_LOG) {
            recentDebugEvents.removeLast();
        }
    }

    public boolean isDebugOverlayEnabled() {
        return debugOverlayEnabled;
    }

    public void setDebugOverlayEnabled(boolean enabled) {
        this.debugOverlayEnabled = enabled;
        if (!enabled) recentDebugEvents.clear();
    }

    public void toggleDebugOverlay() {
        setDebugOverlayEnabled(!debugOverlayEnabled);
    }

    public List<DebugInputEvent> recentDebugEvents() {
        long cutoff = System.currentTimeMillis() - 4000L;
        while (!recentDebugEvents.isEmpty() && recentDebugEvents.peekLast().timestampMs() < cutoff) {
            recentDebugEvents.removeLast();
        }
        return List.copyOf(recentDebugEvents);
    }

    public void clearHeldKeys() {
        heldKeys.clear();
    }
}
