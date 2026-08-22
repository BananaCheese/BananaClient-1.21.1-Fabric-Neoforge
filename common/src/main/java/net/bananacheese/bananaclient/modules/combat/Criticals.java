package net.bananacheese.bananaclient.modules.combat;

import net.bananacheese.bananaclient.modules.CycleSetting;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

/**
 * Criticals — makes every hit land as a critical, even without a natural jump.
 *
 * Real critical-hit eligibility is decided server-side from the server's own
 * tracked fall state (fallDistance > 0 && !onGround), built up over time from
 * the position packets it actually receives from you — it isn't something a
 * client mod can just flip on with no downside, and editing local fields on
 * your own Player object does nothing server-side (true even in singleplayer,
 * since the integrated server still runs its own separate, authoritative
 * player object in the same process). This module offers two ways to
 * actually get a real crit:
 *
 *  - Auto-Jump (legitimate): automatically jumps for you, then holds the
 *    swing until you're genuinely airborne and falling — exactly what a
 *    player does by hand — before firing the attack. Guaranteed to register
 *    as a real crit on any server, at the cost of a small (~4-6 tick) delay
 *    between clicking and the hit landing, and a visible hop before each hit.
 *
 *  - Packet (spoofed): reports a tiny (1/16 block) upward then immediate
 *    downward position change to the server right before attacking — enough
 *    for the server's own fall tracking to register a fall without you
 *    actually jumping or any visible delay. This is a generic version of a
 *    known technique; it does not include the NoCheatPlus-version-specific
 *    tuned bypass values some cheat clients use, since those are built
 *    specifically to defeat a named anti-cheat plugin's exact detection
 *    thresholds rather than being a general technique — this stays at the
 *    "gray area position fudge" level rather than "targeted anti-cheat
 *    exploit" level. It may still get flagged on servers with strict
 *    movement verification.
 */
public class Criticals extends Module {

    private static Criticals INSTANCE;

    // ── Settings ──────────────────────────────────────────────────────────

    private final CycleSetting method = addCycleSetting(
            new CycleSetting("Method", "How the critical is produced",
                    0, "Auto-Jump", "Packet")
    );

    private final CycleSetting targetMode = addCycleSetting(
            new CycleSetting("Target Mode", "Which entities to auto-crit against",
                    2, "PVE", "PVP", "Both")
    );

    private final ModuleSetting<Boolean> onlyWhenNeeded = addSetting(
            new ModuleSetting<>("Only When Needed",
                    "Skip intervening if you're already going to land a critical naturally", true)
    );

    // ── Auto-Jump pending state ──────────────────────────────────────────

    private Entity pendingTarget;
    private int pendingTicks;

    public Criticals() {
        super("Criticals", "Makes every hit land as a critical", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        pendingTarget = null;
        pendingTicks = 0;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    /**
     * Called from MixinMinecraft#startAttack. Returns true if the normal
     * attack should be suppressed this click (because we're going to fire it
     * ourselves once conditions are met, or because we just resolved it
     * instantly right here).
     */
    public static boolean onStartAttack() {
        if (!isActive() || INSTANCE == null) return false;
        // Don't fight Freecam's own interaction-blocking.
        if (Freecam.isActive() && Freecam.shouldPreventInteractions()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return false;
        if (!(mc.hitResult instanceof EntityHitResult entityHit)) return false;

        Entity target = entityHit.getEntity();
        if (!INSTANCE.matchesTargetMode(target)) return false;

        boolean willAlreadyCrit = mc.player.fallDistance > 0.0f && !mc.player.onGround();
        if (INSTANCE.onlyWhenNeeded.getValue() && willAlreadyCrit) return false;

        if (INSTANCE.method.is("Packet")) {
            INSTANCE.doPacketCrit(mc, target);
            return true;
        }

        // Auto-Jump: kick off the jump now, actual attack fires from onClientTick.
        if (INSTANCE.pendingTarget == null) {
            mc.player.jumpFromGround();
            INSTANCE.pendingTarget = target;
            INSTANCE.pendingTicks = 0;
        }
        return true;
    }

    /** Called every client tick (see loader entrypoints). */
    public static void onClientTick(Minecraft mc) {
        if (INSTANCE == null || INSTANCE.pendingTarget == null) return;
        if (mc.player == null || mc.gameMode == null) {
            INSTANCE.pendingTarget = null;
            return;
        }

        INSTANCE.pendingTicks++;
        boolean airborneAndFalling = mc.player.fallDistance > 0.0f && !mc.player.onGround();
        boolean stillValid = INSTANCE.pendingTarget.isAlive()
                && mc.player.distanceToSqr(INSTANCE.pendingTarget) < 36.0; // ~6 blocks, generous margin

        if (airborneAndFalling && stillValid) {
            mc.gameMode.attack(mc.player, INSTANCE.pendingTarget);
            mc.player.swing(InteractionHand.MAIN_HAND);
            INSTANCE.pendingTarget = null;
        } else if (!stillValid || INSTANCE.pendingTicks > 15) {
            // Gave up — target moved out of range/died, or the jump never
            // resulted in a fall (e.g. blocked by a ceiling).
            INSTANCE.pendingTarget = null;
        }
    }

    // ── Packet mode ───────────────────────────────────────────────────────

    private void doPacketCrit(Minecraft mc, Entity target) {
        LocalPlayer player = mc.player;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        sendFakeMove(player, x, y + 0.0625, z);
        sendFakeMove(player, x, y, z);

        // Also flip the LOCAL copy of these fields — the fake packets above
        // only affect what the server thinks happened. Without this, the
        // client's own crit-particle/sound prediction still correctly
        // "knows" it never left the ground, so nothing would visually show
        // even if the server-side spoof is doing its job.
        float savedFall = player.fallDistance;
        boolean savedOnGround = player.onGround();
        player.fallDistance = Math.max(savedFall, 0.6f);
        player.setOnGround(false);

        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);

        player.fallDistance = savedFall;
        player.setOnGround(savedOnGround);
    }

    // NOTE: if your mappings' ServerboundMovePlayerPacket.Pos constructor
    // takes an extra trailing boolean (horizontalCollision), add
    // `, player.horizontalCollision` as a 5th argument below.
    private void sendFakeMove(LocalPlayer player, double x, double y, double z) {
        player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, player.onGround()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean matchesTargetMode(Entity target) {
        if (!(target instanceof LivingEntity)) return false;
        return switch (targetMode.getValue()) {
            case "Both" -> true;
            case "PVP" -> target instanceof Player;
            case "PVE" -> !(target instanceof Player);
            default -> false;
        };
    }
}