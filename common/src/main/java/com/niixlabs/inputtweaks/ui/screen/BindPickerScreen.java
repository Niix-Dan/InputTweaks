package com.niixlabs.inputtweaks.ui.screen;

import com.niixlabs.inputtweaks.InputTweaks;
import com.niixlabs.inputtweaks.bridge.InputKeyMapper;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;
import com.niixlabs.inputtweaks.ui.theme.UiPalette;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public final class BindPickerScreen extends Screen {
    private final Screen parent;
    private final BiConsumer<String, String> onSelect;

    private EditBox searchBox;
    private ActionList listWidget;

    public BindPickerScreen(Screen parent, BiConsumer<String, String> onSelect) {
        super(Component.translatable("inputtweaks.radial.bind_picker.title"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int w = Math.min(this.width - 30, 340);

        this.searchBox = new EditBox(this.font, cx - w / 2, 26, w, 18,
                Component.translatable("inputtweaks.radial.bind_picker.search"));
        this.searchBox.setHint(Component.translatable("inputtweaks.radial.bind_picker.hint"));
        this.searchBox.setResponder(q -> {
            if (listWidget != null) listWidget.refresh(q);
        });
        addRenderableWidget(this.searchBox);
        setInitialFocus(this.searchBox);

        this.listWidget = new ActionList(this.minecraft, this.width, Math.max(40, this.height - 84), 50, w);
        this.listWidget.refresh("");
        addRenderableWidget(this.listWidget);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> onClose())
                .bounds(cx - 55, this.height - 26, 110, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        graphics.fill(0, 0, this.width, 48, UiPalette.BAR_BG);
        UiPalette.drawHorizontalLine(graphics, 0, this.width, 48, UiPalette.BAR_BORDER);
        graphics.fill(0, this.height - 32, this.width, this.height, UiPalette.BAR_BG);
        UiPalette.drawHorizontalLine(graphics, 0, this.width, this.height - 32, UiPalette.BAR_BORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD),
                this.width / 2, 10, UiPalette.TEXT_PRIMARY);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private final class ActionList extends ObjectSelectionList<ActionList.Entry> {
        private final int rowW;

        ActionList(Minecraft mc, int width, int height, int topY, int rowW) {
            super(mc, width, topY + height, topY, topY + height, 22);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.rowW = rowW;
        }

        void refresh(String rawQuery) {
            clearEntries();
            String q = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);

            List<ResolvedBind> matches = InputTweaks.get().profileManager().allResolved().stream()
                    .filter(b -> !b.id().startsWith("inputtweaks.radial."))
                    .filter(b -> matchesQuery(b, q))
                    .sorted(Comparator.comparing(b -> I18n.get(b.action().translationKey())))
                    .toList();

            for (ResolvedBind b : matches) {
                addEntry(new Entry(b));
            }
            setScrollAmount(0.0);
        }

        private boolean matchesQuery(ResolvedBind b, String q) {
            if (q.isEmpty()) return true;
            if (q.startsWith("@")) {
                return b.action().modId().toLowerCase(Locale.ROOT).contains(q.substring(1));
            }
            String name = I18n.get(b.action().translationKey()).toLowerCase(Locale.ROOT);
            String cat = I18n.get(b.category()).toLowerCase(Locale.ROOT);
            return name.contains(q)
                    || cat.contains(q)
                    || b.action().modId().toLowerCase(Locale.ROOT).contains(q)
                    || b.id().toLowerCase(Locale.ROOT).contains(q);
        }

        @Override
        public int getRowWidth() {
            return rowW;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + rowW / 2 + 6;
        }

        final class Entry extends ObjectSelectionList.Entry<Entry> {
            private final ResolvedBind bind;
            private final String displayName;
            private final String keyText;

            Entry(ResolvedBind bind) {
                this.bind = bind;
                this.displayName = I18n.get(bind.action().translationKey());
                this.keyText = InputKeyMapper.formatChord(bind.chord()).getString();
            }

            @Override
            public void render(
                    GuiGraphics graphics, int index, int top, int left, int width, int height,
                    int mouseX, int mouseY, boolean hovering, float partialTick
            ) {
                int bg = hovering ? 0x44409EFF : (index % 2 == 0 ? UiPalette.ROW_EVEN : UiPalette.ROW_ODD);
                graphics.fill(left, top + 1, left + width, top + height - 1, bg);

                String rightTag = "@" + bind.action().modId() + "  [" + keyText + "]";
                int rightW = font.width(rightTag);
                int maxLeft = Math.max(40, width - rightW - 14);
                String nameStr = font.plainSubstrByWidth(displayName, maxLeft);

                graphics.drawString(font, nameStr, left + 6, top + 6, UiPalette.TEXT_PRIMARY);
                graphics.drawString(font, rightTag, left + width - rightW - 6, top + 6, UiPalette.TEXT_MUTED);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    onSelect.accept(bind.id(), displayName);
                    onClose();
                    return true;
                }
                return false;
            }

            @Override
            public Component getNarration() {
                return Component.literal(displayName);
            }
        }
    }
}