package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.profile.PanelState;
import net.bananacheese.bananaclient.gui.profile.Profile;
import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeEditorPanel;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.utils.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleScreen extends Screen {

    // ── Constants ─────────────────────────────────────────────────────────
    private static final int PP_WIDTH    = 140;
    private static final int PP_HEADER_H = 18;
    private static final int PP_ROW_H    = 16;
    private static final int PP_FOOTER_H = 18;
    private static final int PP_PAD      = 6;

    // ── Panels ────────────────────────────────────────────────────────────
    private final List<CategoryPanel> categoryPanels = new ArrayList<>();

    // Profile panel drag state — position lives in ProfileManager.getActive().profilePanelState
    private boolean ppDragging = false;
    private int     ppDragOffX, ppDragOffY;
    private int     ppDragStartX, ppDragStartY;
    private boolean ppCollapsed = false;

    // Overlays
    private ModuleVisibilityPanel visibilityPanel = null;
    private ModuleSettingsPanel   settingsPanel   = null;
    private ContextMenu           contextMenu     = null;
    private RenameDialog          renameDialog    = null;
    private ThemeEditorPanel      themeEditor     = null;

    private Module rebindingModule = null;

    public ModuleScreen() {
        super(Component.literal("BananaClient"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Profile panel geometry ─────────────────────────────────────────────

    private PanelState ppState() {
        return ProfileManager.getActive().profilePanelState;
    }

    private int ppHeight() {
        if (ppCollapsed) return PP_HEADER_H;
        return PP_HEADER_H + ProfileManager.getAll().size() * PP_ROW_H + PP_FOOTER_H;
    }

    private boolean ppInHeader(double mx, double my) {
        PanelState s = ppState();
        return mx >= s.x && mx <= s.x + PP_WIDTH
                && my >= s.y && my <= s.y + PP_HEADER_H;
    }

    private boolean ppInPanel(double mx, double my) {
        PanelState s = ppState();
        return mx >= s.x && mx <= s.x + PP_WIDTH
                && my >= s.y && my <= s.y + ppHeight();
    }

    // ── Init ──────────────────────────────────────────────────────────────

    @Override
    public void init() {
        categoryPanels.clear();
        for (Module.Category cat : Module.Category.values()) {
            CategoryPanel panel = new CategoryPanel(cat);
            panel.setOnHeaderRightClick(() -> openPanelContextMenu(panel));
            categoryPanels.add(panel);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        for (CategoryPanel p : categoryPanels)
            p.render(gfx, font, mouseX, mouseY);

        renderProfilePanel(gfx, mouseX, mouseY);

        if (visibilityPanel != null)
            visibilityPanel.render(gfx, font, mouseX, mouseY);
        if (themeEditor != null)
            themeEditor.render(gfx, font, mouseX, mouseY);
        if (settingsPanel != null)
            settingsPanel.render(gfx, font, mouseX, mouseY);
        if (contextMenu != null)
            contextMenu.render(gfx, font, mouseX, mouseY);
        if (renameDialog != null)
            renameDialog.render(gfx, font);
    }

    private void renderProfilePanel(GuiGraphics gfx, int mouseX, int mouseY) {
        Theme t = ThemeManager.get();
        PanelState s = ppState();
        int x = s.x, y = s.y, w = PP_WIDTH, h = ppHeight();
        int bgColor = RenderUtil.applyOpacity(t.backgroundColor, t.backgroundOpacity);
        gfx.fill(x, y, x + w, y + h, bgColor);
        gfx.fill(x, y, x + w, y + PP_HEADER_H, t.headerColor);
        gfx.renderOutline(x, y, w, h, t.borderColor);

        // Header text with lock indicator — same pattern as CategoryPanel
        gfx.drawString(font, "Profiles" + (s.locked ? " \uD83D\uDD12" : "")
                        + (ppCollapsed ? " \u25B6" : " \u25BC"),
                x + PP_PAD, y + 5, t.headerTextColor, false);

        // Save button
        String saveLabel = "Save";
        int saveX = x + w - font.width(saveLabel) - PP_PAD - 2;
        boolean saveHov = mouseX >= saveX - 2 && mouseX <= saveX + font.width(saveLabel) + 2
                && mouseY >= y + 3 && mouseY <= y + PP_HEADER_H - 3;
        gfx.drawString(font, saveLabel, saveX, y + 5,
                saveHov ? t.accentColor : t.disabledTextColor, false);

        if (ppCollapsed) return;

        // Profile rows
        List<Profile> profiles = ProfileManager.getAll();
        String activeName = ProfileManager.getActive().name;
        for (int i = 0; i < profiles.size(); i++) {
            Profile p = profiles.get(i);
            int rowY = y + PP_HEADER_H + i * PP_ROW_H;
            boolean isActive = p.name.equals(activeName);
            boolean hovered  = mouseX >= x && mouseX <= x + w
                    && mouseY >= rowY && mouseY <= rowY + PP_ROW_H;

            if (hovered) gfx.fill(x, rowY, x + w, rowY + PP_ROW_H, 0x18FFFFFF);
            gfx.fill(x, rowY, x + 2, rowY + PP_ROW_H,
                    isActive ? t.accentColor : t.borderColor);
            gfx.drawString(font, p.name, x + PP_PAD + 2, rowY + 4,
                    isActive ? t.accentColor : t.disabledTextColor, false);
            if (isActive)
                gfx.drawString(font, "●",
                        x + w - font.width("●") - PP_PAD, rowY + 4, t.accentColor, false);
        }

        // Footer buttons
        int footY = y + h - PP_FOOTER_H;
        gfx.fill(x, footY, x + w, footY + 1, t.borderColor);

        int btnW = w / 3;
        String[] labels = { "+ New", "Modules", "Theme" };
        for (int i = 0; i < 3; i++) {
            int btnX = x + i * btnW;
            boolean hov = mouseX >= btnX && mouseX <= btnX + btnW
                    && mouseY >= footY && mouseY <= footY + PP_FOOTER_H;
            if (hov) gfx.fill(btnX, footY + 1, btnX + btnW, footY + PP_FOOTER_H, 0x22FFFFFF);
            if (i > 0) gfx.fill(btnX, footY + 3, btnX + 1, footY + PP_FOOTER_H - 3, t.borderColor);
            int lx = btnX + (btnW - font.width(labels[i])) / 2;
            gfx.drawString(font, labels[i], lx, footY + 5,
                    hov ? t.enabledTextColor : t.disabledTextColor, false);
        }
    }

    // ── Context menus ─────────────────────────────────────────────────────

    private void openProfileContextMenu(String profileName, int mx, int my) {
        boolean isOnly = ProfileManager.getAll().size() == 1;
        contextMenu = new ContextMenu(mx, my)
                .add(ContextMenu.Entry.of("Load", () -> {
                    ProfileManager.switchTo(profileName);
                    ThemeManager.apply(ProfileManager.getActive().theme);
                    // Re-apply keybinds from newly loaded profile
                    net.bananacheese.bananaclient.modules.ModuleManager.applyKeybinds();
                    init();
                    contextMenu = null;
                }))
                .add(ContextMenu.Entry.of("Duplicate", () -> {
                    var copy = ProfileManager.getActive().copy(profileName + "_copy");
                    ProfileManager.save(copy);
                    contextMenu = null;
                }))
                .add(ContextMenu.Entry.of("Rename", () -> {
                    contextMenu = null;
                    renameDialog = new RenameDialog(mx, my, "Rename profile", profileName, newName -> {
                        var p = ProfileManager.getAll().stream()
                                .filter(pr -> pr.name.equals(profileName)).findFirst().orElse(null);
                        if (p != null) {
                            var renamed = p.copy(newName);
                            ProfileManager.save(renamed);
                            boolean wasActive = profileName.equals(ProfileManager.getActive().name);
                            ProfileManager.delete(profileName);
                            if (wasActive) {
                                ProfileManager.switchTo(newName);
                                ThemeManager.apply(ProfileManager.getActive().theme);
                            }
                        }
                        renameDialog = null;
                    });
                }))
                .add(ContextMenu.Entry.separator());

        if (!isOnly) {
            contextMenu.add(ContextMenu.Entry.danger("Delete", () -> {
                ProfileManager.delete(profileName);
                if (ProfileManager.getActive() != null)
                    ThemeManager.apply(ProfileManager.getActive().theme);
                init();
                contextMenu = null;
            }));
        }
    }

    private void openPanelContextMenu(CategoryPanel panel) {
        PanelState state = panel.getState();
        int mx = state.x + CategoryPanel.WIDTH;
        int my = state.y;

        contextMenu = new ContextMenu(mx, my)
                .add(ContextMenu.Entry.of(
                        state.locked ? "Unlock Position" : "Lock Position", () -> {
                            state.locked = !state.locked;
                            panel.setVisible(state.visible);
                            contextMenu = null;
                        }))
                .add(ContextMenu.Entry.of("Reset Position", () -> {
                    PanelState defaults = Profile.defaultStateFor(panel.getCategory());
                    state.x = defaults.x;
                    state.y = defaults.y;
                    panel.setVisible(state.visible);
                    contextMenu = null;
                }))
                .add(ContextMenu.Entry.separator())
                .add(ContextMenu.Entry.of("Hide Panel", () -> {
                    panel.setVisible(false);
                    contextMenu = null;
                }));
    }

    // ── Tick ──────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        if (renameDialog != null) renameDialog.tick();
    }

    // ── Mouse ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (renameDialog != null) {
            if (!renameDialog.isInDialog(mx, my)) renameDialog = null;
            return true;
        }
        if (contextMenu != null) {
            boolean hit = contextMenu.mouseClicked(mx, my);
            if (!hit) contextMenu = null;
            return true;
        }
        if (settingsPanel != null) {
            if (settingsPanel.isRegistrySelectorOpen(mx, my)) {
                settingsPanel.forwardClickToRegistrySelector(mx, my, button);
                return true;
            }
            if (settingsPanel.isInPanel(mx, my)) {
                settingsPanel.mouseClicked(mx, my, button);
                return true;
            }
            settingsPanel = null;
        }
        if (visibilityPanel != null) {
            if (visibilityPanel.isInPanel(mx, my)) {
                visibilityPanel.mouseClicked(mx, my, button); return true;
            }
            visibilityPanel = null;
        }
        // After:
        if (themeEditor != null) {
            // Color picker renders outside the theme panel bounds — check it first
            if (themeEditor.isColorPickerInPanel(mx, my)) {
                themeEditor.colorPickerMouseClicked(mx, my, button);
                return true;
            }
            if (themeEditor.isInPanel(mx, my)) {
                themeEditor.mouseClicked(mx, my, button);
                return true;
            }
        }

        // Profile panel
        if (ppInPanel(mx, my)) {
            handleProfilePanelClick(mx, my, button);
            return true;
        }

        // Category panels
        for (CategoryPanel p : categoryPanels) {
            if (p.isInPanel(mx, my)) {
                Module rightClicked = p.mouseClicked(mx, my, button);
                if (rightClicked != null) {
                    if (rightClicked.hasSettings()) {
                        settingsPanel = new ModuleSettingsPanel(
                                rightClicked, p.getState().x + CategoryPanel.WIDTH + 4, (int) my);
                    } else {
                        rebindingModule = rightClicked;
                        p.setRebindingModule(rightClicked);
                    }
                }
                return true;
            }
        }

        onClose();
        return true;
    }

    private void handleProfilePanelClick(double mx, double my, int button) {
        PanelState s = ppState();

        if (ppInHeader(mx, my)) {
            if (button == 1) {
                // Right click header — panel context menu (lock/reset)
                contextMenu = new ContextMenu((int)mx, (int)my)
                        .add(ContextMenu.Entry.of(
                                s.locked ? "Unlock Position" : "Lock Position", () -> {
                                    s.locked = !s.locked;
                                    ProfileManager.saveActive();
                                    contextMenu = null;
                                }))
                        .add(ContextMenu.Entry.of("Reset Position", () -> {
                            PanelState defaults = Profile.defaultProfilePanelState();
                            s.x = defaults.x;
                            s.y = defaults.y;
                            ProfileManager.saveActive();
                            contextMenu = null;
                        }));
            } else {
                // Left click — check save button first
                int saveX = s.x + PP_WIDTH - 30;
                if (mx >= saveX) {
                    ProfileManager.saveActive();
                    return;
                }
                // Otherwise start drag (collapse on release if didn't move)
                if (!s.locked) {
                    ppDragging   = true;
                    ppDragOffX   = (int) mx - s.x;
                    ppDragOffY   = (int) my - s.y;
                    ppDragStartX = s.x;
                    ppDragStartY = s.y;
                } else {
                    ppCollapsed = !ppCollapsed;
                }
            }
            return;
        }

        if (ppCollapsed) return;

        // Footer
        int footY = s.y + ppHeight() - PP_FOOTER_H;
        if (my >= footY && button == 0) {
            int col = (int)(mx - s.x) / (PP_WIDTH / 3);
            if (col == 0) {
                // New profile
                Profile newP = ProfileManager.getActive().copy(
                        "profile_" + (ProfileManager.getAll().size() + 1));
                ProfileManager.save(newP);
                ProfileManager.switchTo(newP.name);
                ThemeManager.apply(ProfileManager.getActive().theme);
            } else if (col == 1) {
                // Modules visibility toggle
                if (visibilityPanel != null) {
                    visibilityPanel = null;
                } else {
                    visibilityPanel = new ModuleVisibilityPanel(s.x + PP_WIDTH + 4, s.y);
                    visibilityPanel.setCategoryPanels(categoryPanels); // ← add this line
                }
            } else {
                // Theme editor toggle
                if (themeEditor != null) themeEditor = null;
                else themeEditor = new ThemeEditorPanel(s.x + PP_WIDTH + 4, s.y);
            }
            return;
        }

        // Profile rows
        List<Profile> profiles = ProfileManager.getAll();
        for (int i = 0; i < profiles.size(); i++) {
            int rowY = s.y + PP_HEADER_H + i * PP_ROW_H;
            if (my >= rowY && my <= rowY + PP_ROW_H) {
                String name = profiles.get(i).name;
                if (button == 0) {
                    ProfileManager.switchTo(name);
                    ThemeManager.apply(ProfileManager.getActive().theme);
                    net.bananacheese.bananaclient.modules.ModuleManager.applyKeybinds();
                } else if (button == 1) {
                    openProfileContextMenu(name, (int) mx, (int) my);
                }
                return;
            }
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (settingsPanel != null && settingsPanel.mouseDragged(mx, my, button)) return true;
        if (ppDragging) {
            PanelState s = ppState();
            s.x = (int) mx - ppDragOffX;
            s.y = (int) my - ppDragOffY;
            return true;
        }
        if (visibilityPanel != null && visibilityPanel.isDragging()) {
            visibilityPanel.drag(mx, my); return true;
        }
        // After:
        if (themeEditor != null) {
            if (themeEditor.mouseDragged(mx, my)) return true;
        }
        for (CategoryPanel p : categoryPanels) {
            if (p.isDragging()) { p.drag(mx, my); return true; }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (settingsPanel != null) settingsPanel.mouseReleased();
        if (ppDragging) {
            ppDragging = false;
            PanelState s = ppState();
            // Collapse if position didn't change
            if (s.x == ppDragStartX && s.y == ppDragStartY)
                ppCollapsed = !ppCollapsed;
            ProfileManager.saveActive();
        }
        if (visibilityPanel != null) visibilityPanel.stopDrag();
        // After — just one call, mouseReleased handles both:
        if (themeEditor != null) {
            themeEditor.stopDrag();
            themeEditor.mouseReleased();
        }
        for (CategoryPanel p : categoryPanels) p.stopDrag();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (settingsPanel != null && settingsPanel.mouseScrolled(mx, my, dy)) return true;
        if (visibilityPanel != null && visibilityPanel.mouseScrolled(mx, my, dy)) return true;
        if (themeEditor != null && themeEditor.mouseScrolled(mx, my, dy)) return true;
        return super.mouseScrolled(mx, my, dx, dy);
    }

    // ── Keyboard ──────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renameDialog != null) {
            boolean done = renameDialog.keyPressed(keyCode);
            if (done) renameDialog = null;
            return true;
        }
        if (rebindingModule != null) {
            // setKeyCode writes through to profile and saves automatically
            rebindingModule.setKeyCode(
                    keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            rebindingModule = null;
            for (CategoryPanel p : categoryPanels) p.setRebindingModule(null);
            return true;
        }
        if (settingsPanel != null && settingsPanel.keyPressed(keyCode)) return true;
        if (themeEditor != null && themeEditor.keyPressed(keyCode)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (renameDialog != null) { renameDialog.charTyped(c); return true; }
        if (settingsPanel != null) { settingsPanel.charTyped(c); return true; }
        if (themeEditor != null)  { themeEditor.charTyped(c);  return true; }
        return super.charTyped(c, modifiers);
    }
}