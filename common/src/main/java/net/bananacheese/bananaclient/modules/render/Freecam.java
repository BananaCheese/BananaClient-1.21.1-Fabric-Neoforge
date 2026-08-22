package net.bananacheese.bananaclient.modules.render;

import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.bananacheese.bananaclient.modules.render.freecam.FreeCamera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Freecam - detaches the camera from the player.
 *
 * This module now delegates the actual camera work to {@link FreeCamera}, a
 * real client-side entity ported from Xolt's standalone Freecam mod, rather
 * than manually tracking position/rotation fields that were never actually
 * hooked into rendering or mouse/keyboard input (which is why the old
 * implementation didn't really work).
 *
 * All settings below are still driven through BananaClient's normal
 * Module/ModuleSetting system, so they show up in the existing options menu
 * exactly like before.
 */
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

    private final ModuleSetting<Boolean> returnOnDeath = addSetting(
            new ModuleSetting<>("Return on Death",
                    "Disable freecam when you die or respawn", true)
    );

    private final ModuleSetting<Boolean> noClip = addSetting(
            new ModuleSetting<>("No Clip",
                    "Camera passes through blocks", true)
    );

    private final ModuleSetting<Boolean> showPlayer = addSetting(
            new ModuleSetting<>("Show Player",
                    "Render the player body at its original position", true)
    );

    private final ModuleSetting<Boolean> showHands = addSetting(
            new ModuleSetting<>("Show Hands",
                    "Render hands while in freecam", false)
    );

    private final ModuleSetting<Boolean> staticView = addSetting(
            new ModuleSetting<>("Static View",
                    "Disables FOV effects and view bobbing", false)
    );

    private final ModuleSetting<Boolean> freezePlayer = addSetting(
            new ModuleSetting<>("Freeze Player",
                    "Keeps your real body from moving/falling while freecam is active", false)
    );

    private final ModuleSetting<Boolean> allowInteractions = addSetting(
            new ModuleSetting<>("Allow Interactions",
                    "Lets clicks/attacks still come from your real player instead of being blocked", false)
    );

    // ── Camera state ──────────────────────────────────────────────────────

    private FreeCamera freeCamera;
    private CameraType savedPerspective;
    private double savedFovScale;
    private boolean savedBobView;
    private boolean playerFrozen = false;
    private Vec3 frozenPos = Vec3.ZERO;

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
            toggle(); // bail back out — can't start a freecam with no world/player
            return;
        }

        savedPerspective = mc.options.getCameraType();
        savedFovScale    = mc.options.fovEffectScale().get();
        savedBobView     = mc.options.bobView().get();

        if (staticView.getValue()) {
            mc.options.fovEffectScale().set(0.0);
            mc.options.bobView().set(false);
        }

        // If already in a detached (F5/F6) view, snap back to first person
        // first so the freecam always starts from the player's actual eyes.
        if (mc.gameRenderer.getMainCamera().isDetached()) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }

        // Disables vanilla's aggressive distance/behind-camera culling so the
        // real player's body (and other entities) render properly even though
        // the camera is no longer attached to it. Without this, "Show Player"
        // silently does nothing on some setups.
        mc.smartCull = false;

        // Large negative sentinel ID — well outside the range of real
        // (server-assigned) entity IDs, so it can never collide with one.
        freeCamera = new FreeCamera(-2_000_000_000);
        freeCamera.copyPosition(mc.player);
        freeCamera.spawn();
        mc.setCameraEntity(freeCamera);

        // WASD/jump/sneak now drive the camera, not the real body — this
        // applies regardless of the Freeze Player setting.
        mc.player.input = new Input();
        playerFrozen = false;
    }

    // ── Disable ───────────────────────────────────────────────────────────

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.options != null) {
            mc.options.setCameraType(savedPerspective != null ? savedPerspective : CameraType.FIRST_PERSON);
            if (staticView.getValue()) {
                mc.options.fovEffectScale().set(savedFovScale);
                mc.options.bobView().set(savedBobView);
            }
        }

        mc.smartCull = true;
        mc.setCameraEntity(mc.player);

        if (freeCamera != null) {
            freeCamera.despawn();
            freeCamera = null;
        }

        if (mc.player != null) {
            mc.player.input = new KeyboardInput(mc.options);
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.fallDistance = 0f;
        }

        playerFrozen = false;
    }

    /** Called every client tick (see BananaClient#init's tick registration). */
    public static void onClientTick(Minecraft mc) {
        if (!isActive()) return;

        // Catch edge cases like the world unloading while freecam is active.
        if (mc.player == null || mc.level == null) {
            INSTANCE.toggle();
            return;
        }

        // WASD/jump/sneak always drive the camera rather than the real body,
        // regardless of Freeze Player — this can flip live if input somehow
        // gets swapped back (e.g. by another mod).
        if (mc.player.input instanceof KeyboardInput) {
            mc.player.input = new Input();
        }

        if (INSTANCE.freezePlayer.getValue()) {
            // Snapshot wherever the player currently is the moment freezing
            // (re-)engages, then hold them there every tick after.
            if (!INSTANCE.playerFrozen) {
                INSTANCE.frozenPos = mc.player.position();
                INSTANCE.playerFrozen = true;
            }
            mc.player.setPos(INSTANCE.frozenPos.x, INSTANCE.frozenPos.y, INSTANCE.frozenPos.z);
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.fallDistance = 0f;
            mc.player.setOnGround(true);
        } else {
            // Freeze off: let gravity/normal physics run — the player falls
            // to the ground naturally instead of staying pinned in midair.
            INSTANCE.playerFrozen = false;
        }
    }

    // ── Damage / Death callbacks ──────────────────────────────────────────

    public static void onDamage() {
        if (!isActive() || INSTANCE == null) return;
        if (INSTANCE.returnOnDamage.getValue()) INSTANCE.toggle();
    }

    /** Called from MixinClientPacketListener#handleRespawn (covers death respawns AND dimension changes). */
    public static void onRespawn() {
        if (!isActive() || INSTANCE == null) return;
        if (INSTANCE.returnOnDeath.getValue()) INSTANCE.toggle();
    }

    // ── Accessors used by the freecam mixins/FreeCamera ─────────────────────

    public static FreeCamera getFreeCamera() { return INSTANCE != null ? INSTANCE.freeCamera : null; }

    public static float getSpeed() { return INSTANCE != null ? INSTANCE.speed.getValue() : 1.0f; }

    public static boolean isNoClip() { return INSTANCE != null && INSTANCE.noClip.getValue(); }

    public static boolean shouldRenderPlayer() {
        return isActive() && INSTANCE.showPlayer.getValue();
    }

    public static boolean shouldRenderHands() {
        return !isActive() || INSTANCE.showHands.getValue();
    }

    public static boolean allowInteractionsFromPlayer() {
        return isActive() && INSTANCE.allowInteractions.getValue();
    }

    public static boolean shouldPreventInteractions() {
        return isActive() && !INSTANCE.allowInteractions.getValue();
    }
}