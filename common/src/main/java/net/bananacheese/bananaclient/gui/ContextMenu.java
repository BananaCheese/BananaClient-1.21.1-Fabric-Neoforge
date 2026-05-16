package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class ContextMenu {

    private static final int WIDTH  = 110;
    private static final int ROW_H  = 16;
    private static final int PAD    = 6;

    public static class Entry {
        public final String label;
        public final boolean danger;
        public final boolean separator; // true = draw a line instead of text
        public final Runnable action;

        private Entry(String label, boolean danger, boolean separator, Runnable action) {
            this.label     = label;
            this.danger    = danger;
            this.separator = separator;
            this.action    = action;
        }

        public static Entry of(String label, Runnable action) {
            return new Entry(label, false, false, action);
        }

        public static Entry danger(String label, Runnable action) {
            return new Entry(label, true, false, action);
        }

        public static Entry separator() {
            return new Entry("", false, true, () -> {});
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private int x, y;

    public ContextMenu(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public ContextMenu add(Entry e) { entries.add(e); return this; }

    // ── Geometry ──────────────────────────────────────────────────────────

    private int totalHeight() {
        int h = 0;
        for (Entry e : entries) h += e.separator ? 5 : ROW_H;
        return h;
    }

    public boolean isInMenu(double mx, double my) {
        return mx >= x && mx <= x + WIDTH
                && my >= y && my <= y + totalHeight();
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        Theme t = ThemeManager.get();
        int h = totalHeight();

        gfx.fill(x, y, x + WIDTH, y + h, t.backgroundColor);
        gfx.renderOutline(x, y, WIDTH, h, t.borderColor);

        int curY = y;
        for (Entry e : entries) {
            if (e.separator) {
                gfx.fill(x + PAD, curY + 2, x + WIDTH - PAD, curY + 3, t.borderColor);
                curY += 5;
                continue;
            }

            boolean hovered = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= curY && mouseY <= curY + ROW_H;

            if (hovered) gfx.fill(x, curY, x + WIDTH, curY + ROW_H, 0x22FFFFFF);

            int color = e.danger
                    ? (hovered ? 0xFFE05555 : 0xFF883333)
                    : (hovered ? t.enabledTextColor : t.disabledTextColor);

            gfx.drawString(font, e.label, x + PAD, curY + 4, color, false);
            curY += ROW_H;
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    // Returns true if click was consumed, fires the action if on an entry
    public boolean mouseClicked(double mx, double my) {
        if (!isInMenu(mx, my)) return false;

        int curY = y;
        for (Entry e : entries) {
            if (e.separator) { curY += 5; continue; }
            if (my >= curY && my <= curY + ROW_H) {
                e.action.run();
                return true;
            }
            curY += ROW_H;
        }
        return true;
    }
}
