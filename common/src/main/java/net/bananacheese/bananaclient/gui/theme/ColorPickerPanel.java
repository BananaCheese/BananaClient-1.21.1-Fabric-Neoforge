package net.bananacheese.bananaclient.gui.theme;

import net.bananacheese.bananaclient.utils.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class ColorPickerPanel {

    private static final int WIDTH    = 160;
    private static final int HEADER_H = 16;
    private static final int HS_H     = 80;  // hue/sat square height
    private static final int BAR_H    = 6;   // hue + alpha bar height
    private static final int ROW_H    = 14;  // slider row height
    private static final int HEX_H    = 18;
    private static final int PREVIEW_H = 16;
    private static final int PAD      = 8;

    private int x, y;
    private final String fieldName;
    private final IntSupplier getColor;     // reads current color
    private final Consumer<Integer> setColor; // writes new color + applies live

    // Current HSVA state — we edit in HSV space, convert to RGB for output
    private float hue        = 0f;   // 0-360
    private float saturation = 0f;   // 0-1
    private float brightness = 1f;   // 0-1
    private int   alpha      = 255;  // 0-255

    // Original color for the preview swatch
    private final int originalColor;

    // Drag state
    private boolean draggingPanel = false;
    private boolean draggingHS    = false;
    private boolean draggingHue   = false;
    private boolean draggingAlpha = false;
    private boolean draggingR = false, draggingG = false, draggingB = false;
    private int panelDragOffX, panelDragOffY;

    // Hex input
    private final StringBuilder hexInput = new StringBuilder();
    private boolean hexFocused = false;

    public ColorPickerPanel(int x, int y, String fieldName,
                            IntSupplier getColor, Consumer<Integer> setColor) {
        this.x         = x;
        this.y         = y;
        this.fieldName = fieldName;
        this.getColor  = getColor;
        this.setColor  = setColor;
        this.originalColor = getColor.getAsInt();

        // Parse initial color into HSV
        fromArgb(originalColor);
        hexInput.append(String.format("%08X", originalColor));
    }

    // ── Geometry ──────────────────────────────────────────────────────────

    private int totalHeight() {
        return HEADER_H + HS_H + PAD
                + BAR_H + PAD   // hue bar
                + BAR_H + PAD   // alpha bar
                + 4 * ROW_H + PAD  // RGBA sliders
                + HEX_H
                + PREVIEW_H;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= x && mx <= x + WIDTH
                && my >= y && my <= y + totalHeight();
    }

    // ── Color conversion ──────────────────────────────────────────────────

    /** Parse ARGB int → HSV + alpha fields */
    private void fromArgb(int argb) {
        alpha = (argb >> 24) & 0xFF;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        brightness = max;
        saturation = (max == 0) ? 0 : delta / max;

        if (delta == 0) {
            hue = 0;
        } else if (max == r) {
            hue = 60f * (((g - b) / delta) % 6f);
        } else if (max == g) {
            hue = 60f * (((b - r) / delta) + 2f);
        } else {
            hue = 60f * (((r - g) / delta) + 4f);
        }
        if (hue < 0) hue += 360f;
    }

    /** HSV + alpha → ARGB int */
    private int toArgb() {
        float h = hue / 60f;
        float s = saturation;
        float v = brightness;

        float c = v * s;
        float x2 = c * (1 - Math.abs(h % 2 - 1));
        float m = v - c;

        float r, g, b;
        int hi = (int) h % 6;
        switch (hi) {
            case 0  -> { r = c;  g = x2; b = 0; }
            case 1  -> { r = x2; g = c;  b = 0; }
            case 2  -> { r = 0;  g = c;  b = x2; }
            case 3  -> { r = 0;  g = x2; b = c; }
            case 4  -> { r = x2; g = 0;  b = c; }
            default -> { r = c;  g = 0;  b = x2; }
        }

        int ri = Math.round((r + m) * 255);
        int gi = Math.round((g + m) * 255);
        int bi = Math.round((b + m) * 255);
        return (alpha << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private int hueColor() {
        // Pure hue at full saturation/brightness for the hue bar gradient
        ColorPickerPanel tmp = new ColorPickerPanel(0, 0, "", () -> 0, c -> {});
        tmp.hue = hue; tmp.saturation = 1f; tmp.brightness = 1f; tmp.alpha = 255;
        return tmp.toArgb();
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        Theme t = ThemeManager.get();
        int bgColor = RenderUtil.applyOpacity(t.backgroundColor, t.backgroundOpacity);

        // Panel background + border
        gfx.fill(x, y, x + WIDTH, y + totalHeight(), bgColor);
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.renderOutline(x, y, WIDTH, totalHeight(), t.borderColor);

        // Header
        gfx.drawString(font, fieldName, x + PAD, y + 4, t.headerTextColor, false);
        gfx.drawString(font, "✕", x + WIDTH - font.width("✕") - PAD, y + 4,
                t.disabledTextColor, false);

        int curY = y + HEADER_H;

        // ── Hue/Saturation square ─────────────────────────────────────────
        renderHSSquare(gfx, curY, t);
        // Cursor on HS square
        int cursorX = x + (int)(saturation * WIDTH);
        int cursorY = curY + (int)((1f - brightness) * HS_H);
        gfx.fill(cursorX - 3, cursorY - 3, cursorX + 3, cursorY + 3, 0xFFFFFFFF);
        gfx.fill(cursorX - 2, cursorY - 2, cursorX + 2, cursorY + 2, toArgb() | 0xFF000000);
        curY += HS_H + PAD;

        // ── Hue bar ───────────────────────────────────────────────────────
        renderHueBar(gfx, curY);
        int hueThumbX = x + (int)(hue / 360f * WIDTH);
        gfx.fill(hueThumbX - 2, curY - 2, hueThumbX + 2, curY + BAR_H + 2, 0xFFFFFFFF);
        gfx.fill(hueThumbX - 1, curY - 1, hueThumbX + 1, curY + BAR_H + 1, hueColor());
        curY += BAR_H + PAD;

        // ── Alpha bar ─────────────────────────────────────────────────────
        // Checkerboard hint then color gradient
        gfx.fill(x, curY, x + WIDTH, curY + BAR_H, 0xFF888888);
        int currentRgb = toArgb() & 0x00FFFFFF;
        for (int px = 0; px < WIDTH; px++) {
            int a = (int)(px / (float) WIDTH * 255);
            gfx.fill(x + px, curY, x + px + 1, curY + BAR_H, (a << 24) | currentRgb);
        }
        int alphaThumbX = x + (int)(alpha / 255f * WIDTH);
        gfx.fill(alphaThumbX - 2, curY - 2, alphaThumbX + 2, curY + BAR_H + 2, 0xFFFFFFFF);
        curY += BAR_H + PAD;

        // ── RGBA sliders ──────────────────────────────────────────────────
        int currentColor = toArgb();
        int r = (currentColor >> 16) & 0xFF;
        int g = (currentColor >>  8) & 0xFF;
        int b =  currentColor        & 0xFF;

        curY = renderSlider(gfx, font, curY, "R", r, 0xFFFF0000, t);
        curY = renderSlider(gfx, font, curY, "G", g, 0xFF00FF00, t);
        curY = renderSlider(gfx, font, curY, "B", b, 0xFF6666FF, t);
        curY = renderSlider(gfx, font, curY, "A", alpha, 0xFFAAAAAA, t);
        curY += PAD;

        // ── Hex input ─────────────────────────────────────────────────────
        gfx.fill(x + PAD, curY, x + WIDTH - PAD, curY + HEX_H, 0xFF000000);
        gfx.renderOutline(x + PAD, curY, WIDTH - PAD * 2, HEX_H,
                hexFocused ? t.accentColor : t.borderColor);
        String hexDisplay = "#" + hexInput
                + (hexFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
        gfx.drawString(font, hexDisplay, x + PAD + 3, curY + 5, t.enabledTextColor, false);
        curY += HEX_H;

        // ── Preview ───────────────────────────────────────────────────────
        int halfW = (WIDTH - PAD * 3) / 2;
        gfx.fill(x + PAD, curY + 4, x + PAD + halfW, curY + PREVIEW_H, originalColor);
        gfx.fill(x + PAD + halfW + PAD, curY + 4,
                x + PAD + halfW + PAD + halfW, curY + PREVIEW_H, toArgb());
        gfx.drawString(font, "→", x + PAD + halfW + 2, curY + 5, t.disabledTextColor, false);
    }

    private void renderHSSquare(GuiGraphics gfx, int startY, Theme t) {
        // Draw hue/saturation square column by column
        // X = saturation (0 left → 1 right), Y = brightness (1 top → 0 bottom)
        int cols = WIDTH;
        for (int col = 0; col < cols; col++) {
            float s = col / (float) cols;
            // Top of column (brightness=1), bottom (brightness=0)
            ColorPickerPanel tmp = new ColorPickerPanel(0, 0, "", () -> 0, c -> {});
            tmp.hue = hue; tmp.saturation = s; tmp.brightness = 1f; tmp.alpha = 255;
            int topColor = tmp.toArgb();
            tmp.brightness = 0f;
            int botColor = tmp.toArgb();
            // Vertical gradient for this column
            for (int row = 0; row < HS_H; row++) {
                float t2 = row / (float) HS_H;
                int blended = blendColors(topColor, botColor, t2);
                gfx.fill(x + col, startY + row, x + col + 1, startY + row + 1, blended);
            }
        }
    }

    private void renderHueBar(GuiGraphics gfx, int barY) {
        // Draw hue bar in 6 segments
        int segW = WIDTH / 6;
        int[][] hueColors = {
                {0xFF, 0x00, 0x00}, // red
                {0xFF, 0xFF, 0x00}, // yellow
                {0x00, 0xFF, 0x00}, // green
                {0x00, 0xFF, 0xFF}, // cyan
                {0x00, 0x00, 0xFF}, // blue
                {0xFF, 0x00, 0xFF}, // magenta
        };
        for (int seg = 0; seg < 6; seg++) {
            int[] c1 = hueColors[seg];
            int[] c2 = hueColors[(seg + 1) % 6];
            int startX = x + seg * segW;
            int endX = (seg == 5) ? x + WIDTH : startX + segW;
            for (int px = startX; px < endX; px++) {
                float t2 = (px - startX) / (float)(endX - startX);
                int r = (int)(c1[0] + t2 * (c2[0] - c1[0]));
                int g = (int)(c1[1] + t2 * (c2[1] - c1[1]));
                int b = (int)(c1[2] + t2 * (c2[2] - c1[2]));
                gfx.fill(px, barY, px + 1, barY + BAR_H,
                        0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
    }

    private int renderSlider(GuiGraphics gfx, Font font,
                             int rowY, String label, int value, int color, Theme t) {
        int trackX = x + PAD + 12;
        int trackW = WIDTH - PAD * 2 - 12 - 24;
        int trackY = rowY + ROW_H / 2 - 2;

        gfx.drawString(font, label, x + PAD, rowY + 3, t.disabledTextColor, false);

        // Track bg
        gfx.fill(trackX, trackY, trackX + trackW, trackY + 4, t.borderColor);
        // Track fill
        int fillW = (int)(value / 255f * trackW);
        gfx.fill(trackX, trackY, trackX + fillW, trackY + 4, color);
        // Thumb
        int thumbX = trackX + fillW;
        gfx.fill(thumbX - 3, trackY - 2, thumbX + 3, trackY + 6, 0xFFFFFFFF);
        gfx.fill(thumbX - 2, trackY - 1, thumbX + 2, trackY + 5, color);

        // Value
        String val = String.valueOf(value);
        gfx.drawString(font, val, x + WIDTH - PAD - font.width(val), rowY + 3,
                t.enabledTextColor, false);

        return rowY + ROW_H;
    }

    private static int blendColors(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, a2 = (c2 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF, r2 = (c2 >> 16) & 0xFF;
        int g1 = (c1 >>  8) & 0xFF, g2 = (c2 >>  8) & 0xFF;
        int b1 =  c1        & 0xFF, b2 =  c2        & 0xFF;
        return ((int)(a1 + t * (a2 - a1)) << 24)
                | ((int)(r1 + t * (r2 - r1)) << 16)
                | ((int)(g1 + t * (g2 - g1)) <<  8)
                |  (int)(b1 + t * (b2 - b1));
    }

    // ── Input ─────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mx, double my, int button) {
        if (!isInPanel(mx, my)) return false;

        // Header — drag or close
        if (my >= y && my < y + HEADER_H) {
            // Close button
            if (mx >= x + WIDTH - 16) return false; // signal caller to close
            draggingPanel = true;
            panelDragOffX = (int) mx - x;
            panelDragOffY = (int) my - y;
            return true;
        }

        int curY = y + HEADER_H;

        // HS square
        if (my >= curY && my < curY + HS_H) {
            draggingHS = true;
            applyHS(mx, my, curY);
            return true;
        }
        curY += HS_H + PAD;

        // Hue bar
        if (my >= curY && my < curY + BAR_H) {
            draggingHue = true;
            applyHue(mx);
            return true;
        }
        curY += BAR_H + PAD;

        // Alpha bar
        if (my >= curY && my < curY + BAR_H) {
            draggingAlpha = true;
            applyAlpha(mx);
            return true;
        }
        curY += BAR_H + PAD;

        // RGBA sliders
        for (int i = 0; i < 4; i++) {
            if (my >= curY && my < curY + ROW_H) {
                switch (i) {
                    case 0 -> draggingR = true;
                    case 1 -> draggingG = true;
                    case 2 -> draggingB = true;
                    case 3 -> draggingAlpha = true;
                }
                applySliderDrag(i, mx);
                return true;
            }
            curY += ROW_H;
        }
        curY += PAD;

        // Hex input
        if (my >= curY && my < curY + HEX_H) {
            hexFocused = !hexFocused;
            return true;
        }

        return true;
    }

    /** Returns true if close button was clicked */
    public boolean isCloseClick(double mx, double my) {
        return my >= y && my < y + HEADER_H && mx >= x + WIDTH - 16;
    }

    public boolean mouseDragged(double mx, double my) {
        if (draggingPanel) {
            x = (int) mx - panelDragOffX;
            y = (int) my - panelDragOffY;
            return true;
        }

        int hsTop = y + HEADER_H;
        if (draggingHS) { applyHS(mx, my, hsTop); return true; }

        int hueTop = hsTop + HS_H + PAD;
        if (draggingHue) { applyHue(mx); return true; }

        int alphaBarTop = hueTop + BAR_H + PAD;
        if (draggingAlpha) {
            // Could be alpha bar or alpha slider — apply alpha either way
            applyAlpha(mx);
            return true;
        }

        int slidersTop = alphaBarTop + BAR_H + PAD;
        if (draggingR) { applySliderDrag(0, mx); return true; }
        if (draggingG) { applySliderDrag(1, mx); return true; }
        if (draggingB) { applySliderDrag(2, mx); return true; }

        return false;
    }

    public void mouseReleased() {
        draggingPanel = false;
        draggingHS    = false;
        draggingHue   = false;
        draggingAlpha = false;
        draggingR = draggingG = draggingB = false;
    }

    public boolean keyPressed(int keyCode) {
        if (!hexFocused) return false;
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            commitHex();
            hexFocused = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            hexFocused = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && hexInput.length() > 0) {
            hexInput.deleteCharAt(hexInput.length() - 1);
            return true;
        }
        return false;
    }

    public void charTyped(char c) {
        if (!hexFocused) return;
        if (hexInput.length() < 8 && "0123456789AaBbCcDdEeFf".indexOf(c) >= 0) {
            hexInput.append(Character.toUpperCase(c));
        }
    }

    // ── Apply helpers ─────────────────────────────────────────────────────

    private void applyHS(double mx, double my, int hsTop) {
        saturation = (float) Math.max(0, Math.min(1, (mx - x) / WIDTH));
        brightness = (float) Math.max(0, Math.min(1, 1 - (my - hsTop) / HS_H));
        pushColor();
    }

    private void applyHue(double mx) {
        hue = (float) Math.max(0, Math.min(360, (mx - x) / WIDTH * 360));
        pushColor();
    }

    private void applyAlpha(double mx) {
        alpha = (int) Math.max(0, Math.min(255, (mx - x) / WIDTH * 255));
        pushColor();
    }

    private void applySliderDrag(int channel, double mx) {
        int trackX = x + PAD + 12;
        int trackW = WIDTH - PAD * 2 - 12 - 24;
        int val = (int) Math.max(0, Math.min(255, (mx - trackX) / trackW * 255));

        int current = toArgb();
        int r = (current >> 16) & 0xFF;
        int g = (current >>  8) & 0xFF;
        int b =  current        & 0xFF;

        switch (channel) {
            case 0 -> r = val;
            case 1 -> g = val;
            case 2 -> b = val;
            case 3 -> { alpha = val; pushColor(); return; }
        }
        // Re-derive HSV from new RGB so HS square stays in sync
        fromArgb((alpha << 24) | (r << 16) | (g << 8) | b);
        pushColor();
    }

    private void commitHex() {
        try {
            String hex = hexInput.toString().trim();
            if (hex.length() == 6) hex = "FF" + hex;
            if (hex.length() == 8) {
                int parsed = (int) Long.parseLong(hex, 16);
                fromArgb(parsed);
                pushColor();
            }
        } catch (NumberFormatException ignored) {}
    }

    private void pushColor() {
        int color = toArgb();
        setColor.accept(color);
        // Sync hex field
        hexInput.setLength(0);
        hexInput.append(String.format("%08X", color));
    }
}
