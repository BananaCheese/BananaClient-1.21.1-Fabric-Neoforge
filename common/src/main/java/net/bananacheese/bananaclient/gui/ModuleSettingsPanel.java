package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.bananacheese.bananaclient.modules.RegistryListSetting;
import net.bananacheese.bananaclient.utils.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ModuleSettingsPanel {

    private static final int WIDTH    = 160;
    private static final int HEADER_H = 16;
    private static final int BOOL_H   = 20; // boolean toggle row
    private static final int SLIDER_H = 28; // numeric slider row — taller for the track
    private static final int PADDING  = 5;

    private final Module module;
    private int x, y;

    private RegistrySelectorPanel registrySelector = null;
    private RegistryListSetting activeSelectorSetting = null;

    // Drag state for sliders
    private int  draggingSettingIndex = -1;

    public ModuleSettingsPanel(Module module, int anchorX, int anchorY) {
        this.module = module;
        this.x = anchorX;
        this.y = anchorY;
    }

    // ── Geometry ──────────────────────────────────────────────────────────

    private int rowHeight(ModuleSetting<?> s) {
        return (s.isFloat() || s.isInteger()) ? SLIDER_H : BOOL_H;
    }

    public int getHeight() {
        List<ModuleSetting<?>> settings = module.getSettings();
        List<RegistryListSetting> regSettings = module.getRegistrySettings();
        if (settings.isEmpty() && regSettings.isEmpty())
            return HEADER_H + BOOL_H + PADDING;
        int total = HEADER_H;
        for (ModuleSetting<?> s : settings) total += rowHeight(s);
        total += regSettings.size() * BOOL_H;
        return total + PADDING;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= x && mx <= x + WIDTH
                && my >= y && my <= y + getHeight();
    }

    /** Y position of the top of row i */
    private int rowTop(int index) {
        int top = y + HEADER_H;
        List<ModuleSetting<?>> settings = module.getSettings();
        for (int i = 0; i < index; i++) top += rowHeight(settings.get(i));
        return top;
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        Theme t = ThemeManager.get();

        int bgColor = RenderUtil.applyOpacity(t.backgroundColor, t.backgroundOpacity);
        gfx.fill(x, y, x + WIDTH, y + getHeight(), bgColor);
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.renderOutline(x, y, WIDTH, getHeight(), t.borderColor);
        gfx.drawString(font, module.getName(), x + PADDING, y + 4, t.headerTextColor, false);

        List<ModuleSetting<?>> settings = module.getSettings();
        if (settings.isEmpty()) {
            gfx.drawString(font, "No settings", x + PADDING, y + HEADER_H + 6,
                    t.disabledTextColor, false);
            return;
        }

        // ── Block list settings ───────────────────────────────────────────────
        List<RegistryListSetting> regSettings = module.getRegistrySettings();
        int regRowStartY = rowTop(module.getSettings().size());

        for (int i = 0; i < regSettings.size(); i++) {
            RegistryListSetting rs = regSettings.get(i);
            int rowY = regRowStartY + i * BOOL_H;
            boolean isOpen = activeSelectorSetting == rs;

            gfx.drawString(font, rs.getName(), x + PADDING, rowY + 5,
                    t.disabledTextColor, false);

            String label = rs.size() + " entries \u25B6";
            gfx.drawString(font, label,
                    x + WIDTH - font.width(label) - PADDING, rowY + 5,
                    isOpen ? t.accentColor : t.enabledTextColor, false);

            gfx.fill(x + PADDING, rowY + BOOL_H - 1,
                    x + WIDTH - PADDING, rowY + BOOL_H, t.borderColor);
        }

        if (registrySelector != null) {
            registrySelector.render(gfx, font, mouseX, mouseY);
        }

        for (int i = 0; i < settings.size(); i++) {
            renderSetting(gfx, font, settings.get(i), rowTop(i), rowHeight(settings.get(i)));
        }
    }

    @SuppressWarnings("unchecked")
    private void renderSetting(GuiGraphics gfx, Font font,
                               ModuleSetting<?> s, int rowY, int rowH) {
        Theme t = ThemeManager.get();

        // Label
        gfx.drawString(font, s.getName(), x + PADDING, rowY + 5, t.disabledTextColor, false);

        // Row divider
        gfx.fill(x + PADDING, rowY + rowH - 1, x + WIDTH - PADDING, rowY + rowH, t.borderColor);

        if (s.isBoolean()) {
            ModuleSetting<Boolean> bs = (ModuleSetting<Boolean>) s;
            String val = bs.getValue() ? "ON" : "OFF";
            int col = bs.getValue() ? t.accentColor : t.disabledTextColor;
            gfx.drawString(font, val, x + WIDTH - font.width(val) - PADDING, rowY + 5, col, false);

        } else if (s.isFloat()) {
            ModuleSetting<Float> fs = (ModuleSetting<Float>) s;
            renderSlider(gfx, font, fs, rowY, rowH, t);

        } else if (s.isInteger()) {
            ModuleSetting<Integer> is = (ModuleSetting<Integer>) s;
            renderIntSlider(gfx, font, is, rowY, rowH, t);
        }
    }

    private void renderSlider(GuiGraphics gfx, Font font,
                              ModuleSetting<Float> s, int rowY, int rowH, Theme t) {
        float value = s.getValue();
        boolean bounded = s.hasBounds();
        float min = bounded ? (Float) s.getMin() : 0f;
        float max = bounded ? (Float) s.getMax() : 1f;
        float pct = bounded ? (value - min) / (max - min) : 0f;

        // Value text top-right
        String valStr = String.format("%.1f", value);
        gfx.drawString(font, valStr,
                x + WIDTH - font.width(valStr) - PADDING, rowY + 5,
                t.enabledTextColor, false);

        // Track
        int trackX = x + PADDING;
        int trackW = WIDTH - PADDING * 2;
        int trackY = rowY + rowH - 10;

        gfx.fill(trackX, trackY, trackX + trackW, trackY + 4, t.borderColor);
        int fillW = (int)(pct * trackW);
        gfx.fill(trackX, trackY, trackX + fillW, trackY + 4, t.accentColor);

        // Thumb
        int thumbX = trackX + fillW;
        gfx.fill(thumbX - 3, trackY - 2, thumbX + 3, trackY + 6, t.accentColor);

        // Min / Max labels
        if (bounded) {
            String minStr = String.format("%.0f", min);
            String maxStr = String.format("%.0f", max);
            gfx.drawString(font, minStr, trackX, trackY + 6, t.disabledTextColor, false);
            gfx.drawString(font, maxStr,
                    trackX + trackW - font.width(maxStr), trackY + 6,
                    t.disabledTextColor, false);
        }
    }

    private void renderIntSlider(GuiGraphics gfx, Font font,
                                 ModuleSetting<Integer> s, int rowY, int rowH, Theme t) {
        int value = s.getValue();
        boolean bounded = s.hasBounds();
        int min = bounded ? (Integer) s.getMin() : 0;
        int max = bounded ? (Integer) s.getMax() : 100;
        float pct = bounded ? (value - min) / (float)(max - min) : 0f;

        String valStr = String.valueOf(value);
        gfx.drawString(font, valStr,
                x + WIDTH - font.width(valStr) - PADDING, rowY + 5,
                t.enabledTextColor, false);

        int trackX = x + PADDING;
        int trackW = WIDTH - PADDING * 2;
        int trackY = rowY + rowH - 10;

        gfx.fill(trackX, trackY, trackX + trackW, trackY + 4, t.borderColor);
        int fillW = (int)(pct * trackW);
        gfx.fill(trackX, trackY, trackX + fillW, trackY + 4, t.accentColor);
        int thumbX = trackX + fillW;
        gfx.fill(thumbX - 3, trackY - 2, thumbX + 3, trackY + 6, t.accentColor);

        if (bounded) {
            String minStr = String.valueOf(min);
            String maxStr = String.valueOf(max);
            gfx.drawString(font, minStr, trackX, trackY + 6, t.disabledTextColor, false);
            gfx.drawString(font, maxStr,
                    trackX + trackW - font.width(maxStr), trackY + 6,
                    t.disabledTextColor, false);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isInPanel(mx, my)) return false;
        if (my < y + HEADER_H) return true;

        List<ModuleSetting<?>> settings = module.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            ModuleSetting<?> s = settings.get(i);
            int rowY = rowTop(i);
            int rowH = rowHeight(s);

            if (my >= rowY && my <= rowY + rowH) {
                if (s.isBoolean()) {
                    ModuleSetting<Boolean> bs = (ModuleSetting<Boolean>) s;
                    bs.setValue(!bs.getValue());

                } else if (s.isFloat() && s.hasBounds()) {
                    draggingSettingIndex = i;
                    applyFloatSlider((ModuleSetting<Float>) s, mx, rowY, rowH);

                } else if (s.isInteger() && s.hasBounds()) {
                    draggingSettingIndex = i;
                    applyIntSlider((ModuleSetting<Integer>) s, mx, rowY, rowH);
                }
                return true;
            }
        }

        // After regular settings loop in mouseClicked:
        List<RegistryListSetting> regSettings = module.getRegistrySettings();
        int regRowStartY = rowTop(module.getSettings().size());

        for (int i = 0; i < regSettings.size(); i++) {
            RegistryListSetting rs = regSettings.get(i);
            int rowY = regRowStartY + i * BOOL_H;
            if (my >= rowY && my <= rowY + BOOL_H) {
                if (registrySelector != null && activeSelectorSetting == rs) {
                    registrySelector = null;
                    activeSelectorSetting = null;
                } else {
                    activeSelectorSetting = rs;
                    registrySelector = new RegistrySelectorPanel(x + WIDTH + 4, rowY, rs);
                }
                return true;
            }
        }

        if (registrySelector != null) {
            if (registrySelector.isInPanel(mx, my)) {
                registrySelector.mouseClicked(mx, my, button);
                return true;
            } else {
                registrySelector = null;
                activeSelectorSetting = null;
            }
        }
        return true;
    }

    public boolean isRegistrySelectorOpen(double mx, double my) {
        return registrySelector != null && registrySelector.isInPanel(mx, my);
    }

    public void forwardClickToRegistrySelector(double mx, double my, int button) {
        if (registrySelector == null) return;
        registrySelector.mouseClicked(mx, my, button);
    }

    @SuppressWarnings("unchecked")
    public boolean mouseDragged(double mx, double my, int button) {
        if (draggingSettingIndex < 0) return false;
        List<ModuleSetting<?>> settings = module.getSettings();
        if (draggingSettingIndex >= settings.size()) return false;

        ModuleSetting<?> s = settings.get(draggingSettingIndex);
        int rowY = rowTop(draggingSettingIndex);
        int rowH = rowHeight(s);

        if (s.isFloat()) applyFloatSlider((ModuleSetting<Float>) s, mx, rowY, rowH);
        else if (s.isInteger()) applyIntSlider((ModuleSetting<Integer>) s, mx, rowY, rowH);

        if (registrySelector != null && registrySelector.isDragging()) {
            registrySelector.drag(mx, my); return true;
        }

        return true;
    }

    private void applyFloatSlider(ModuleSetting<Float> s, double mx, int rowY, int rowH) {
        int trackX = x + PADDING;
        int trackW = WIDTH - PADDING * 2;
        float pct = (float)((mx - trackX) / trackW);
        pct = Math.max(0f, Math.min(1f, pct));
        float min = (Float) s.getMin();
        float max = (Float) s.getMax();
        float raw = min + pct * (max - min);
        // Round to 1 decimal place
        s.setValue(s.clampFloat(Math.round(raw * 10) / 10f));
    }

    private void applyIntSlider(ModuleSetting<Integer> s, double mx, int rowY, int rowH) {
        int trackX = x + PADDING;
        int trackW = WIDTH - PADDING * 2;
        float pct = (float)((mx - trackX) / trackW);
        pct = Math.max(0f, Math.min(1f, pct));
        int min = (Integer) s.getMin();
        int max = (Integer) s.getMax();
        s.setValue(s.clampInt(Math.round(min + pct * (max - min))));
    }

    public void mouseReleased() {
        draggingSettingIndex = -1;
        if (registrySelector != null) registrySelector.stopDrag();
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (registrySelector != null && registrySelector.isInPanel(mx, my)) {
            return registrySelector.mouseScrolled(mx, my, delta);
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (registrySelector != null) return registrySelector.keyPressed(keyCode);
        return false;
    }

    public void charTyped(char c) {
        if (registrySelector != null) registrySelector.charTyped(c);
    }

    public Module getModule() { return module; }
}