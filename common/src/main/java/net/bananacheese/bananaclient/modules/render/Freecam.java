package net.bananacheese.bananaclient.modules.render;

import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {

    private static Freecam INSTANCE;

    // ── Settings ──────────────────────────────────────────────────────────

    private final ModuleSetting<Float> speed = addSetting(
            new ModuleSetting<>("Speed", "Camera movement speed", 1.0f, 0.1f, 10.0f)
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
                    "Render the player body at original position", true)
    );

    // ── Camera state — public so Camera mixin can read them ───────────────

    // Current and previous positions for lerp interpolation
    public double posX, posY, posZ;
    public double prevPosX, prevPosY, prevPosZ;

    // Current and previous rotations for lerp interpolation
    public float yaw, pitch;
    public float lastYaw, lastPitch;

    // ── Input state ───────────────────────────────────────────────────────
    // Tracked as booleans so keys work correctly (not polled every tick)
    private boolean forward, backward, left, right, up, down;

    // ── Saved state ───────────────────────────────────────────────────────
    private CameraType savedPerspective;

    public Freecam() {
        super("Freecam", "Detaches camera from player", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    public static Freecam getInstance() { return INSTANCE; }
    public static boolean isActive()    { return INSTANCE != null && INSTANCE.isEnabled(); }

    // ── Enable / Disable ──────────────────────────────────────────────────

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { toggle(); return; }

        // Start camera at the current camera position (not player feet)
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        posX = camPos.x;  posY = camPos.y;  posZ = camPos.z;
        prevPosX = posX;  prevPosY = posY;  prevPosZ = posZ;

        yaw      = mc.player.getYRot();
        pitch    = mc.player.getXRot();
        lastYaw  = yaw;
        lastPitch = pitch;

        savedPerspective = mc.options.getCameraType();

        // Unpress all movement keys so they don't carry over
        unpressKeys(mc);

        // Reset boolean input state
        forward = backward = left = right = up = down = false;
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();

        // Restore perspective
        if (savedPerspective != null && mc.options != null) {
            mc.options.setCameraType(savedPerspective);
        }

        // Restore player rotation so view snaps back correctly
        if (mc.player != null) {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.setDeltaMovement(Vec3.ZERO);
        }

        // Re-press no keys — just clear state
        forward = backward = left = right = up = down = false;
    }

    private void unpressKeys(Minecraft mc) {
        // Unpress vanilla keybinds so they don't affect the player
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
    }

    // ── Per-tick update ───────────────────────────────────────────────────

    /**
     * Called from MixinLocalPlayer.aiStep every tick.
     * Updates camera position from input state and freezes the player.
     */
    public static void onTick(LocalPlayer player) {
        if (!isActive()) return;
        INSTANCE.tick(player);
    }

    private void tick(LocalPlayer player) {
        // Save previous position for interpolation
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        lastYaw   = yaw;
        lastPitch = pitch;

        // Freeze the real player
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0f;

        // Build velocity from input booleans — same approach as Meteor
        // Vec3.directionFromRotation handles Minecraft's coordinate system correctly
        Vec3 fwdVec   = Vec3.directionFromRotation(0,   yaw);      // horizontal forward
        Vec3 rightVec = Vec3.directionFromRotation(0,   yaw + 90); // horizontal right
        Vec3 lookVec  = Vec3.directionFromRotation(pitch, yaw);    // true look direction

        double spd = speed.getValue() * 0.1;

        // Sprint doubles speed (respects player's sprint key)
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keySprint.isDown()) spd *= 2.0;

        double velX = 0, velY = 0, velZ = 0;

        // Forward/backward move in the look direction (including pitch)
        if (forward)  { velX += lookVec.x * spd; velY += lookVec.y * spd; velZ += lookVec.z * spd; }
        if (backward) { velX -= lookVec.x * spd; velY -= lookVec.y * spd; velZ -= lookVec.z * spd; }

        // Left/right strafe horizontally
        if (right)    { velX += rightVec.x * spd; velZ += rightVec.z * spd; }
        if (left)     { velX -= rightVec.x * spd; velZ -= rightVec.z * spd; }

        // Up/down — vertical only
        if (up)       { velY += spd; }
        if (down)     { velY -= spd; }

        posX += velX;
        posY += velY;
        posZ += velZ;
    }

    /**
     * Called from MixinMouseHandler to redirect mouse input to camera rotation.
     * Mouse rotation is applied immediately (not tick-delayed) for responsiveness.
     */
    public static void onMouseTurn(double deltaX, double deltaY) {
        if (!isActive()) return;
        INSTANCE.lastYaw   = INSTANCE.yaw;
        INSTANCE.lastPitch = INSTANCE.pitch;
        INSTANCE.yaw       += (float) deltaX;
        INSTANCE.pitch     += (float) deltaY;
        INSTANCE.pitch      = Mth.clamp(INSTANCE.pitch, -90f, 90f);
    }

    /**
     * Called from MixinKeyboardHandler to track key state.
     * Returns true if the key was consumed by freecam.
     * action: GLFW.GLFW_PRESS or GLFW.GLFW_RELEASE
     */
    public static boolean onKey(int key, int action, Minecraft mc) {
        if (!isActive()) return false;

        boolean pressed = action != GLFW.GLFW_RELEASE;

        // Match against the player's configured keybinds
        if (matchesKey(mc.options.keyUp, key))    { INSTANCE.forward  = pressed; return true; }
        if (matchesKey(mc.options.keyDown, key))  { INSTANCE.backward = pressed; return true; }
        if (matchesKey(mc.options.keyLeft, key))  { INSTANCE.left     = pressed; return true; }
        if (matchesKey(mc.options.keyRight, key)) { INSTANCE.right    = pressed; return true; }
        if (matchesKey(mc.options.keyJump, key))  { INSTANCE.up       = pressed; return true; }
        // Down = player's crouch/sneak key — respects custom bindings
        if (matchesKey(mc.options.keyShift, key)) { INSTANCE.down     = pressed; return true; }

        return false;
    }

    private static boolean matchesKey(net.minecraft.client.KeyMapping mapping, int key) {
        return mapping.getKey().getValue() == key;
    }

    /**
     * Called when the real player takes damage.
     */
    public static void onPlayerDamage() {
        if (!isActive()) return;
        if (INSTANCE.returnOnDamage.getValue()) INSTANCE.toggle();
    }

    public static boolean shouldRenderPlayer() {
        return isActive() && INSTANCE.showPlayer.getValue();
    }

    // ── Interpolated getters for Camera mixin ─────────────────────────────

    public static double getX(float partialTick) {
        return Mth.lerp(partialTick, INSTANCE.prevPosX, INSTANCE.posX);
    }

    public static double getY(float partialTick) {
        return Mth.lerp(partialTick, INSTANCE.prevPosY, INSTANCE.posY);
    }

    public static double getZ(float partialTick) {
        return Mth.lerp(partialTick, INSTANCE.prevPosZ, INSTANCE.posZ);
    }

    public static float getYaw(float partialTick) {
        return Mth.lerp(partialTick, INSTANCE.lastYaw, INSTANCE.yaw);
    }

    public static float getPitch(float partialTick) {
        return Mth.lerp(partialTick, INSTANCE.lastPitch, INSTANCE.pitch);
    }
}