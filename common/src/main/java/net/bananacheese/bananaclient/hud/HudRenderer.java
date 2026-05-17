package net.bananacheese.bananaclient.hud;

import net.bananacheese.bananaclient.modules.HudModule;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;

public class HudRenderer {

    public static void onRenderHud(GuiGraphics gfx) {
        for (Module m : ModuleManager.getAll()) {
            if (m.isEnabled() && m instanceof HudModule hud) {
                hud.onRenderHud(gfx);
            }
        }
    }
}
