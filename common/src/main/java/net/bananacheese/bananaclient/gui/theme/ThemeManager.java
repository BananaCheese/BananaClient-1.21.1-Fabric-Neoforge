package net.bananacheese.bananaclient.gui.theme;

import net.bananacheese.bananaclient.gui.profile.ProfileManager;

public class ThemeManager {

    private static Theme active = ThemePresets.sharp();

    public static void init() {
        // Pull theme from whatever profile is active
        if (ProfileManager.getActive() != null
                && ProfileManager.getActive().theme != null) {
            active = ProfileManager.getActive().theme;
        }
    }

    public static Theme get() { return active; }

    public static void apply(Theme theme) {
        active = theme;
        // Also update the active profile so it persists on next save
        if (ProfileManager.getActive() != null) {
            ProfileManager.getActive().theme = theme;
        }
    }
}