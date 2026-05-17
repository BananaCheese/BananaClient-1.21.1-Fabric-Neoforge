package net.bananacheese.bananaclient.modules.movement;

import net.bananacheese.bananaclient.modules.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class NoFall extends Module {

    private static NoFall INSTANCE;

    public NoFall() {
        super("NoFall", "Cancels fall damage", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static void onTick() {
        if (!isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // Reset fall distance every tick so it never accumulates enough to deal damage
        mc.player.fallDistance = 0f;
    }
}