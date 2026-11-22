package com.niixlabs.inputtweaks.ui.overlay;

import com.niixlabs.inputtweaks.InputTweaks;
import com.niixlabs.inputtweaks.bridge.InputKeyMapper;
import com.niixlabs.inputtweaks.core.model.DebugInputEvent;
import com.niixlabs.inputtweaks.ui.theme.UiPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class DebugInputOverlay {
    private static final long EVENT_LIFETIME_MS = 4000L;

    private DebugInputOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        InputTweaks app = InputTweaks.get();
        if (app == null || !app.dispatcher().isDebugOverlayEnabled()) return;

        List<DebugInputEvent> events = app.dispatcher().recentDebugEvents();
        if (events.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int x = 8;
        int y = 8;
        int lineH = 12;
        int boxWidth = 256;
        int boxHeight = 18 + events.size() * lineH;

        UiPalette.drawFramedPanel(graphics, x - 4, y - 4, boxWidth + 8, boxHeight + 4, 0xBA10131A, UiPalette.ACCENT_BLUE);
        graphics.drawString(font, "Input Tweaks Debug", x, y, UiPalette.ACCENT_BLUE);
        UiPalette.drawHorizontalLine(graphics, x - 3, x + boxWidth + 3, y + 11, UiPalette.BAR_BORDER);

        long now = System.currentTimeMillis();
        int cy = y + 15;
        for (DebugInputEvent e : events) {
            long age = now - e.timestampMs();
            float fade = age > EVENT_LIFETIME_MS - 800L
                    ? Math.max(0.15f, (EVENT_LIFETIME_MS - age) / 800.0f)
                    : 1.0f;
            int alpha = ((int) (fade * 255)) << 24;

            String keyLabel = InputKeyMapper.formatKey(e.key()).getString();
            String line = "[" + keyLabel + "] @" + e.modId() + " " + e.bindId() + " (" + e.resultTag() + ")";
            int baseRgb = "BLOCKED".equals(e.resultTag()) ? 0x00FF5555
                    : ("CONSUMED".equals(e.resultTag()) ? 0x00FFCC55
                    : ("FIRED".equals(e.resultTag()) ? 0x0066DD88 : 0x00E0E5EE));

            graphics.drawString(font, font.plainSubstrByWidth(line, boxWidth - 2), x, cy, alpha | baseRgb);
            cy += lineH;
        }
    }
}
