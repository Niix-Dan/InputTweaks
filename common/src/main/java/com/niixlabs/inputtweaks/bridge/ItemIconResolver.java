package com.niixlabs.inputtweaks.bridge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public final class ItemIconResolver {
    private static final List<String> CURATED_IDS = List.of(
            "minecraft:compass",
            "minecraft:recovery_compass",
            "minecraft:filled_map",
            "minecraft:book",
            "minecraft:writable_book",
            "minecraft:enchanted_book",
            "minecraft:knowledge_book",
            "minecraft:diamond_sword",
            "minecraft:netherite_pickaxe",
            "minecraft:iron_axe",
            "minecraft:bow",
            "minecraft:crossbow",
            "minecraft:shield",
            "minecraft:elytra",
            "minecraft:ender_pearl",
            "minecraft:ender_eye",
            "minecraft:chest",
            "minecraft:ender_chest",
            "minecraft:shulker_box",
            "minecraft:crafting_table",
            "minecraft:furnace",
            "minecraft:anvil",
            "minecraft:torch",
            "minecraft:lantern",
            "minecraft:spyglass",
            "minecraft:clock",
            "minecraft:bell",
            "minecraft:redstone",
            "minecraft:lever",
            "minecraft:beacon",
            "minecraft:totem_of_undying",
            "minecraft:golden_apple",
            "minecraft:potion",
            "minecraft:feather",
            "minecraft:name_tag",
            "minecraft:saddle",
            "minecraft:oak_boat",
            "minecraft:minecart",
            "minecraft:tnt",
            "minecraft:nether_star"
    );
    private static final Map<String, ItemStack> STACK_CACHE = new HashMap<>();
    private static List<IconOption> cachedCatalog = null;
    private ItemIconResolver() {
    }

    public static ItemStack resolve(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return new ItemStack(Items.COMPASS);
        }
        return STACK_CACHE.computeIfAbsent(itemId, key -> {
            ResourceLocation rl = ResourceLocation.tryParse(key);
            if (rl != null) {
                Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(Items.COMPASS);
                if (item != Items.AIR) {
                    return new ItemStack(item);
                }
            }
            return new ItemStack(Items.COMPASS);
        });
    }

    public static String displayName(String itemId) {
        return resolve(itemId).getHoverName().getString();
    }

    public static List<IconOption> searchCatalog(String query) {
        List<IconOption> all = fullCatalog();
        if (query == null || query.isBlank()) {
            return all;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<IconOption> filtered = new ArrayList<>();
        for (IconOption opt : all) {
            if (opt.displayName().toLowerCase(Locale.ROOT).contains(q)
                    || opt.itemId().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(opt);
            }
        }
        return filtered;
    }

    private static List<IconOption> fullCatalog() {
        if (cachedCatalog != null) return cachedCatalog;

        List<IconOption> list = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String id : CURATED_IDS) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) continue;
            BuiltInRegistries.ITEM.getOptional(rl).ifPresent(item -> {
                if (item != Items.AIR && seen.add(id)) {
                    ItemStack stack = new ItemStack(item);
                    list.add(new IconOption(id, stack.getHoverName().getString(), stack));
                }
            });
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            String id = key.toString();
            if (seen.add(id)) {
                ItemStack stack = new ItemStack(item);
                list.add(new IconOption(id, stack.getHoverName().getString(), stack));
            }
        }

        cachedCatalog = List.copyOf(list);
        return cachedCatalog;
    }

    public record IconOption(String itemId, String displayName, ItemStack stack) {
    }
}
