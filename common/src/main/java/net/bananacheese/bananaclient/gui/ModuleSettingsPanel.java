package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ModuleSettingsPanel {

    private static final int WIDTH   = 140;
    private static final int ROW_H   = 20;
    private static final int HEADER_H = 16;
    private static final int PADDING  = 5;

    private final Module module;
    private int x, y;

    public ModuleSettingsPanel(Module module, int anchorX, int anchorY) {
        this.module = module;
        this.x = anchorX;
        this.y = anchorY;
    }

    public int getHeight() {
        int rows = Math.max(1, module.getSettings().size());
        return HEADER_H + rows * ROW_H + PADDING;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= x && mx <= x + WIDTH
                && my >= y && my <= y + getHeight();
    }

    public void render(GuiGraphics gfx, Font font) {
        Theme t = ThemeManager.get();

        // Background + header
        gfx.fill(x, y, x + WIDTH, y + getHeight(), t.backgroundColor);
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.drawString(font, module.getName(), x + PADDING, y + 4, t.headerTextColor, false);
        gfx.renderOutline(x, y, WIDTH, getHeight(), t.borderColor);

        List<ModuleSetting<?>> settings = module.getSettings();

        if (settings.isEmpty()) {
            gfx.drawString(font, "No settings", x + PADDING, y + HEADER_H + 6,
                    t.disabledTextColor, false);
            return;
        }

        for (int i = 0; i < settings.size(); i++) {
            ModuleSetting<?> s = settings.get(i);
            int rowY = y + HEADER_H + i * ROW_H;
            renderSetting(gfx, font, s, rowY);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderSetting(GuiGraphics gfx, Font font, ModuleSetting<?> s, int rowY) {
        Theme t = ThemeManager.get();
        gfx.drawString(font, s.getName(), x + PADDING, rowY + 6, t.disabledTextColor, false);

        if (s.isBoolean()) {
            ModuleSetting<Boolean> bs = (ModuleSetting<Boolean>) s;
            String val = bs.getValue() ? "ON" : "OFF";
            int col = bs.getValue() ? t.accentColor : t.disabledTextColor;
            int valX = x + WIDTH - font.width(val) - PADDING;
            gfx.drawString(font, val, valX, rowY + 6, col, false);
        } else if (s.isInteger() || s.isFloat()) {
            String val = String.valueOf(s.getValue());
            int valX = x + WIDTH - font.width(val) - PADDING;
            gfx.drawString(font, val, valX, rowY + 6, t.enabledTextColor, false);
        }

        // Subtle row divider
        gfx.fill(x + PADDING, rowY + ROW_H - 1,
                x + WIDTH - PADDING, rowY + ROW_H, t.borderColor);
    }

    @SuppressWarnings("unchecked")
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isInPanel(mx, my)) return false;
        if (my < y + HEADER_H) return true; // header click, consume but do nothing

        List<ModuleSetting<?>> settings = module.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            ModuleSetting<?> s = settings.get(i);
            int rowY = y + HEADER_H + i * ROW_H;
            if (my >= rowY && my <= rowY + ROW_H) {
                if (s.isBoolean()) {
                    ModuleSetting<Boolean> bs = (ModuleSetting<Boolean>) s;
                    bs.setValue(!bs.getValue());
                }
                // Integer/Float sliders and color pickers come in a later pass
                return true;
            }
        }
        return true;
    }

    public Module getModule() { return module; }
}
