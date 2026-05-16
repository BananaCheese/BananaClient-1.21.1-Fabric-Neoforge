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

    public static final int WIDTH      = 120;
    public static final int ROW_H      = 20;
    public static final int HEADER_H   = 18;
    public static final int PADDING    = 5;

    private final Module.Category category;
    private final PanelState state;

    // Dragging
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    // Rebinding — set by ModuleScreen, read here during render
    private Module rebindingModule = null;

    public CategoryPanel(Module.Category category) {
        this.category = category;
        this.state = ProfileManager.getActive().getPanelState(category);
    }

    // ── Geometry helpers ──────────────────────────────────────────────────

    private List<Module> visibleModules() {
        String profileName = ProfileManager.getActive().name;
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
        int x = state.x;
        int y = state.y;
        int w = WIDTH;
        int h = getHeight();

        // Background
        gfx.fill(x, y, x + w, y + h, t.backgroundColor);

        // Header
        gfx.fill(x, y, x + w, y + HEADER_H, t.headerColor);
        gfx.drawString(font,
                category.name().charAt(0) + category.name().substring(1).toLowerCase(),
                x + PADDING, y + 5, t.headerTextColor, false);

        // Collapse arrow on the right of header
        String arrow = state.collapsed ? "▶" : "▼";
        int arrowX = x + w - font.width(arrow) - PADDING;
        gfx.drawString(font, arrow, arrowX, y + 5, t.headerTextColor, false);

        // Border
        gfx.renderOutline(x, y, w, h, t.borderColor);

        if (state.collapsed) return;

        // Module rows
        List<Module> mods = visibleModules();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int rowY = y + HEADER_H + i * ROW_H;

            renderRow(gfx, font, m, x, rowY, w, mouseX, mouseY);
        }
    }

    private void renderRow(GuiGraphics gfx, Font font,
                           Module m, int x, int rowY, int w,
                           int mouseX, int mouseY) {
        Theme t = ThemeManager.get();

        // Hover tint
        boolean hovered = mouseX >= x && mouseX <= x + w
                && mouseY >= rowY && mouseY <= rowY + ROW_H;
        if (hovered) gfx.fill(x, rowY, x + w, rowY + ROW_H, 0x18FFFFFF);

        if (t.showToggleSwitches) {
            renderToggleSwitch(gfx, m, x + PADDING, rowY + (ROW_H - 10) / 2);
            int nameX = x + PADDING + 26;
            int nameColor = m.isEnabled() ? t.enabledTextColor : t.disabledTextColor;
            gfx.drawString(font, m.getName(), nameX, rowY + 6, nameColor, false);
        } else {
            // Left accent strip
            int stripColor = m.isEnabled() ? t.accentColor : t.disabledTextColor;
            gfx.fill(x, rowY, x + 2, rowY + ROW_H, stripColor);
            int nameColor = m.isEnabled() ? t.enabledTextColor : t.disabledTextColor;
            gfx.drawString(font, m.getName(), x + PADDING + 2, rowY + 6, nameColor, false);
        }

        // Key badge — right aligned
        boolean isRebinding = rebindingModule == m;
        String keyLabel = isRebinding ? "..." : m.getKeyName();
        int badgeW = font.width(keyLabel) + 6;
        int badgeX = x + w - badgeW - PADDING;
        int badgeY = rowY + (ROW_H - 10) / 2;

        if (t.keyBadgeBackground != 0) {
            gfx.fill(badgeX - 1, badgeY - 1, badgeX + badgeW + 1, badgeY + 11, t.keyBadgeBackground);
        }
        int keyColor = isRebinding ? t.accentColor : t.keyBadgeText;
        gfx.drawString(font, keyLabel, badgeX + 3, badgeY + 1, keyColor, false);
    }

    private void renderToggleSwitch(GuiGraphics gfx, Module m, int x, int y) {
        Theme t = ThemeManager.get();
        int trackW = 22;
        int trackH = 10;

        // Track
        int trackColor = m.isEnabled() ? (t.accentColor & 0x00FFFFFF | 0x55000000) : 0x33FFFFFF;
        if (m.isEnabled()) trackColor = t.accentColor & 0x00FFFFFF | 0x88000000;
        gfx.fill(x, y, x + trackW, y + trackH, m.isEnabled() ? 0xFF005580 : 0xFF333333);

        // Thumb
        int thumbX = m.isEnabled() ? x + trackW - 9 : x + 1;
        gfx.fill(thumbX, y + 1, thumbX + 8, y + trackH - 1,
                m.isEnabled() ? t.accentColor : 0xFF888888);
    }

    // ── Input ─────────────────────────────────────────────────────────────

    // Returns the module that was right-clicked, or null
    public Module mouseClicked(double mouseX, double mouseY, int button) {
        if (!state.visible || !isInPanel(mouseX, mouseY)) return null;

        // Header click — toggle collapse
        if (isInHeader(mouseX, mouseY)) {
            state.collapsed = !state.collapsed;
            saveState();
            return null;
        }

        if (state.collapsed) return null;

        List<Module> mods = visibleModules();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int rowY = state.y + HEADER_H + i * ROW_H;

            if (mouseY >= rowY && mouseY <= rowY + ROW_H) {
                if (button == 0) {
                    // Left click — toggle
                    rebindingModule = null;
                    m.toggle();
                    return null;
                } else if (button == 1) {
                    // Right click — signal to ModuleScreen to open settings
                    return m;
                }
            }
        }
        return null;
    }

    public boolean startDrag(double mouseX, double mouseY) {
        if (isInHeader(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = (int) mouseX - state.x;
            dragOffsetY = (int) mouseY - state.y;
            return true;
        }
        return false;
    }

    public void drag(double mouseX, double mouseY) {
        if (!dragging) return;
        state.x = (int) mouseX - dragOffsetX;
        state.y = (int) mouseY - dragOffsetY;
    }

    public void stopDrag() {
        if (dragging) {
            dragging = false;
            saveState();
        }
    }

    public boolean handleKeyPress(int keyCode, Module rebinding) {
        if (rebinding == null) return false;
        if (rebindingModule == rebinding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                rebinding.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                rebinding.setKeyCode(keyCode);
            }
            rebindingModule = null;
            return true;
        }
        return false;
    }

    // ── State ─────────────────────────────────────────────────────────────

    public void setRebindingModule(Module m) { this.rebindingModule = m; }
    public boolean isDragging()              { return dragging; }
    public boolean isVisible()               { return state.visible; }
    public void setVisible(boolean v)        { state.visible = v; saveState(); }
    public Module.Category getCategory()     { return category; }
    public PanelState getState()             { return state; }

    private void saveState() {
        ProfileManager.getActive().setPanelState(category, state);
        ProfileManager.saveActive();
    }
}
