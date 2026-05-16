package net.bananacheese.bananaclient;

import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.ModuleManager;

public final class BananaClient {
    public static final String MOD_ID = "bananaclient";

    public static void init() {
        ModuleManager.init();    // modules must exist first
        ProfileManager.init();   // reads disk, sets active profile
        ThemeManager.init();     // pulls theme from active profile
    }
}
