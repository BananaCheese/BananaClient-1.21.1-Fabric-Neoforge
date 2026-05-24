package net.bananacheese.bananaclient.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.MoverType;

public class FreeCamera extends AbstractClientPlayer {

    private float   speedModifier = 1.0f;
    private boolean noClip        = true;

    public FreeCamera(LocalPlayer player) {
        super((ClientLevel) player.level(), player.getGameProfile());

        copyPosition(player);
        setXRot(player.getXRot());
        setYRot(player.getYRot());
        xRotO = getXRot();
        yRotO = getYRot();
        xo = getX();
        yo = getY();
        zo = getZ();

        noPhysics = true;
        setInvisible(false);
    }

    public void setSpeedModifier(float s) { this.speedModifier = s; }
    public void setNoClip(boolean v)      { this.noClip = v; this.noPhysics = v; }

    @Override
    public void tick() {
        // Save previous position for interpolation
        xo = getX(); yo = getY(); zo = getZ();
        xRotO = getXRot(); yRotO = getYRot();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;

        float spd = 0.1f * speedModifier;

        float fwd    = 0f;
        float strafe = 0f;
        float vert   = 0f;

        if (mc.options.keyUp.isDown())     fwd    += spd;
        if (mc.options.keyDown.isDown())   fwd    -= spd;
        // A/D — note: strafe left is negative X in Minecraft's right-hand system
        if (mc.options.keyLeft.isDown())   strafe += spd;
        if (mc.options.keyRight.isDown())  strafe -= spd;
        if (mc.options.keyJump.isDown())   vert   += spd;
        if (mc.options.keySprint.isDown()) vert   -= spd; // shift = down

        // Minecraft yaw: 0=south, 90=west, 180=north, 270=east
        // sin(yaw) points WEST when yaw=90, so forward = (-sin, 0, cos)
        double yawRad   = Math.toRadians(getYRot());
        double pitchRad = Math.toRadians(getXRot());

        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        // Forward vector (into the screen, looking direction)
        double fwdX = -Math.sin(yawRad) * cosPitch;
        double fwdY = -sinPitch;
        double fwdZ =  Math.cos(yawRad) * cosPitch;

        // Right vector (perpendicular to forward, horizontal only)
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double dx = fwdX * fwd + rightX * strafe;
        double dy = fwdY * fwd + vert;
        double dz = fwdZ * fwd + rightZ * strafe;

        if (noClip) {
            setPos(getX() + dx, getY() + dy, getZ() + dz);
        } else {
            setDeltaMovement(dx, dy, dz);
            move(MoverType.SELF, getDeltaMovement());
        }
    }

    @Override public boolean isLocalPlayer() { return false; }

    @Override
    public net.minecraft.client.resources.PlayerSkin getSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) return mc.player.getSkin();
        return DefaultPlayerSkin.get(getGameProfile().getId());
    }
}
