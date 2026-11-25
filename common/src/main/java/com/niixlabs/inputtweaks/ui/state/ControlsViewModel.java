package com.niixlabs.inputtweaks.ui.state;

import com.niixlabs.inputtweaks.core.analytics.UsageTracker;
import com.niixlabs.inputtweaks.core.conflict.ConflictDetector;
import com.niixlabs.inputtweaks.core.context.ContextRegistry;
import com.niixlabs.inputtweaks.core.dispatch.InputDispatcher;
import com.niixlabs.inputtweaks.core.history.EditCommand;
import com.niixlabs.inputtweaks.core.history.EditHistory;
import com.niixlabs.inputtweaks.core.model.KeyChord;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import com.niixlabs.inputtweaks.core.model.ProfileEntry;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;
import com.niixlabs.inputtweaks.core.profile.ProfileManager;
import com.niixlabs.inputtweaks.core.share.ShareCodeCodec;
import com.niixlabs.inputtweaks.ui.filter.BindFilter;
import com.niixlabs.inputtweaks.ui.capture.ChordCaptureSession;
import com.niixlabs.inputtweaks.ui.filter.FilterMode;

import java.util.List;
import java.util.Map;

public final class ControlsViewModel {
    private final ProfileManager profileManager;
    private final ConflictDetector conflictDetector;
    private final ContextRegistry contextRegistry;
    private final InputDispatcher dispatcher;
    private final UsageTracker usageTracker;
    private final EditHistory history;
    private final BindFilter filter;
    private final ChordCaptureSession captureSession = new ChordCaptureSession();

    private String searchQuery = "";
    private FilterMode filterMode = FilterMode.ALL;
    private KeyCode selectedKeyFilter = null;
    private boolean heatmapVisible = false;
    private boolean packDevMode = false;
    private boolean quickUnbindArmed = false;
    private String capturingBindId = null;
    private int conflictWizardIndex = -1;
    private double listScrollAmount = 0.0;

    private String statusBanner = null;
    private long statusBannerExpiresMs = 0L;

    public ControlsViewModel(
        ProfileManager profileManager,
        ConflictDetector conflictDetector,
        ContextRegistry contextRegistry,
        InputDispatcher dispatcher,
        UsageTracker usageTracker,
        EditHistory history
    ) {
        this.profileManager = profileManager;
        this.conflictDetector = conflictDetector;
        this.contextRegistry = contextRegistry;
        this.dispatcher = dispatcher;
        this.usageTracker = usageTracker;
        this.history = history;
        this.filter = new BindFilter(conflictDetector, usageTracker);
    }

    public List<ResolvedBind> filteredBinds() {
        return profileManager.allResolved().stream()
            .filter(b -> filter.matches(b, searchQuery, filterMode, selectedKeyFilter, packDevMode))
            .toList();
    }

    public int totalBindCount() {
        int count = 0;
        for (ResolvedBind b : profileManager.allResolved()) {
            if (!b.hidden() || packDevMode) count++;
        }
        return count;
    }

    public int conflictingKeyCount() {
        return conflictDetector.getConflictingKeys().size();
    }

    public void assignChord(String bindId, KeyChord newChord) {
        ResolvedBind current = profileManager.getResolved(bindId);
        if (current == null || current.locked()) return;

        ProfileEntry before = profileManager.snapshotEntry(bindId);
        history.perform(new EditCommand(
            "Bind " + bindId,
            () -> profileManager.updateEntry(bindId, e -> e.withChord(newChord)),
            () -> profileManager.restoreEntry(bindId, before)
        ));
    }

    public void resetSingleBind(String bindId) {
        ResolvedBind current = profileManager.getResolved(bindId);
        if (current == null || current.locked()) return;

        ProfileEntry before = profileManager.snapshotEntry(bindId);
        history.perform(new EditCommand(
            "Reset " + bindId,
            () -> profileManager.updateEntry(bindId, e -> ProfileEntry.empty()),
            () -> profileManager.restoreEntry(bindId, before)
        ));
    }

    public void adjustPriority(String bindId, int delta) {
        ResolvedBind current = profileManager.getResolved(bindId);
        if (current == null || current.locked()) return;

        int next = Math.max(-9, Math.min(99, current.priority() + delta));
        if (next == current.priority()) return;

        ProfileEntry before = profileManager.snapshotEntry(bindId);
        history.perform(new EditCommand(
            "Priority " + bindId,
            () -> profileManager.updateEntry(bindId, e -> e.withPriority(next)),
            () -> profileManager.restoreEntry(bindId, before)
        ));
    }

    public void toggleConflictPolicy(String bindId) {
        ResolvedBind current = profileManager.getResolved(bindId);
        if (current == null || current.locked()) return;

        ProfileEntry before = profileManager.snapshotEntry(bindId);
        history.perform(new EditCommand(
            "Policy " + bindId,
            () -> profileManager.updateEntry(bindId, e -> e.withPolicy(current.policy().toggle())),
            () -> profileManager.restoreEntry(bindId, before)
        ));
    }

    public void cycleContext(String bindId, boolean backwards) {
        ResolvedBind current = profileManager.getResolved(bindId);
        if (current == null || current.locked()) return;

        String nextCtx = backwards
            ? contextRegistry.previousId(current.contextId())
            : contextRegistry.nextId(current.contextId());
        ProfileEntry before = profileManager.snapshotEntry(bindId);
        history.perform(new EditCommand(
            "Context " + bindId,
            () -> profileManager.updateEntry(bindId, e -> e.withContextId(nextCtx)),
            () -> profileManager.restoreEntry(bindId, before)
        ));
    }

