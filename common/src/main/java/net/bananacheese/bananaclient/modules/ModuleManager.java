package net.bananacheese.bananaclient.modules;

import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.bananacheese.bananaclient.modules.player.Reach;
import net.bananacheese.bananaclient.modules.render.Fullbright;
import net.bananacheese.bananaclient.modules.movement.NoFall;
import net.bananacheese.bananaclient.modules.render.Coordinates;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private static final List<Module> modules     = new ArrayList<>();
    private static boolean            initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        //Combat

        //Player
        register(new Reach());

        // Movement
        register(new NoFall());

        // Render
        register(new Coordinates());

        // Misc
        register(new Fullbright());
    }

    // Called by BananaClient.init() after ProfileManager is ready
    // Restores each module's keybind from the active profile
    public static void applyKeybinds() {
        var profile = ProfileManager.getActive();
        if (profile == null) return;
        for (Module m : modules) {
            int stored = profile.getKeybind(m.getName(), m.getDefaultKey());
            m.loadKeyCode(stored);
        }
    }

    private static void register(Module m) { modules.add(m); }

    public static List<Module> getAll() { return modules; }

    public static <T extends Module> T get(Class<T> clazz) {
        for (Module m : modules) {
            if (m.getClass() == clazz) return clazz.cast(m);
        }
        return null;
    }
}