package net.bananacheese.bananaclient.modules.misc;

import net.bananacheese.bananaclient.mixin.OptionInstanceAccessor;
import net.bananacheese.bananaclient.modules.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class Fullbright extends Module {

    private static Fullbright INSTANCE;
    private double previousGamma = 0.5;

    public Fullbright() {
        super("Fullbright", "Removes all darkness", Category.RENDER, GLFW.GLFW_KEY_B);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            OptionInstanceAccessor accessor = (OptionInstanceAccessor)(Object) mc.options.gamma();
            previousGamma = (Double) accessor.getValue();
            accessor.setValue(16.0);
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            OptionInstanceAccessor accessor = (OptionInstanceAccessor)(Object) mc.options.gamma();
            accessor.setValue(previousGamma);
        }
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
}