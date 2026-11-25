package com.niixlabs.inputtweaks.core.dispatch;

import com.niixlabs.inputtweaks.api.context.InputContext;
import com.niixlabs.inputtweaks.api.trigger.TriggerMode;
import com.niixlabs.inputtweaks.api.trigger.TriggerStrategy;
import com.niixlabs.inputtweaks.core.analytics.UsageTracker;
import com.niixlabs.inputtweaks.core.conflict.PriorityResolver;
import com.niixlabs.inputtweaks.core.context.ContextRegistry;
import com.niixlabs.inputtweaks.core.model.ConflictPolicy;
import com.niixlabs.inputtweaks.core.model.DebugInputEvent;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;
import com.niixlabs.inputtweaks.core.trigger.TriggerStrategyRegistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InputDispatcher {
    private static final int MAX_DEBUG_LOG = 8;

    private final ContextRegistry contextRegistry;
    private final PriorityResolver priorityResolver;
    private final TriggerStrategyRegistry triggerStrategies;
    private final UsageTracker usageTracker;

    private final Map<KeyCode, List<ResolvedBind>> indexedByKey = new HashMap<>();
    private final Set<KeyCode> heldKeys = new HashSet<>();
    private final Map<String, ResolvedBind> activeManagedBinds = new LinkedHashMap<>();
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
        releaseAllActive();
        indexedByKey.clear();
        for (ResolvedBind bind : binds) {
            if (bind.chord().isUnbound()) continue;
            indexedByKey.computeIfAbsent(bind.chord().primary(), k -> new ArrayList<>()).add(bind);
        }
        indexedByKey.replaceAll((k, list) -> priorityResolver.sort(list));
    }

    public boolean handleInput(KeyCode key, boolean pressed, boolean textInputActive) {
        long now = System.currentTimeMillis();

        if (pressed) {
            heldKeys.add(key);
        } else {
            heldKeys.remove(key);
        }

        if (textInputActive) {
            if (!activeManagedBinds.isEmpty()) {
                releaseAllActive();
            }
            return false;
        }

        if (!pressed) {
            return handleRelease(key, now);
        }

        return handlePress(key, now);
    }

    private boolean handlePress(KeyCode key, long now) {
        List<ResolvedBind> candidates = indexedByKey.get(key);
        if (candidates == null || candidates.isEmpty()) return false;

        List<ResolvedBind> winning = null;
        int maxModifiers = -1;

        for (ResolvedBind bind : candidates) {
            if (!bind.chord().matches(key, heldKeys)) {
                continue;
            }
            InputContext ctx = contextRegistry.getOrDefault(bind.contextId());
            if (!ctx.isActive()) {
                continue;
            }

            int modCount = bind.chord().modifiers().size();
            if (maxModifiers == -1) {
                maxModifiers = modCount;
                winning = new ArrayList<>(2);
                winning.add(bind);
            } else if (modCount == maxModifiers) {
                winning.add(bind);
            } else {
                break;
            }
        }

        if (winning == null || winning.isEmpty()) {
            return false;
        }

        boolean requiresCustom = false;
        for (ResolvedBind bind : winning) {
            if (bind.requiresCustomDispatch()) {
                requiresCustom = true;
                break;
            }
        }

        if (!requiresCustom) {
            for (ResolvedBind bind : winning) {
                usageTracker.recordUse(bind.id());
                recordDebug(now, key, bind, "VANILLA");
            }
            return false;
        }

        boolean handledAny = false;
        for (ResolvedBind bind : winning) {
            if (bind.hardDisabled()) {
                recordDebug(now, key, bind, "BLOCKED");
                handledAny = true;
                continue;
            }

            if (bind.chord().hasModifiers()) {
                cancelPendingModifierTriggers(bind.chord().modifiers());
            }

            TriggerStrategy strategy = triggerStrategies.get(bind.triggerMode());
            boolean fired = strategy.handle(bind, true, now);
            activeManagedBinds.put(bind.id(), bind);
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

    private boolean handleRelease(KeyCode key, long now) {
        if (activeManagedBinds.isEmpty()) {
            return false;
        }

        List<ResolvedBind> toRelease = null;
        for (ResolvedBind active : activeManagedBinds.values()) {
            if (active.chord().primary().equals(key)) {
                if (toRelease == null) toRelease = new ArrayList<>(2);
                toRelease.add(active);
            }
        }

        if (toRelease == null) {
            return false;
        }

        for (ResolvedBind bind : toRelease) {
            activeManagedBinds.remove(bind.id());
            TriggerStrategy strategy = triggerStrategies.get(bind.triggerMode());
            boolean fired = strategy.handle(bind, false, now);
            if (fired) {
                usageTracker.recordUse(bind.id());
                recordDebug(now, key, bind, "FIRED");
            }
        }
        return true;
    }

    private void cancelPendingModifierTriggers(Set<KeyCode> modifiers) {
        if (activeManagedBinds.isEmpty()) return;
        activeManagedBinds.entrySet().removeIf(entry -> {
            ResolvedBind active = entry.getValue();
            if (!active.chord().hasModifiers() && modifiers.contains(active.chord().primary())) {
                if (active.triggerMode() == TriggerMode.TAP || active.triggerMode() == TriggerMode.HOLD) {
                    triggerStrategies.resetAllForBind(active.id());
                    return true;
                }
            }
            return false;
        });
    }

    private void releaseAllActive() {
        if (activeManagedBinds.isEmpty()) return;
        List<ResolvedBind> snapshot = new ArrayList<>(activeManagedBinds.values());
        activeManagedBinds.clear();
        for (ResolvedBind bind : snapshot) {
            if (bind.triggerMode() == TriggerMode.PRESS || bind.triggerMode() == TriggerMode.DOUBLE_TAP) {
                bind.action().setPressed(false);
            } else if (bind.triggerMode() == TriggerMode.TAP || bind.triggerMode() == TriggerMode.HOLD) {
                triggerStrategies.resetAllForBind(bind.id());
            }
        }
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

    public void releaseTrackedKey(KeyCode key) {
        heldKeys.remove(key);
        handleRelease(key, System.currentTimeMillis());
    }

    public void clearHeldKeys() {
        heldKeys.clear();
        releaseAllActive();
    }
}
