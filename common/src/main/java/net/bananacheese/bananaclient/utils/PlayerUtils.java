package net.bananacheese.bananaclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class PlayerUtils {

    public static LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    public static boolean isPlayerValid() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    public static Vec3 getPos() {
        LocalPlayer p = getPlayer();
        return p != null ? p.position() : Vec3.ZERO;
    }

    public static boolean isOnGround() {
        LocalPlayer p = getPlayer();
        return p != null && p.onGround();
    }

    public static boolean isFalling() {
        LocalPlayer p = getPlayer();
        return p != null && p.fallDistance > 0;
    }

    public static float getFallDistance() {
        LocalPlayer p = getPlayer();
        return p != null ? p.fallDistance : 0f;
    }

    public static void resetFallDistance() {
        LocalPlayer p = getPlayer();
        if (p != null) p.fallDistance = 0f;
    }
}
