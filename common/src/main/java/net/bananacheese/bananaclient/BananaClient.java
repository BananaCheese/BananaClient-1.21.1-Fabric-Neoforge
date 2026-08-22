package net.bananacheese.bananaclient;

import dev.architectury.event.events.client.ClientTickEvent;
import net.bananacheese.bananaclient.gui.profile.ProfileManager;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.ModuleManager;
import net.bananacheese.bananaclient.modules.render.Freecam;

public final class BananaClient {
    public static final String MOD_ID = "bananaclient";

    public static void init() {
        ModuleManager.init();     // register modules with their default keys
        ProfileManager.init();    // load profiles from disk, set active
        ModuleManager.applyKeybinds(); // overwrite defaults with saved keys
        ThemeManager.init();      // pull theme from active profile

        ClientTickEvent.CLIENT_POST.register(Freecam::onClientTick);
    }
}
