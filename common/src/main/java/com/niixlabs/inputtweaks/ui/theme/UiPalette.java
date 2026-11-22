package com.niixlabs.inputtweaks.ui.theme;

import net.minecraft.client.gui.GuiGraphics;

public final class UiPalette {
    public static final int BAR_BG = 0xCC111318;
    public static final int BAR_BORDER = 0xFF343B48;
    public static final int CARD_BG = 0xD814171F;
    public static final int CARD_BORDER = 0xFF3E4756;

    public static final int CATEGORY_BG = 0x550C0E13;
    public static final int CATEGORY_LINE = 0x665A6475;
    public static final int ROW_EVEN = 0x1A000000;
    public static final int ROW_ODD = 0x0A000000;
    public static final int ROW_HOVER = 0x24FFFFFF;
    public static final int ROW_CONFLICT_BG = 0x2EA82020;
    public static final int ROW_CAPTURE_BG = 0x38D4A017;
    public static final int ROW_CAPTURE_BORDER = 0xFFFFD24C;

    public static final int ACCENT_BLUE = 0xFF409EFF;
    public static final int ACCENT_GOLD = 0xFFE6A23C;
    public static final int ACCENT_GREEN = 0xFF55CC77;
    public static final int ACCENT_RED = 0xFFFF5555;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_MUTED = 0xFFA8B0BC;
    public static final int TEXT_DISABLED = 0xFF7D5555;
    public static final int TEXT_HIDDEN = 0xFFA6A672;

    private UiPalette() {
    }

    public static void drawFramedPanel(GuiGraphics graphics, int x, int y, int width, int height, int bg, int border) {
        graphics.fill(x, y, x + width, y + height, bg);
        graphics.renderOutline(x, y, width, height, border);
    }

    public static void drawHorizontalLine(GuiGraphics graphics, int x1, int x2, int y, int color) {
        if (x2 > x1) {
            graphics.fill(x1, y, x2, y + 1, color);
        }
    }
}
