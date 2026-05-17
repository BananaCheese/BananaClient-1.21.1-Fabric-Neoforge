package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleManager;
import net.bananacheese.bananaclient.utils.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ModuleVisibilityPanel {

    private static final int WIDTH     = 160;
    private static final int HEADER_H  = 18;
    private static final int SECTION_H = 14; // section label row
    private static final int ROW_H     = 18;
    private static final int MAX_MOD_ROWS = 8;
    private static final int PAD       = 6;

    private int x, y;
    private int scrollOffset = 0;

    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    // Reference to category panels so we can toggle visibility
    // Set by ModuleScreen after construction
    private List<CategoryPanel> categoryPanels = null;

    public ModuleVisibilityPanel(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setCategoryPanels(List<CategoryPanel> panels) {
        this.categoryPanels = panels;
    }

    // ── Geometry ──────────────────────────────────────────────────────────

    private int panelCount() {
        return Module.Category.values().length; // always 4
    }

    private List<Module> allModules() {
        return ModuleManager.getAll();
    }

    private int visibleModRows() {
        return Math.min(allModules().size(), MAX_MOD_ROWS);
    }

    private int panelHeight() {
        // Header + panel section label + panel rows + module section label + module rows + pad
        return HEADER_H
                + SECTION_H + panelCount() * ROW_H
                + SECTION_H + visibleModRows() * ROW_H
                + PAD;
    }

    // Y position where module rows start
    private int moduleSectionY() {
        return y + HEADER_H + SECTION_H + panelCount() * ROW_H + SECTION_H;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= x && mx <= x + WIDTH
                && my >= y && my <= y + panelHeight();
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        Theme t = ThemeManager.get();
        int h = panelHeight();

        int bgColor = RenderUtil.applyOpacity(t.backgroundColor, t.backgroundOpacity);
        gfx.fill(x, y, x + WIDTH, y + h, bgColor);
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.renderOutline(x, y, WIDTH, h, t.borderColor);
        gfx.drawString(font, "Visibility", x + PAD, y + 5, t.headerTextColor, false);

        int curY = y + HEADER_H;

        // ── Panels section ────────────────────────────────────────────────
        gfx.fill(x, curY, x + WIDTH, curY + SECTION_H, t.headerColor & 0xBBFFFFFF);
        gfx.drawString(font, "PANELS", x + PAD, curY + 3, t.categoryTextColor, false);
        curY += SECTION_H;

        for (Module.Category cat : Module.Category.values()) {
            boolean hidden = false;
            if (categoryPanels != null) {
                for (CategoryPanel cp : categoryPanels) {
                    if (cp.getCategory() == cat) {
                        hidden = !cp.isVisible();
                        break;
                    }
                }
            }

            boolean hovered = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= curY && mouseY <= curY + ROW_H;
            if (hovered) gfx.fill(x, curY, x + WIDTH, curY + ROW_H, 0x18FFFFFF);

            // Checkbox
            int checkX = x + WIDTH - 14 - PAD;
            int checkY = curY + (ROW_H - 8) / 2;
            gfx.fill(checkX, checkY, checkX + 8, checkY + 8,
                    hidden ? t.disabledTextColor : t.accentColor);

            // Category name
            String name = cat.name().charAt(0) + cat.name().substring(1).toLowerCase();
            int nameColor = hidden ? t.disabledTextColor : t.enabledTextColor;
            gfx.drawString(font, name, x + PAD, curY + 5, nameColor, false);

            curY += ROW_H;
        }

        // ── Modules section ───────────────────────────────────────────────
        gfx.fill(x, curY, x + WIDTH, curY + SECTION_H, t.headerColor & 0xBBFFFFFF);
        gfx.drawString(font, "MODULES", x + PAD, curY + 3, t.categoryTextColor, false);
        curY += SECTION_H;

        List<Module> mods = allModules();
        int startIdx = Math.max(0, Math.min(scrollOffset, mods.size() - MAX_MOD_ROWS));

        for (int i = 0; i < visibleModRows(); i++) {
            int modIdx = startIdx + i;
            if (modIdx >= mods.size()) break;

            Module m = mods.get(modIdx);
            int rowY = curY + i * ROW_H;
            boolean hidden = ProfileManager.getActive().isModuleHidden(m.getName());
            boolean hovered = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;

            if (hovered) gfx.fill(x, rowY, x + WIDTH, rowY + ROW_H, 0x18FFFFFF);

            // Checkbox
            int checkX = x + WIDTH - 14 - PAD;
            int checkY = rowY + (ROW_H - 8) / 2;
            gfx.fill(checkX, checkY, checkX + 8, checkY + 8,
                    hidden ? t.disabledTextColor : t.accentColor);

            // Category badge + name
            String cat = m.getCategory().name().substring(0, 3);
            gfx.drawString(font, cat, x + PAD, rowY + 5, t.categoryTextColor, false);
            int nameColor = hidden ? t.disabledTextColor : t.enabledTextColor;
            gfx.drawString(font, m.getName(), x + PAD + 22, rowY + 5, nameColor, false);
        }

        // Scroll indicator
        if (mods.size() > MAX_MOD_ROWS) {
            gfx.drawString(font,
                    (startIdx + visibleModRows()) + "/" + mods.size(),
                    x + WIDTH - 30, y + panelHeight() - 10,
                    t.disabledTextColor, false);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mx, double my, int button) {
        if (!isInPanel(mx, my)) return false;

        // Header drag
        if (my >= y && my <= y + HEADER_H) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = (int) mx - x;
                dragOffsetY = (int) my - y;
            }
            return true;
        }

        int curY = y + HEADER_H;

        // Panel section label — not clickable
        if (my >= curY && my <= curY + SECTION_H) return true;
        curY += SECTION_H;

        // Panel rows
        for (Module.Category cat : Module.Category.values()) {
            if (my >= curY && my <= curY + ROW_H) {
                if (categoryPanels != null) {
                    for (CategoryPanel cp : categoryPanels) {
                        if (cp.getCategory() == cat) {
                            cp.setVisible(!cp.isVisible());
                            break;
                        }
                    }
                }
                return true;
            }
            curY += ROW_H;
        }

        // Module section label — not clickable
        if (my >= curY && my <= curY + SECTION_H) return true;
        curY += SECTION_H;

        // Module rows
        List<Module> mods = allModules();
        int startIdx = Math.max(0, Math.min(scrollOffset, mods.size() - MAX_MOD_ROWS));

        for (int i = 0; i < visibleModRows(); i++) {
            int modIdx = startIdx + i;
            if (modIdx >= mods.size()) break;
            int rowY = curY + i * ROW_H;
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
        // Only scroll if mouse is in the modules section
        if (my >= moduleSectionY()) {
            scrollOffset = Math.max(0,
                    Math.min(allModules().size() - MAX_MOD_ROWS,
                            scrollOffset - (int) Math.signum(delta)));
        }
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