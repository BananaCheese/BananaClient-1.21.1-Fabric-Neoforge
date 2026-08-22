package net.bananacheese.bananaclient.mixin.movement;

import net.bananacheese.bananaclient.modules.movement.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

    @Unique
    private Vec3 bananaclient$beforePush = Vec3.ZERO;
    @Unique
    private Vec3 bananaclient$beforePushArg = Vec3.ZERO;
    @Unique
    private Vec3 bananaclient$beforeFluid = Vec3.ZERO;

    // ── Entity Push: Entity#push(Entity) affects BOTH participants in one
    // call (vanilla applies half the separation force to each side), so we
    // snapshot whichever side (this, or the argument) is the local player.

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void bananaclient$onPushHead(Entity entity, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        Entity self = (Entity) (Object) this;
        if (self == player) bananaclient$beforePush = player.getDeltaMovement();
        if (entity == player) bananaclient$beforePushArg = player.getDeltaMovement();
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void bananaclient$onPushTail(Entity entity, CallbackInfo ci) {
        if (!Velocity.isEntityPushEnabled()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        Entity self = (Entity) (Object) this;
        float mult = Velocity.getMultiplier();

        if (self == player) {
            bananaclient$applyScaled(player, bananaclient$beforePush, mult);
        }
        if (entity == player) {
            bananaclient$applyScaled(player, bananaclient$beforePushArg, mult);
        }
    }

    // ── Liquids: fluid current push, fully client-predicted.
    // NOTE: trying both known historical names for this method — push(Entity)
    // above works fine (same mixin class), so if this still doesn't fire,
    // it's almost certainly a third, different name in your mappings; check
    // the Entity class for whatever method actually calls
    // Level#getFluidState()-based current push and swap the name(s) below.

    @Inject(method = {"updateFluidHeightAndDoFluidPushing", "updateInWaterStateAndDoFluidPushing"}, at = @At("HEAD"), require = 0)
    private void bananaclient$onFluidPushHead(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (player != null && (Entity) (Object) this == player) {
            bananaclient$beforeFluid = player.getDeltaMovement();
        }
    }

    @Inject(method = {"updateFluidHeightAndDoFluidPushing", "updateInWaterStateAndDoFluidPushing"}, at = @At("TAIL"), require = 0)
    private void bananaclient$onFluidPushTail(CallbackInfoReturnable<Boolean> cir) {
        if (!Velocity.isLiquidsEnabled()) return;
        var player = Minecraft.getInstance().player;
        if (player == null || (Entity) (Object) this != player) return;
        bananaclient$applyScaled(player, bananaclient$beforeFluid, Velocity.getMultiplier());
    }

    @Unique
    private void bananaclient$applyScaled(Entity player, Vec3 before, float mult) {
        Vec3 after = player.getDeltaMovement();
        Vec3 diff = after.subtract(before);
        if (diff.lengthSqr() < 1.0E-9) return;
        player.setDeltaMovement(before.add(diff.scale(mult)));
    }
}