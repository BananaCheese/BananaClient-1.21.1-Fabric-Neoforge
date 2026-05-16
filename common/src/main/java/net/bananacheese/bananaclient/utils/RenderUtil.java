package net.bananacheese.bananaclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class RenderUtil {

    /**
     * Draws a filled rounded rectangle.
     * @param x      left edge
     * @param y      top edge
     * @param w      width
     * @param h      height
     * @param r      corner radius in pixels (clamped to half the shorter side)
     * @param color  ARGB packed int
     */
    public static void fillRounded(PoseStack poses, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            // Degenerate — just fill a plain rect
            fill(poses, x, y, x + w, y + h, color);
            return;
        }

        float a = ((color >> 24) & 0xFF) / 255f;
        float red   = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >>  8) & 0xFF) / 255f;
        float blue  = ( color        & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buf = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f mat = poses.last().pose();

        // Center fill
        fillQuad(buf, mat, x + r, y + r, x + w - r, y + h - r, red, green, blue, a);

        // Top and bottom center strips
        fillQuad(buf, mat, x + r, y,         x + w - r, y + r,     red, green, blue, a);
        fillQuad(buf, mat, x + r, y + h - r, x + w - r, y + h,     red, green, blue, a);

        // Left and right strips
        fillQuad(buf, mat, x,         y + r, x + r,     y + h - r, red, green, blue, a);
        fillQuad(buf, mat, x + w - r, y + r, x + w,     y + h - r, red, green, blue, a);

        // Four corners — drawn as triangle fans
        int steps = 8; // smoothness, 8 is plenty for small radii
        drawCorner(buf, mat, x + r,     y + r,     r, 180, 270, red, green, blue, a, steps);
        drawCorner(buf, mat, x + w - r, y + r,     r, 270, 360, red, green, blue, a, steps);
        drawCorner(buf, mat, x + w - r, y + h - r, r,   0,  90, red, green, blue, a, steps);
        drawCorner(buf, mat, x + r,     y + h - r, r,  90, 180, red, green, blue, a, steps);

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
    }

    /**
     * Draws a rounded outline (border only, no fill).
     */
    public static void outlineRounded(PoseStack poses, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            // Plain outline
            fill(poses, x,         y,         x + w, y + 1, color); // top
            fill(poses, x,         y + h - 1, x + w, y + h, color); // bottom
            fill(poses, x,         y,         x + 1, y + h, color); // left
            fill(poses, x + w - 1, y,         x + w, y + h, color); // right
            return;
        }

        float a = ((color >> 24) & 0xFF) / 255f;
        float red   = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >>  8) & 0xFF) / 255f;
        float blue  = ( color        & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buf = Tesselator.getInstance().begin(
                VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f mat = poses.last().pose();
        int steps = 8;

        // Trace the full perimeter as one continuous line strip
        drawCornerLine(buf, mat, x + r,     y + r,     r, 180, 270, red, green, blue, a, steps);
        drawCornerLine(buf, mat, x + w - r, y + r,     r, 270, 360, red, green, blue, a, steps);
        drawCornerLine(buf, mat, x + w - r, y + h - r, r,   0,  90, red, green, blue, a, steps);
        drawCornerLine(buf, mat, x + r,     y + h - r, r,  90, 180, red, green, blue, a, steps);

        // Close the strip back to start
        float startX = x + r + (float)(r * Math.cos(Math.toRadians(180)));
        float startY = y + r + (float)(r * Math.sin(Math.toRadians(180)));
        buf.addVertex(mat, startX, startY, 0).setColor(red, green, blue, a);

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private static void fillQuad(BufferBuilder buf, Matrix4f mat,
                                 float x1, float y1, float x2, float y2,
                                 float r, float g, float b, float a) {
        buf.addVertex(mat, x1, y1, 0).setColor(r, g, b, a);
        buf.addVertex(mat, x2, y1, 0).setColor(r, g, b, a);
        buf.addVertex(mat, x2, y2, 0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y2, 0).setColor(r, g, b, a);
    }

    private static void drawCorner(BufferBuilder buf, Matrix4f mat,
                                   float cx, float cy, int r,
                                   int startDeg, int endDeg,
                                   float red, float green, float blue, float a,
                                   int steps) {
        // Center vertex of the fan
        buf.addVertex(mat, cx, cy, 0).setColor(red, green, blue, a);
        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(startDeg + (endDeg - startDeg) * i / (double) steps);
            float px = cx + (float)(r * Math.cos(angle));
            float py = cy + (float)(r * Math.sin(angle));
            buf.addVertex(mat, px, py, 0).setColor(red, green, blue, a);
        }
    }

    private static void drawCornerLine(BufferBuilder buf, Matrix4f mat,
                                       float cx, float cy, int r,
                                       int startDeg, int endDeg,
                                       float red, float green, float blue, float a,
                                       int steps) {
        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(startDeg + (endDeg - startDeg) * i / (double) steps);
            float px = cx + (float)(r * Math.cos(angle));
            float py = cy + (float)(r * Math.sin(angle));
            buf.addVertex(mat, px, py, 0).setColor(red, green, blue, a);
        }
    }

    public static void fill(PoseStack poses, int x1, int y1, int x2, int y2, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = poses.last().pose();
        buf.addVertex(mat, x1, y1, 0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y2, 0).setColor(r, g, b, a);
        buf.addVertex(mat, x2, y2, 0).setColor(r, g, b, a);
        buf.addVertex(mat, x2, y1, 0).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
