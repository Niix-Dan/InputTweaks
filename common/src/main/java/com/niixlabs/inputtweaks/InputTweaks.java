package com.niixlabs.inputtweaks;

import com.niixlabs.inputtweaks.bridge.BuiltinContexts;
import com.niixlabs.inputtweaks.bridge.VanillaKeyRegistry;
import com.niixlabs.inputtweaks.core.analytics.UsageTracker;
import com.niixlabs.inputtweaks.core.conflict.ConflictDetector;
import com.niixlabs.inputtweaks.core.conflict.PriorityResolver;
import com.niixlabs.inputtweaks.core.context.ContextRegistry;
import com.niixlabs.inputtweaks.core.dispatch.InputDispatcher;
import com.niixlabs.inputtweaks.core.history.EditHistory;
import com.niixlabs.inputtweaks.core.layer.LayerMerger;
import com.niixlabs.inputtweaks.core.profile.ProfileManager;
import com.niixlabs.inputtweaks.core.radial.RadialMenuManager;
import com.niixlabs.inputtweaks.core.trigger.TriggerStrategyRegistry;
import com.niixlabs.inputtweaks.platform.PlatformKeyBridge;
import com.niixlabs.inputtweaks.platform.PlatformPaths;
import com.niixlabs.inputtweaks.storage.ModpackRuleRepository;
import com.niixlabs.inputtweaks.storage.PlayerRadialRepository;
import com.niixlabs.inputtweaks.storage.ProfileRepository;
import com.niixlabs.inputtweaks.storage.UsageStatsRepository;
import com.niixlabs.inputtweaks.storage.json.JsonModpackRuleRepository;
import com.niixlabs.inputtweaks.storage.json.JsonPlayerRadialRepository;
import com.niixlabs.inputtweaks.storage.json.JsonProfileRepository;
import com.niixlabs.inputtweaks.storage.json.JsonUsageStatsRepository;

public final class InputTweaks {
    public static final String MOD_ID = "inputtweaks";
    public static final String MOD_NAME = "Input Tweaks";

    private static InputTweaks instance;

    private final ContextRegistry contextRegistry;
    private final ConflictDetector conflictDetector;
    private final InputDispatcher dispatcher;
    private final UsageTracker usageTracker;
    private final EditHistory editHistory;
    private final ProfileManager profileManager;
    private final RadialMenuManager radialMenuManager;

    private InputTweaks(PlatformPaths paths, PlatformKeyBridge keyBridge) {
        this.contextRegistry = new ContextRegistry();
        BuiltinContexts.registerAll(this.contextRegistry);

        ProfileRepository profileRepo = new JsonProfileRepository(
                paths.globalProfilesDir(),
                paths.localOverridesFile(),
                paths.activeProfileStateFile()
        );
        ModpackRuleRepository modpackRepo = new JsonModpackRuleRepository(paths.modpackRulesFile());
        PlayerRadialRepository playerRadialRepo = new JsonPlayerRadialRepository(paths.globalRadialsFile());
        UsageStatsRepository usageRepo = new JsonUsageStatsRepository(paths.usageStatsFile());

        this.usageTracker = new UsageTracker(usageRepo);
        this.usageTracker.load();

        this.radialMenuManager = new RadialMenuManager(playerRadialRepo);
        this.radialMenuManager.loadPlayerRadials();

        this.conflictDetector = new ConflictDetector(this.contextRegistry);
        PriorityResolver priorityResolver = new PriorityResolver();
        TriggerStrategyRegistry triggerStrategies = new TriggerStrategyRegistry();
        this.dispatcher = new InputDispatcher(
                this.contextRegistry, priorityResolver, triggerStrategies, this.usageTracker
        );

        VanillaKeyRegistry vanillaRegistry = new VanillaKeyRegistry(keyBridge);
        this.editHistory = new EditHistory();

        this.profileManager = new ProfileManager(
                profileRepo,
                modpackRepo,
                new LayerMerger(),
                this.conflictDetector,
                this.dispatcher,
                vanillaRegistry::collectAll,
                vanillaRegistry::resolveDefaultContext,
                vanillaRegistry::syncVanillaMappings
        );

        this.radialMenuManager.wireModpackHooks(
                this.profileManager::modpackRadialMenus,
                this.profileManager::replaceModpackRadialMenus,
                this.profileManager::rebuildAndApply
        );
        vanillaRegistry.setRadialSupplier(this.radialMenuManager::allEffectiveMenus);
    }

    public static void init(PlatformPaths paths, PlatformKeyBridge keyBridge) {
        if (instance != null) return;
        instance = new InputTweaks(paths, keyBridge);
    }

    public static InputTweaks get() {
        return instance;
    }

    public ContextRegistry contextRegistry() {
        return contextRegistry;
    }

    public ConflictDetector conflictDetector() {
        return conflictDetector;
    }

    public InputDispatcher dispatcher() {
        return dispatcher;
    }

    public UsageTracker usageTracker() {
        return usageTracker;
    }

    public EditHistory editHistory() {
        return editHistory;
    }

    public ProfileManager profileManager() {
        return profileManager;
    }

    public RadialMenuManager radialMenuManager() {
        return radialMenuManager;
    }
}
