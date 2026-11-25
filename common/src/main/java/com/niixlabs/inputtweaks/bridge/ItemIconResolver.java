package com.niixlabs.inputtweaks.bridge;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ItemIconResolver {
    public record IconOption(String itemId, String displayName, ItemStack stack) {}

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
    private static final Set<String> UNSAFE_WITHOUT_WORLD = new HashSet<>();
    private static final Set<String> BROKEN_RENDERERS = new HashSet<>();
    private static List<IconOption> cachedCatalog = null;

    private ItemIconResolver() {}

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
        String fallback = (itemId == null || itemId.isBlank()) ? "minecraft:compass" : itemId;
        return safeDisplayName(resolve(fallback), fallback);
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
                    try {
                        ItemStack stack = new ItemStack(item);
                        if (!stack.isEmpty()) {
                            list.add(new IconOption(id, safeDisplayName(stack, id), stack));
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            String id = key.toString();
            if (seen.add(id)) {
                try {
                    ItemStack stack = new ItemStack(item);
                    if (!stack.isEmpty()) {
                        list.add(new IconOption(id, safeDisplayName(stack, id), stack));
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        cachedCatalog = List.copyOf(list);
        return cachedCatalog;
    }

    private static String safeDisplayName(ItemStack stack, String fallbackId) {
        try {
            return stack.getHoverName().getString();
        } catch (Throwable ignored) {
            return fallbackId;
        }
    }

    public static boolean isRenderBlocked(String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        if (BROKEN_RENDERERS.contains(itemId)) return true;
        Minecraft mc = Minecraft.getInstance();
        return (mc.player == null || mc.level == null) && UNSAFE_WITHOUT_WORLD.contains(itemId);
    }

    public static void renderSafe(GuiGraphics graphics, String itemId, ItemStack stack, int x, int y) {
        String key = (itemId == null || itemId.isBlank()) ? "minecraft:compass" : itemId;
        if (stack == null || stack.isEmpty() || isRenderBlocked(key)) {
            drawFallbackIcon(graphics, x, y);
            return;
        }

        boolean wasPoseClear = graphics.pose().clear();
        try {
            graphics.renderFakeItem(stack, x, y);
        } catch (Throwable t) {
            if (wasPoseClear) {
                while (!graphics.pose().clear()) {
                    graphics.pose().popPose();
                }
            }
            Lighting.setupFor3DItems();

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                UNSAFE_WITHOUT_WORLD.add(key);
            } else {
                BROKEN_RENDERERS.add(key);
            }
            drawFallbackIcon(graphics, x, y);
        }
    }

    private static void drawFallbackIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 2, y + 2, x + 14, y + 14, 0x663A4354);
        graphics.renderOutline(x + 2, y + 2, 12, 12, 0xAA6A768C);
        graphics.drawCenteredString(Minecraft.getInstance().font, "?", x + 8, y + 4, 0xFFCBD4E1);
    }
}
