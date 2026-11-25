package com.niixlabs.inputtweaks.ui.widget;

import com.niixlabs.inputtweaks.api.context.InputContext;
import com.niixlabs.inputtweaks.bridge.InputKeyMapper;
import com.niixlabs.inputtweaks.core.model.ConflictPolicy;
import com.niixlabs.inputtweaks.core.model.KeyChord;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;
import com.niixlabs.inputtweaks.ui.filter.FilterMode;
import com.niixlabs.inputtweaks.ui.state.ControlsViewModel;
import com.niixlabs.inputtweaks.ui.theme.UiPalette;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BindListWidget extends ContainerObjectSelectionList<BindListWidget.RowEntry> {
    private final ControlsViewModel viewModel;
    private final Runnable softRefreshCallback;

    private List<Component> pendingTooltip = null;
    private int pendingTooltipX = 0;
    private int pendingTooltipY = 0;

    public BindListWidget(
            Minecraft minecraft,
            int width,
            int height,
            int topY,
            ControlsViewModel viewModel,
            Runnable softRefreshCallback
    ) {
        super(minecraft, width, height, topY, 24);
        this.viewModel = viewModel;
        this.softRefreshCallback = softRefreshCallback;
        rebuildEntries(viewModel.listScrollAmount());
    }

    public void rebuildEntries() {
        rebuildEntries(getScrollAmount());
    }

    public void rebuildEntries(double targetScroll) {
        clearEntries();

        List<ResolvedBind> binds = viewModel.filteredBinds();
        if (binds.isEmpty()) {
            Component msg = viewModel.filterMode() == FilterMode.CONFLICTS
                    ? Component.translatable("inputtweaks.empty.no_conflicts").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("inputtweaks.empty.no_matches").withStyle(ChatFormatting.GRAY);
            addEntry(new EmptyStateEntry(msg));
            super.setScrollAmount(0.0);
            viewModel.setListScrollAmount(0.0);
            return;
        }

        Map<String, List<ResolvedBind>> grouped = new LinkedHashMap<>();
        binds.stream()
                .sorted(Comparator.comparing(ResolvedBind::category))
                .forEach(b -> grouped.computeIfAbsent(b.category(), k -> new ArrayList<>()).add(b));

        for (Map.Entry<String, List<ResolvedBind>> cat : grouped.entrySet()) {
            addEntry(new CategoryEntry(Component.translatable(cat.getKey()), cat.getValue().size()));
            int rowIdx = 0;
            for (ResolvedBind bind : cat.getValue()) {
                addEntry(new KeyBindRowEntry(bind, rowIdx++));
            }
        }

        double clamped = Math.max(0.0, Math.min(targetScroll, getMaxScroll()));
        super.setScrollAmount(clamped);
        viewModel.setListScrollAmount(clamped);
    }

    @Override
    public void setScrollAmount(double scroll) {
        super.setScrollAmount(scroll);
        viewModel.setListScrollAmount(getScrollAmount());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.pendingTooltip = null;
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    public void renderDeferredTooltip(GuiGraphics graphics) {
        if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
            graphics.renderComponentTooltip(minecraft.font, pendingTooltip, pendingTooltipX, pendingTooltipY);
        }
    }

    @Override
    public int getRowWidth() {
        return Math.min(this.width - 28, 444);
    }

    @Override
    protected int getScrollbarPosition() {
        return this.width / 2 + getRowWidth() / 2 + 6;
    }

    public abstract static class RowEntry extends ContainerObjectSelectionList.Entry<RowEntry> {}

    private final class EmptyStateEntry extends RowEntry {
        private final Component message;

        EmptyStateEntry(Component message) {
            this.message = message;
        }

        @Override
        public void render(
                GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovering, float partialTick
        ) {
            graphics.drawCenteredString(minecraft.font, message, left + width / 2, top + 8, UiPalette.TEXT_MUTED);
        }

        @Override public List<? extends GuiEventListener> children() { return List.of(); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(); }
    }

    private final class CategoryEntry extends RowEntry {
        private final Component label;
        private final int count;

        CategoryEntry(Component label, int count) {
            this.label = label;
            this.count = count;
        }

        @Override
        public void render(
                GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovering, float partialTick
        ) {
            graphics.fill(left, top + 3, left + width, top + height - 2, UiPalette.CATEGORY_BG);

            MutableComponent full = label.copy().withStyle(ChatFormatting.BOLD)
                    .append(Component.literal(" (" + count + ")").withStyle(ChatFormatting.DARK_GRAY));

            int textW = minecraft.font.width(full);
            int centerX = left + width / 2;
            int lineY = top + height / 2 + 1;

            UiPalette.drawHorizontalLine(graphics, left + 6, centerX - textW / 2 - 8, lineY, UiPalette.CATEGORY_LINE);
            UiPalette.drawHorizontalLine(graphics, centerX + textW / 2 + 8, left + width - 6, lineY, UiPalette.CATEGORY_LINE);

            graphics.drawCenteredString(minecraft.font, full, centerX, top + (height - minecraft.font.lineHeight) / 2 + 1, UiPalette.TEXT_PRIMARY);
        }

        @Override public List<? extends GuiEventListener> children() { return List.of(); }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(new NarratableEntry() {
                @Override public NarrationPriority narrationPriority() { return NarrationPriority.HOVERED; }
                @Override public void updateNarration(NarrationElementOutput output) {
                    output.add(NarratedElementType.TITLE, label);
                }
            });
        }
    }

    /**
     * Lightweight Button extension supporting right-click callbacks (SRP: keeps row controls ergonomic).
     */
    private static final class ActionButton extends Button {
        private final Runnable onRightClick;

        ActionButton(int width, int height, Component message, OnPress onLeftClick, Runnable onRightClick, Tooltip tooltip) {
            super(0, 0, width, height, message, onLeftClick, DEFAULT_NARRATION);
            this.onRightClick = onRightClick;
            if (tooltip != null) {
                setTooltip(tooltip);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.active && this.visible && button == 1 && onRightClick != null && isMouseOver(mouseX, mouseY)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                onRightClick.run();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    private final class KeyBindRowEntry extends RowEntry {
        private final ResolvedBind bind;
        private final int visualRowIndex;
        private final Component displayName;
        private final Button keyButton;
        private final List<Button> extraButtons = new ArrayList<>();
        private final List<GuiEventListener> allChildren = new ArrayList<>();

        KeyBindRowEntry(ResolvedBind bind, int visualRowIndex) {
            this.bind = bind;
            this.visualRowIndex = visualRowIndex;
            this.displayName = Component.translatable(bind.action().translationKey());

            this.keyButton = new ActionButton(
                    90, 18,
                    buildKeyLabel(),
                    btn -> {
                        if (bind.locked()) return;
                        viewModel.setQuickUnbindArmed(false);
                        viewModel.setCapturingBindId(bind.id());
                        softRefreshCallback.run();
                    },
                    () -> {
                        if (bind.locked()) return;
                        viewModel.setCapturingBindId(null);
                        viewModel.assignChord(bind.id(), KeyChord.UNBOUND);
                        softRefreshCallback.run();
                    },
                    buildKeyTooltip()
            );
            this.keyButton.active = !bind.locked() && !bind.hardDisabled();

            if (viewModel.isPackDevMode()) {
                buildPackDevControls();
            } else {
                buildPlayerControls();
            }

            allChildren.add(keyButton);
            allChildren.addAll(extraButtons);
        }

        private void buildPlayerControls() {
            String prioText = bind.priority() > 0 ? ("P+" + bind.priority()) : ("P" + bind.priority());
            ChatFormatting prioColor = bind.priority() > 0
                    ? ChatFormatting.AQUA
                    : (bind.priority() < 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY);

            Button prioBtn = new ActionButton(
                    30, 18,
                    Component.literal(prioText).withStyle(prioColor),
                    btn -> {
                        int delta = Screen.hasShiftDown() ? -1 : 1;
                        viewModel.adjustPriority(bind.id(), delta);
                        softRefreshCallback.run();
                    },
                    () -> {
                        viewModel.adjustPriority(bind.id(), -1);
                        softRefreshCallback.run();
                    },
                    Tooltip.create(Component.translatable("inputtweaks.tooltip.priority"))
            );
            prioBtn.active = !bind.locked();

            Button policyBtn = new ActionButton(
                    34, 18,
                    Component.literal(bind.policy().shortLabel())
                            .withStyle(bind.policy() == ConflictPolicy.CONSUME ? ChatFormatting.GOLD : ChatFormatting.GRAY),
                    btn -> {
                        viewModel.toggleConflictPolicy(bind.id());
                        softRefreshCallback.run();
                    },
                    () -> {
                        viewModel.toggleConflictPolicy(bind.id());
                        softRefreshCallback.run();
                    },
                    Tooltip.create(Component.translatable("inputtweaks.tooltip.policy"))
            );
            policyBtn.active = !bind.locked();

            InputContext ctx = viewModel.contextRegistry().getOrDefault(bind.contextId());
            Button ctxBtn = new ActionButton(
                    36, 18,
                    Component.literal(ctx.shortLabel())
                            .withStyle(!"inputtweaks:universal".equals(bind.contextId()) ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    btn -> {
                        viewModel.cycleContext(bind.id(), Screen.hasShiftDown());
                        softRefreshCallback.run();
                    },
                    () -> {
                        viewModel.cycleContext(bind.id(), true);
                        softRefreshCallback.run();
                    },
                    Tooltip.create(Component.translatable(ctx.translationKey()))
            );
            ctxBtn.active = !bind.locked();

            Button trigBtn = new ActionButton(
                    34, 18,
                    Component.literal(bind.triggerMode().shortLabel())
                            .withStyle(bind.triggerMode().ordinal() != 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY),
                    btn -> {
                        viewModel.cycleTriggerMode(bind.id(), Screen.hasShiftDown());
                        softRefreshCallback.run();
                    },
                    () -> {
                        viewModel.cycleTriggerMode(bind.id(), true);
                        softRefreshCallback.run();
                    },
                    Tooltip.create(Component.translatable(bind.triggerMode().translationKey()))
            );
            trigBtn.active = !bind.locked();

            boolean isUnbound = bind.chord().isUnbound();
            Button clearBtn = new ActionButton(
                    18, 18,
                    Component.literal(isUnbound ? "R" : "x")
                            .withStyle(isUnbound ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    btn -> {
                        if (isUnbound || Screen.hasShiftDown()) {
                            viewModel.resetSingleBind(bind.id());
                        } else {
                            viewModel.assignChord(bind.id(), KeyChord.UNBOUND);
                        }
                        softRefreshCallback.run();
                    },
                    () -> {
                        viewModel.resetSingleBind(bind.id());
                        softRefreshCallback.run();
                    },
                    Tooltip.create(Component.translatable("inputtweaks.tooltip.clear_or_reset"))
            );
            clearBtn.active = !bind.locked();

            extraButtons.add(prioBtn);
            extraButtons.add(policyBtn);
            extraButtons.add(ctxBtn);
            extraButtons.add(trigBtn);
            extraButtons.add(clearBtn);
        }

        private void buildPackDevControls() {
            Button hideBtn = Button.builder(
                            Component.literal("H").withStyle(bind.hidden() ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY),
                            btn -> { viewModel.togglePackHidden(bind.id()); softRefreshCallback.run(); }
                    ).bounds(0, 0, 22, 18)
                    .tooltip(Tooltip.create(Component.translatable("inputtweaks.packdev.hide")))
                    .build();

            Button disableBtn = Button.builder(
                            Component.literal("D").withStyle(bind.hardDisabled() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY),
                            btn -> { viewModel.togglePackDisabled(bind.id()); softRefreshCallback.run(); }
                    ).bounds(0, 0, 22, 18)
                    .tooltip(Tooltip.create(Component.translatable("inputtweaks.packdev.disable")))
                    .build();

            Button lockBtn = Button.builder(
                            Component.literal("L").withStyle(bind.locked() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY),
                            btn -> { viewModel.togglePackLocked(bind.id()); softRefreshCallback.run(); }
                    ).bounds(0, 0, 22, 18)
                    .tooltip(Tooltip.create(Component.translatable("inputtweaks.packdev.lock")))
                    .build();

            Button resetBtn = Button.builder(
                    Component.translatable("controls.reset"),
                    btn -> { viewModel.resetSingleBind(bind.id()); softRefreshCallback.run(); }
            ).bounds(0, 0, 48, 18).build();

            extraButtons.add(hideBtn);
            extraButtons.add(disableBtn);
            extraButtons.add(lockBtn);
            extraButtons.add(resetBtn);
        }

        private Component buildKeyLabel() {
            if (bind.hardDisabled()) {
                return Component.translatable("inputtweaks.state.disabled").withStyle(ChatFormatting.DARK_RED);
            }
            if (bind.id().equals(viewModel.capturingBindId())) {
                Component inner = viewModel.captureSession().hasKeysPressed()
                        ? InputKeyMapper.formatCapturePreview(viewModel.captureSession().orderedKeys())
                        : Component.translatable("inputtweaks.state.press_key");
                return Component.literal("> ")
                        .append(inner.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                        .append(" <")
                        .withStyle(ChatFormatting.YELLOW);
            }
            MutableComponent base = InputKeyMapper.formatChordCompact(bind.chord()).copy();
            if (bind.chord().isUnbound()) {
                return base.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            }
            if (viewModel.conflictDetector().hasRealConflict(bind)) {
                return base.withStyle(ChatFormatting.RED);
            }
            if (bind.locked()) {
                return base.withStyle(ChatFormatting.AQUA);
            }
            return base;
        }

        private List<Component> buildTooltipLines() {
            List<Component> lines = new ArrayList<>();
            lines.add(displayName.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            if (bind.chord().hasModifiers()) {
                lines.add(Component.literal("Combo: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(InputKeyMapper.formatChord(bind.chord()).copy().withStyle(ChatFormatting.AQUA)));
            }
            lines.add(Component.translatable(bind.origin().translationKey()).withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("  •  @" + bind.action().modId()).withStyle(ChatFormatting.DARK_GRAY)));

            long uses = viewModel.usageTracker().getUsageCount(bind.id());
            lines.add(Component.translatable("inputtweaks.tooltip.uses", uses).withStyle(ChatFormatting.GRAY));

            List<ResolvedBind> conflicts = viewModel.conflictDetector().getRealConflicts(bind);
            if (!conflicts.isEmpty()) {
                lines.add(Component.translatable("inputtweaks.tooltip.conflicts_with").withStyle(ChatFormatting.RED));
                for (int i = 0; i < Math.min(conflicts.size(), 5); i++) {
                    lines.add(Component.literal(" • ")
                            .append(Component.translatable(conflicts.get(i).action().translationKey()))
                            .withStyle(ChatFormatting.RED));
                }
            }

            if (!bind.locked() && !bind.hardDisabled()) {
                lines.add(Component.translatable("inputtweaks.tooltip.key_hint").withStyle(ChatFormatting.DARK_GRAY));
            }
            return lines;
        }

        private Tooltip buildKeyTooltip() {
            MutableComponent combined = Component.empty();
            List<Component> lines = buildTooltipLines();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) combined.append(Component.literal("\n"));
                combined.append(lines.get(i));
            }
            return Tooltip.create(combined);
        }

        @Override
        public void render(
                GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovering, float partialTick
        ) {
            boolean isCapturing = bind.id().equals(viewModel.capturingBindId());
            boolean hasConflict = viewModel.conflictDetector().hasRealConflict(bind);

            int bgColor;
            if (isCapturing) {
                bgColor = UiPalette.ROW_CAPTURE_BG;
            } else if (hasConflict) {
                bgColor = UiPalette.ROW_CONFLICT_BG;
            } else {
                bgColor = (visualRowIndex % 2 == 0) ? UiPalette.ROW_EVEN : UiPalette.ROW_ODD;
            }
            graphics.fill(left, top + 1, left + width, top + height - 1, bgColor);

            if (hovering && !isCapturing) {
                graphics.fill(left, top + 1, left + width, top + height - 1, UiPalette.ROW_HOVER);
            }
            if (isCapturing) {
                graphics.renderOutline(left, top + 1, width, height - 2, UiPalette.ROW_CAPTURE_BORDER);
            }

            int accent = hasConflict ? UiPalette.ACCENT_RED : bind.origin().badgeColor();
            graphics.fill(left, top + 2, left + 3, top + height - 2, accent);

            int rightCursor = left + width - 3;
            int btnY = top + (height - 18) / 2;

            for (int i = extraButtons.size() - 1; i >= 0; i--) {
                Button b = extraButtons.get(i);
                rightCursor -= b.getWidth();
                b.setPosition(rightCursor, btnY);
                b.render(graphics, mouseX, mouseY, partialTick);
                rightCursor -= 2;
            }

            rightCursor -= keyButton.getWidth();
            keyButton.setPosition(rightCursor, btnY);
            keyButton.render(graphics, mouseX, mouseY, partialTick);

            int labelLeft = left + 8;
            int maxNameWidth = Math.max(40, (rightCursor - labelLeft) - 6);
            int textColor = bind.hardDisabled()
                    ? UiPalette.TEXT_DISABLED
                    : (bind.hidden() ? UiPalette.TEXT_HIDDEN : UiPalette.TEXT_PRIMARY);

            String rawName = displayName.getString();
            String truncated = minecraft.font.plainSubstrByWidth(rawName, maxNameWidth);
            if (truncated.length() < rawName.length() && maxNameWidth > 18) {
                truncated = minecraft.font.plainSubstrByWidth(rawName, maxNameWidth - 8) + "...";
            }

            graphics.drawString(
                    minecraft.font,
                    truncated,
                    labelLeft,
                    top + (height - minecraft.font.lineHeight) / 2 + 1,
                    textColor
            );

            if (hovering && mouseX >= left && mouseX < rightCursor) {
                pendingTooltip = buildTooltipLines();
                pendingTooltipX = mouseX;
                pendingTooltipY = mouseY;
            }
        }

        @Override public List<? extends GuiEventListener> children() { return allChildren; }
        @Override public List<? extends NarratableEntry> narratables() {
            List<NarratableEntry> list = new ArrayList<>();
            list.add(keyButton);
            list.addAll(extraButtons);
            return list;
        }
    }
}
