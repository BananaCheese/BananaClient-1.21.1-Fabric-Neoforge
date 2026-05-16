package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.profile.PanelState;
import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.bananacheese.bananaclient.gui.theme.ThemeEditorPanel;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleScreen extends Screen {

    private final List<CategoryPanel> categoryPanels = new ArrayList<>();

    private ProfilePanel          profilePanel;
    private ModuleVisibilityPanel visibilityPanel   = null;
    private ModuleSettingsPanel   settingsPanel     = null;
    private ContextMenu           contextMenu       = null;
    private RenameDialog          renameDialog      = null;
    private ThemeEditorPanel      themeEditor       = null;

    private Module rebindingModule = null;

    public ModuleScreen() {
        super(Component.literal("BananaClient"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // game keeps running while GUI is open
    }

    @Override
    public void init() {
        categoryPanels.clear();
        for (Module.Category cat : Module.Category.values()) {
            CategoryPanel panel = new CategoryPanel(cat);
            panel.setOnHeaderRightClick(() -> openPanelContextMenu(panel));
            categoryPanels.add(panel);
        }

        profilePanel = new ProfilePanel(
                () -> {
                    if (visibilityPanel != null) visibilityPanel = null;
                    else visibilityPanel = new ModuleVisibilityPanel(200, 50);
                },
                () -> {},
                (name, anchor) -> openProfileContextMenu(name, anchor[0], anchor[1])
        );
    }

    // ── Profile context menu ───────────────────────────────────────────────

    private void openProfileContextMenu(String profileName, int mx, int my) {
        boolean isActive = profileName.equals(ProfileManager.getActive().name);
        boolean isOnly   = ProfileManager.getAll().size() == 1;

        contextMenu = new ContextMenu(mx, my)
                .add(ContextMenu.Entry.of("Load", () -> {
                    ProfileManager.switchTo(profileName);
                    ThemeManager.apply(ProfileManager.getActive().theme);
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
                    renameDialog = new RenameDialog(mx, my, "Rename profile",
                            profileName, newName -> {
                        // Rename = copy with new name + delete old
                        var p = ProfileManager.getAll().stream()
                                .filter(pr -> pr.name.equals(profileName))
                                .findFirst().orElse(null);
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
                if (ProfileManager.getActive() != null) {
                    ThemeManager.apply(ProfileManager.getActive().theme);
                }
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
                            // saveState is private — toggle via setVisible trick to trigger save
                            panel.setVisible(state.visible);
                            contextMenu = null;
                        }))
                .add(ContextMenu.Entry.of("Reset Position", () -> {
                    // Reset to whatever default was — just nudge slightly from origin
                    state.x = panel.getCategory().ordinal() * (CategoryPanel.WIDTH + 4) + 4;
                    state.y = 4;
                    panel.setVisible(state.visible); // triggers saveState
                    contextMenu = null;
                }))
                .add(ContextMenu.Entry.separator())
                .add(ContextMenu.Entry.of("Hide Panel", () -> {
                    panel.setVisible(false);
                    contextMenu = null;
                }));
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        // Category panels
        for (CategoryPanel p : categoryPanels) {
            p.render(gfx, font, mouseX, mouseY);
        }

        // Profile panel
        profilePanel.render(gfx, font, mouseX, mouseY);

        // Overlays — rendered on top in z order
        if (visibilityPanel != null)
            visibilityPanel.render(gfx, font, mouseX, mouseY);

        if (themeEditor != null)
            themeEditor.render(gfx, font, mouseX, mouseY);

        if (settingsPanel != null)
            settingsPanel.render(gfx, font);

        if (contextMenu != null)
            contextMenu.render(gfx, font, mouseX, mouseY);

        if (renameDialog != null)
            renameDialog.render(gfx, font);
    }

    // ── Tick ──────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        if (renameDialog != null) renameDialog.tick();
    }

    // ── Mouse ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Rename dialog absorbs all clicks while open
        if (renameDialog != null) {
            if (!renameDialog.isInDialog(mx, my)) renameDialog = null;
            return true;
        }

        // Context menu absorbs clicks
        if (contextMenu != null) {
            boolean hit = contextMenu.mouseClicked(mx, my);
            if (!hit) contextMenu = null;
            return true;
        }

        // Settings panel
        if (settingsPanel != null) {
            if (settingsPanel.isInPanel(mx, my)) {
                settingsPanel.mouseClicked(mx, my, button);
                return true;
            }
            settingsPanel = null;
        }

        // Visibility panel
        if (visibilityPanel != null) {
            if (visibilityPanel.isInPanel(mx, my)) {
                visibilityPanel.mouseClicked(mx, my, button);
                return true;
            }
            visibilityPanel = null;
        }

        if (themeEditor != null) {
            if (themeEditor.isInPanel(mx, my)) {
                themeEditor.mouseClicked(mx, my, button);
                return true;
            }
            // Don't close on outside click — let it persist
        }

        // Profile panel
        if (profilePanel.isInPanel(mx, my)) {
            profilePanel.mouseClicked(mx, my, button);
            return true;
        }

        // Category panels
        for (CategoryPanel p : categoryPanels) {
            if (p.isInPanel(mx, my)) {
                Module rightClicked = p.mouseClicked(mx, my, button);
                if (rightClicked != null) {
                    if (rightClicked.hasSettings()) {
                        int spawnX = p.getState().x + CategoryPanel.WIDTH + 4;
                        settingsPanel = new ModuleSettingsPanel(rightClicked, spawnX, (int) my);
                    } else {
                        rebindingModule = rightClicked;
                        p.setRebindingModule(rightClicked);
                    }
                }
                return true;
            }
        }

        // Click outside everything — close
        onClose();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (profilePanel.isDragging())    { profilePanel.drag(mx, my); return true; }
        if (visibilityPanel != null && visibilityPanel.isDragging()) {
            visibilityPanel.drag(mx, my); return true;
        }

        if (themeEditor != null && themeEditor.isDragging()) {
            themeEditor.drag(mx, my); return true;
        }

        for (CategoryPanel p : categoryPanels) {
            if (p.isDragging()) { p.drag(mx, my); return true; }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        profilePanel.stopDrag();
        if (visibilityPanel != null) visibilityPanel.stopDrag();

        if (themeEditor != null) themeEditor.stopDrag();

        for (CategoryPanel p : categoryPanels) p.stopDrag();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (visibilityPanel != null && visibilityPanel.mouseScrolled(mx, my, dy)) return true;

        if (themeEditor != null && themeEditor.mouseScrolled(mx, my, dy)) return true;

        return super.mouseScrolled(mx, my, dx, dy);
    }

    // ── Keyboard ──────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Rename dialog gets keys first
        if (renameDialog != null) {
            boolean done = renameDialog.keyPressed(keyCode);
            if (done) renameDialog = null;
            return true;
        }

        // Rebinding
        if (rebindingModule != null) {
            rebindingModule.setKeyCode(
                    keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode
            );
            rebindingModule = null;
            for (CategoryPanel p : categoryPanels) p.setRebindingModule(null);
            return true;
        }

        if (themeEditor != null && themeEditor.keyPressed(keyCode)) return true;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (renameDialog != null) {
            renameDialog.charTyped(c);
            return true;
        }

        if (themeEditor != null) {
            themeEditor.charTyped(c);
            return true;
        }

        return super.charTyped(c, modifiers);
    }
}