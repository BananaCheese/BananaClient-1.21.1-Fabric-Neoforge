package net.bananacheese.bananaclient.modules.movement;

import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.bananacheese.bananaclient.modules.RegistryListSetting;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class NoFall extends Module {

    private static NoFall INSTANCE;

    private final ModuleSetting<String> mode = addSetting(
            new ModuleSetting<>("Mode", "How fall damage is cancelled", "Both")
    );

    private final ModuleSetting<Boolean> autoPlace = addSetting(
            new ModuleSetting<>("Auto Place", "Place a block when about to take fall damage", false)
    );

    private final ModuleSetting<Boolean> hotbarOnly = addSetting(
            new ModuleSetting<>("Hotbar Only", "Only use items from the hotbar", true)
    );

    private final ModuleSetting<Boolean> autoPickup = addSetting(
            new ModuleSetting<>("Auto Pickup", "Automatically pick up placed liquid after landing", true)
    );

    private final RegistryListSetting allowedItems = addRegistrySettings(
            RegistryListSetting.items("Allowed Items", "Items to auto-place",
                    ResourceLocation.withDefaultNamespace("water_bucket"),
                    ResourceLocation.withDefaultNamespace("powder_snow_bucket"))
    );

    public static RegistryListSetting getAllowedItems() {
        return INSTANCE != null ? INSTANCE.allowedItems : null;
    }

    public NoFall() {
        super("NoFall", "Cancels fall damage", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    public static boolean isActive()          { return INSTANCE != null && INSTANCE.isEnabled(); }
    public static boolean isAutoPlace()       { return isActive() && INSTANCE.autoPlace.getValue(); }
    public static boolean isHotbarOnly()      { return isActive() && INSTANCE.hotbarOnly.getValue(); }
    public static boolean isAutoPickup()      { return isActive() && INSTANCE.autoPickup.getValue(); }

    public static boolean shouldReset() {
        if (!isActive()) return false;
        String m = INSTANCE.mode.getValue();
        return m.equals("Reset") || m.equals("Both");
    }

    public static boolean shouldSendPacket() {
        if (!isActive()) return false;
        String m = INSTANCE.mode.getValue();
        return m.equals("Packet") || m.equals("Both");
    }
}