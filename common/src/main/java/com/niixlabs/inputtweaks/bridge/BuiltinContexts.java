package com.niixlabs.inputtweaks.bridge;

import com.niixlabs.inputtweaks.api.context.InputContext;
import com.niixlabs.inputtweaks.core.context.ContextRegistry;
import net.minecraft.client.Minecraft;

public final class BuiltinContexts {
    public static final String UNIVERSAL = "inputtweaks:universal";
    public static final String IN_GAME = "inputtweaks:in_game";
    public static final String GUI = "inputtweaks:gui";
    public static final String RIDING = "inputtweaks:riding";
    public static final String FLYING = "inputtweaks:flying";
    public static final String SNEAKING = "inputtweaks:sneaking";

    private BuiltinContexts() {
    }

    public static void registerAll(ContextRegistry registry) {
        registry.register(new SimpleContext(
                UNIVERSAL, "inputtweaks.context.universal", "ALL",
                () -> true,
                (self, other) -> true
        ));

        registry.register(new SimpleContext(
                IN_GAME, "inputtweaks.context.in_game", "GAME",
                () -> Minecraft.getInstance().screen == null,
                (self, other) -> !GUI.equals(other.id())
        ));

        registry.register(new SimpleContext(
                GUI, "inputtweaks.context.gui", "GUI",
                () -> Minecraft.getInstance().screen != null,
                (self, other) -> GUI.equals(other.id()) || UNIVERSAL.equals(other.id())
        ));

        registry.register(new SimpleContext(
                RIDING, "inputtweaks.context.riding", "RIDE",
                () -> Minecraft.getInstance().screen == null
                        && Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.isPassenger(),
                (self, other) -> !GUI.equals(other.id()) && !FLYING.equals(other.id())
        ));

        registry.register(new SimpleContext(
                FLYING, "inputtweaks.context.flying", "FLY",
                () -> Minecraft.getInstance().screen == null
                        && Minecraft.getInstance().player != null
                        && (Minecraft.getInstance().player.isFallFlying()
                        || Minecraft.getInstance().player.getAbilities().flying),
                (self, other) -> !GUI.equals(other.id()) && !RIDING.equals(other.id())
        ));

        registry.register(new SimpleContext(
                SNEAKING, "inputtweaks.context.sneaking", "SNK",
                () -> Minecraft.getInstance().screen == null
                        && Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.isShiftKeyDown(),
                (self, other) -> !GUI.equals(other.id())
        ));
    }

    @FunctionalInterface
    private interface ActiveCheck {
        boolean test();
    }

    @FunctionalInterface
    private interface ConflictCheck {
        boolean conflicts(InputContext self, InputContext other);
    }

    private record SimpleContext(
            String id,
            String translationKey,
            String shortLabel,
            ActiveCheck activeCheck,
            ConflictCheck conflictCheck
    ) implements InputContext {
        @Override
        public boolean isActive() {
            return activeCheck.test();
        }

        @Override
        public boolean conflictsWith(InputContext other) {
            return UNIVERSAL.equals(other.id()) || conflictCheck.conflicts(this, other);
        }
    }
}
