package com.niixlabs.inputtweaks.ui.screen;

import com.niixlabs.inputtweaks.bridge.ItemIconResolver;
import com.niixlabs.inputtweaks.ui.theme.UiPalette;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class IconPickerScreen extends Screen {
    private static final int COLS = 10;
    private static final int CELL = 26;
    private static final int CARD_W = COLS * CELL + 34;
    private static final int VISIBLE_ROWS = 6;
    private static final int CARD_H = 74 + VISIBLE_ROWS * CELL + 34;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLL_STEP_ROWS = 3;

    private final Screen parent;
    private final String currentIconId;
    private final Consumer<String> onSelect;

    private EditBox searchBox;
    private List<ItemIconResolver.IconOption> filtered = List.of();
    private int scrollRow = 0;
    private boolean draggingScrollbar = false;

    public IconPickerScreen(Screen parent, String currentIconId, Consumer<String> onSelect) {
        super(Component.translatable("inputtweaks.radial.icon_picker.title"));
        this.parent = parent;
        this.currentIconId = currentIconId;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        int cardLeft = this.width / 2 - CARD_W / 2;
        int cardTop = this.height / 2 - CARD_H / 2;

        this.filtered = ItemIconResolver.searchCatalog("");

        this.searchBox = new EditBox(this.font, cardLeft + 12, cardTop + 30, CARD_W - 24, 18,
                Component.translatable("inputtweaks.radial.icon_picker.search"));
        this.searchBox.setHint(Component.translatable("inputtweaks.radial.icon_picker.hint"));
        this.searchBox.setResponder(q -> {
            this.filtered = ItemIconResolver.searchCatalog(q);
            this.scrollRow = 0;
            this.draggingScrollbar = false;
        });
        addRenderableWidget(this.searchBox);
        setInitialFocus(this.searchBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> onClose())
                .bounds(this.width / 2 - 50, cardTop + CARD_H - 26, 100, 18).build());
    }

    private int gridLeft() { return this.width / 2 - (COLS * CELL) / 2 - 3; }
    private int gridTop() { return this.height / 2 - CARD_H / 2 + 56; }
    private int scrollbarX() { return gridLeft() + COLS * CELL + 4; }
    private int scrollbarHeight() { return VISIBLE_ROWS * CELL - 2; }

    private int maxScrollRow() {
        int totalRows = (filtered.size() + COLS - 1) / COLS;
        return Math.max(0, totalRows - VISIBLE_ROWS);
    }

    private void updateScrollFromMouseY(double mouseY) {
        int maxScroll = maxScrollRow();
        if (maxScroll <= 0) {
            scrollRow = 0;
            return;
        }
        int gy = gridTop();
        int trackH = scrollbarHeight();
        int thumbH = Math.max(16, trackH * VISIBLE_ROWS / (maxScroll + VISIBLE_ROWS));
        int usableH = Math.max(1, trackH - thumbH);
        double ratio = (mouseY - gy - thumbH / 2.0) / usableH;
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        scrollRow = (int) Math.round(ratio * maxScroll);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int cardLeft = this.width / 2 - CARD_W / 2;
        int cardTop = this.height / 2 - CARD_H / 2;
        UiPalette.drawFramedPanel(graphics, cardLeft, cardTop, CARD_W, CARD_H, UiPalette.CARD_BG, UiPalette.CARD_BORDER);
        UiPalette.drawHorizontalLine(graphics, cardLeft + 1, cardLeft + CARD_W - 1, cardTop + 22, UiPalette.BAR_BORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int cardTop = this.height / 2 - CARD_H / 2;
        graphics.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD),
                this.width / 2, cardTop + 8, UiPalette.TEXT_PRIMARY);

        int gx = gridLeft();
        int gy = gridTop();
        ItemIconResolver.IconOption hovered = null;

        int startIdx = scrollRow * COLS;
        int endIdx = Math.min(filtered.size(), startIdx + VISIBLE_ROWS * COLS);

        for (int i = startIdx; i < endIdx; i++) {
            int rel = i - startIdx;
            int col = rel % COLS;
            int row = rel / COLS;
            int x = gx + col * CELL;
            int y = gy + row * CELL;

            ItemIconResolver.IconOption opt = filtered.get(i);
            boolean isHovered = mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2;
            boolean isCurrent = opt.itemId().equals(currentIconId);
            if (isHovered) hovered = opt;

            int bg = isHovered ? 0xCC2B5E94 : (isCurrent ? 0xAA1F3A56 : 0x8810131A);
            int border = isCurrent ? UiPalette.ACCENT_BLUE : (isHovered ? 0xFFFFFFFF : 0xFF2B3240);
            UiPalette.drawFramedPanel(graphics, x, y, CELL - 2, CELL - 2, bg, border);
            ItemIconResolver.renderSafe(graphics, opt.itemId(), opt.stack(), x + 4, y + 4);
        }

        int maxScroll = maxScrollRow();
        if (maxScroll > 0) {
            int trackX = scrollbarX();
            int trackH = scrollbarHeight();
            graphics.fill(trackX, gy, trackX + SCROLLBAR_WIDTH, gy + trackH, 0x66000000);
            int thumbH = Math.max(16, trackH * VISIBLE_ROWS / (maxScroll + VISIBLE_ROWS));
            int thumbY = gy + (trackH - thumbH) * scrollRow / maxScroll;
            boolean hoverBar = draggingScrollbar || (mouseX >= trackX - 2 && mouseX <= trackX + SCROLLBAR_WIDTH + 2
                    && mouseY >= gy && mouseY <= gy + trackH);
            int thumbColor = hoverBar ? 0xFF66B2FF : UiPalette.ACCENT_BLUE;
            graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH, thumbColor);
        }

        if (hovered != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(hovered.displayName()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            tip.add(Component.literal(hovered.itemId()).withStyle(ChatFormatting.DARK_GRAY));
            if (ItemIconResolver.isRenderBlocked(hovered.itemId())) {
                tip.add(Component.literal("Preview unavailable outside a world").withStyle(ChatFormatting.YELLOW));
            }
            graphics.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int maxScroll = maxScrollRow();
            if (maxScroll > 0) {
                int trackX = scrollbarX();
                int gy = gridTop();
                if (mouseX >= trackX - 2 && mouseX <= trackX + SCROLLBAR_WIDTH + 2
                        && mouseY >= gy && mouseY <= gy + scrollbarHeight()) {
                    draggingScrollbar = true;
                    updateScrollFromMouseY(mouseY);
                    return true;
                }
            }

            int gx = gridLeft();
            int gy = gridTop();
            int startIdx = scrollRow * COLS;
            int endIdx = Math.min(filtered.size(), startIdx + VISIBLE_ROWS * COLS);

            for (int i = startIdx; i < endIdx; i++) {
                int rel = i - startIdx;
                int x = gx + (rel % COLS) * CELL;
                int y = gy + (rel / COLS) * CELL;
                if (mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2) {
                    onSelect.accept(filtered.get(i).itemId());
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            updateScrollFromMouseY(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = maxScrollRow();
        if (maxScroll > 0) {
            int step = Screen.hasShiftDown() ? 8 : SCROLL_STEP_ROWS;
            int rows = (int) Math.round(Math.abs(scrollY) * step);
            if (rows < 1) rows = 1;
            scrollRow = Math.max(0, Math.min(maxScroll, scrollRow + (scrollY > 0 ? -rows : rows)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}