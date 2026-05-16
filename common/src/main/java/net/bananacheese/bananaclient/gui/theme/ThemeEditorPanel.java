package net.bananacheese.bananaclient.gui.theme;

import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ThemeEditorPanel {

    private static final int WIDTH    = 180;
    private static final int HEADER_H = 18;
    private static final int TAB_H    = 16;
    private static final int ROW_H    = 18;
    private static final int PAD      = 6;
    private static final int MAX_ROWS = 10;

    // Position
    private int x, y;

    // Dragging
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    // Tab state — 0=Presets, 1=Colors, 2=Style
    private int activeTab = 0;

    // Color editing state
    private int    editingColorField = -1; // index into COLOR_FIELDS
    private final StringBuilder hexInput = new StringBuilder();

    // Scroll for colors tab
    private int scrollOffset = 0;

    // Color field descriptors — name + getter + setter via index
    // Order matches the rows rendered in the Colors tab
    private static final String[] COLOR_LABELS = {
            "Background", "Header", "Border", "Accent",
            "Text On",    "Text Off", "Header Text", "Category Text",
            "Badge BG",   "Badge Text"
    };

    public ThemeEditorPanel(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // ── Geometry ──────────────────────────────────────────────────────────

    private int contentRows() {
        return switch (activeTab) {
            case 0 -> ThemePresets.all().size() + 1; // presets + divider
            case 1 -> Math.min(COLOR_LABELS.length, MAX_ROWS);
            case 2 -> 4; // 3 booleans + 1 float
            default -> 0;
        };
    }

    private int panelHeight() {
        int inputH = (editingColorField >= 0) ? 22 : 0;
        return HEADER_H + TAB_H + contentRows() * ROW_H + PAD + inputH;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= x && mx <= x + WIDTH
                && my >= y && my <= y + panelHeight();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int getColor(Theme t, int index) {
        return switch (index) {
            case 0 -> t.backgroundColor;
            case 1 -> t.headerColor;
            case 2 -> t.borderColor;
            case 3 -> t.accentColor;
            case 4 -> t.enabledTextColor;
            case 5 -> t.disabledTextColor;
            case 6 -> t.headerTextColor;
            case 7 -> t.categoryTextColor;
            case 8 -> t.keyBadgeBackground;
            case 9 -> t.keyBadgeText;
            default -> 0;
        };
    }

    private void setColor(Theme t, int index, int value) {
        switch (index) {
            case 0 -> t.backgroundColor    = value;
            case 1 -> t.headerColor        = value;
            case 2 -> t.borderColor        = value;
            case 3 -> t.accentColor        = value;
            case 4 -> t.enabledTextColor   = value;
            case 5 -> t.disabledTextColor  = value;
            case 6 -> t.headerTextColor    = value;
            case 7 -> t.categoryTextColor  = value;
            case 8 -> t.keyBadgeBackground = value;
            case 9 -> t.keyBadgeText       = value;
        }
        ThemeManager.apply(t);
    }

    private String toHex(int argb) {
        return String.format("%08X", argb);
    }

    private int fromHex(String hex) {
        try {
            if (hex.length() == 6) hex = "FF" + hex;
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFF000000;
        }
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        Theme t = ThemeManager.get();

        // Panel background + header
        gfx.fill(x, y, x + WIDTH, y + panelHeight(), t.backgroundColor);
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.renderOutline(x, y, WIDTH, panelHeight(), t.borderColor);
        gfx.drawString(font, "Theme: " + t.name, x + PAD, y + 5, t.headerTextColor, false);

        // Tabs
        int tabY = y + HEADER_H;
        int tabW = WIDTH / 3;
        String[] tabNames = { "Presets", "Colors", "Style" };
        for (int i = 0; i < 3; i++) {
            int tabX = x + i * tabW;
            boolean active = activeTab == i;
            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabW
                    && mouseY >= tabY && mouseY <= tabY + TAB_H;
            int tabBg = active ? t.accentColor & 0x00FFFFFF | 0x33000000 : t.headerColor;
            gfx.fill(tabX, tabY, tabX + tabW, tabY + TAB_H, tabBg);
            if (hovered && !active)
                gfx.fill(tabX, tabY, tabX + tabW, tabY + TAB_H, 0x18FFFFFF);
            // Bottom line on active tab
            if (active)
                gfx.fill(tabX, tabY + TAB_H - 1, tabX + tabW, tabY + TAB_H, t.accentColor);
            int tColor = active ? t.accentColor : t.disabledTextColor;
            int labelX = tabX + (tabW - font.width(tabNames[i])) / 2;
            gfx.drawString(font, tabNames[i], labelX, tabY + 4, tColor, false);
        }

        // Content area
        int contentY = tabY + TAB_H;
        switch (activeTab) {
            case 0 -> renderPresetsTab(gfx, font, contentY, mouseX, mouseY, t);
            case 1 -> renderColorsTab(gfx, font, contentY, mouseX, mouseY, t);
            case 2 -> renderStyleTab(gfx, font, contentY, mouseX, mouseY, t);
        }
    }

    private void renderPresetsTab(GuiGraphics gfx, Font font,
                                  int contentY, int mouseX, int mouseY, Theme t) {
        List<Theme> presets = ThemePresets.all();
        for (int i = 0; i < presets.size(); i++) {
            Theme preset = presets.get(i);
            int rowY = contentY + i * ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;
            boolean isCurrent = preset.name.equals(t.name);

            if (hovered || isCurrent)
                gfx.fill(x, rowY, x + WIDTH, rowY + ROW_H, 0x18FFFFFF);

            // Color swatch — shows the preset's accent color
            int swatchX = x + PAD;
            int swatchY = rowY + (ROW_H - 8) / 2;
            gfx.fill(swatchX, swatchY, swatchX + 12, swatchY + 8, preset.accentColor);
            gfx.renderOutline(swatchX, swatchY, 12, 8, t.borderColor);

            int nameColor = isCurrent ? t.accentColor : t.enabledTextColor;
            gfx.drawString(font, preset.name, x + PAD + 16, rowY + 5, nameColor, false);

            if (isCurrent)
                gfx.drawString(font, "✓", x + WIDTH - font.width("✓") - PAD,
                        rowY + 5, t.accentColor, false);
        }
    }

    private void renderColorsTab(GuiGraphics gfx, Font font,
                                 int contentY, int mouseX, int mouseY, Theme t) {
        int startIdx = Math.max(0, Math.min(scrollOffset, COLOR_LABELS.length - MAX_ROWS));

        for (int i = 0; i < Math.min(COLOR_LABELS.length, MAX_ROWS); i++) {
            int fieldIdx = startIdx + i;
            if (fieldIdx >= COLOR_LABELS.length) break;

            int rowY = contentY + i * ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;
            boolean isEditing = editingColorField == fieldIdx;

            if (hovered || isEditing)
                gfx.fill(x, rowY, x + WIDTH, rowY + ROW_H, 0x18FFFFFF);

            // Label
            int labelColor = isEditing ? t.accentColor : t.disabledTextColor;
            gfx.drawString(font, COLOR_LABELS[fieldIdx], x + PAD, rowY + 5, labelColor, false);

            // Color swatch on right
            int colorVal = getColor(t, fieldIdx);
            int swatchW  = 20;
            int swatchX  = x + WIDTH - swatchW - PAD;
            int swatchY  = rowY + (ROW_H - 8) / 2;
            gfx.fill(swatchX, swatchY, swatchX + swatchW, swatchY + 8, colorVal);
            gfx.renderOutline(swatchX, swatchY, swatchW, 8,
                    isEditing ? t.accentColor : t.borderColor);
        }

        // Hex input field shown below the list when editing
        if (editingColorField >= 0) {
            int inputY = contentY + Math.min(COLOR_LABELS.length, MAX_ROWS) * ROW_H + 2;
            gfx.fill(x + PAD, inputY, x + WIDTH - PAD, inputY + 16, 0xFF000000);
            gfx.renderOutline(x + PAD, inputY, WIDTH - PAD * 2, 16, t.accentColor);
            String display = "#" + hexInput + (((System.currentTimeMillis() / 500) % 2 == 0) ? "|" : "");
            gfx.drawString(font, display, x + PAD + 3, inputY + 4, t.enabledTextColor, false);
        }

        // Scroll indicator
        if (COLOR_LABELS.length > MAX_ROWS) {
            gfx.drawString(font,
                    (startIdx + Math.min(COLOR_LABELS.length, MAX_ROWS)) + "/" + COLOR_LABELS.length,
                    x + WIDTH - 30, y + panelHeight() - 10, t.disabledTextColor, false);
        }
    }

    private void renderStyleTab(GuiGraphics gfx, Font font,
                                int contentY, int mouseX, int mouseY, Theme t) {
        // Boolean rows
        String[][] boolRows = {
                { "Toggle Switches", String.valueOf(t.showToggleSwitches) },
                { "Category Labels", String.valueOf(t.showCategoryLabels) },
                { "Rounded Corners", String.valueOf(t.roundedCorners) }
        };

        for (int i = 0; i < boolRows.length; i++) {
            int rowY = contentY + i * ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;
            if (hovered) gfx.fill(x, rowY, x + WIDTH, rowY + ROW_H, 0x18FFFFFF);

            gfx.drawString(font, boolRows[i][0], x + PAD, rowY + 5, t.disabledTextColor, false);

            boolean val = boolRows[i][1].equals("true");
            String indicator = val ? "ON" : "OFF";
            int iColor = val ? t.accentColor : t.disabledTextColor;
            gfx.drawString(font, indicator,
                    x + WIDTH - font.width(indicator) - PAD, rowY + 5, iColor, false);
        }

        // Opacity slider
        int sliderRowY = contentY + boolRows.length * ROW_H;
        gfx.drawString(font, "Opacity", x + PAD, sliderRowY + 5, t.disabledTextColor, false);

        int sliderX = x + PAD + 55;
        int sliderW = WIDTH - PAD - 55 - PAD;
        int sliderY = sliderRowY + ROW_H / 2 - 2;

        // Track
        gfx.fill(sliderX, sliderY, sliderX + sliderW, sliderY + 4, t.borderColor);
        // Fill
        int fillW = (int)(sliderW * t.backgroundOpacity);
        gfx.fill(sliderX, sliderY, sliderX + fillW, sliderY + 4, t.accentColor);
        // Thumb
        int thumbX = sliderX + fillW - 3;
        gfx.fill(thumbX, sliderY - 2, thumbX + 6, sliderY + 6, t.accentColor);

        // Value label
        String opVal = Math.round(t.backgroundOpacity * 100) + "%";
        gfx.drawString(font, opVal,
                x + WIDTH - font.width(opVal) - PAD, sliderRowY + 5, t.enabledTextColor, false);
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

        // Tab clicks
        int tabY = y + HEADER_H;
        if (my >= tabY && my <= tabY + TAB_H) {
            int tabW = WIDTH / 3;
            int col = (int)(mx - x) / tabW;
            if (col >= 0 && col <= 2) {
                activeTab = col;
                editingColorField = -1;
                hexInput.setLength(0);
                scrollOffset = 0;
            }
            return true;
        }

        // Content clicks
        int contentY = tabY + TAB_H;
        switch (activeTab) {
            case 0 -> handlePresetsClick(mx, my, contentY);
            case 1 -> handleColorsClick(mx, my, contentY);
            case 2 -> handleStyleClick(mx, my, contentY);
        }
        return true;
    }

    private void handlePresetsClick(double mx, double my, int contentY) {
        List<Theme> presets = ThemePresets.all();
        for (int i = 0; i < presets.size(); i++) {
            int rowY = contentY + i * ROW_H;
            if (my >= rowY && my <= rowY + ROW_H) {
                Theme copy = presets.get(i).copy();
                copy.name = presets.get(i).name; // keep original name, not "(copy)"
                ThemeManager.apply(copy);
                ProfileManager.getActive().theme = copy;
                return;
            }
        }
    }

    private void handleColorsClick(double mx, double my, int contentY) {
        // If clicking while hex input is active, try to commit
        if (editingColorField >= 0) {
            int inputY = contentY + Math.min(COLOR_LABELS.length, MAX_ROWS) * ROW_H + 2;
            if (my >= inputY && my <= inputY + 16) return; // click in input field, ignore
            // Click elsewhere — commit if valid
            commitHexInput();
        }

        int startIdx = Math.max(0, Math.min(scrollOffset, COLOR_LABELS.length - MAX_ROWS));
        for (int i = 0; i < Math.min(COLOR_LABELS.length, MAX_ROWS); i++) {
            int fieldIdx = startIdx + i;
            int rowY = contentY + i * ROW_H;
            if (my >= rowY && my <= rowY + ROW_H) {
                if (editingColorField == fieldIdx) {
                    // Already editing this one — commit
                    commitHexInput();
                } else {
                    // Start editing
                    editingColorField = fieldIdx;
                    hexInput.setLength(0);
                    hexInput.append(toHex(getColor(ThemeManager.get(), fieldIdx)));
                }
                return;
            }
        }
    }

    private void handleStyleClick(double mx, double my, int contentY) {
        Theme t = ThemeManager.get();

        // Boolean rows
        for (int i = 0; i < 3; i++) {
            int rowY = contentY + i * ROW_H;
            if (my >= rowY && my <= rowY + ROW_H) {
                switch (i) {
                    case 0 -> t.showToggleSwitches = !t.showToggleSwitches;
                    case 1 -> t.showCategoryLabels = !t.showCategoryLabels;
                    case 2 -> t.roundedCorners     = !t.roundedCorners;
                }
                ThemeManager.apply(t);
                return;
            }
        }

        // Opacity slider
        int sliderRowY = contentY + 3 * ROW_H;
        if (my >= sliderRowY && my <= sliderRowY + ROW_H) {
            int sliderX = x + PAD + 55;
            int sliderW = WIDTH - PAD - 55 - PAD;
            if (mx >= sliderX && mx <= sliderX + sliderW) {
                t.backgroundOpacity = (float)((mx - sliderX) / sliderW);
                t.backgroundOpacity = Math.max(0.1f, Math.min(1.0f, t.backgroundOpacity));
                ThemeManager.apply(t);
            }
        }
    }

    private void commitHexInput() {
        if (editingColorField < 0) return;
        String hex = hexInput.toString().trim().replace("#", "");
        if (hex.length() == 6 || hex.length() == 8) {
            setColor(ThemeManager.get(), editingColorField, fromHex(hex));
        }
        editingColorField = -1;
        hexInput.setLength(0);
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isInPanel(mx, my)) return false;
        if (activeTab == 1) {
            scrollOffset = Math.max(0,
                    Math.min(COLOR_LABELS.length - MAX_ROWS,
                            scrollOffset - (int) Math.signum(delta)));
        }
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (editingColorField < 0) return false;
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            commitHexInput();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            editingColorField = -1;
            hexInput.setLength(0);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && hexInput.length() > 0) {
            hexInput.deleteCharAt(hexInput.length() - 1);
            return true;
        }
        return false;
    }

    public void charTyped(char c) {
        if (editingColorField < 0) return;
        // Only allow hex characters, max 8 chars (AARRGGBB)
        if (hexInput.length() < 8 && "0123456789AaBbCcDdEeFf".indexOf(c) >= 0) {
            hexInput.append(Character.toUpperCase(c));
        }
    }

    public void drag(double mx, double my) {
        if (!dragging) return;
        x = (int) mx - dragOffsetX;
        y = (int) my - dragOffsetY;
    }

    public void stopDrag()      { dragging = false; }
    public boolean isDragging() { return dragging; }
}