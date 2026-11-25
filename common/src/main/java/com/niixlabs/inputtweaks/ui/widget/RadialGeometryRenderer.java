package com.niixlabs.inputtweaks.ui.widget;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.niixlabs.inputtweaks.bridge.ItemIconResolver;
import com.niixlabs.inputtweaks.core.model.RadialSlot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.List;

public final class RadialGeometryRenderer {
    public static final int[] ACCENT_PALETTE = {
            0xFF409EFF,
            0xFF36CFC9,
            0xFF55CC77,
            0xFFE6A23C,
            0xFFFF7A45,
            0xFFFF5555,
            0xFFB37FEB,
            0xFFF759AB
    };

    private RadialGeometryRenderer() {
    }

    public static void renderWheel(
            GuiGraphics graphics,
            Font font,
            float cx,
            float cy,
            float innerRadius,
            float outerRadius,
            float openScale,
            float globalAlpha,
            List<RadialSlot> slots,
            int hoveredIndex,
            int selectedIndex,
            float[] slotHoverFactors,
            boolean showPointer,
            float pointerAngleRad,
            int pointerColor,
            String centerTitle,
            String centerSubtitle,
            ItemStack centerIcon
    ) {
        if (globalAlpha <= 0.01f) return;

        float rIn = innerRadius * openScale;
        float rOut = outerRadius * openScale;
        int count = slots.size();

        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer vc = graphics.bufferSource().getBuffer(RenderType.gui());

        drawAnnulus(vc, matrix, cx, cy, rIn - 2.0f, rOut + 5.0f, 0.0f, (float) (Math.PI * 2.0), 48,
                scaleAlpha(0x55080A0F, globalAlpha));

        if (count == 0) {
            drawAnnulus(vc, matrix, cx, cy, rIn, rOut, 0.0f, (float) (Math.PI * 2.0), 48,
                    scaleAlpha(0xBB151821, globalAlpha));
            drawAnnulus(vc, matrix, cx, cy, rOut - 1.5f, rOut, 0.0f, (float) (Math.PI * 2.0), 48,
                    scaleAlpha(0xFF3E4756, globalAlpha));
        } else {
            float fullCircle = (float) (Math.PI * 2.0);
            float step = fullCircle / count;
            float gap = count == 1 ? 0.0f : Math.min(0.045f, step * 0.09f);

            for (int i = 0; i < count; i++) {
                RadialSlot slot = slots.get(i);
                float hoverAnim = (slotHoverFactors != null && i < slotHoverFactors.length)
                        ? slotHoverFactors[i]
                        : (i == hoveredIndex ? 1.0f : 0.0f);
                boolean isSelected = (i == selectedIndex);
                float focus = Math.max(hoverAnim, isSelected ? 0.85f : 0.0f);

                float centerAngle = (float) (-Math.PI / 2.0 + i * step);
                float startA = centerAngle - step / 2.0f + gap / 2.0f;
                float endA = centerAngle + step / 2.0f - gap / 2.0f;

                float wedgeOut = rOut + focus * 7.0f * openScale;
                float wedgeIn = rIn + (1.0f - focus);
                int segments = Math.max(8, Math.round(52.0f / count));

                int baseFill = lerpArgb(0xCC141822, tintDark(slot.accentColor(), 0.58f, 0xE6), focus);
                drawAnnulus(vc, matrix, cx, cy, wedgeIn, wedgeOut, startA, endA, segments,
                        scaleAlpha(baseFill, globalAlpha));

                int innerRim = lerpArgb(0x663A4354, 0xCCFFFFFF, focus * 0.6f);
                drawAnnulus(vc, matrix, cx, cy, wedgeIn, wedgeIn + 1.4f, startA, endA, segments,
                        scaleAlpha(innerRim, globalAlpha));

                float rimThickness = 2.0f + focus * 2.4f;
                int outerRim = lerpArgb(tintDark(slot.accentColor(), 0.65f, 0xAA), slot.accentColor(), focus);
                drawAnnulus(vc, matrix, cx, cy, wedgeOut - rimThickness, wedgeOut, startA, endA, segments,
                        scaleAlpha(outerRim, globalAlpha));

                if (isSelected) {
                    drawAnnulus(vc, matrix, cx, cy, wedgeOut, wedgeOut + 1.5f, startA, endA, segments,
                            scaleAlpha(0xFFFFFFFF, globalAlpha));
                }
            }
        }

        float hubR = Math.max(12.0f, rIn - 4.0f);
        drawDisc(vc, matrix, cx, cy, hubR, 36, scaleAlpha(0xE010131B, globalAlpha));
        int hubBorder = hoveredIndex >= 0 ? pointerColor : 0xFF455066;
        drawAnnulus(vc, matrix, cx, cy, hubR - 1.6f, hubR, 0.0f, (float) (Math.PI * 2.0), 36,
                scaleAlpha(hubBorder, globalAlpha));

        if (showPointer) {
            float tipR = hubR + 9.5f * openScale;
            float baseR = hubR + 1.5f * openScale;
            drawPointerArrow(vc, matrix, cx, cy, pointerAngleRad, baseR, tipR, 0.24f,
                    scaleAlpha(pointerColor, globalAlpha));
            drawPointerArrow(vc, matrix, cx, cy, pointerAngleRad, baseR + 1.6f, tipR - 2.0f, 0.13f,
                    scaleAlpha(0xFFFFFFFF, globalAlpha));
        }

        graphics.flush();

        if (count > 0 && openScale > 0.72f) {
            float fullCircle = (float) (Math.PI * 2.0);
            float step = fullCircle / count;
            boolean compact = (outerRadius - innerRadius) < 42.0f;

            for (int i = 0; i < count; i++) {
                RadialSlot slot = slots.get(i);
                float hoverAnim = (slotHoverFactors != null && i < slotHoverFactors.length)
                        ? slotHoverFactors[i]
                        : (i == hoveredIndex ? 1.0f : 0.0f);
                float angle = (float) (-Math.PI / 2.0 + i * step);
                float midR = (rIn + rOut) * 0.5f + hoverAnim * 3.5f * openScale;

                int ix = Math.round(cx + (float) Math.cos(angle) * midR);
                int iy = Math.round(cy + (float) Math.sin(angle) * midR);

                ItemStack stack = ItemIconResolver.resolve(slot.iconItemId());
                int iconY = compact ? (iy - 8) : (iy - 12);
                ItemIconResolver.renderSafe(graphics, slot.iconItemId(), stack, ix - 8, iconY);

                if (!compact) {
                    String shortLabel = font.plainSubstrByWidth(slot.label(), count > 8 ? 44 : 58);
                    int textColor = scaleAlpha(i == hoveredIndex || i == selectedIndex ? 0xFFFFFFFF : 0xFFD5DCE8, globalAlpha);
                    graphics.drawCenteredString(font, shortLabel, ix, iy + 6, textColor);
                }
            }
        }

        if (openScale > 0.72f) {
            int maxHubTextW = Math.max(24, Math.round(hubR * 1.75f));
            if (centerIcon != null && !centerIcon.isEmpty() && hubR >= 26.0f) {
                int focusIdx = hoveredIndex >= 0 ? hoveredIndex : selectedIndex;
                String centerIconId = (focusIdx >= 0 && focusIdx < count)
                        ? slots.get(focusIdx).iconItemId()
                        : BuiltInRegistries.ITEM.getKey(centerIcon.getItem()).toString();
                ItemIconResolver.renderSafe(graphics, centerIconId, centerIcon, Math.round(cx) - 8, Math.round(cy) - 16);
                if (centerTitle != null && !centerTitle.isEmpty()) {
                    String clipped = font.plainSubstrByWidth(centerTitle, maxHubTextW);
                    graphics.drawCenteredString(font, clipped, Math.round(cx), Math.round(cy) + 3,
                            scaleAlpha(0xFFFFFFFF, globalAlpha));
                }
                if (centerSubtitle != null && !centerSubtitle.isEmpty() && hubR >= 32.0f) {
                    String sub = font.plainSubstrByWidth(centerSubtitle, maxHubTextW);
                    graphics.drawCenteredString(font, sub, Math.round(cx), Math.round(cy) + 14,
                            scaleAlpha(0xFF9AA5B8, globalAlpha));
                }
            } else if (centerTitle != null && !centerTitle.isEmpty()) {
                String clipped = font.plainSubstrByWidth(centerTitle, maxHubTextW);
                graphics.drawCenteredString(font, clipped, Math.round(cx), Math.round(cy) - 4,
                        scaleAlpha(0xFFFFFFFF, globalAlpha));
            }
        }
    }

