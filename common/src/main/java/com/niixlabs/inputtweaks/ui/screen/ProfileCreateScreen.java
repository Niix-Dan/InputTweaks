package com.niixlabs.inputtweaks.ui.screen;

import com.niixlabs.inputtweaks.core.profile.ProfileManager;
import com.niixlabs.inputtweaks.ui.state.ControlsViewModel;
import com.niixlabs.inputtweaks.ui.theme.UiPalette;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ProfileCreateScreen extends Screen {
    private static final int CARD_W = 264;
    private static final int CARD_H = 216;

    private final Screen parent;
    private final ControlsViewModel viewModel;
    private EditBox nameBox;
    private EditBox shareCodeBox;

    public ProfileCreateScreen(Screen parent, ControlsViewModel viewModel) {
        super(Component.translatable("inputtweaks.screen.profiles.title"));
        this.parent = parent;
        this.viewModel = viewModel;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int cardLeft = cx - CARD_W / 2;
        int cardTop = cy - CARD_H / 2;
        int innerW = CARD_W - 24;
        int x = cardLeft + 12;

        int row1Y = cardTop + 34;
        String active = viewModel.profileManager().activeProfileName();
        List<String> all = viewModel.profileManager().profileNames();

        addRenderableWidget(Button.builder(
                Component.translatable("inputtweaks.profile.active_row", active, all.size()),
                btn -> {
                    viewModel.profileManager().cycleProfile();
                    this.rebuildWidgets();
                }
        ).bounds(x, row1Y, innerW - 58, 18).build());

        Button deleteBtn = addRenderableWidget(Button.builder(
                Component.translatable("inputtweaks.profile.delete").withStyle(ChatFormatting.RED),
                btn -> {
                    if (viewModel.profileManager().deleteProfile(active)) {
                        viewModel.showBanner("Deleted profile: " + active, 2200);
                        this.rebuildWidgets();
                    }
                }
        ).bounds(x + innerW - 54, row1Y, 54, 18).build());
        deleteBtn.active = !ProfileManager.DEFAULT_PROFILE.equals(active);

        int createY = cardTop + 70;
        this.nameBox = new EditBox(this.font, x, createY, innerW, 18, Component.translatable("inputtweaks.profile.name"));
        this.nameBox.setHint(Component.translatable("inputtweaks.profile.name_hint"));
        addRenderableWidget(this.nameBox);

        int half = (innerW - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("inputtweaks.profile.create_copy"), btn -> {
            if (viewModel.profileManager().createProfile(nameBox.getValue(), true)) {
                viewModel.showBanner("Created profile: " + nameBox.getValue().trim(), 2500);
                onClose();
            }
        }).bounds(x, createY + 22, half, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("inputtweaks.profile.create_blank"), btn -> {
            if (viewModel.profileManager().createProfile(nameBox.getValue(), false)) {
                viewModel.showBanner("Created blank profile: " + nameBox.getValue().trim(), 2500);
                onClose();
            }
        }).bounds(x + half + 4, createY + 22, innerW - half - 4, 18).build());

        int shareY = cardTop + 130;
        this.shareCodeBox = new EditBox(this.font, x, shareY, innerW, 18, Component.translatable("inputtweaks.profile.share_code"));
        this.shareCodeBox.setMaxLength(16384);
        this.shareCodeBox.setHint(Component.translatable("inputtweaks.profile.share_hint"));
        addRenderableWidget(this.shareCodeBox);

        addRenderableWidget(Button.builder(Component.translatable("inputtweaks.profile.copy_share"), btn -> {
            String code = viewModel.exportActiveShareCode();
            if (this.minecraft != null) {
                this.minecraft.keyboardHandler.setClipboard(code);
                viewModel.showBanner("Share code copied to clipboard!", 2500);
                onClose();
            }
        }).bounds(x, shareY + 22, half, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("inputtweaks.profile.import_share"), btn -> {
            if (viewModel.importShareCode(shareCodeBox.getValue())) {
                viewModel.showBanner("Imported profile successfully!", 2500);
                onClose();
            }
        }).bounds(x + half + 4, shareY + 22, innerW - half - 4, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose())
                .bounds(cx - 56, cardTop + CARD_H - 28, 112, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        int cardLeft = this.width / 2 - CARD_W / 2;
        int cardTop = this.height / 2 - CARD_H / 2;

        UiPalette.drawFramedPanel(graphics, cardLeft, cardTop, CARD_W, CARD_H, UiPalette.CARD_BG, UiPalette.CARD_BORDER);
        UiPalette.drawHorizontalLine(graphics, cardLeft + 1, cardLeft + CARD_W - 1, cardTop + 22, UiPalette.BAR_BORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cardLeft = this.width / 2 - CARD_W / 2;
        int cardTop = this.height / 2 - CARD_H / 2;

        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), this.width / 2, cardTop + 8, UiPalette.TEXT_PRIMARY);
        graphics.drawString(this.font, Component.translatable("inputtweaks.profile.section_create"), cardLeft + 12, cardTop + 59, UiPalette.TEXT_MUTED);
        graphics.drawString(this.font, Component.translatable("inputtweaks.profile.section_share"), cardLeft + 12, cardTop + 119, UiPalette.TEXT_MUTED);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
