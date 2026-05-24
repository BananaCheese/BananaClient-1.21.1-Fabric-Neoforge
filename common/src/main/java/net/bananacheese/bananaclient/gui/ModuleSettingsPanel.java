package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.CycleSetting;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.bananacheese.bananaclient.modules.RegistryListSetting;
import net.bananacheese.bananaclient.utils.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ModuleSettingsPanel {

    private static final int WIDTH    = 160;
    private static final int HEADER_H = 16;
    private static final int BOOL_H   = 20;
    private static final int SLIDER_H = 28;
    private static final int PADDING  = 5;

    private final Module module;
    private int x, y;

    // Slider drag
    private int draggingSettingIndex = -1;

    // Registry selector
    private RegistrySelectorPanel registrySelector     = null;
    private RegistryListSetting   activeSelectorSetting = null;

    // Keybind rebinding
    private boolean rebinding = false;

    public ModuleSettingsPanel(Module module, int anchorX, int anchorY) {
        this.module = module;
        this.x      = anchorX;
        this.y      = anchorY;
    }

    // ── Geometry ──────────────────────────────────────────────────────────

    private int rowHeight(ModuleSetting<?> s) {
        return (s.isFloat() || s.isInteger()) ? SLIDER_H : BOOL_H;
    }

    public int getHeight() {
        int total = HEADER_H;
        total += module.getCycleSettings().size() * BOOL_H;
        for (ModuleSetting<?> s : module.getSettings()) total += rowHeight(s);
        total += module.getRegistrySettings().size() * BOOL_H;
        total += BOOL_H; // keybind row — always present
        return total + PADDING;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= x && mx <= x + WIDTH
                && my >= y && my <= y + getHeight();
    }

    // ── Y position helpers ────────────────────────────────────────────────

    private int cycleRowTop(int i) {
        return y + HEADER_H + i * BOOL_H;
    }

    private int regularRowTop(int i) {
        int base = y + HEADER_H + module.getCycleSettings().size() * BOOL_H;
        List<ModuleSetting<?>> settings = module.getSettings();
        for (int j = 0; j < i; j++) base += rowHeight(settings.get(j));
        return base;
    }

    private int registryRowTop(int i) {
        int base = y + HEADER_H + module.getCycleSettings().size() * BOOL_H;
        for (ModuleSetting<?> s : module.getSettings()) base += rowHeight(s);
        return base + i * BOOL_H;
    }

    private int keybindRowTop() {
        int base = y + HEADER_H + module.getCycleSettings().size() * BOOL_H;
        for (ModuleSetting<?> s : module.getSettings()) base += rowHeight(s);
        base += module.getRegistrySettings().size() * BOOL_H;
        return base;
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        Theme t = ThemeManager.get();

        int bgColor = RenderUtil.applyOpacity(t.backgroundColor, t.backgroundOpacity);
        gfx.fill(x, y, x + WIDTH, y + getHeight(), bgColor);
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.renderOutline(x, y, WIDTH, getHeight(), t.borderColor);
        gfx.drawString(font, module.getName(), x + PADDING, y + 4, t.headerTextColor, false);

        // ── 1. Cycle settings ─────────────────────────────────────────────
        for (int i = 0; i < module.getCycleSettings().size(); i++) {
            CycleSetting cs = module.getCycleSettings().get(i);
            int rowY = cycleRowTop(i);
            gfx.drawString(font, cs.getName(), x + PADDING, rowY + 5,
                    t.disabledTextColor, false);
            String val = "\u25C4 " + cs.getValue() + " \u25BA";
            gfx.drawString(font, val,
                    x + WIDTH - font.width(val) - PADDING, rowY + 5,
                    t.accentColor, false);
            gfx.fill(x + PADDING, rowY + BOOL_H - 1,
                    x + WIDTH - PADDING, rowY + BOOL_H, t.borderColor);
        }

        // ── 2. Regular settings ───────────────────────────────────────────
        List<ModuleSetting<?>> settings = module.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            renderSetting(gfx, font, settings.get(i),
                    regularRowTop(i), rowHeight(settings.get(i)));
        }

        // ── 3. Registry settings ──────────────────────────────────────────
        List<RegistryListSetting> regSettings = module.getRegistrySettings();
        for (int i = 0; i < regSettings.size(); i++) {
            RegistryListSetting rs = regSettings.get(i);
            int rowY = registryRowTop(i);
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

        // ── 4. Keybind row — always shown ─────────────────────────────────
        int kbY = keybindRowTop();
        gfx.drawString(font, "Keybind", x + PADDING, kbY + 5,
                t.disabledTextColor, false);

        // Show "..." while waiting for input, otherwise show current key
        String keyLabel = rebinding ? "\u2022\u2022\u2022" : module.getKeyName();
        int keyColor = rebinding ? t.accentColor : t.keyBadgeText;

        // Draw key badge background
        int badgeW = font.width(keyLabel) + 6;
        int badgeX = x + WIDTH - badgeW - PADDING;
        int badgeY = kbY + (BOOL_H - 10) / 2;
        if (t.keyBadgeBackground != 0)
            gfx.fill(badgeX - 1, badgeY - 1, badgeX + badgeW + 1, badgeY + 11,
                    t.keyBadgeBackground);
        gfx.drawString(font, keyLabel, badgeX + 3, badgeY + 1, keyColor, false);

        // Blinking border when rebinding
        if (rebinding && (System.currentTimeMillis() / 500) % 2 == 0)
            gfx.renderOutline(badgeX - 2, badgeY - 2, badgeW + 4, 13, t.accentColor);

        gfx.fill(x + PADDING, kbY + BOOL_H - 1,
                x + WIDTH - PADDING, kbY + BOOL_H, t.borderColor);

        // Registry selector on top
        if (registrySelector != null) {
            registrySelector.render(gfx, font, mouseX, mouseY);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderSetting(GuiGraphics gfx, Font font,
                               ModuleSetting<?> s, int rowY, int rowH) {
        Theme t = ThemeManager.get();
        gfx.drawString(font, s.getName(), x + PADDING, rowY + 5,
                t.disabledTextColor, false);
        gfx.fill(x + PADDING, rowY + rowH - 1,
                x + WIDTH - PADDING, rowY + rowH, t.borderColor);

        if (s.isBoolean()) {
            ModuleSetting<Boolean> bs = (ModuleSetting<Boolean>) s;
            String val = bs.getValue() ? "ON" : "OFF";
            int col = bs.getValue() ? t.accentColor : t.disabledTextColor;
            gfx.drawString(font, val,
                    x + WIDTH - font.width(val) - PADDING, rowY + 5, col, false);
        } else if (s.isFloat()) {
            renderSlider(gfx, font, (ModuleSetting<Float>) s, rowY, rowH, t);
        } else if (s.isInteger()) {
            renderIntSlider(gfx, font, (ModuleSetting<Integer>) s, rowY, rowH, t);
        }
    }

    private void renderSlider(GuiGraphics gfx, Font font,
                              ModuleSetting<Float> s, int rowY, int rowH, Theme t) {
        float value   = s.getValue();
        boolean bounded = s.hasBounds();
        float min = bounded ? (Float) s.getMin() : 0f;
        float max = bounded ? (Float) s.getMax() : 1f;
        float pct = bounded ? (value - min) / (max - min) : 0f;

        String valStr = String.format("%.1f", value);
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
        int value   = s.getValue();
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

        // Cancel rebinding on any click
        if (rebinding) { rebinding = false; return true; }

        // ── Cycle settings ────────────────────────────────────────────────
        for (int i = 0; i < module.getCycleSettings().size(); i++) {
            int rowY = cycleRowTop(i);
            if (my >= rowY && my <= rowY + BOOL_H) {
                if (button == 0) module.getCycleSettings().get(i).next();
                else if (button == 1) module.getCycleSettings().get(i).prev();
                return true;
            }
        }

        // ── Regular settings ──────────────────────────────────────────────
        List<ModuleSetting<?>> settings = module.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            ModuleSetting<?> s = settings.get(i);
            int rowY = regularRowTop(i);
            int rowH = rowHeight(s);
            if (my >= rowY && my <= rowY + rowH) {
                if (s.isBoolean()) {
                    ((ModuleSetting<Boolean>) s).setValue(!((ModuleSetting<Boolean>) s).getValue());
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

        // ── Registry settings ─────────────────────────────────────────────
        if (registrySelector != null && registrySelector.isInPanel(mx, my)) {
            registrySelector.mouseClicked(mx, my, button);
            return true;
        }

        List<RegistryListSetting> regSettings = module.getRegistrySettings();
        for (int i = 0; i < regSettings.size(); i++) {
            RegistryListSetting rs = regSettings.get(i);
            int rowY = registryRowTop(i);
            if (my >= rowY && my <= rowY + BOOL_H) {
                if (activeSelectorSetting == rs) {
                    registrySelector      = null;
                    activeSelectorSetting = null;
                } else {
                    activeSelectorSetting = rs;
                    registrySelector = new RegistrySelectorPanel(x + WIDTH + 4, rowY, rs);
                }
                return true;
            }
        }

        // Close registry selector on outside click
        if (registrySelector != null) {
            registrySelector      = null;
            activeSelectorSetting = null;
        }

        // ── Keybind row ───────────────────────────────────────────────────
        int kbY = keybindRowTop();
        if (my >= kbY && my <= kbY + BOOL_H) {
            rebinding = !rebinding; // toggle rebinding mode
            return true;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean mouseDragged(double mx, double my, int button) {
        if (registrySelector != null && registrySelector.isDragging()) {
            registrySelector.drag(mx, my);
            return true;
        }
        if (draggingSettingIndex < 0) return false;
        List<ModuleSetting<?>> settings = module.getSettings();
        if (draggingSettingIndex >= settings.size()) return false;
        ModuleSetting<?> s = settings.get(draggingSettingIndex);
        int rowY = regularRowTop(draggingSettingIndex);
        int rowH = rowHeight(s);
        if (s.isFloat()) applyFloatSlider((ModuleSetting<Float>) s, mx, rowY, rowH);
        else if (s.isInteger()) applyIntSlider((ModuleSetting<Integer>) s, mx, rowY, rowH);
        return true;
    }

    public void mouseReleased() {
        draggingSettingIndex = -1;
        if (registrySelector != null) registrySelector.stopDrag();
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (registrySelector != null && registrySelector.isInPanel(mx, my))
            return registrySelector.mouseScrolled(mx, my, delta);
        return false;
    }

    public boolean keyPressed(int keyCode) {
        // Keybind capture takes priority
        if (rebinding) {
            module.setKeyCode(keyCode == GLFW.GLFW_KEY_ESCAPE
                    ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            rebinding = false;
            return true;
        }
        if (registrySelector != null) return registrySelector.keyPressed(keyCode);
        return false;
    }

    public void charTyped(char c) {
        if (registrySelector != null) registrySelector.charTyped(c);
    }

    public boolean isRegistrySelectorOpen(double mx, double my) {
        return registrySelector != null && registrySelector.isInPanel(mx, my);
    }

    public void forwardClickToRegistrySelector(double mx, double my, int button) {
        if (registrySelector != null) registrySelector.mouseClicked(mx, my, button);
    }

    private void applyFloatSlider(ModuleSetting<Float> s, double mx, int rowY, int rowH) {
        int trackX = x + PADDING;
        int trackW = WIDTH - PADDING * 2;
        float pct = Math.clamp((float)((mx - trackX) / trackW), 0f, 1f);
        float min = (Float) s.getMin();
        float max = (Float) s.getMax();
        s.setValue(s.clampFloat(Math.round((min + pct * (max - min)) * 10) / 10f));
    }

    private void applyIntSlider(ModuleSetting<Integer> s, double mx, int rowY, int rowH) {
        int trackX = x + PADDING;
        int trackW = WIDTH - PADDING * 2;
        float pct = Math.clamp((float)((mx - trackX) / trackW), 0f, 1f);
        int min = (Integer) s.getMin();
        int max = (Integer) s.getMax();
        s.setValue(s.clampInt(Math.round(min + pct * (max - min))));
    }

    public Module getModule() { return module; }
}