    public static int hitTestSector(float cx, float cy, double mx, double my, float deadzoneRadius, int count) {
        if (count <= 0) return -1;
        double dx = mx - cx;
        double dy = my - cy;
        if (dx * dx + dy * dy < deadzoneRadius * deadzoneRadius) return -1;

        double angle = Math.atan2(dy, dx) + Math.PI / 2.0;
        if (angle < 0) angle += Math.PI * 2.0;
        double step = (Math.PI * 2.0) / count;
        int idx = (int) Math.floor(((angle + step / 2.0) % (Math.PI * 2.0)) / step);
        return Math.max(0, Math.min(count - 1, idx));
    }

    public static float lerpAngle(float current, float target, float t) {
        float diff = (float) ((target - current) % (Math.PI * 2.0));
        if (diff > Math.PI) diff -= (float) (Math.PI * 2.0);
        if (diff < -Math.PI) diff += (float) (Math.PI * 2.0);
        return current + diff * t;
    }

    private static void drawAnnulus(
            VertexConsumer vc, Matrix4f m,
            float cx, float cy, float rInner, float rOuter,
            float startAngle, float endAngle, int segments, int argb
    ) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a <= 0 || rOuter <= rInner || endAngle <= startAngle) return;

        float step = (endAngle - startAngle) / segments;
        for (int i = 0; i < segments; i++) {
            float a0 = startAngle + i * step;
            float a1 = a0 + step;

            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);

            vc.vertex(m, cx + c0 * rOuter, cy + s0 * rOuter, 0).color(r, g, b, a).endVertex();
            vc.vertex(m, cx + c0 * rInner, cy + s0 * rInner, 0).color(r, g, b, a).endVertex();
            vc.vertex(m, cx + c1 * rInner, cy + s1 * rInner, 0).color(r, g, b, a).endVertex();
            vc.vertex(m, cx + c1 * rOuter, cy + s1 * rOuter, 0).color(r, g, b, a).endVertex();
        }
    }

    private static void drawDisc(
            VertexConsumer vc, Matrix4f m,
            float cx, float cy, float radius, int segments, int argb
    ) {
        drawAnnulus(vc, m, cx, cy, 0.0f, radius, 0.0f, (float) (Math.PI * 2.0), segments, argb);
    }

    private static void drawPointerArrow(
            VertexConsumer vc, Matrix4f m,
            float cx, float cy, float angle, float baseR, float tipR, float halfSpreadRad, int argb
    ) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a <= 0) return;

        float tipX = cx + (float) Math.cos(angle) * tipR;
        float tipY = cy + (float) Math.sin(angle) * tipR;

        float leftX = cx + (float) Math.cos(angle - halfSpreadRad) * baseR;
        float leftY = cy + (float) Math.sin(angle - halfSpreadRad) * baseR;

        float notchR = baseR + (tipR - baseR) * 0.25f;
        float notchX = cx + (float) Math.cos(angle) * notchR;
        float notchY = cy + (float) Math.sin(angle) * notchR;

        float rightX = cx + (float) Math.cos(angle + halfSpreadRad) * baseR;
        float rightY = cy + (float) Math.sin(angle + halfSpreadRad) * baseR;

        vc.vertex(m, tipX, tipY, 0).color(r, g, b, a).endVertex();
        vc.vertex(m, leftX, leftY, 0).color(r, g, b, a).endVertex();
        vc.vertex(m, notchX, notchY, 0).color(r, g, b, a).endVertex();
        vc.vertex(m, rightX, rightY, 0).color(r, g, b, a).endVertex();
    }

    private static int tintDark(int rgb, float brightness, int alphaByte) {
        int r = Math.min(255, Math.round(((rgb >>> 16) & 0xFF) * brightness));
        int g = Math.min(255, Math.round(((rgb >>> 8) & 0xFF) * brightness));
        int b = Math.min(255, Math.round((rgb & 0xFF) * brightness));
        return ((alphaByte & 0xFF) << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpArgb(int c0, int c1, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a = Math.round(((c0 >>> 24) & 0xFF) + (((c1 >>> 24) & 0xFF) - ((c0 >>> 24) & 0xFF)) * t);
        int r = Math.round(((c0 >>> 16) & 0xFF) + (((c1 >>> 16) & 0xFF) - ((c0 >>> 16) & 0xFF)) * t);
        int g = Math.round(((c0 >>> 8) & 0xFF) + (((c1 >>> 8) & 0xFF) - ((c0 >>> 8) & 0xFF)) * t);
        int b = Math.round((c0 & 0xFF) + ((c1 & 0xFF) - (c0 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int scaleAlpha(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}