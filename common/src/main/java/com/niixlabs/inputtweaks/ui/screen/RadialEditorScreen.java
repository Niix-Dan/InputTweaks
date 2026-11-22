package com.niixlabs.inputtweaks.ui.screen;

import com.niixlabs.inputtweaks.InputTweaks;
import com.niixlabs.inputtweaks.bridge.InputKeyMapper;
import com.niixlabs.inputtweaks.bridge.ItemIconResolver;
import com.niixlabs.inputtweaks.core.model.*;
import com.niixlabs.inputtweaks.core.radial.RadialMenuManager;
import com.niixlabs.inputtweaks.ui.overlay.RadialMenuScreen;
import com.niixlabs.inputtweaks.ui.theme.UiPalette;
import com.niixlabs.inputtweaks.ui.widget.RadialGeometryRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RadialEditorScreen extends Screen {
    private static final int MAX_SLOTS = 12;
    private static final float PREVIEW_INNER_R = 28.0f;
    private static final float PREVIEW_OUTER_R = 72.0f;

    private final Screen parent;
    private final RadialMenuManager radialManager;
    private final List<RadialSlot> slots = new ArrayList<>();
    private RadialScope scope;
    private String editingId;
    private String titleText = "Quick Menu";
    private KeyChord triggerChord = KeyChord.of(KeyCode.keyboard(GLFW.GLFW_KEY_V));
    private RadialActivationMode activationMode = RadialActivationMode.HOLD_RELEASE;
    private int selectedSlotIdx = -1;
    private boolean capturingKey = false;
    private int pendingModifierKey = -1;
    private boolean hasWheel = false;

    private float pointerAngleRad = (float) (-Math.PI / 2.0);
    private boolean pointerInit = false;
    private long lastFrameNanos = System.nanoTime();

    private EditBox wheelTitleBox;
    private EditBox slotLabelBox;
    private Button keyButton;
    private Button modeButton;
    private Button colorButton;
    private int iconPreviewX = 0;
    private int iconPreviewY = 0;

    private String statusMsg = null;
    private long statusExpiresMs = 0L;

    public RadialEditorScreen(Screen parent, boolean startInPackDevScope) {
        super(Component.translatable("inputtweaks.radial.editor.title"));
        this.parent = parent;
        this.radialManager = InputTweaks.get().radialMenuManager();
        this.scope = startInPackDevScope ? RadialScope.MODPACK_DEFAULT : RadialScope.PLAYER_GLOBAL;
        loadFirstInScope();
    }

    private void loadFirstInScope() {
        List<RadialMenuDefinition> list = radialManager.getByScope(scope);
        if (list.isEmpty()) {
            hasWheel = false;
            editingId = null;
            slots.clear();
            selectedSlotIdx = -1;
        } else {
            loadDefinition(list.get(0));
        }
    }

    private void loadDefinition(RadialMenuDefinition def) {
        this.hasWheel = true;
        this.editingId = def.id();
        this.titleText = def.title();
        this.triggerChord = def.defaultChord();
        this.activationMode = def.activationMode();
        this.slots.clear();
        this.slots.addAll(def.slots());
        this.selectedSlotIdx = slots.isEmpty() ? -1 : 0;
        this.capturingKey = false;
    }

    private void createStarterWheel() {
        String id = radialManager.generateUniqueId("Quick Menu", scope);
        RadialMenuDefinition starter = new RadialMenuDefinition(
                id,
                scope == RadialScope.MODPACK_DEFAULT ? "Pack Menu" : "Quick Menu",
                KeyChord.of(KeyCode.keyboard(GLFW.GLFW_KEY_V)),
                RadialActivationMode.HOLD_RELEASE,
                scope,
                List.of(
                        new RadialSlot("Advancements", "key.advancements", "minecraft:knowledge_book", RadialGeometryRenderer.ACCENT_PALETTE[0]),
                        new RadialSlot("Perspective", "key.togglePerspective", "minecraft:spyglass", RadialGeometryRenderer.ACCENT_PALETTE[1]),
                        new RadialSlot("Screenshot", "key.screenshot", "minecraft:filled_map", RadialGeometryRenderer.ACCENT_PALETTE[2]),
                        new RadialSlot("Inventory", "key.inventory", "minecraft:chest", RadialGeometryRenderer.ACCENT_PALETTE[3])
                )
        );
        radialManager.saveMenu(starter);
        loadDefinition(starter);
        showStatus("Created new radial wheel!", 2200);
        rebuildWidgets();
    }

    private void saveCurrentWheel(boolean notify) {
        if (!hasWheel || editingId == null) return;
        RadialMenuDefinition def = buildCurrentDefinition();
        radialManager.saveMenu(def);
        if (notify) {
            String target = scope == RadialScope.MODPACK_DEFAULT
                    ? "config/inputtweaks/modpack_rules.json"
                    : "Global AppData (all modpacks)";
            showStatus("Saved \"" + def.title() + "\" to " + target, 2600);
        }
    }

    private RadialMenuDefinition buildCurrentDefinition() {
        String cleanTitle = (titleText == null || titleText.isBlank()) ? "Radial Menu" : titleText.trim();
        return new RadialMenuDefinition(editingId, cleanTitle, triggerChord, activationMode, scope, List.copyOf(slots));
    }

    private void showStatus(String msg, long durationMs) {
        this.statusMsg = msg;
        this.statusExpiresMs = System.currentTimeMillis() + durationMs;
    }

    private float previewCenterX() {
        int panelW = Math.min(this.width - 20, 456);
        int left = this.width / 2 - panelW / 2;
        return left + panelW * 0.25f;
    }

    private float previewCenterY() {
        int bodyTop = 74;
        int bodyBottom = this.height - 36;
        return bodyTop + (bodyBottom - bodyTop) * 0.44f;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int panelW = Math.min(this.width - 20, 456);
        int left = cx - panelW / 2;

        int row1Y = 22;
        int scopeW = 128;
        int newW = 66;
        int delW = 54;
        int wheelSelW = Math.max(80, panelW - (scopeW + newW + delW + 10));

        addRenderableWidget(Button.builder(
                        Component.translatable(scope.translationKey())
                                .withStyle(scope == RadialScope.MODPACK_DEFAULT ? ChatFormatting.GOLD : ChatFormatting.AQUA),
                        btn -> {
                            saveCurrentWheel(false);
                            scope = scope.toggle();
                            loadFirstInScope();
                            rebuildWidgets();
                        }
                ).bounds(left, row1Y, scopeW, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.radial.editor.scope_tip")))
                .build());

        List<RadialMenuDefinition> scopeList = radialManager.getByScope(scope);
        String wheelBtnLabel;
        if (!hasWheel || scopeList.isEmpty()) {
            wheelBtnLabel = "No Wheels Yet";
        } else {
            int idx = 0;
            for (int i = 0; i < scopeList.size(); i++) {
                if (scopeList.get(i).id().equals(editingId)) {
                    idx = i;
                    break;
                }
            }
            wheelBtnLabel = titleText + " (" + (idx + 1) + "/" + scopeList.size() + ")";
        }

        Button wheelSelBtn = addRenderableWidget(Button.builder(
                Component.literal(wheelBtnLabel),
                btn -> {
                    if (scopeList.size() <= 1) return;
                    saveCurrentWheel(false);
                    int cur = 0;
                    for (int i = 0; i < scopeList.size(); i++) {
                        if (scopeList.get(i).id().equals(editingId)) {
                            cur = i;
                            break;
                        }
                    }
                    loadDefinition(scopeList.get((cur + 1) % scopeList.size()));
                    rebuildWidgets();
                }
        ).bounds(left + scopeW + 3, row1Y, wheelSelW, 18).build());
        wheelSelBtn.active = scopeList.size() > 1;

        addRenderableWidget(Button.builder(
                Component.translatable("inputtweaks.radial.editor.new").withStyle(ChatFormatting.GREEN),
                btn -> {
                    saveCurrentWheel(false);
                    createStarterWheel();
                }
        ).bounds(left + scopeW + 3 + wheelSelW + 3, row1Y, newW, 18).build());

        Button delBtn = addRenderableWidget(Button.builder(
                Component.translatable("inputtweaks.profile.delete").withStyle(ChatFormatting.RED),
                btn -> {
                    if (!hasWheel || editingId == null) return;
                    radialManager.deleteMenu(editingId, scope);
                    loadFirstInScope();
                    showStatus("Deleted radial wheel.", 2000);
                    rebuildWidgets();
                }
        ).bounds(left + panelW - delW, row1Y, delW, 18).build());
        delBtn.active = hasWheel;

        if (hasWheel) {
            int row2Y = 44;
            int keyW = 104;
            int modeW = 112;
            int nameW = Math.max(90, panelW - (keyW + modeW + 8));

            this.wheelTitleBox = new EditBox(this.font, left, row2Y, nameW, 18,
                    Component.translatable("inputtweaks.radial.editor.wheel_name"));
            this.wheelTitleBox.setValue(titleText);
            this.wheelTitleBox.setHint(Component.literal("Wheel Name..."));
            this.wheelTitleBox.setResponder(v -> this.titleText = v);
            addRenderableWidget(this.wheelTitleBox);

            this.keyButton = addRenderableWidget(Button.builder(buildTriggerKeyLabel(), btn -> {
                        capturingKey = !capturingKey;
                        pendingModifierKey = -1;
                        btn.setMessage(buildTriggerKeyLabel());
                    }).bounds(left + nameW + 4, row2Y, keyW, 18)
                    .tooltip(Tooltip.create(Component.translatable("inputtweaks.radial.editor.key_tip")))
                    .build());

            this.modeButton = addRenderableWidget(Button.builder(
                            Component.translatable(activationMode.translationKey()),
                            btn -> {
                                activationMode = activationMode.toggle();
                                btn.setMessage(Component.translatable(activationMode.translationKey()));
                            }
                    ).bounds(left + panelW - modeW, row2Y, modeW, 18)
                    .tooltip(Tooltip.create(Component.translatable("inputtweaks.radial.editor.mode_tip")))
                    .build());

            int btnRowY = Math.min(this.height - 58, Math.round(previewCenterY() + PREVIEW_OUTER_R + 14));
            int pCx = Math.round(previewCenterX());

            Button addSlotBtn = addRenderableWidget(Button.builder(
                    Component.translatable("inputtweaks.radial.editor.add_slot").withStyle(ChatFormatting.GREEN),
                    btn -> {
                        if (slots.size() >= MAX_SLOTS) return;
                        int color = RadialGeometryRenderer.ACCENT_PALETTE[slots.size() % RadialGeometryRenderer.ACCENT_PALETTE.length];
                        slots.add(new RadialSlot("Slot " + (slots.size() + 1), "", RadialSlot.DEFAULT_ICON, color));
                        selectedSlotIdx = slots.size() - 1;
                        rebuildWidgets();
                    }
            ).bounds(pCx - 92, btnRowY, 90, 18).build());
            addSlotBtn.active = slots.size() < MAX_SLOTS;

            addRenderableWidget(Button.builder(
                    Component.translatable("inputtweaks.radial.editor.test").withStyle(ChatFormatting.AQUA),
                    btn -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(new RadialMenuScreen(buildCurrentDefinition(), triggerChord, this));
                        }
                    }
            ).bounds(pCx + 2, btnRowY, 90, 18).build());

            if (selectedSlotIdx >= 0 && selectedSlotIdx < slots.size()) {
                initSlotEditorPanel(cx + 8, 78, left + panelW - (cx + 8));
            }
        } else {
            addRenderableWidget(Button.builder(
                    Component.translatable("inputtweaks.radial.editor.create_first").withStyle(ChatFormatting.GREEN),
                    btn -> createStarterWheel()
            ).bounds(cx - 86, this.height / 2 - 4, 172, 22).build());
        }

        int footerY = this.height - 26;
        Button saveBtn = addRenderableWidget(Button.builder(
                Component.translatable("inputtweaks.radial.editor.save").withStyle(ChatFormatting.GREEN),
                btn -> saveCurrentWheel(true)
        ).bounds(cx - 112, footerY, 108, 20).build());
        saveBtn.active = hasWheel;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose())
                .bounds(cx + 4, footerY, 108, 20).build());
    }

    private void initSlotEditorPanel(int cardX, int cardY, int cardW) {
        RadialSlot slot = slots.get(selectedSlotIdx);
        int x = cardX + 10;
        int w = cardW - 20;

        this.slotLabelBox = new EditBox(this.font, x, cardY + 34, w, 18, Component.literal("Slot Label"));
        this.slotLabelBox.setValue(slot.label());
        this.slotLabelBox.setResponder(v -> {
            if (selectedSlotIdx >= 0 && selectedSlotIdx < slots.size()) {
                slots.set(selectedSlotIdx, slots.get(selectedSlotIdx).withLabel(v));
            }
        });
        addRenderableWidget(this.slotLabelBox);

        String actionName = slot.targetBindId().isEmpty()
                ? "Choose Keybind..."
                : I18n.get(slot.targetBindId());
        String truncatedAction = this.font.plainSubstrByWidth(actionName, w - 12);
        addRenderableWidget(Button.builder(
                        Component.literal(truncatedAction)
                                .withStyle(slot.targetBindId().isEmpty() ? ChatFormatting.YELLOW : ChatFormatting.WHITE),
                        btn -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new BindPickerScreen(this, (bindId, displayName) -> {
                                    if (selectedSlotIdx >= 0 && selectedSlotIdx < slots.size()) {
                                        RadialSlot cur = slots.get(selectedSlotIdx);
                                        RadialSlot updated = cur.withTargetBindId(bindId);
                                        if (cur.label().startsWith("Slot ") || cur.label().equals("Action")) {
                                            updated = updated.withLabel(displayName);
                                        }
                                        slots.set(selectedSlotIdx, updated);
                                    }
                                }));
                            }
                        }
                ).bounds(x, cardY + 68, w, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.radial.editor.pick_bind_tip")))
                .build());

        this.iconPreviewX = x + 3;
        this.iconPreviewY = cardY + 103;
        String iconName = this.font.plainSubstrByWidth(ItemIconResolver.displayName(slot.iconItemId()), w - 34);
        addRenderableWidget(Button.builder(
                        Component.literal(iconName),
                        btn -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new IconPickerScreen(this, slot.iconItemId(), newIconId -> {
                                    if (selectedSlotIdx >= 0 && selectedSlotIdx < slots.size()) {
                                        slots.set(selectedSlotIdx, slots.get(selectedSlotIdx).withIconItemId(newIconId));
                                    }
                                }));
                            }
                        }
                ).bounds(x + 24, cardY + 102, w - 24, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.radial.editor.pick_icon_tip")))
                .build());

        this.colorButton = addRenderableWidget(Button.builder(
                Component.translatable("inputtweaks.radial.editor.color_cycle"),
                btn -> {
                    if (selectedSlotIdx >= 0 && selectedSlotIdx < slots.size()) {
                        RadialSlot cur = slots.get(selectedSlotIdx);
                        int nextColor = nextAccentColor(cur.accentColor());
                        slots.set(selectedSlotIdx, cur.withAccentColor(nextColor));
                    }
                }
        ).bounds(x + 24, cardY + 126, w - 24, 18).build());

        int bottomY = cardY + 154;
        int moveW = 42;
        int removeW = Math.max(60, w - moveW * 2 - 6);

        Button leftBtn = addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            if (selectedSlotIdx > 0) {
                RadialSlot moved = slots.remove(selectedSlotIdx);
                selectedSlotIdx--;
                slots.add(selectedSlotIdx, moved);
                rebuildWidgets();
            }
        }).bounds(x, bottomY, moveW, 18).build());
        leftBtn.active = selectedSlotIdx > 0;

        Button rightBtn = addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            if (selectedSlotIdx >= 0 && selectedSlotIdx < slots.size() - 1) {
                RadialSlot moved = slots.remove(selectedSlotIdx);
                selectedSlotIdx++;
                slots.add(selectedSlotIdx, moved);
                rebuildWidgets();
            }
        }).bounds(x + moveW + 3, bottomY, moveW, 18).build());
        rightBtn.active = selectedSlotIdx < slots.size() - 1;

        addRenderableWidget(Button.builder(
                Component.translatable("inputtweaks.radial.editor.remove_slot").withStyle(ChatFormatting.RED),
                btn -> {
                    if (selectedSlotIdx >= 0 && selectedSlotIdx < slots.size()) {
                        slots.remove(selectedSlotIdx);
                        selectedSlotIdx = slots.isEmpty() ? -1 : Math.min(selectedSlotIdx, slots.size() - 1);
                        rebuildWidgets();
                    }
                }
        ).bounds(x + moveW * 2 + 6, bottomY, removeW, 18).build());
    }

    private Component buildTriggerKeyLabel() {
        if (capturingKey) {
            return Component.literal("> Press Key <").withStyle(ChatFormatting.YELLOW);
        }
        return Component.literal("Key: ")
                .withStyle(ChatFormatting.GRAY)
                .append(InputKeyMapper.formatChord(triggerChord).copy().withStyle(ChatFormatting.WHITE));
    }

    private int nextAccentColor(int current) {
        int[] pal = RadialGeometryRenderer.ACCENT_PALETTE;
        for (int i = 0; i < pal.length; i++) {
            if ((pal[i] & 0xFFFFFF) == (current & 0xFFFFFF)) {
                return pal[(i + 1) % pal.length];
            }
        }
        return pal[0];
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, this.width, 68, UiPalette.BAR_BG);
        UiPalette.drawHorizontalLine(graphics, 0, this.width, 68, UiPalette.BAR_BORDER);

        int footerTop = this.height - 32;
        graphics.fill(0, footerTop, this.width, this.height, UiPalette.BAR_BG);
        UiPalette.drawHorizontalLine(graphics, 0, this.width, footerTop, UiPalette.BAR_BORDER);

        if (hasWheel && selectedSlotIdx >= 0 && selectedSlotIdx < slots.size()) {
            int cx = this.width / 2;
            int panelW = Math.min(this.width - 20, 456);
            int rightEdge = cx + panelW / 2;
            int cardX = cx + 8;
            int cardW = rightEdge - cardX;
            UiPalette.drawFramedPanel(graphics, cardX, 78, cardW, 182, UiPalette.CARD_BG, UiPalette.CARD_BORDER);
            UiPalette.drawHorizontalLine(graphics, cardX + 1, cardX + cardW - 1, 98, UiPalette.BAR_BORDER);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int panelW = Math.min(this.width - 20, 456);
        int left = this.width / 2 - panelW / 2;
        graphics.drawString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), left, 7, UiPalette.TEXT_PRIMARY);

        Component scopeBadge = scope == RadialScope.MODPACK_DEFAULT
                ? Component.literal("Modpack Defaults (config/)").withStyle(ChatFormatting.GOLD)
                : Component.literal("Global Player Wheels (AppData)").withStyle(ChatFormatting.AQUA);
        graphics.drawString(this.font, scopeBadge, left + panelW - this.font.width(scopeBadge), 7, UiPalette.TEXT_MUTED);

        if (!hasWheel) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("inputtweaks.radial.editor.empty_hint").withStyle(ChatFormatting.GRAY),
                    this.width / 2, this.height / 2 - 22, UiPalette.TEXT_MUTED
            );
        } else {
            renderLivePreview(graphics, mouseX, mouseY);
            renderSlotCardLabels(graphics);
        }

        if (statusMsg != null && System.currentTimeMillis() < statusExpiresMs) {
            int w = this.font.width(statusMsg) + 18;
            int bx = this.width / 2 - w / 2;
            int by = this.height - 52;
            UiPalette.drawFramedPanel(graphics, bx, by, w, 16, 0xE014171F, UiPalette.ACCENT_BLUE);
            graphics.drawCenteredString(this.font, statusMsg, this.width / 2, by + 4, UiPalette.TEXT_PRIMARY);
        }
    }

    private void renderLivePreview(GuiGraphics graphics, int mouseX, int mouseY) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, Math.max(0.001f, (now - lastFrameNanos) / 1_000_000_000.0f));
        lastFrameNanos = now;

        float pcx = previewCenterX();
        float pcy = previewCenterY();

        double dx = mouseX - pcx;
        double dy = mouseY - pcy;
        double distSq = dx * dx + dy * dy;
        boolean insideWheelBounds = distSq <= (PREVIEW_OUTER_R + 18) * (PREVIEW_OUTER_R + 18);
        int hovered = insideWheelBounds
                ? RadialGeometryRenderer.hitTestSector(pcx, pcy, mouseX, mouseY, PREVIEW_INNER_R * 0.7f, slots.size())
                : -1;

        boolean showPointer = insideWheelBounds && distSq > 64.0;
        if (showPointer) {
            float targetAngle = (float) Math.atan2(dy, dx);
            if (!pointerInit) {
                pointerAngleRad = targetAngle;
                pointerInit = true;
            } else {
                pointerAngleRad = RadialGeometryRenderer.lerpAngle(pointerAngleRad, targetAngle, Math.min(1.0f, dt * 20.0f));
            }
        }

        int pointerColor = UiPalette.ACCENT_BLUE;
        String centerLabel = titleText;
        ItemStack centerIcon = ItemStack.EMPTY;
        int focusIdx = hovered >= 0 ? hovered : selectedSlotIdx;
        if (focusIdx >= 0 && focusIdx < slots.size()) {
            RadialSlot focus = slots.get(focusIdx);
            pointerColor = focus.accentColor();
            centerLabel = focus.label();
            centerIcon = ItemIconResolver.resolve(focus.iconItemId());
        }

        RadialGeometryRenderer.renderWheel(
                graphics, this.font,
                pcx, pcy,
                PREVIEW_INNER_R, PREVIEW_OUTER_R,
                1.0f, 1.0f,
                slots,
                hovered, selectedSlotIdx,
                null,
                showPointer, pointerAngleRad, pointerColor,
                centerLabel, null, centerIcon
        );

        graphics.drawCenteredString(
                this.font,
                Component.translatable("inputtweaks.radial.editor.click_slice").withStyle(ChatFormatting.DARK_GRAY),
                Math.round(pcx),
                Math.round(pcy - PREVIEW_OUTER_R - 13),
                UiPalette.TEXT_MUTED
        );
    }

    private void renderSlotCardLabels(GuiGraphics graphics) {
        if (selectedSlotIdx < 0 || selectedSlotIdx >= slots.size()) return;
        RadialSlot slot = slots.get(selectedSlotIdx);

        int cardX = this.width / 2 + 8;
        int cardY = 78;
        int x = cardX + 10;

        graphics.drawString(
                this.font,
                Component.literal("Slot #" + (selectedSlotIdx + 1) + " of " + slots.size()).withStyle(ChatFormatting.BOLD),
                x, cardY + 6, UiPalette.TEXT_PRIMARY
        );
        graphics.drawString(this.font, Component.literal("Label"), x, cardY + 23, UiPalette.TEXT_MUTED);
        graphics.drawString(this.font, Component.literal("Target Action"), x, cardY + 57, UiPalette.TEXT_MUTED);
        graphics.drawString(this.font, Component.literal("Icon & Accent Color"), x, cardY + 91, UiPalette.TEXT_MUTED);

        UiPalette.drawFramedPanel(graphics, x, cardY + 102, 20, 18, 0xCC0E1117, UiPalette.CARD_BORDER);
        graphics.renderFakeItem(ItemIconResolver.resolve(slot.iconItemId()), iconPreviewX - 1, iconPreviewY);

        UiPalette.drawFramedPanel(graphics, x, cardY + 126, 20, 18, slot.accentColor(), 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                triggerChord = KeyChord.UNBOUND;
                capturingKey = false;
                pendingModifierKey = -1;
                if (keyButton != null) keyButton.setMessage(buildTriggerKeyLabel());
                return true;
            }
            if (isStandaloneModifier(keyCode)) {
                pendingModifierKey = keyCode;
                return true;
            }
            triggerChord = KeyChord.of(KeyCode.keyboard(keyCode), extractHeldModifiers(keyCode));
            capturingKey = false;
            pendingModifierKey = -1;
            if (keyButton != null) keyButton.setMessage(buildTriggerKeyLabel());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (capturingKey && pendingModifierKey == keyCode) {
            triggerChord = KeyChord.of(KeyCode.keyboard(keyCode));
            capturingKey = false;
            pendingModifierKey = -1;
            if (keyButton != null) keyButton.setMessage(buildTriggerKeyLabel());
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (capturingKey) {
            triggerChord = KeyChord.of(KeyCode.mouse(button), extractHeldModifiers(-1));
            capturingKey = false;
            pendingModifierKey = -1;
            if (keyButton != null) keyButton.setMessage(buildTriggerKeyLabel());
            return true;
        }

        if (button == 1 && keyButton != null && keyButton.isMouseOver(mouseX, mouseY)) {
            triggerChord = KeyChord.UNBOUND;
            keyButton.setMessage(buildTriggerKeyLabel());
            return true;
        }

        if (hasWheel && button == 0 && !slots.isEmpty()) {
            float pcx = previewCenterX();
            float pcy = previewCenterY();
            double dx = mouseX - pcx;
            double dy = mouseY - pcy;
            if (dx * dx + dy * dy <= (PREVIEW_OUTER_R + 10) * (PREVIEW_OUTER_R + 10)) {
                int hit = RadialGeometryRenderer.hitTestSector(pcx, pcy, mouseX, mouseY, PREVIEW_INNER_R * 0.7f, slots.size());
                if (hit >= 0 && hit < slots.size()) {
                    selectedSlotIdx = hit;
                    rebuildWidgets();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isStandaloneModifier(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                || keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
                || keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private Set<KeyCode> extractHeldModifiers(int primaryKey) {
        Set<KeyCode> mods = new HashSet<>();
        if (Screen.hasControlDown() && primaryKey != GLFW.GLFW_KEY_LEFT_CONTROL && primaryKey != GLFW.GLFW_KEY_RIGHT_CONTROL) {
            mods.add(KeyCode.keyboard(GLFW.GLFW_KEY_LEFT_CONTROL));
        }
        if (Screen.hasAltDown() && primaryKey != GLFW.GLFW_KEY_LEFT_ALT && primaryKey != GLFW.GLFW_KEY_RIGHT_ALT) {
            mods.add(KeyCode.keyboard(GLFW.GLFW_KEY_LEFT_ALT));
        }
        if (Screen.hasShiftDown() && primaryKey != GLFW.GLFW_KEY_LEFT_SHIFT && primaryKey != GLFW.GLFW_KEY_RIGHT_SHIFT) {
            mods.add(KeyCode.keyboard(GLFW.GLFW_KEY_LEFT_SHIFT));
        }
        return mods;
    }

    @Override
    public void onClose() {
        saveCurrentWheel(false);
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
