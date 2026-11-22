package com.niixlabs.inputtweaks.core.radial;

import com.niixlabs.inputtweaks.core.model.RadialMenuDefinition;
import com.niixlabs.inputtweaks.core.model.RadialScope;
import com.niixlabs.inputtweaks.storage.PlayerRadialRepository;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RadialMenuManager {
    private final PlayerRadialRepository playerRepository;
    private final Map<String, RadialMenuDefinition> playerRadials = new LinkedHashMap<>();

    private Supplier<List<RadialMenuDefinition>> modpackRadialSupplier = List::of;
    private Consumer<List<RadialMenuDefinition>> modpackRadialSaver = list -> {
    };
    private Runnable onChangedCallback = () -> {
    };

    public RadialMenuManager(PlayerRadialRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public void wireModpackHooks(
            Supplier<List<RadialMenuDefinition>> modpackRadialSupplier,
            Consumer<List<RadialMenuDefinition>> modpackRadialSaver,
            Runnable onChangedCallback
    ) {
        this.modpackRadialSupplier = modpackRadialSupplier;
        this.modpackRadialSaver = modpackRadialSaver;
        this.onChangedCallback = onChangedCallback;
    }

    public void loadPlayerRadials() {
        playerRadials.clear();
        for (RadialMenuDefinition def : playerRepository.loadPlayerRadials()) {
            playerRadials.put(def.id(), def.withScope(RadialScope.PLAYER_GLOBAL));
        }
    }

    public List<RadialMenuDefinition> getByScope(RadialScope scope) {
        if (scope == RadialScope.MODPACK_DEFAULT) {
            return List.copyOf(modpackRadialSupplier.get());
        }
        return List.copyOf(playerRadials.values());
    }

    public List<RadialMenuDefinition> allEffectiveMenus() {
        Map<String, RadialMenuDefinition> merged = new LinkedHashMap<>();
        for (RadialMenuDefinition def : modpackRadialSupplier.get()) {
            merged.put(def.id(), def);
        }
        for (RadialMenuDefinition def : playerRadials.values()) {
            merged.put(def.id(), def);
        }
        return List.copyOf(merged.values());
    }

    public void saveMenu(RadialMenuDefinition menu) {
        if (menu.scope() == RadialScope.PLAYER_GLOBAL) {
            playerRadials.put(menu.id(), menu);
            playerRepository.savePlayerRadials(List.copyOf(playerRadials.values()));
        } else {
            List<RadialMenuDefinition> updated = new ArrayList<>(modpackRadialSupplier.get());
            boolean replaced = false;
            for (int i = 0; i < updated.size(); i++) {
                if (updated.get(i).id().equals(menu.id())) {
                    updated.set(i, menu);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                updated.add(menu);
            }
            modpackRadialSaver.accept(updated);
        }
        onChangedCallback.run();
    }

    public void deleteMenu(String id, RadialScope scope) {
        if (scope == RadialScope.PLAYER_GLOBAL) {
            if (playerRadials.remove(id) != null) {
                playerRepository.savePlayerRadials(List.copyOf(playerRadials.values()));
                onChangedCallback.run();
            }
        } else {
            List<RadialMenuDefinition> updated = new ArrayList<>(modpackRadialSupplier.get());
            if (updated.removeIf(m -> m.id().equals(id))) {
                modpackRadialSaver.accept(updated);
                onChangedCallback.run();
            }
        }
    }

    public String generateUniqueId(String baseTitle, RadialScope scope) {
        String slug = (baseTitle == null ? "wheel" : baseTitle)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (slug.isEmpty()) slug = "wheel";

        List<String> existing = getByScope(scope).stream().map(RadialMenuDefinition::id).toList();
        String candidate = slug;
        int n = 2;
        while (existing.contains(candidate)) {
            candidate = slug + "_" + n++;
        }
        return candidate;
    }
}
