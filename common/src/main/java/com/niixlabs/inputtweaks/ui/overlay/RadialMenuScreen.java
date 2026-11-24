package com.niixlabs.inputtweaks.ui.overlay;

import com.niixlabs.inputtweaks.InputTweaks;
import com.niixlabs.inputtweaks.bridge.ItemIconResolver;
import com.niixlabs.inputtweaks.core.model.*;
import com.niixlabs.inputtweaks.ui.theme.UiPalette;
import com.niixlabs.inputtweaks.ui.widget.RadialGeometryRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class RadialMenuScreen extends Screen {
    private static final float INNER_RADIUS = 40.0f;
    private static final float OUTER_RADIUS = 104.0f;
    private static final float OPEN_DURATION_SEC = 0.15f;

    private final RadialMenuDefinition definition;
    private final KeyChord triggerChord;
    private final Screen returnScreen;

    private final long openedAtNanos;
    private final float[] slotHoverFactors;
    private long lastFrameNanos;
    private float pointerAngleRad = (float) (-Math.PI / 2.0);
    private boolean pointerInitialized = false;
    private int hoveredSlot = -1;

    public RadialMenuScreen(RadialMenuDefinition definition, KeyChord triggerChord, Screen returnScreen) {
        super(Component.literal(definition.title()));
        this.definition = definition;
        this.triggerChord = triggerChord == null ? KeyChord.UNBOUND : triggerChord;
        this.returnScreen = returnScreen;
        this.openedAtNanos = System.nanoTime();
        this.lastFrameNanos = this.openedAtNanos;
        this.slotHoverFactors = new float[definition.slots().size()];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, 0x28080A10);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, Math.max(0.001f, (now - lastFrameNanos) / 1_000_000_000.0f));
        lastFrameNanos = now;

        float elapsed = (now - openedAtNanos) / 1_000_000_000.0f;
        float progress = Math.min(1.0f, elapsed / OPEN_DURATION_SEC);
        float easeOut = 1.0f - (float) Math.pow(1.0f - progress, 3.0);
        float openScale = 0.68f + 0.32f * easeOut;

        float cx = this.width / 2.0f;
        float cy = this.height / 2.0f;
        List<RadialSlot> slots = definition.slots();

        hoveredSlot = RadialGeometryRenderer.hitTestSector(cx, cy, mouseX, mouseY, INNER_RADIUS * 0.65f, slots.size());

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        boolean showPointer = (dx * dx + dy * dy) > 100.0;
        if (showPointer) {
            float targetAngle = (float) Math.atan2(dy, dx);
            if (!pointerInitialized) {
                pointerAngleRad = targetAngle;
                pointerInitialized = true;
            } else {
                float speed = Math.min(1.0f, dt * 22.0f);
                pointerAngleRad = RadialGeometryRenderer.lerpAngle(pointerAngleRad, targetAngle, speed);
            }
        }

        for (int i = 0; i < slotHoverFactors.length; i++) {
            float target = (i == hoveredSlot) ? 1.0f : 0.0f;
            slotHoverFactors[i] += (target - slotHoverFactors[i]) * Math.min(1.0f, dt * 18.0f);
        }

        int pointerColor = UiPalette.ACCENT_BLUE;
        String centerTitle = definition.title();
        String centerSub = slots.isEmpty() ? "Empty" : (slots.size() + " actions");
        ItemStack centerIcon = ItemStack.EMPTY;

        if (hoveredSlot >= 0 && hoveredSlot < slots.size()) {
            RadialSlot hovered = slots.get(hoveredSlot);
            pointerColor = hovered.accentColor();
            centerTitle = hovered.label();
            centerSub = definition.activationMode() == RadialActivationMode.HOLD_RELEASE ? "Release" : "Click";
            centerIcon = ItemIconResolver.resolve(hovered.iconItemId());
        }

        RadialGeometryRenderer.renderWheel(
                graphics, this.font,
                cx, cy,
                INNER_RADIUS, OUTER_RADIUS,
                openScale, easeOut,
                slots,
                hoveredSlot, -1,
                slotHoverFactors,
                showPointer, pointerAngleRad, pointerColor,
                centerTitle, centerSub, centerIcon
        );

        Component hint = definition.activationMode() == RadialActivationMode.HOLD_RELEASE
                ? Component.translatable("inputtweaks.radial.hint_hold")
                : Component.translatable("inputtweaks.radial.hint");
        graphics.drawCenteredString(
                this.font,
                hint.copy().withStyle(ChatFormatting.GRAY),
                Math.round(cx),
                Math.round(cy + OUTER_RADIUS + 18),
                UiPalette.TEXT_MUTED
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredSlot >= 0 && hoveredSlot < definition.slots().size()) {
            activateAndClose(definition.slots().get(hoveredSlot));
            return true;
        }
        onClose();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (definition.activationMode() == RadialActivationMode.CLICK && matchesTriggerKeyboard(keyCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (definition.activationMode() == RadialActivationMode.HOLD_RELEASE && matchesTriggerKeyboard(keyCode)) {
            if (hoveredSlot >= 0 && hoveredSlot < definition.slots().size()) {
                activateAndClose(definition.slots().get(hoveredSlot));
            } else {
                onClose();
            }
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (definition.activationMode() == RadialActivationMode.HOLD_RELEASE && matchesTriggerMouse(button)) {
            if (hoveredSlot >= 0 && hoveredSlot < definition.slots().size()) {
                activateAndClose(definition.slots().get(hoveredSlot));
            } else {
                onClose();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean matchesTriggerKeyboard(int glfwKey) {
        KeyCode primary = triggerChord.primary();
        return !primary.isUnknown() && primary.type() == InputType.KEYBOARD && primary.code() == glfwKey;
    }

    private boolean matchesTriggerMouse(int button) {
        KeyCode primary = triggerChord.primary();
        return !primary.isUnknown() && primary.type() == InputType.MOUSE && primary.code() == button;
    }

    private void activateAndClose(RadialSlot slot) {
        onClose();
        if (returnScreen != null) {
            return;
        }
        ResolvedBind target = InputTweaks.get().profileManager().getResolved(slot.targetBindId());
        if (target != null && !target.hardDisabled()) {
            target.action().setPressed(true);
            target.action().triggerClick();
            target.action().setPressed(false);
            InputTweaks.get().usageTracker().recordUse(target.id());
        }
    }

    @Override
    public void onClose() {
        InputTweaks app = InputTweaks.get();
        if (app != null) {
            app.dispatcher().clearHeldKeys();
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(returnScreen);
        }
    }
}
