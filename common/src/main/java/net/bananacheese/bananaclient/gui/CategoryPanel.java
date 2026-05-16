package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.profile.PanelState;
import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CategoryPanel {

    public static final int WIDTH    = 120;
    public static final int ROW_H    = 20;
    public static final int HEADER_H = 18;
    public static final int PADDING  = 5;

    private final Module.Category category;
    private final PanelState state;

    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;
    private int _dragStartX, _dragStartY;

    private Module rebindingModule = null;

    // Callback set by ModuleScreen so context menu creation stays outside this class
    private Runnable onHeaderRightClick = null;

    public CategoryPanel(Module.Category category) {
        this.category = category;
        this.state    = ProfileManager.getActive().getPanelState(category);
    }

    public void setOnHeaderRightClick(Runnable r) { this.onHeaderRightClick = r; }

    // ── Geometry ──────────────────────────────────────────────────────────

    private List<Module> visibleModules() {
        return ModuleManager.getAll().stream()
                .filter(m -> m.getCategory() == category)
                .filter(m -> !ProfileManager.getActive().isModuleHidden(m.getName()))
                .toList();
    }

    public int getHeight() {
        if (state.collapsed) return HEADER_H;
        return HEADER_H + visibleModules().size() * ROW_H + PADDING;
    }

    public boolean isInHeader(double mx, double my) {
        return mx >= state.x && mx <= state.x + WIDTH
                && my >= state.y && my <= state.y + HEADER_H;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= state.x && mx <= state.x + WIDTH
                && my >= state.y && my <= state.y + getHeight();
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        if (!state.visible) return;

        Theme t = ThemeManager.get();
        int x = state.x, y = state.y, w = WIDTH, h = getHeight();

        gfx.fill(x, y, x + w, y + h, t.backgroundColor);
        gfx.fill(x, y, x + w, y + HEADER_H, t.headerColor);

        // Lock icon replaces drag hint when locked
        String lockIcon = state.locked ? "🔒" : "";
        gfx.drawString(font,
                category.name().charAt(0) + category.name().substring(1).toLowerCase()
                        + (state.locked ? " 🔒" : ""),
                x + PADDING, y + 5, t.headerTextColor, false);

        // Collapse arrow
        String arrow = state.collapsed ? "▶" : "▼";
        int arrowX = x + w - font.width(arrow) - PADDING;
        gfx.drawString(font, arrow, arrowX, y + 5, t.headerTextColor, false);

        gfx.renderOutline(x, y, w, h, t.borderColor);

        if (state.collapsed) return;

        List<Module> mods = visibleModules();
        for (int i = 0; i < mods.size(); i++) {
            renderRow(gfx, font, mods.get(i), x, y + HEADER_H + i * ROW_H, w, mouseX, mouseY);
        }
    }

    private void renderRow(GuiGraphics gfx, Font font,
                           Module m, int x, int rowY, int w,
                           int mouseX, int mouseY) {
        Theme t = ThemeManager.get();

        boolean hovered = mouseX >= x && mouseX <= x + w
                && mouseY >= rowY && mouseY <= rowY + ROW_H;
        if (hovered) gfx.fill(x, rowY, x + w, rowY + ROW_H, 0x18FFFFFF);

        if (t.showToggleSwitches) {
            renderToggleSwitch(gfx, m, x + PADDING, rowY + (ROW_H - 10) / 2);
            gfx.drawString(font, m.getName(), x + PADDING + 26, rowY + 6,
                    m.isEnabled() ? t.enabledTextColor : t.disabledTextColor, false);
        } else {
            gfx.fill(x, rowY, x + 2, rowY + ROW_H,
                    m.isEnabled() ? t.accentColor : t.disabledTextColor);
            gfx.drawString(font, m.getName(), x + PADDING + 2, rowY + 6,
                    m.isEnabled() ? t.enabledTextColor : t.disabledTextColor, false);
        }

        boolean isRebinding = rebindingModule == m;
        String keyLabel = isRebinding ? "..." : m.getKeyName();
        int badgeW = font.width(keyLabel) + 6;
        int badgeX = x + w - badgeW - PADDING;
        int badgeY = rowY + (ROW_H - 10) / 2;
        if (t.keyBadgeBackground != 0)
            gfx.fill(badgeX - 1, badgeY - 1, badgeX + badgeW + 1, badgeY + 11, t.keyBadgeBackground);
        gfx.drawString(font, keyLabel, badgeX + 3, badgeY + 1,
                isRebinding ? t.accentColor : t.keyBadgeText, false);
    }

    private void renderToggleSwitch(GuiGraphics gfx, Module m, int x, int y) {
        Theme t = ThemeManager.get();
        gfx.fill(x, y, x + 22, y + 10, m.isEnabled() ? 0xFF005580 : 0xFF333333);
        int thumbX = m.isEnabled() ? x + 13 : x + 1;
        gfx.fill(thumbX, y + 1, thumbX + 8, y + 9,
                m.isEnabled() ? t.accentColor : 0xFF888888);
    }

    // ── Input ─────────────────────────────────────────────────────────────

    // Returns module that was right-clicked on its row, or null
    public Module mouseClicked(double mx, double my, int button) {
        if (!state.visible || !isInPanel(mx, my)) return null;

        if (isInHeader(mx, my)) {
            if (button == 1) {
                if (onHeaderRightClick != null) onHeaderRightClick.run();
            } else if (button == 0) {
                if (!state.locked) {
                    // Begin drag — collapse handled on mouse release if didn't actually move
                    dragging    = true;
                    dragOffsetX = (int) mx - state.x;
                    dragOffsetY = (int) my - state.y;
                    // We'll toggle collapse in stopDrag if position didn't change
                    _dragStartX = state.x;
                    _dragStartY = state.y;
                } else {
                    // Locked — just collapse
                    state.collapsed = !state.collapsed;
                    saveState();
                }
            }
            return null;
        }

        if (state.collapsed) return null;

        List<Module> mods = visibleModules();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int rowY = state.y + HEADER_H + i * ROW_H;
            if (my >= rowY && my <= rowY + ROW_H) {
                if (button == 0) { rebindingModule = null; m.toggle(); return null; }
                else if (button == 1) return m;
            }
        }
        return null;
    }

    // Only starts drag on left-button header press, and only if unlocked
    public boolean startDrag(double mx, double my, int button) {
        if (button != 0) return false;
        if (state.locked) return false;
        if (!isInHeader(mx, my)) return false;
        dragging    = true;
        dragOffsetX = (int) mx - state.x;
        dragOffsetY = (int) my - state.y;
        return true;
    }

    public void drag(double mx, double my) {
        if (!dragging) return;
        state.x = (int) mx - dragOffsetX;
        state.y = (int) my - dragOffsetY;
    }

    public void stopDrag() {
        if (dragging) {
            dragging = false;
            // If position didn't change it was a click not a drag — toggle collapse
            if (state.x == _dragStartX && state.y == _dragStartY) {
                state.collapsed = !state.collapsed;
            }
            saveState();
        }
    }

    // ── State ─────────────────────────────────────────────────────────────

    public void setRebindingModule(Module m) { this.rebindingModule = m; }
    public boolean isDragging()              { return dragging; }
    public boolean isVisible()               { return state.visible; }
    public void    setVisible(boolean v)     { state.visible = v; saveState(); }
    public Module.Category getCategory()     { return category; }
    public PanelState getState()             { return state; }

    private void saveState() {
        ProfileManager.getActive().setPanelState(category, state);
        ProfileManager.saveActive();
    }
}