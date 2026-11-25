package com.niixlabs.inputtweaks.ui.widget;

import com.niixlabs.inputtweaks.bridge.InputKeyMapper;
import com.niixlabs.inputtweaks.core.conflict.ConflictDetector;
import com.niixlabs.inputtweaks.core.conflict.KeyHeatStatus;
import com.niixlabs.inputtweaks.core.model.KeyCode;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;
import com.niixlabs.inputtweaks.ui.theme.UiPalette;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class KeyboardHeatmapPanel {
    private record KeyTile(String label, KeyCode key, int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private static final Object[][] KEYBOARD_ROWS = {
        { "`", GLFW.GLFW_KEY_GRAVE_ACCENT, "1", GLFW.GLFW_KEY_1, "2", GLFW.GLFW_KEY_2, "3", GLFW.GLFW_KEY_3,
          "4", GLFW.GLFW_KEY_4, "5", GLFW.GLFW_KEY_5, "6", GLFW.GLFW_KEY_6, "7", GLFW.GLFW_KEY_7,
          "8", GLFW.GLFW_KEY_8, "9", GLFW.GLFW_KEY_9, "0", GLFW.GLFW_KEY_0, "-", GLFW.GLFW_KEY_MINUS,
          "=", GLFW.GLFW_KEY_EQUAL, "M1", -101, "M2", -102 },
        { "TAB", GLFW.GLFW_KEY_TAB, "Q", GLFW.GLFW_KEY_Q, "W", GLFW.GLFW_KEY_W, "E", GLFW.GLFW_KEY_E,
          "R", GLFW.GLFW_KEY_R, "T", GLFW.GLFW_KEY_T, "Y", GLFW.GLFW_KEY_Y, "U", GLFW.GLFW_KEY_U,
          "I", GLFW.GLFW_KEY_I, "O", GLFW.GLFW_KEY_O, "P", GLFW.GLFW_KEY_P, "[", GLFW.GLFW_KEY_LEFT_BRACKET,
          "]", GLFW.GLFW_KEY_RIGHT_BRACKET, "M3", -103 },
        { "CAPS", GLFW.GLFW_KEY_CAPS_LOCK, "A", GLFW.GLFW_KEY_A, "S", GLFW.GLFW_KEY_S, "D", GLFW.GLFW_KEY_D,
          "F", GLFW.GLFW_KEY_F, "G", GLFW.GLFW_KEY_G, "H", GLFW.GLFW_KEY_H, "J", GLFW.GLFW_KEY_J,
          "K", GLFW.GLFW_KEY_K, "L", GLFW.GLFW_KEY_L, ";", GLFW.GLFW_KEY_SEMICOLON, "'", GLFW.GLFW_KEY_APOSTROPHE,
          "M4", -104 },
        { "SHIFT", GLFW.GLFW_KEY_LEFT_SHIFT, "Z", GLFW.GLFW_KEY_Z, "X", GLFW.GLFW_KEY_X, "C", GLFW.GLFW_KEY_C,
          "V", GLFW.GLFW_KEY_V, "B", GLFW.GLFW_KEY_B, "N", GLFW.GLFW_KEY_N, "M", GLFW.GLFW_KEY_M,
          ",", GLFW.GLFW_KEY_COMMA, ".", GLFW.GLFW_KEY_PERIOD, "/", GLFW.GLFW_KEY_SLASH,
          "CTRL", GLFW.GLFW_KEY_LEFT_CONTROL, "ALT", GLFW.GLFW_KEY_LEFT_ALT, "SPC", GLFW.GLFW_KEY_SPACE, "M5", -105 }
    };

    private final List<KeyTile> tiles = new ArrayList<>();
    private final ConflictDetector conflictDetector;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public KeyboardHeatmapPanel(ConflictDetector conflictDetector) {
        this.conflictDetector = conflictDetector;
    }

    public void layout(int centerX, int topY) {
        tiles.clear();
        int keyW = 21;
        int keyH = 16;
        int gap = 2;

        int maxRowWidth = 0;
        int[] rowWidths = new int[KEYBOARD_ROWS.length];
        for (int r = 0; r < KEYBOARD_ROWS.length; r++) {
            Object[] row = KEYBOARD_ROWS[r];
            int count = row.length / 2;
            int w = 0;
            for (int i = 0; i < count; i++) {
                String label = (String) row[i * 2];
                w += widthFor(label, keyW) + gap;
                if (label.startsWith("M") && i > 0 && !((String) row[(i - 1) * 2]).startsWith("M")) {
                    w += 6;
                }
            }
            rowWidths[r] = Math.max(0, w - gap);
            maxRowWidth = Math.max(maxRowWidth, rowWidths[r]);
        }

        this.panelW = maxRowWidth + 16;
        this.panelH = height();
        this.panelX = centerX - panelW / 2;
        this.panelY = topY;

        for (int r = 0; r < KEYBOARD_ROWS.length; r++) {
            Object[] row = KEYBOARD_ROWS[r];
            int count = row.length / 2;
            int x = centerX - rowWidths[r] / 2;
            int y = topY + 6 + r * (keyH + gap);

            for (int i = 0; i < count; i++) {
                String label = (String) row[i * 2];
                if (label.startsWith("M") && i > 0 && !((String) row[(i - 1) * 2]).startsWith("M")) {
                    x += 6;
                }
                int rawCode = (int) row[i * 2 + 1];
                KeyCode key = rawCode < 0 ? KeyCode.mouse(-(rawCode + 100) - 1) : KeyCode.keyboard(rawCode);
                int w = widthFor(label, keyW);
                tiles.add(new KeyTile(label, key, x, y, w, keyH));
                x += w + gap;
            }
        }
    }

    private int widthFor(String label, int base) {
        if ("SHIFT".equals(label)) return base + 12;
        if ("CTRL".equals(label) || "CAPS".equals(label)) return base + 8;
        if (label.length() == 3) return base + 5;
        return base;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, KeyCode selectedFilter) {
        UiPalette.drawFramedPanel(graphics, panelX, panelY, panelW, panelH, UiPalette.CARD_BG, UiPalette.CARD_BORDER);

        KeyTile hoveredTile = null;
        for (KeyTile tile : tiles) {
            KeyHeatStatus status = conflictDetector.getKeyStatus(tile.key());
            boolean hovered = tile.contains(mouseX, mouseY);
            boolean selected = tile.key().equals(selectedFilter);
            if (hovered) hoveredTile = tile;

            graphics.fill(tile.x(), tile.y(), tile.x() + tile.w(), tile.y() + tile.h(), status.fillColor());
            int border = selected ? 0xFFFFFFFF : (hovered ? 0xFFE0E0E0 : 0xFF0E1015);
            graphics.renderOutline(tile.x(), tile.y(), tile.w(), tile.h(), border);

            if (conflictDetector.isUsedAsModifier(tile.key())) {
                graphics.fill(tile.x() + tile.w() - 4, tile.y() + 2, tile.x() + tile.w() - 2, tile.y() + 4, 0xFF36CFC9);
            }

            graphics.drawCenteredString(
                font, tile.label(),
                tile.x() + tile.w() / 2,
                tile.y() + (tile.h() - font.lineHeight) / 2 + 1,
                0xFFFFFFFF
            );
        }

        renderLegend(graphics, font);

        if (hoveredTile != null) {
            List<ResolvedBind> binds = conflictDetector.getBindsForKey(hoveredTile.key());
            List<ResolvedBind> modCombos = conflictDetector.getModifierCombos(hoveredTile.key());
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(hoveredTile.label() + " (" + binds.size() + ")").withStyle(ChatFormatting.BOLD));
            for (int i = 0; i < Math.min(binds.size(), 6); i++) {
                ResolvedBind b = binds.get(i);
                var line = Component.literal(" • ").append(Component.translatable(b.action().translationKey()));
                if (b.chord().hasModifiers()) {
                    line.append(Component.literal(" [" + InputKeyMapper.formatChordCompact(b.chord()).getString() + "]")
                        .withStyle(ChatFormatting.AQUA));
                }
                lines.add(line);
            }
            if (binds.size() > 6) {
                lines.add(Component.literal(" + " + (binds.size() - 6) + " more...").withStyle(ChatFormatting.GRAY));
            }
            if (!modCombos.isEmpty()) {
                lines.add(Component.literal("Combo modifier for " + modCombos.size() + " action(s):")
                    .withStyle(ChatFormatting.AQUA));
                for (int i = 0; i < Math.min(modCombos.size(), 4); i++) {
                    ResolvedBind b = modCombos.get(i);
                    lines.add(Component.literal(" + " + InputKeyMapper.formatChordCompact(b.chord()).getString() + " -> ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable(b.action().translationKey()).withStyle(ChatFormatting.WHITE)));
                }
            }
            lines.add(Component.translatable("inputtweaks.heatmap.hint").withStyle(ChatFormatting.DARK_GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void renderLegend(GuiGraphics graphics, Font font) {
        int ly = panelY + panelH - 12;
        int lx = panelX + 8;

        lx = drawLegendItem(graphics, font, lx, ly, KeyHeatStatus.FREE.fillColor(), "Free");
        lx = drawLegendItem(graphics, font, lx, ly, KeyHeatStatus.BOUND.fillColor(), "Bound");
        lx = drawLegendItem(graphics, font, lx, ly, KeyHeatStatus.MULTI_CONTEXT.fillColor(), "Multi");
        drawLegendItem(graphics, font, lx, ly, KeyHeatStatus.CONFLICT.fillColor(), "Conflict");

        String hint = "LMB: Filter  |  RMB: Quick Unbind";
        int hintW = font.width(hint);
        graphics.drawString(font, hint, panelX + panelW - hintW - 8, ly, UiPalette.TEXT_MUTED);
    }

    private int drawLegendItem(GuiGraphics graphics, Font font, int x, int y, int color, String label) {
        graphics.fill(x, y + 1, x + 7, y + 8, color);
        graphics.renderOutline(x, y + 1, 7, 7, 0xFF0E1015);
        graphics.drawString(font, label, x + 10, y, UiPalette.TEXT_MUTED);
        return x + 10 + font.width(label) + 10;
    }

    public boolean mouseClicked(
        double mouseX,
        double mouseY,
        int button,
        Consumer<KeyCode> onSelectFilter,
        Consumer<KeyCode> onQuickUnbind
    ) {
        for (KeyTile tile : tiles) {
            if (tile.contains(mouseX, mouseY)) {
                if (button == 1) {
                    onQuickUnbind.accept(tile.key());
                } else if (button == 0) {
                    onSelectFilter.accept(tile.key());
                }
                return true;
            }
        }
        return false;
    }

    public int height() {
        return 94;
    }
}
