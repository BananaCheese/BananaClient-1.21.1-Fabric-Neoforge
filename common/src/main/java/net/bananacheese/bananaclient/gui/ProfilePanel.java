package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.profile.Profile;
import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ProfilePanel {

    private static final int WIDTH    = 140;
    private static final int HEADER_H = 18;
    private static final int ROW_H    = 16;
    private static final int FOOTER_H = 18;
    private static final int PAD      = 6;

    // Position persists across open/close within the session
    private static int panelX = 5;
    private static int panelY = 5;

    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    // Sub-menus owned by this panel, managed by ModuleScreen
    // We expose callbacks so ModuleScreen can create them
    private final Runnable onModulesClicked;
    private final Runnable onThemeClicked;
    private final java.util.function.BiConsumer<String, Integer[]> onContextMenu;
    // BiConsumer args: profile name, anchor [x,y]

    public ProfilePanel(Runnable onModulesClicked,
                        Runnable onThemeClicked,
                        java.util.function.BiConsumer<String, Integer[]> onContextMenu) {
        this.onModulesClicked = onModulesClicked;
        this.onThemeClicked   = onThemeClicked;
        this.onContextMenu    = onContextMenu;
    }

    // ── Geometry ──────────────────────────────────────────────────────────

    private int panelHeight() {
        return HEADER_H + ProfileManager.getAll().size() * ROW_H + FOOTER_H;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= panelX && mx <= panelX + WIDTH
                && my >= panelY && my <= panelY + panelHeight();
    }

    public boolean isInHeader(double mx, double my) {
        return mx >= panelX && mx <= panelX + WIDTH
                && my >= panelY && my <= panelY + HEADER_H;
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        Theme t = ThemeManager.get();
        int x = panelX;
        int y = panelY;
        int h = panelHeight();

        // Background + border
        gfx.fill(x, y, x + WIDTH, y + h, t.backgroundColor);
        gfx.renderOutline(x, y, WIDTH, h, t.borderColor);

        // Header
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.drawString(font, "Profiles", x + PAD, y + 5, t.headerTextColor, false);

        // Save button in header — right side
        String saveLabel = "Save";
        int saveX = x + WIDTH - font.width(saveLabel) - PAD - 2;
        boolean saveHovered = mouseX >= saveX - 2 && mouseX <= saveX + font.width(saveLabel) + 2
                && mouseY >= y + 3 && mouseY <= y + HEADER_H - 3;
        int saveColor = saveHovered ? t.accentColor : t.disabledTextColor;
        gfx.drawString(font, saveLabel, saveX, y + 5, saveColor, false);

        // Profile rows
        List<Profile> profiles = ProfileManager.getAll();
        String activeName = ProfileManager.getActive().name;

        for (int i = 0; i < profiles.size(); i++) {
            Profile p = profiles.get(i);
            int rowY = y + HEADER_H + i * ROW_H;
            boolean isActive = p.name.equals(activeName);
            boolean hovered  = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;

            if (hovered) gfx.fill(x, rowY, x + WIDTH, rowY + ROW_H, 0x18FFFFFF);

            // Active indicator strip
            gfx.fill(x, rowY, x + 2, rowY + ROW_H,
                    isActive ? t.accentColor : t.borderColor);

            int nameColor = isActive ? t.accentColor : t.disabledTextColor;
            gfx.drawString(font, p.name, x + PAD + 2, rowY + 4, nameColor, false);

            // Active dot
            if (isActive) {
                gfx.drawString(font, "●", x + WIDTH - font.width("●") - PAD,
                        rowY + 4, t.accentColor, false);
            }
        }

        // Footer with action buttons
        int footY = y + h - FOOTER_H;
        gfx.fill(x, footY, x + WIDTH, footY + FOOTER_H, t.headerColor);
        gfx.fill(x, footY, x + WIDTH, footY + 1, t.borderColor);

        // Three equal buttons: New | Modules | Theme
        int btnW = WIDTH / 3;
        String[] labels = { "+ New", "Modules", "Theme" };

        for (int i = 0; i < 3; i++) {
            int btnX = x + i * btnW;
            boolean h2 = mouseX >= btnX && mouseX <= btnX + btnW
                    && mouseY >= footY && mouseY <= footY + FOOTER_H;
            if (h2) gfx.fill(btnX, footY + 1, btnX + btnW, footY + FOOTER_H, 0x22FFFFFF);
            if (i > 0) gfx.fill(btnX, footY + 3, btnX + 1, footY + FOOTER_H - 3, t.borderColor);
            int bColor = h2 ? t.enabledTextColor : t.disabledTextColor;
            int labelX = btnX + (btnW - font.width(labels[i])) / 2;
            gfx.drawString(font, labels[i], labelX, footY + 5, bColor, false);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    // Returns true if click consumed. button=0 left, button=1 right.
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isInPanel(mx, my)) return false;

        int x = panelX;
        int y = panelY;

        // Header — drag or save button
        if (isInHeader(mx, my)) {
            // Check save button hit area
            Theme t = ThemeManager.get();
            // approximate save button right area
            int saveX = x + WIDTH - 30;
            if (mx >= saveX && button == 0) {
                ProfileManager.saveActive();
                return true;
            }
            // Otherwise start drag
            if (button == 0) {
                dragging = true;
                dragOffsetX = (int) mx - panelX;
                dragOffsetY = (int) my - panelY;
            }
            return true;
        }

        // Footer buttons
        int footY = y + panelHeight() - FOOTER_H;
        if (my >= footY && button == 0) {
            int btnW = WIDTH / 3;
            int col = (int)(mx - x) / btnW;
            if (col == 0) {
                // New — duplicate active profile with generated name
                Profile newP = ProfileManager.getActive().copy(
                        "profile_" + (ProfileManager.getAll().size() + 1)
                );
                ProfileManager.save(newP);
                ProfileManager.switchTo(newP.name);
                ThemeManager.apply(ProfileManager.getActive().theme);
            } else if (col == 1) {
                onModulesClicked.run();
            } else {
                onThemeClicked.run();
            }
            return true;
        }

        // Profile rows
        List<Profile> profiles = ProfileManager.getAll();
        for (int i = 0; i < profiles.size(); i++) {
            int rowY = y + HEADER_H + i * ROW_H;
            if (my >= rowY && my <= rowY + ROW_H) {
                String name = profiles.get(i).name;
                if (button == 0) {
                    // Left click — load profile
                    ProfileManager.switchTo(name);
                    ThemeManager.apply(ProfileManager.getActive().theme);
                } else if (button == 1) {
                    // Right click — open context menu
                    onContextMenu.accept(name, new Integer[]{ (int)mx, (int)my });
                }
                return true;
            }
        }

        return true;
    }

    public void drag(double mx, double my) {
        if (!dragging) return;
        panelX = (int) mx - dragOffsetX;
        panelY = (int) my - dragOffsetY;
    }

    public void stopDrag() { dragging = false; }
    public boolean isDragging() { return dragging; }
}
