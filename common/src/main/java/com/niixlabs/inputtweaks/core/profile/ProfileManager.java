package com.niixlabs.inputtweaks.core.profile;

import com.niixlabs.inputtweaks.api.action.BindableAction;
import com.niixlabs.inputtweaks.core.conflict.ConflictDetector;
import com.niixlabs.inputtweaks.core.dispatch.InputDispatcher;
import com.niixlabs.inputtweaks.core.layer.ConfigLayer;
import com.niixlabs.inputtweaks.core.layer.LayerMerger;
import com.niixlabs.inputtweaks.core.layer.ModpackConfigLayer;
import com.niixlabs.inputtweaks.core.layer.ProfileConfigLayer;
import com.niixlabs.inputtweaks.core.model.*;
import com.niixlabs.inputtweaks.storage.ModpackRuleRepository;
import com.niixlabs.inputtweaks.storage.ProfileRepository;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class ProfileManager {
    public static final String DEFAULT_PROFILE = "Default";

    private final ProfileRepository profileRepository;
    private final ModpackRuleRepository modpackRuleRepository;
    private final LayerMerger layerMerger;
    private final ConflictDetector conflictDetector;
    private final InputDispatcher dispatcher;
    private final Supplier<List<BindableAction>> actionSupplier;
    private final Function<BindableAction, String> defaultContextResolver;
    private final Runnable syncVanillaCallback;

    private final Map<String, ProfileData> globalProfiles = new LinkedHashMap<>();
    private final Map<String, ResolvedBind> resolvedBinds = new LinkedHashMap<>();

    private ModpackConfig modpackConfig = ModpackConfig.empty();
    private ProfileData localOverrides = ProfileData.empty("local");
    private String activeProfileName = DEFAULT_PROFILE;
    private boolean initialized = false;

    public ProfileManager(
            ProfileRepository profileRepository,
            ModpackRuleRepository modpackRuleRepository,
            LayerMerger layerMerger,
            ConflictDetector conflictDetector,
            InputDispatcher dispatcher,
            Supplier<List<BindableAction>> actionSupplier,
            Function<BindableAction, String> defaultContextResolver,
            Runnable syncVanillaCallback
    ) {
        this.profileRepository = profileRepository;
        this.modpackRuleRepository = modpackRuleRepository;
        this.layerMerger = layerMerger;
        this.conflictDetector = conflictDetector;
        this.dispatcher = dispatcher;
        this.actionSupplier = actionSupplier;
        this.defaultContextResolver = defaultContextResolver;
        this.syncVanillaCallback = syncVanillaCallback;
    }

    public void ensureLoaded() {
        if (!initialized) {
            loadAll();
        }
    }

    public void loadAll() {
        globalProfiles.clear();
        for (ProfileData profile : profileRepository.loadGlobalProfiles()) {
            globalProfiles.put(profile.name(), profile);
        }
        globalProfiles.putIfAbsent(DEFAULT_PROFILE, ProfileData.empty(DEFAULT_PROFILE));

        String remembered = profileRepository.loadActiveProfileName();
        activeProfileName = (remembered != null && globalProfiles.containsKey(remembered))
                ? remembered
                : DEFAULT_PROFILE;

        modpackConfig = modpackRuleRepository.loadRules();
        localOverrides = profileRepository.loadLocalOverrides();
        initialized = true;
        rebuildAndApply();
    }

    public void rebuildAndApply() {
        resolvedBinds.clear();

        List<ConfigLayer> layers = List.of(
                new ModpackConfigLayer(modpackConfig),
                new ProfileConfigLayer(BindOrigin.GLOBAL_PROFILE, 20, activeProfile()),
                new ProfileConfigLayer(BindOrigin.LOCAL_OVERRIDE, 30, localOverrides)
        );

        for (BindableAction action : actionSupplier.get()) {
            String defaultCtx = defaultContextResolver.apply(action);
            ResolvedBind resolved = layerMerger.merge(action, defaultCtx, layers);
            resolvedBinds.put(action.id(), resolved);
            action.applyChord(resolved.hardDisabled() ? KeyChord.UNBOUND : resolved.chord());
        }

        syncVanillaCallback.run();
        conflictDetector.rebuildIndex(resolvedBinds.values());
        dispatcher.rebuildIndex(resolvedBinds.values());
    }

    public ProfileData activeProfile() {
        return globalProfiles.computeIfAbsent(activeProfileName, ProfileData::empty);
    }

    public String activeProfileName() {
        return activeProfileName;
    }

    public List<String> profileNames() {
        return List.copyOf(globalProfiles.keySet());
    }

    public Collection<ResolvedBind> allResolved() {
        return resolvedBinds.values();
    }

    public ResolvedBind getResolved(String bindId) {
        return resolvedBinds.get(bindId);
    }

    public List<RadialMenuDefinition> modpackRadialMenus() {
        return List.copyOf(modpackConfig.radialMenus());
    }

    public void replaceModpackRadialMenus(List<RadialMenuDefinition> menus) {
        modpackConfig.radialMenus().clear();
        modpackConfig.radialMenus().addAll(menus);
        modpackRuleRepository.saveRules(modpackConfig);
    }

    public void switchProfile(String name) {
        if (!globalProfiles.containsKey(name)) return;
        activeProfileName = name;
        profileRepository.saveActiveProfileName(name);
        rebuildAndApply();
    }

    public String cycleProfile() {
        List<String> names = profileNames();
        int idx = names.indexOf(activeProfileName);
        String next = names.get((idx + 1) % names.size());
        switchProfile(next);
        return next;
    }

    public boolean createProfile(String name, boolean copyCurrent) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty() || globalProfiles.containsKey(clean)) return false;

        Map<String, ProfileEntry> initial = copyCurrent
                ? new LinkedHashMap<>(activeProfile().entries())
                : new LinkedHashMap<>();
        ProfileData created = new ProfileData(clean, initial);
        globalProfiles.put(clean, created);
        activeProfileName = clean;
        profileRepository.saveGlobalProfile(created);
        profileRepository.saveActiveProfileName(clean);
        rebuildAndApply();
        return true;
    }

    public void importProfile(ProfileData imported) {
        String name = imported.name().trim();
        if (name.isEmpty()) name = "Imported";
        String unique = name;
        int suffix = 2;
        while (globalProfiles.containsKey(unique)) {
            unique = name + " (" + suffix++ + ")";
        }
        ProfileData saved = new ProfileData(unique, imported.entries());
        globalProfiles.put(unique, saved);
        activeProfileName = unique;
        profileRepository.saveGlobalProfile(saved);
        profileRepository.saveActiveProfileName(unique);
        rebuildAndApply();
    }

    public boolean deleteProfile(String name) {
        if (DEFAULT_PROFILE.equals(name) || !globalProfiles.containsKey(name)) return false;
        globalProfiles.remove(name);
        profileRepository.deleteGlobalProfile(name);
        if (activeProfileName.equals(name)) {
            activeProfileName = DEFAULT_PROFILE;
            profileRepository.saveActiveProfileName(activeProfileName);
        }
        rebuildAndApply();
        return true;
    }

    public void updateEntry(String bindId, UnaryOperator<ProfileEntry> mutator) {
        ProfileData profile = activeProfile();
        ProfileEntry current = profile.entries().getOrDefault(bindId, ProfileEntry.empty());
        ProfileEntry updated = mutator.apply(current);
        if (updated == null || updated.isEmpty()) {
            profile.entries().remove(bindId);
        } else {
            profile.entries().put(bindId, updated);
        }
        profileRepository.saveGlobalProfile(profile);
        rebuildAndApply();
    }

    public ProfileEntry snapshotEntry(String bindId) {
        return activeProfile().entries().get(bindId);
    }

    public void restoreEntry(String bindId, ProfileEntry snapshot) {
        ProfileData profile = activeProfile();
        if (snapshot == null) {
            profile.entries().remove(bindId);
        } else {
            profile.entries().put(bindId, snapshot);
        }
        profileRepository.saveGlobalProfile(profile);
        rebuildAndApply();
    }

    public Map<String, ProfileEntry> quickUnbindKey(KeyCode key) {
        Map<String, ProfileEntry> previous = new LinkedHashMap<>();
        List<ResolvedBind> targets = new ArrayList<>(conflictDetector.getBindsForKey(key));
        if (targets.isEmpty()) return previous;

        ProfileData profile = activeProfile();
        for (ResolvedBind bind : targets) {
            if (bind.locked()) continue;
            previous.put(bind.id(), profile.entries().get(bind.id()));
            ProfileEntry updated = profile.entries()
                    .getOrDefault(bind.id(), ProfileEntry.empty())
                    .withChord(KeyChord.UNBOUND);
            profile.entries().put(bind.id(), updated);
        }
        profileRepository.saveGlobalProfile(profile);
        rebuildAndApply();
        return previous;
    }

    public void restoreBatch(Map<String, ProfileEntry> snapshots) {
        ProfileData profile = activeProfile();
        snapshots.forEach((id, entry) -> {
            if (entry == null) profile.entries().remove(id);
            else profile.entries().put(id, entry);
        });
        profileRepository.saveGlobalProfile(profile);
        rebuildAndApply();
    }

    public Map<String, ProfileEntry> resetActiveProfile() {
        ProfileData profile = activeProfile();
        Map<String, ProfileEntry> backup = new LinkedHashMap<>(profile.entries());
        profile.entries().clear();
        profileRepository.saveGlobalProfile(profile);
        rebuildAndApply();
        return backup;
    }

    public void updateModpackRule(String bindId, UnaryOperator<ModpackRule> mutator) {
        ModpackRule current = modpackConfig.rules().getOrDefault(bindId, ModpackRule.none());
        ModpackRule next = mutator.apply(current);
        if (next.isEmpty()) {
            modpackConfig.rules().remove(bindId);
        } else {
            modpackConfig.rules().put(bindId, next);
        }
        rebuildAndApply();
    }

    public void exportModpackRules() {
        modpackRuleRepository.saveRules(modpackConfig);
    }
}
