package net.bananacheese.bananaclient.modules.render;

import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {

    private static Freecam INSTANCE;

    private final ModuleSetting<Float> speed = addSetting(
            new ModuleSetting<>("Speed", "Camera movement speed", 1.0f, 0.1f, 5.0f)
    );

    private final ModuleSetting<Boolean> returnOnDamage = addSetting(
            new ModuleSetting<>("Return on Damage",
                    "Disable freecam when taking damage", true)
    );

    private final ModuleSetting<Boolean> noClip = addSetting(
            new ModuleSetting<>("No Clip", "Camera passes through blocks", true)
    );

    private final ModuleSetting<Boolean> showPlayer = addSetting(
            new ModuleSetting<>("Show Player",
                    "Render the player body while in freecam", true)
    );

    // The fake camera entity
    private FreeCamera freeCamera = null;

    // Saved player state to restore on disable
    private double savedX, savedY, savedZ;
    private float  savedYaw, savedPitch;

    public Freecam() {
        super("Freecam", "Detaches camera from player", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    public static Freecam getInstance() { return INSTANCE; }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.freeCamera != null;
    }

    public static FreeCamera getCamera() {
        return INSTANCE != null ? INSTANCE.freeCamera : null;
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            toggle(); // can't enable without a player
            return;
        }
        LocalPlayer player = mc.player;

        // Save state
        savedX   = player.getX();
        savedY   = player.getY();
        savedZ   = player.getZ();
        savedYaw   = player.getYRot();
        savedPitch = player.getXRot();

        // Create the camera entity and add it to the world
        freeCamera = new FreeCamera(player);
        freeCamera.setSpeedModifier(speed.getValue());
        freeCamera.setNoClip(noClip.getValue());

        mc.level.addEntity(freeCamera);

        // Switch Minecraft's camera to follow our fake entity
        mc.setCameraEntity(freeCamera);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();

        if (freeCamera != null) {
            // Remove the fake entity from the world
            if (mc.level != null) {
                mc.level.removeEntity(freeCamera.getId(),
                        net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
            freeCamera = null;
        }

        // Restore camera to the real player
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            // Restore exact position — player body never moved
            mc.player.moveTo(savedX, savedY, savedZ, savedYaw, savedPitch);
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
    }

    /**
     * Called every tick. Updates speed setting dynamically.
     */
    public static void onTick(LocalPlayer player) {
        if (!isActive()) return;
        INSTANCE.freeCamera.setSpeedModifier(INSTANCE.speed.getValue());
        INSTANCE.freeCamera.setNoClip(INSTANCE.noClip.getValue());
    }

    /**
     * Called when the real player takes damage.
     */
    public static void onPlayerDamage() {
        if (!isActive()) return;
        if (INSTANCE.returnOnDamage.getValue()) {
            INSTANCE.toggle();
        }
    }

    /**
     * Whether to render the real player body.
     * Used by the render mixin to decide visibility.
     */
    public static boolean shouldRenderPlayer() {
        return isActive() && INSTANCE.showPlayer.getValue();
    }
}
