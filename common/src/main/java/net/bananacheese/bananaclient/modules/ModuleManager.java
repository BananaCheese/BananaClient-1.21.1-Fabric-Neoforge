package net.bananacheese.bananaclient.modules;

import net.bananacheese.bananaclient.modules.misc.Fullbright;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private static final List<Module> modules = new ArrayList<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        register(new Fullbright());
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