    public void cycleTriggerMode(String bindId, boolean backwards) {
        ResolvedBind current = profileManager.getResolved(bindId);
        if (current == null || current.locked()) return;

        var nextMode = backwards ? current.triggerMode().previous() : current.triggerMode().next();
        ProfileEntry before = profileManager.snapshotEntry(bindId);
        history.perform(new EditCommand(
            "Trigger " + bindId,
            () -> profileManager.updateEntry(bindId, e -> e.withTriggerMode(nextMode)),
            () -> profileManager.restoreEntry(bindId, before)
        ));
    }

    public int quickUnbindKey(KeyCode key) {
        Map<String, ProfileEntry> before = profileManager.quickUnbindKey(key);
        if (before.isEmpty()) return 0;

        history.perform(new EditCommand(
            "Quick Unbind " + key.serialize(),
            () -> profileManager.quickUnbindKey(key),
            () -> profileManager.restoreBatch(before)
        ));
        return before.size();
    }

    public void resetAll() {
        Map<String, ProfileEntry> backup = profileManager.resetActiveProfile();
        if (backup.isEmpty()) return;

        history.perform(new EditCommand(
            "Reset All",
            profileManager::resetActiveProfile,
            () -> profileManager.restoreBatch(backup)
        ));
    }

    public boolean stepConflictWizard() {
        List<KeyCode> conflicting = conflictDetector.getConflictingKeys();
        if (conflicting.isEmpty()) {
            selectedKeyFilter = null;
            conflictWizardIndex = -1;
            return false;
        }
        conflictWizardIndex = (conflictWizardIndex + 1) % conflicting.size();
        selectedKeyFilter = conflicting.get(conflictWizardIndex);
        filterMode = FilterMode.CONFLICTS;
        listScrollAmount = 0.0;
        return true;
    }

    public void clearAllFilters() {
        this.searchQuery = "";
        this.filterMode = FilterMode.ALL;
        this.selectedKeyFilter = null;
        this.conflictWizardIndex = -1;
        this.listScrollAmount = 0.0;
    }

    public void togglePackHidden(String bindId) {
        profileManager.updateModpackRule(bindId, r -> r.withHidden(!r.hidden()));
    }

    public void togglePackDisabled(String bindId) {
        profileManager.updateModpackRule(bindId, r -> r.withHardDisabled(!r.hardDisabled()));
    }

    public void togglePackLocked(String bindId) {
        profileManager.updateModpackRule(bindId, r -> r.withLocked(!r.locked()));
    }

    public void exportPackRules() {
        profileManager.exportModpackRules();
    }

    public String exportActiveShareCode() {
        return ShareCodeCodec.encode(profileManager.activeProfile());
    }

    public boolean importShareCode(String code) {
        return ShareCodeCodec.decode(code).map(data -> {
            profileManager.importProfile(data);
            return true;
        }).orElse(false);
    }

    public void showBanner(String message, long durationMs) {
        this.statusBanner = message;
        this.statusBannerExpiresMs = System.currentTimeMillis() + durationMs;
    }

    public String activeBanner() {
        if (statusBanner != null && System.currentTimeMillis() > statusBannerExpiresMs) {
            statusBanner = null;
        }
        return statusBanner;
    }

    public float bannerAlpha() {
        if (statusBanner == null) return 0.0f;
        long remaining = statusBannerExpiresMs - System.currentTimeMillis();
        if (remaining <= 0) return 0.0f;
        if (remaining < 400L) return Math.max(0.0f, remaining / 400.0f);
        return 1.0f;
    }

    public ProfileManager profileManager() { return profileManager; }
    public ConflictDetector conflictDetector() { return conflictDetector; }
    public ContextRegistry contextRegistry() { return contextRegistry; }
    public InputDispatcher dispatcher() { return dispatcher; }
    public UsageTracker usageTracker() { return usageTracker; }
    public EditHistory history() { return history; }

    public String searchQuery() { return searchQuery; }
    public void setSearchQuery(String query) { this.searchQuery = query; }

    public FilterMode filterMode() { return filterMode; }
    public void cycleFilterMode() {
        this.filterMode = filterMode.next();
        if (filterMode != FilterMode.CONFLICTS) {
            conflictWizardIndex = -1;
        }
    }

    public KeyCode selectedKeyFilter() { return selectedKeyFilter; }
    public void clearKeyFilter() { this.selectedKeyFilter = null; }
    public void toggleKeyFilter(KeyCode key) {
        this.selectedKeyFilter = key.equals(selectedKeyFilter) ? null : key;
        this.listScrollAmount = 0.0;
    }

    public boolean isHeatmapVisible() { return heatmapVisible; }
    public void toggleHeatmap() { this.heatmapVisible = !heatmapVisible; }

    public boolean isPackDevMode() { return packDevMode; }
    public void togglePackDevMode() { this.packDevMode = !packDevMode; }

    public boolean isQuickUnbindArmed() { return quickUnbindArmed; }
    public void setQuickUnbindArmed(boolean armed) { this.quickUnbindArmed = armed; }

    public ChordCaptureSession captureSession() { return captureSession; }
    public String capturingBindId() { return capturingBindId; }
    public void setCapturingBindId(String bindId) {
        this.capturingBindId = bindId;
        if (bindId != null) {
            this.captureSession.start();
        } else {
            this.captureSession.cancel();
        }
    }

    public double listScrollAmount() { return listScrollAmount; }
    public void setListScrollAmount(double amount) { this.listScrollAmount = Math.max(0.0, amount); }
}
