package me.padej.arcadegames_svc.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class DrawUtil {
    private static final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

    public static void drawColoredCenteredText(DrawContext context, String str, int color, int x, int y) {
        drawColoredText(context, str, color, x - halfTextWidth(str), y);
    }

    public static void drawBigCenteredText(DrawContext context, String str, int x, int y) {
        MatrixStack ms = context.getMatrices();

        ms.push();

        ms.scale(2.0f, 2.0f, 1.0f);

        int scaledX = x / 2;
        int scaledY = y / 2;

        int halfWidth = halfTextWidth(str);

        drawSimpleText(context, str, scaledX - halfWidth, scaledY);

        ms.pop();
    }


    public static void drawCenteredText(DrawContext context, String str, int x, int y) {
        drawSimpleText(context, str, x - halfTextWidth(str), y);
    }

    public static void drawCenteredText(DrawContext context, Text text, int x, int y) {
        drawSimpleText(context, text, x - halfTextWidth(text.getString()), y);
    }

    /**
     * Метод, чтобы просто вывести информацию, не задумываясь о цвете, тенях и т.д.
     */
    public static void drawSimpleText(DrawContext context, String str, int x, int y) {
        context.drawText(tr, str, x, y, 0xFF_FFFFFF, false);
    }
    public static void drawSimpleText(DrawContext context, Text text, int x, int y) {
        context.drawText(tr, text, x, y, 0xFF_FFFFFF, false);
    }
    public static void drawColoredText(DrawContext context, String str, int color, int x, int y) {
        context.drawText(tr, str, x, y, color, false);
    }

    public static int halfTextWidth(String str) {
        return tr.getWidth(str) / 2;
    }

    public static void simpleDrawTexture(DrawContext context, Identifier id, int x, int y, int w, int h) {
        context.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0, 0, w, h, w, h, w, h);
    }
}
