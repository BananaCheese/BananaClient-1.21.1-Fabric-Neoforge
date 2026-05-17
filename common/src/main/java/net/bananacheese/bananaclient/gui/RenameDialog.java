package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.utils.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class RenameDialog {

    private static final int WIDTH    = 160;
    private static final int HEIGHT   = 50;
    private static final int HEADER_H = 16;
    private static final int PAD      = 6;

    private final int x, y;
    private final String title;
    private final Consumer<String> onConfirm;
    private final StringBuilder text;
    private int cursorTick = 0;

    public RenameDialog(int x, int y, String title, String initial, Consumer<String> onConfirm) {
        this.x         = x;
        this.y         = y;
        this.title     = title;
        this.onConfirm = onConfirm;
        this.text      = new StringBuilder(initial);
    }

    public boolean isInDialog(double mx, double my) {
        return mx >= x && mx <= x + WIDTH && my >= y && my <= y + HEIGHT;
    }

    public void tick() { cursorTick++; }

    public void render(GuiGraphics gfx, Font font) {
        Theme t = ThemeManager.get();

        int bgColor = RenderUtil.applyOpacity(t.backgroundColor, t.backgroundOpacity);
        gfx.fill(x, y, x + WIDTH, y + HEIGHT, bgColor);
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.renderOutline(x, y, WIDTH, HEIGHT, t.borderColor);
        gfx.drawString(font, title, x + PAD, y + 4, t.headerTextColor, false);

        // Text field background
        int fieldY = y + HEADER_H + 4;
        int fieldH = 14;
        gfx.fill(x + PAD, fieldY, x + WIDTH - PAD, fieldY + fieldH, 0xFF000000);
        gfx.renderOutline(x + PAD, fieldY, WIDTH - PAD * 2, fieldH, t.accentColor);

        // Text + blinking cursor
        String display = text.toString();
        boolean showCursor = (cursorTick / 10) % 2 == 0;
        if (showCursor) display += "|";
        gfx.drawString(font, display, x + PAD + 3, fieldY + 3, t.enabledTextColor, false);

        // Confirm hint
        gfx.drawString(font, "Enter to confirm  Esc to cancel",
                x + PAD, y + HEIGHT - 10, t.disabledTextColor, false);
    }

    public boolean keyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            String result = text.toString().trim();
            if (!result.isEmpty()) onConfirm.accept(result);
            return true; // signal: close dialog
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true; // signal: cancel, close dialog
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && text.length() > 0) {
            text.deleteCharAt(text.length() - 1);
        }
        return false;
    }

    public void charTyped(char c) {
        // Limit to reasonable filename-safe characters, max 32 chars
        if (text.length() < 32 && c >= 32 && c != '/' && c != '\\' && c != ':') {
            text.append(c);
        }
    }
}
