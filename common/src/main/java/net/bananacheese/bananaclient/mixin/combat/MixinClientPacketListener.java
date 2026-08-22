package net.bananacheese.bananaclient.mixin.combat;

import net.bananacheese.bananaclient.modules.movement.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NOTE: ClientboundDamageEventPacket#entityId() assumes a record-style
 * accessor (no "get" prefix) — if your mappings use getEntityId() instead,
 * that's the only thing to change here.
 */
@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    @Unique
    private Vec3 bananaclient$beforeExplosion = Vec3.ZERO;

    // Attacks vs Fishing Rod (both delivered via the same generic velocity
    // channel elsewhere) are told apart by timestamping the local player's
    // own damage events — see Velocity's class docs.
    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void bananaclient$onHandleDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && packet.entityId() == player.getId()) {
            Velocity.markDamageTick();
        }
    }

    // Explosions apply their knockback directly inside this method (not via
    // LocalPlayer#lerpMotion the way attack/fishing-rod knockback does), so
    // this is diffed directly rather than timestamp-correlated.
    @Inject(method = "handleExplosion", at = @At("HEAD"))
    private void bananaclient$onExplosionHead(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null) bananaclient$beforeExplosion = player.getDeltaMovement();
    }

    @Inject(method = "handleExplosion", at = @At("TAIL"))
    private void bananaclient$onExplosionTail(CallbackInfo ci) {
        if (!Velocity.isExplosionsEnabled()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        Vec3 after = player.getDeltaMovement();
        Vec3 diff = after.subtract(bananaclient$beforeExplosion);
        if (diff.lengthSqr() < 1.0E-9) return;

        player.setDeltaMovement(bananaclient$beforeExplosion.add(diff.scale(Velocity.getMultiplier())));
    }
}