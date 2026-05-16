package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ModuleVisibilityPanel {

    private static final int WIDTH    = 150;
    private static final int HEADER_H = 18;
    private static final int ROW_H    = 18;
    private static final int MAX_ROWS = 8; // scroll after this many
    private static final int PAD      = 6;

    private int x, y;
    private int scrollOffset = 0;

    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    public ModuleVisibilityPanel(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private List<Module> allModules() {
        return ModuleManager.getAll();
    }

    private int visibleRows() {
        return Math.min(allModules().size(), MAX_ROWS);
    }

    private int panelHeight() {
        return HEADER_H + visibleRows() * ROW_H + PAD;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= x && mx <= x + WIDTH
                && my >= y && my <= y + panelHeight();
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        Theme t = ThemeManager.get();

        gfx.fill(x, y, x + WIDTH, y + panelHeight(), t.backgroundColor);
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.renderOutline(x, y, WIDTH, panelHeight(), t.borderColor);
        gfx.drawString(font, "Module Visibility", x + PAD, y + 5, t.headerTextColor, false);

        List<Module> mods = allModules();
        int startIdx = Math.max(0, Math.min(scrollOffset, mods.size() - MAX_ROWS));

        for (int i = 0; i < visibleRows(); i++) {
            int modIdx = startIdx + i;
            if (modIdx >= mods.size()) break;

            Module m = mods.get(modIdx);
            int rowY = y + HEADER_H + i * ROW_H;
            boolean hidden = ProfileManager.getActive().isModuleHidden(m.getName());
            boolean hovered = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;

            if (hovered) gfx.fill(x, rowY, x + WIDTH, rowY + ROW_H, 0x18FFFFFF);

            // Checkbox area on the right
            int checkX = x + WIDTH - 14 - PAD;
            int checkY = rowY + (ROW_H - 8) / 2;
            gfx.fill(checkX, checkY, checkX + 8, checkY + 8,
                    hidden ? t.disabledTextColor : t.accentColor);

            // Category badge
            String cat = m.getCategory().name().substring(0, 3);
            gfx.drawString(font, cat, x + PAD, rowY + 5, t.categoryTextColor, false);

            // Module name
            int nameColor = hidden ? t.disabledTextColor : t.enabledTextColor;
            gfx.drawString(font, m.getName(), x + PAD + 22, rowY + 5, nameColor, false);
        }

        // Scroll indicator if needed
        if (mods.size() > MAX_ROWS) {
            gfx.drawString(font,
                    (startIdx + visibleRows()) + "/" + mods.size(),
                    x + WIDTH - 30, y + panelHeight() - 10,
                    t.disabledTextColor, false);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mx, double my, int button) {
        if (!isInPanel(mx, my)) return false;

        if (my >= y && my <= y + HEADER_H) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = (int) mx - x;
                dragOffsetY = (int) my - y;
            }
            return true;
        }

        List<Module> mods = allModules();
        int startIdx = Math.max(0, Math.min(scrollOffset, mods.size() - MAX_ROWS));

        for (int i = 0; i < visibleRows(); i++) {
            int modIdx = startIdx + i;
            if (modIdx >= mods.size()) break;
            int rowY = y + HEADER_H + i * ROW_H;
            if (my >= rowY && my <= rowY + ROW_H) {
                Module m = mods.get(modIdx);
                boolean hidden = ProfileManager.getActive().isModuleHidden(m.getName());
                ProfileManager.getActive().setModuleHidden(m.getName(), !hidden);
                return true;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isInPanel(mx, my)) return false;
        scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(delta));
        return true;
    }

    public void drag(double mx, double my) {
        if (!dragging) return;
        x = (int) mx - dragOffsetX;
        y = (int) my - dragOffsetY;
    }

    public void stopDrag() { dragging = false; }
    public boolean isDragging() { return dragging; }
}