package net.bananacheese.bananaclient.modules.render;

import net.bananacheese.bananaclient.modules.CycleSetting;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
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
                    "Disable freecam when taking damage", false)
    );

    private final ModuleSetting<Boolean> returnOnDeath = addSetting(
            new ModuleSetting<>("Return on Death",
                    "Disable freecam when you die", true)
    );

    private final ModuleSetting<Boolean> noClip = addSetting(
            new ModuleSetting<>("No Clip",
                    "Camera passes through blocks", true)
    );

    private final ModuleSetting<Boolean> showPlayer = addSetting(
            new ModuleSetting<>("Show Player",
                    "Render the player body at original position", true)
    );

    private final ModuleSetting<Boolean> showHands = addSetting(
            new ModuleSetting<>("Show Hands",
                    "Render hands while in freecam", false)
    );

    private final ModuleSetting<Boolean> staticView = addSetting(
            new ModuleSetting<>("Static View",
                    "Disables FOV effects and view bobbing", true)
    );

    // ── Camera state ──────────────────────────────────────────────────────
    // All public so mixins can read them directly

    // Current + previous positions — used for lerp between frames
    public double posX, posY, posZ;
    public double prevPosX, prevPosY, prevPosZ;

    // Current + previous rotations — used for lerp between frames
    public float yaw, pitch;
    public float lastYaw, lastPitch;

    // ── Input state ───────────────────────────────────────────────────────
    // Boolean flags updated by key events — not polled per tick
    // This is the key pattern from Meteor that makes left/right work correctly
    public boolean moveForward, moveBackward, moveLeft, moveRight, moveUp, moveDown;

    // ── Saved state for restore ───────────────────────────────────────────
    private CameraType savedPerspective;
    private double savedFovScale;
    private boolean savedBobView;

    public Freecam() {
        super("Freecam", "Detaches camera from player", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    public static Freecam getInstance() { return INSTANCE; }
    public static boolean isActive()    { return INSTANCE != null && INSTANCE.isEnabled(); }

    // ── Enable ────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) {
            toggle(); return;
        }

        // Start camera at current rendered camera position (eye height, not feet)
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        posX = camPos.x;  posY = camPos.y;  posZ = camPos.z;
        prevPosX = posX;  prevPosY = posY;  prevPosZ = posZ;

        // Start rotation from player's current look direction
        yaw      = mc.player.getYRot();
        pitch    = mc.player.getXRot();
        lastYaw  = yaw;
        lastPitch = pitch;

        // Save settings we might temporarily override
        savedPerspective = mc.options.getCameraType();
        savedFovScale    = mc.options.fovEffectScale().get();
        savedBobView     = mc.options.bobView().get();

        if (staticView.getValue()) {
            mc.options.fovEffectScale().set(0.0);
            mc.options.bobView().set(false);
        }

        // Unpress all movement keys so they don't carry into freecam
        unpressKeys(mc);
        moveForward = moveBackward = moveLeft = moveRight = moveUp = moveDown = false;
    }

    // ── Disable ───────────────────────────────────────────────────────────

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();

        // Restore saved options
        if (mc.options != null) {
            mc.options.setCameraType(savedPerspective);
            if (staticView.getValue()) {
                mc.options.fovEffectScale().set(savedFovScale);
                mc.options.bobView().set(savedBobView);
            }
        }

        // Restore player state
        if (mc.player != null) {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.fallDistance = 0f;
        }

        moveForward = moveBackward = moveLeft = moveRight = moveUp = moveDown = false;
    }

    private void unpressKeys(Minecraft mc) {
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
     */
    public static void onTick(LocalPlayer player) {
        if (!isActive()) return;
        INSTANCE.tick(player);
    }

    private void tick(LocalPlayer player) {
        // Save previous state for interpolation
        prevPosX = posX;  prevPosY = posY;  prevPosZ = posZ;
        lastYaw   = yaw;  lastPitch = pitch;

        // Freeze the real player in place
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0f;

        // Build movement vectors using Minecraft's coordinate system
        // Vec3.directionFromRotation correctly handles the yaw/pitch → world space
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
        Vec3 strafe  = Vec3.directionFromRotation(0, yaw + 90);

        // Base speed: 0.5 blocks/tick * multiplier, sprint doubles it
        double spd = 0.5 * speed.getValue();
        if (Minecraft.getInstance().options.keySprint.isDown()) spd *= 2.0;

        double velX = 0, velY = 0, velZ = 0;
        boolean hasFwdBack = false, hasStrafe = false;

        if (moveForward)  { velX += forward.x * spd; velY += forward.y * spd; velZ += forward.z * spd; hasFwdBack = true; }
        if (moveBackward) { velX -= forward.x * spd; velY -= forward.y * spd; velZ -= forward.z * spd; hasFwdBack = true; }
        if (moveRight)    { velX += strafe.x  * spd;                          velZ += strafe.z  * spd; hasStrafe = true; }
        if (moveLeft)     { velX -= strafe.x  * spd;                          velZ -= strafe.z  * spd; hasStrafe = true; }
        if (moveUp)       { velY += spd; }
        if (moveDown)     { velY -= spd; }

        // Normalize diagonal movement so speed is consistent
        if (hasFwdBack && hasStrafe) {
            double diag = 1.0 / Math.sqrt(2);
            velX *= diag; velZ *= diag;
        }

        posX += velX; posY += velY; posZ += velZ;
    }

    // ── Mouse input ───────────────────────────────────────────────────────

    /**
     * Called from MixinMouseHandler — redirects mouse to camera rotation.
     * deltaX/deltaY are already sensitivity-scaled by vanilla.
     */
    public static void changeLookDirection(double deltaX, double deltaY) {
        if (!isActive()) return;
        INSTANCE.lastYaw   = INSTANCE.yaw;
        INSTANCE.lastPitch = INSTANCE.pitch;
        INSTANCE.yaw      += (float) deltaX;
        INSTANCE.pitch    += (float) deltaY;
        INSTANCE.pitch     = Mth.clamp(INSTANCE.pitch, -90f, 90f);
    }

    // ── Key input ─────────────────────────────────────────────────────────

    /**
     * Called from MixinKeyboardHandler for every key event while freecam is active.
     * Returns true if the key was consumed (prevents it from affecting the player).
     * action: GLFW.GLFW_PRESS, GLFW.GLFW_RELEASE, or GLFW.GLFW_REPEAT
     */
    public static boolean onKey(int key, int action) {
        if (!isActive()) return false;
        Minecraft mc = Minecraft.getInstance();
        boolean pressed = action != GLFW.GLFW_RELEASE;

        if (matchesKey(mc.options.keyUp,    key)) { INSTANCE.moveForward  = pressed; mc.options.keyUp.setDown(false);    return true; }
        if (matchesKey(mc.options.keyDown,  key)) { INSTANCE.moveBackward = pressed; mc.options.keyDown.setDown(false);  return true; }
        if (matchesKey(mc.options.keyLeft,  key)) { INSTANCE.moveLeft     = pressed; mc.options.keyLeft.setDown(false);  return true; }
        if (matchesKey(mc.options.keyRight, key)) { INSTANCE.moveRight    = pressed; mc.options.keyRight.setDown(false); return true; }
        if (matchesKey(mc.options.keyJump,  key)) { INSTANCE.moveUp       = pressed; mc.options.keyJump.setDown(false);  return true; }
        // Down uses the player's configured sneak/shift key — not hardcoded
        if (matchesKey(mc.options.keyShift, key)) { INSTANCE.moveDown     = pressed; mc.options.keyShift.setDown(false); return true; }

        return false;
    }

    private static boolean matchesKey(KeyMapping mapping, int key) {
        return mapping.getKey().getValue() == key;
    }

    // ── Damage / Death callbacks ──────────────────────────────────────────

    public static void onDamage() {
        if (!isActive() || INSTANCE == null) return;
        if (INSTANCE.returnOnDamage.getValue()) INSTANCE.toggle();
    }

    public static void onDeath() {
        if (!isActive() || INSTANCE == null) return;
        if (INSTANCE.returnOnDeath.getValue()) INSTANCE.toggle();
    }

    // ── Render helpers ────────────────────────────────────────────────────

    public static boolean shouldRenderPlayer() {
        return isActive() && INSTANCE.showPlayer.getValue();
    }

    public static boolean shouldRenderHands() {
        return !isActive() || INSTANCE.showHands.getValue();
    }

    // ── Interpolated getters for Camera mixin ─────────────────────────────

    public static double getX(float pt)     { return Mth.lerp(pt, INSTANCE.prevPosX, INSTANCE.posX); }
    public static double getY(float pt)     { return Mth.lerp(pt, INSTANCE.prevPosY, INSTANCE.posY); }
    public static double getZ(float pt)     { return Mth.lerp(pt, INSTANCE.prevPosZ, INSTANCE.posZ); }
    public static float  getYaw(float pt)   { return Mth.lerp(pt, INSTANCE.lastYaw,  INSTANCE.yaw);  }
    public static float  getPitch(float pt) { return Mth.lerp(pt, INSTANCE.lastPitch, INSTANCE.pitch); }
}