package net.bananacheese.bananaclient.modules.movement;

import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Velocity — scales down (or fully cancels, at multiplier 0) how much
 * external velocity the player receives from various sources.
 *
 * Implementation note: there's no single vanilla hook that cleanly tags
 * "this velocity change was caused by X" — vanilla just tells the client
 * "your velocity is now this" without saying why. So sources are split
 * across a few different, independently-confident hooks:
 *
 *  - Entity Push, Liquids, and Explosions are each isolated cleanly by
 *    diffing the player's velocity immediately before/after the specific
 *    vanilla method that applies each one (push(Entity), the fluid-push
 *    method, and ClientPacketListener#handleExplosion respectively).
 *  - Attacks and Fishing Rod both deliver knockback via the same generic
 *    "set my velocity" channel (LocalPlayer's inherited lerpMotion), with no
 *    source tag at all. These are told apart with a best-effort heuristic:
 *    velocity changes arriving within a couple ticks of a damage-event
 *    packet for you specifically are treated as Attacks; anything else
 *    arriving that way is treated as Fishing Rod (a catch-all for
 *    everything else that pushes you through that same generic channel).
 */
public class Velocity extends Module {

    private static Velocity INSTANCE;

    // ── Settings ──────────────────────────────────────────────────────────

    private final ModuleSetting<Boolean> attacks = addSetting(
            new ModuleSetting<>("Attacks", "Knockback from being hit", true)
    );

    private final ModuleSetting<Boolean> explosions = addSetting(
            new ModuleSetting<>("Explosions", "Knockback from explosions", true)
    );

    private final ModuleSetting<Boolean> liquids = addSetting(
            new ModuleSetting<>("Liquids", "Push from water/lava currents", true)
    );

    private final ModuleSetting<Boolean> entityPush = addSetting(
            new ModuleSetting<>("Entity Push", "Being shoved by nearby entities", true)
    );

    private final ModuleSetting<Boolean> fishingRod = addSetting(
            new ModuleSetting<>("Fishing Rod", "Being pulled/reeled by a fishing rod", true)
    );

    private final ModuleSetting<Float> multiplier = addSetting(
            new ModuleSetting<>("Multiplier",
                    "How much affected velocity gets through (0 = fully cancelled, 1 = unaffected)",
                    0.0f, 0.0f, 1.0f)
    );

    // ── Correlation state (for Attacks vs Fishing Rod classification) ─────

    private long lastDamageTick = Long.MIN_VALUE;
    private long tickCounter = 0;

    public Velocity() {
        super("Velocity", "Reduces knockback/push from various sources", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static float getMultiplier() {
        return INSTANCE != null ? INSTANCE.multiplier.getValue() : 1.0f;
    }

    public static boolean isEntityPushEnabled()  { return isActive() && INSTANCE.entityPush.getValue(); }
    public static boolean isLiquidsEnabled()     { return isActive() && INSTANCE.liquids.getValue(); }
    public static boolean isExplosionsEnabled()  { return isActive() && INSTANCE.explosions.getValue(); }

    /** Called every client tick (see loader entrypoints) — just advances our own tick counter. */
    public static void onClientTick(Minecraft mc) {
        if (INSTANCE != null) INSTANCE.tickCounter++;
    }

    /** Called from MixinClientPacketListener#handleDamageEvent (combat mixin package). */
    public static void markDamageTick() {
        if (INSTANCE != null) INSTANCE.lastDamageTick = INSTANCE.tickCounter;
    }

    /**
     * Called from MixinEntityMotion (movement mixin package) around
     * Entity#lerpMotion — classifies a server-delivered velocity change as
     * Attack or Fishing Rod and returns whether it should be scaled.
     */
    public static boolean shouldScaleIncomingVelocity() {
        if (!isActive()) return false;
        if (INSTANCE.tickCounter - INSTANCE.lastDamageTick <= 2) {
            return INSTANCE.attacks.getValue();
        }
        return INSTANCE.fishingRod.getValue();
    }
}