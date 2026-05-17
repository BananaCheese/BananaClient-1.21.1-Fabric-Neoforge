package net.bananacheese.bananaclient.gui.profile;

import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.modules.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Profile {

    public String name;
    public Theme  theme;

    // Keyed by Category.name() — e.g. "RENDER", "MOVEMENT"
    public Map<String, PanelState> panelStates = new HashMap<>();

    // Profile panel's own position — stored separately from category panels
    public PanelState profilePanelState = new PanelState(5, 5);

    // Module names hidden from regular panel view
    public List<String> hiddenModules = new ArrayList<>();

    // Keybinds — module name → GLFW key code
    public Map<String, Integer> keybinds = new HashMap<>();

    public Profile(String name, Theme theme) {
        this.name  = name;
        this.theme = theme;
    }

    public Profile() {}

    // ── Category panel states ──────────────────────────────────────────────

    public PanelState getPanelState(Module.Category category) {
        return panelStates.computeIfAbsent(
                category.name(),
                k -> defaultStateFor(category)
        );
    }

    public void setPanelState(Module.Category category, PanelState state) {
        panelStates.put(category.name(), state);
    }

    // ── Module visibility ──────────────────────────────────────────────────

    public boolean isModuleHidden(String moduleName) {
        return hiddenModules.contains(moduleName);
    }

    public void setModuleHidden(String moduleName, boolean hidden) {
        if (hidden) {
            if (!hiddenModules.contains(moduleName)) hiddenModules.add(moduleName);
        } else {
            hiddenModules.remove(moduleName);
        }
    }

    // ── Keybinds ───────────────────────────────────────────────────────────

    public int getKeybind(String moduleName, int defaultKey) {
        return keybinds.getOrDefault(moduleName, defaultKey);
    }

    public void setKeybind(String moduleName, int keyCode) {
        keybinds.put(moduleName, keyCode);
    }

    // ── Copy ───────────────────────────────────────────────────────────────

    public Profile copy(String newName) {
        Profile p = new Profile();
        p.name  = newName;
        p.theme = this.theme.copy();
        p.theme.name = this.theme.name;
        p.profilePanelState = this.profilePanelState.copy();
        for (var entry : panelStates.entrySet())
            p.panelStates.put(entry.getKey(), entry.getValue().copy());
        p.hiddenModules = new ArrayList<>(this.hiddenModules);
        p.keybinds = new HashMap<>(this.keybinds);
        return p;
    }

    public static PanelState defaultStateFor(Module.Category category) {
        return switch (category) {
            case COMBAT   -> new PanelState(150, 5);
            case MOVEMENT -> new PanelState(275, 5);
            case RENDER   -> new PanelState(400, 5);
            case MISC     -> new PanelState(525, 5);
        };
    }

    public static PanelState defaultProfilePanelState() {
        return new PanelState(5, 5);
    }
}