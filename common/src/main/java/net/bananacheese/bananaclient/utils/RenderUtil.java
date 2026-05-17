package net.bananacheese.bananaclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class RenderUtil {

    /**
     * Scales the alpha channel of an ARGB color by the given opacity multiplier.
     */
    public static int applyOpacity(int argb, float opacity) {
        int originalAlpha = (argb >> 24) & 0xFF;
        int newAlpha = Math.round(originalAlpha * opacity);
        return (argb & 0x00FFFFFF) | (newAlpha << 24);
    }

    /**
     * Plain filled rectangle — used everywhere backgrounds are drawn.
     */
    public static void fill(PoseStack poses, int x1, int y1, int x2, int y2, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
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
