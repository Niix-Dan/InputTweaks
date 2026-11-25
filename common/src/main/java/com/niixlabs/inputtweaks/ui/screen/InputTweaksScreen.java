package com.niixlabs.inputtweaks.ui.screen;

import com.niixlabs.inputtweaks.InputTweaks;
import com.niixlabs.inputtweaks.bridge.InputKeyMapper;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import com.niixlabs.inputtweaks.ui.state.ControlsViewModel;
import com.niixlabs.inputtweaks.ui.theme.UiPalette;
import com.niixlabs.inputtweaks.ui.widget.BindListWidget;
import com.niixlabs.inputtweaks.ui.widget.KeyboardHeatmapPanel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class InputTweaksScreen extends Screen {
    private static final long RESET_CONFIRM_WINDOW_MS = 3000L;
    private static final int HEADER_HEIGHT = 68;
    private static final int FOOTER_HEIGHT = 32;

    private final Screen lastScreen;
    private final Options options;
    private final ControlsViewModel viewModel;
    private final KeyboardHeatmapPanel heatmapPanel;

    private EditBox searchBox;
    private Button clearSearchButton;
    private Button keyFilterChipButton;
    private BindListWidget listWidget;

    private Button profileButton;
    private Button heatmapButton;
    private Button debugButton;
    private Button packDevButton;
    private Button filterButton;
    private Button quickUnbindButton;
    private Button wizardButton;
    private Button undoButton;
    private Button redoButton;
    private Button resetAllButton;

    private long resetConfirmUntilMs = 0L;

    public InputTweaksScreen(Screen lastScreen, Options options) {
        super(Component.translatable("controls.keybinds.title"));
        this.lastScreen = lastScreen;
        this.options = options;

        InputTweaks app = InputTweaks.get();
        app.profileManager().ensureLoaded();
        this.viewModel = new ControlsViewModel(
                app.profileManager(),
                app.conflictDetector(),
                app.contextRegistry(),
                app.dispatcher(),
                app.usageTracker(),
                app.editHistory()
        );
        this.heatmapPanel = new KeyboardHeatmapPanel(app.conflictDetector());
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int barW = Math.min(this.width - 24, 444);
        int left = cx - barW / 2;

        int row1Y = 22;
        int plusW = 18;
        int radW = 68;
        int kbW = 68;
        int dbgW = 66;
        int devW = 80;
        int gaps1 = 15;
        int profileW = Math.max(78, barW - (plusW + radW + kbW + dbgW + devW + gaps1));

        int x = left;
        this.profileButton = addRenderableWidget(Button.builder(
                        Component.empty(),
                        btn -> {
                            String next = viewModel.profileManager().cycleProfile();
                            viewModel.showBanner("Switched to profile: " + next, 2000);
                            refreshInPlace();
                        }
                ).bounds(x, row1Y, profileW, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.tooltip.profile_cycle")))
                .build());
        x += profileW + 2;

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
                    if (this.minecraft != null) {
                        if (listWidget != null) viewModel.setListScrollAmount(listWidget.getScrollAmount());
                        this.minecraft.setScreen(new ProfileCreateScreen(this, viewModel));
                    }
                }).bounds(x, row1Y, plusW, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.tooltip.profile_new")))
                .build());
        x += plusW + 4;

        int radialCount = InputTweaks.get().radialMenuManager().allEffectiveMenus().size();
        Component radialLabel = radialCount > 0
                ? Component.translatable("inputtweaks.button.radials_count", radialCount).withStyle(ChatFormatting.AQUA)
                : Component.translatable("inputtweaks.button.radials");
        addRenderableWidget(Button.builder(radialLabel, btn -> {
                    if (this.minecraft != null) {
                        if (listWidget != null) viewModel.setListScrollAmount(listWidget.getScrollAmount());
                        this.minecraft.setScreen(new RadialEditorScreen(this, viewModel.isPackDevMode()));
                    }
                }).bounds(x, row1Y, radW, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.tooltip.radials")))
                .build());
        x += radW + 3;

        this.heatmapButton = addRenderableWidget(Button.builder(
                Component.empty(),
                btn -> {
                    viewModel.toggleHeatmap();
                    rebuildLayoutPreservingScroll();
                }
        ).bounds(x, row1Y, kbW, 18).build());
        x += kbW + 3;

        this.debugButton = addRenderableWidget(Button.builder(
                Component.empty(),
                btn -> {
                    viewModel.dispatcher().toggleDebugOverlay();
                    refreshToolbarStates();
                }
        ).bounds(x, row1Y, dbgW, 18).build());
        x += dbgW + 3;

        this.packDevButton = addRenderableWidget(Button.builder(
                        Component.empty(),
                        btn -> {
                            viewModel.togglePackDevMode();
                            rebuildLayoutPreservingScroll();
                        }
                ).bounds(x, row1Y, devW, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.tooltip.packdev")))
                .build());

        int row2Y = 44;
        int filterW = 80;
        int quickW = 84;
        int wizW = 82;
        int keyChipW = viewModel.selectedKeyFilter() != null ? 58 : 0;
        int searchClearW = 16;
        int fixedRow2 = filterW + quickW + wizW + searchClearW + (keyChipW > 0 ? keyChipW + 3 : 0) + 12;
        int searchW = Math.max(76, barW - fixedRow2);

        int x2 = left;
        this.searchBox = new EditBox(this.font, x2, row2Y, searchW, 18, Component.translatable("inputtweaks.search"));
        this.searchBox.setHint(Component.translatable("inputtweaks.search.hint"));
        this.searchBox.setValue(viewModel.searchQuery());
        this.searchBox.setResponder(q -> {
            viewModel.setSearchQuery(q);
            if (clearSearchButton != null) {
                clearSearchButton.active = !q.isEmpty();
            }
            if (listWidget != null) {
                listWidget.rebuildEntries(0.0);
            }
        });
        addRenderableWidget(this.searchBox);
        x2 += searchW + 1;

        this.clearSearchButton = addRenderableWidget(Button.builder(Component.literal("x"), btn -> {
                    viewModel.setSearchQuery("");
                    searchBox.setValue("");
                    if (listWidget != null) listWidget.rebuildEntries(0.0);
                }).bounds(x2, row2Y, searchClearW, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.tooltip.clear_search")))
                .build());
        this.clearSearchButton.active = !viewModel.searchQuery().isEmpty();
        x2 += searchClearW + 3;

        if (keyChipW > 0) {
            String keyLabel = InputKeyMapper.formatKey(viewModel.selectedKeyFilter()).getString();
            if (keyLabel.length() > 5) keyLabel = keyLabel.substring(0, 5);
            this.keyFilterChipButton = addRenderableWidget(Button.builder(
                            Component.literal(keyLabel + " x").withStyle(ChatFormatting.AQUA),
                            btn -> {
                                viewModel.clearKeyFilter();
                                rebuildLayoutPreservingScroll();
                            }
                    ).bounds(x2, row2Y, keyChipW, 18)
                    .tooltip(Tooltip.create(Component.translatable("inputtweaks.tooltip.clear_key_filter")))
                    .build());
            x2 += keyChipW + 3;
        } else {
            this.keyFilterChipButton = null;
        }

        this.filterButton = addRenderableWidget(Button.builder(
                Component.empty(),
                btn -> {
                    viewModel.cycleFilterMode();
                    refreshInPlace(0.0);
                }
        ).bounds(x2, row2Y, filterW, 18).build());
        x2 += filterW + 3;

        this.quickUnbindButton = addRenderableWidget(Button.builder(
                        Component.empty(),
                        btn -> {
                            viewModel.setCapturingBindId(null);
                            viewModel.setQuickUnbindArmed(!viewModel.isQuickUnbindArmed());
                            refreshToolbarStates();
                        }
                ).bounds(x2, row2Y, quickW, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.tooltip.quick_unbind")))
                .build());
        x2 += quickW + 3;

        this.wizardButton = addRenderableWidget(Button.builder(
                        Component.empty(),
                        btn -> {
                            if (!viewModel.stepConflictWizard()) {
                                viewModel.showBanner("No real conflicts detected!", 2000);
                                rebuildLayoutPreservingScroll();
                            } else {
                                rebuildLayoutPreservingScroll();
                            }
                        }
                ).bounds(x2, row2Y, wizW, 18)
                .tooltip(Tooltip.create(Component.translatable("inputtweaks.tooltip.wizard")))
                .build());

        int listTop = HEADER_HEIGHT + 2;
        if (viewModel.isHeatmapVisible()) {
            heatmapPanel.layout(cx, listTop + 2);
            listTop += heatmapPanel.height() + 6;
        }
        int listBottom = this.height - FOOTER_HEIGHT - 2;
        this.listWidget = new BindListWidget(
                this.minecraft, this.width, Math.max(40, listBottom - listTop), listTop,
                viewModel, this::refreshInPlace
        );
        addRenderableWidget(this.listWidget);

        int footerY = this.height - 26;
        int undoW = 54;
        int redoW = 54;
        int resetW = 98;
        int remainingRight = barW - (undoW + redoW + resetW + 12);

        int fx = left;
        this.undoButton = addRenderableWidget(Button.builder(Component.translatable("inputtweaks.button.undo"), btn -> {
            if (viewModel.history().undo()) refreshInPlace();
        }).bounds(fx, footerY, undoW, 20).build());
        fx += undoW + 4;

        this.redoButton = addRenderableWidget(Button.builder(Component.translatable("inputtweaks.button.redo"), btn -> {
            if (viewModel.history().redo()) refreshInPlace();
        }).bounds(fx, footerY, redoW, 20).build());
        fx += redoW + 4;

        this.resetAllButton = addRenderableWidget(Button.builder(
                Component.empty(),
                btn -> {
                    long now = System.currentTimeMillis();
                    if (now < resetConfirmUntilMs) {
                        resetConfirmUntilMs = 0L;
                        viewModel.resetAll();
                        viewModel.showBanner("Active profile reset to defaults.", 2500);
                        refreshInPlace();
                    } else {
                        resetConfirmUntilMs = now + RESET_CONFIRM_WINDOW_MS;
                        refreshToolbarStates();
                    }
                }
        ).bounds(fx, footerY, resetW, 20).build());
        fx += resetW + 4;

        if (viewModel.isPackDevMode()) {
            int half = Math.max(70, (remainingRight - 4) / 2);
            addRenderableWidget(Button.builder(
                    Component.translatable("inputtweaks.button.export_pack").withStyle(ChatFormatting.GOLD),
                    btn -> {
                        viewModel.exportPackRules();
                        viewModel.showBanner("Exported config/inputtweaks/modpack_rules.json", 3000);
                    }
            ).bounds(fx, footerY, half, 20).build());

            addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose())
                    .bounds(fx + half + 4, footerY, remainingRight - half - 4, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose())
                    .bounds(fx, footerY, Math.max(80, remainingRight), 20).build());
        }

        refreshToolbarStates();
    }

    /**
     * Soft in-place refresh: updates rows and button states WITHOUT rebuilding the screen or resetting scroll!
     */
    private void refreshInPlace() {
        if (listWidget != null) {
            listWidget.rebuildEntries();
        }
        refreshToolbarStates();
    }

    private void refreshInPlace(double targetScroll) {
        if (listWidget != null) {
            listWidget.rebuildEntries(targetScroll);
        }
        refreshToolbarStates();
    }

    /**
     * Structural layout rebuild (used only when Heatmap, Pack Dev Mode, or Key Filter Chip changes geometry).
     * Persists scroll position into ControlsViewModel before rebuilding.
     */
    private void rebuildLayoutPreservingScroll() {
        if (listWidget != null) {
            viewModel.setListScrollAmount(listWidget.getScrollAmount());
        }
        if (this.minecraft != null) {
            this.rebuildWidgets();
        }
    }

    private void refreshToolbarStates() {
        if (profileButton != null) {
            profileButton.setMessage(Component.translatable(
                    "inputtweaks.button.profile", viewModel.profileManager().activeProfileName()
            ));
        }
        if (heatmapButton != null) {
            heatmapButton.setMessage(Component.translatable(
                    viewModel.isHeatmapVisible() ? "inputtweaks.button.keyboard_on" : "inputtweaks.button.keyboard_off"
            ).withStyle(viewModel.isHeatmapVisible() ? ChatFormatting.AQUA : ChatFormatting.WHITE));
        }
        if (debugButton != null) {
            boolean dbgOn = viewModel.dispatcher().isDebugOverlayEnabled();
            debugButton.setMessage(Component.translatable(
                    dbgOn ? "inputtweaks.button.debug_on" : "inputtweaks.button.debug_off"
            ).withStyle(dbgOn ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        }
        if (packDevButton != null) {
            packDevButton.setMessage(Component.translatable(
                    viewModel.isPackDevMode() ? "inputtweaks.button.packdev_on" : "inputtweaks.button.packdev_off"
            ).withStyle(viewModel.isPackDevMode() ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        }
        if (filterButton != null) {
            filterButton.setMessage(Component.translatable(viewModel.filterMode().translationKey())
                    .withStyle(viewModel.filterMode().ordinal() != 0 ? ChatFormatting.AQUA : ChatFormatting.WHITE));
        }
        if (quickUnbindButton != null) {
            quickUnbindButton.setMessage(Component.translatable(
                    viewModel.isQuickUnbindArmed()
                            ? "inputtweaks.button.quick_unbind_armed"
                            : "inputtweaks.button.quick_unbind"
            ).withStyle(viewModel.isQuickUnbindArmed() ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
        }
        if (wizardButton != null) {
            int conflictCount = viewModel.conflictingKeyCount();
            wizardButton.setMessage(Component.translatable("inputtweaks.button.wizard", conflictCount)
                    .withStyle(conflictCount > 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
        if (undoButton != null) {
            undoButton.active = viewModel.history().canUndo();
        }
        if (redoButton != null) {
            redoButton.active = viewModel.history().canRedo();
        }
        if (resetAllButton != null) {
            boolean confirmReset = System.currentTimeMillis() < resetConfirmUntilMs;
            resetAllButton.setMessage(Component.translatable(
                    confirmReset ? "inputtweaks.button.reset_confirm" : "controls.resetAll"
            ).withStyle(confirmReset ? ChatFormatting.RED : ChatFormatting.WHITE));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (viewModel.isQuickUnbindArmed()) {
            viewModel.setQuickUnbindArmed(false);
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                KeyCode key = KeyCode.keyboard(keyCode);
                int cleared = viewModel.quickUnbindKey(key);
                viewModel.showBanner(
                        "Quick Unbind cleared " + cleared + " action(s) from " + InputKeyMapper.formatKey(key).getString(),
                        2500
                );
            }
            refreshInPlace();
            return true;
        }

        if (viewModel.capturingBindId() != null) {
            if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return true;
            String targetId = viewModel.capturingBindId();
            var committed = viewModel.captureSession().onPress(KeyCode.keyboard(keyCode));
            if (committed.isPresent()) {
                viewModel.assignChord(targetId, committed.get());
                viewModel.setCapturingBindId(null);
            } else if (!viewModel.captureSession().isActive()) {
                viewModel.setCapturingBindId(null);
            }
            refreshInPlace();
            return true;
        }

        if (!searchBox.isFocused()) {
            if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
                if (viewModel.history().undo()) refreshInPlace();
                return true;
            }
            if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
                if (viewModel.history().redo()) refreshInPlace();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (viewModel.capturingBindId() != null) {
            String targetId = viewModel.capturingBindId();
            var committed = viewModel.captureSession().onRelease(KeyCode.keyboard(keyCode));
            if (committed.isPresent()) {
                viewModel.assignChord(targetId, committed.get());
                viewModel.setCapturingBindId(null);
                refreshInPlace();
            }
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (viewModel.isQuickUnbindArmed()) {
            viewModel.setQuickUnbindArmed(false);
            KeyCode mouseKey = KeyCode.mouse(button);
            int cleared = viewModel.quickUnbindKey(mouseKey);
            viewModel.showBanner(
                    "Quick Unbind cleared " + cleared + " action(s) from " + InputKeyMapper.formatKey(mouseKey).getString(),
                    2500
            );
            refreshInPlace();
            return true;
        }

        if (viewModel.capturingBindId() != null) {
            String targetId = viewModel.capturingBindId();
            var committed = viewModel.captureSession().onPress(KeyCode.mouse(button));
            if (committed.isPresent()) {
                viewModel.assignChord(targetId, committed.get());
                viewModel.setCapturingBindId(null);
            }
            refreshInPlace();
            return true;
        }

        if (viewModel.isHeatmapVisible()) {
            boolean hit = heatmapPanel.mouseClicked(
                    mouseX, mouseY, button,
                    key -> {
                        viewModel.toggleKeyFilter(key);
                        rebuildLayoutPreservingScroll();
                    },
                    key -> {
                        int cleared = viewModel.quickUnbindKey(key);
                        viewModel.showBanner("Quick Unbind cleared " + cleared + " action(s).", 2200);
                        refreshInPlace();
                    }
            );
            if (hit) return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (viewModel.capturingBindId() != null) {
            String targetId = viewModel.capturingBindId();
            var committed = viewModel.captureSession().onRelease(KeyCode.mouse(button));
            if (committed.isPresent()) {
                viewModel.assignChord(targetId, committed.get());
                viewModel.setCapturingBindId(null);
                refreshInPlace();
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, this.width, HEADER_HEIGHT, UiPalette.BAR_BG);
        UiPalette.drawHorizontalLine(graphics, 0, this.width, HEADER_HEIGHT, UiPalette.BAR_BORDER);

        int footerTop = this.height - FOOTER_HEIGHT;
        graphics.fill(0, footerTop, this.width, this.height, UiPalette.BAR_BG);
        UiPalette.drawHorizontalLine(graphics, 0, this.width, footerTop, UiPalette.BAR_BORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (resetConfirmUntilMs > 0L && System.currentTimeMillis() >= resetConfirmUntilMs) {
            resetConfirmUntilMs = 0L;
            refreshToolbarStates();
        }

        int footerTop = this.height - FOOTER_HEIGHT;

        super.render(graphics, mouseX, mouseY, partialTick);

        int barW = Math.min(this.width - 24, 444);
        int left = this.width / 2 - barW / 2;
        graphics.drawString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), left, 7, UiPalette.TEXT_PRIMARY);

        int conflicts = viewModel.conflictingKeyCount();
        Component summary = conflicts > 0
                ? Component.translatable("inputtweaks.header.summary_conflicts", viewModel.totalBindCount(), conflicts)
                .withStyle(ChatFormatting.RED)
                : Component.translatable("inputtweaks.header.summary_clean", viewModel.totalBindCount())
                .withStyle(ChatFormatting.GREEN);
        graphics.drawString(this.font, summary, left + barW - this.font.width(summary), 7, UiPalette.TEXT_MUTED);

        if (viewModel.isHeatmapVisible()) {
            heatmapPanel.render(graphics, this.font, mouseX, mouseY, viewModel.selectedKeyFilter());
        }

        if (listWidget != null) {
            listWidget.renderDeferredTooltip(graphics);
        }

        String banner = viewModel.activeBanner();
        if (banner != null) {
            float alpha = viewModel.bannerAlpha();
            int aBg = Math.max(20, (int) (alpha * 220)) << 24;
            int aText = Math.max(40, (int) (alpha * 255)) << 24;

            int w = this.font.width(banner) + 18;
            int bx = this.width / 2 - w / 2;
            int by = footerTop - 20;
            graphics.fill(bx, by, bx + w, by + 16, aBg | 0x0014171F);
            graphics.fill(bx, by, bx + 3, by + 16, aText | (UiPalette.ACCENT_BLUE & 0x00FFFFFF));
            graphics.renderOutline(bx, by, w, 16, aText | (UiPalette.CARD_BORDER & 0x00FFFFFF));
            graphics.drawCenteredString(this.font, banner, this.width / 2 + 1, by + 4, aText | 0x00FFFFFF);
        }
    }

    @Override
    public void onClose() {
        options.save();
        InputTweaks.get().usageTracker().flushIfDirty();
        if (this.minecraft != null) {
            this.minecraft.setScreen(lastScreen);
        }
    }
}
