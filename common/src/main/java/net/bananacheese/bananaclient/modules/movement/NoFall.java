package net.bananacheese.bananaclient.modules.movement;

import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import org.lwjgl.glfw.GLFW;

public class NoFall extends Module {

    public enum Mode { Packet, Reset, Both }

    private static NoFall INSTANCE;

    private final ModuleSetting<String> mode = addSetting(
            new ModuleSetting<>("Mode", "How fall damage is cancelled", "Both")
    );

    public NoFall() {
        super("NoFall", "Cancels fall damage", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    /** True if we should reset fallDistance in aiStep */
    public static boolean shouldReset() {
        if (!isActive()) return false;
        String m = INSTANCE.mode.getValue();
        return m.equals("Reset") || m.equals("Both");
    }

    /** True if we should force onGround=true in movement packets */
    public static boolean shouldSendPacket() {
        if (!isActive()) return false;
        String m = INSTANCE.mode.getValue();
        return m.equals("Packet") || m.equals("Both");
    }
}