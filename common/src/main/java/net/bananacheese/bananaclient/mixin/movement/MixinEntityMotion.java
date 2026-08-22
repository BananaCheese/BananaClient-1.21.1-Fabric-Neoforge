package net.bananacheese.bananaclient.mixin.movement;

import net.bananacheese.bananaclient.modules.movement.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * lerpMotion is declared on Entity itself (not overridden by LocalPlayer),
 * so this mixes into Entity directly and gates the actual logic to only
 * fire for the local player — every other entity in the world also calls
 * this method for its own interpolated motion, and we don't want to touch
 * those.
 */
@Mixin(Entity.class)
public class MixinEntityMotion {

    @Unique
    private Vec3 bananaclient$beforeLerp = Vec3.ZERO;

    @Inject(method = "lerpMotion", at = @At("HEAD"))
    private void bananaclient$onLerpMotionHead(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self == Minecraft.getInstance().player) {
            bananaclient$beforeLerp = self.getDeltaMovement();
        }
    }

    @Inject(method = "lerpMotion", at = @At("TAIL"))
    private void bananaclient$onLerpMotionTail(double x, double y, double z, CallbackInfo ci) {
        if (!Velocity.isActive()) return;

        Entity self = (Entity) (Object) this;
        if (self != Minecraft.getInstance().player) return;

        Vec3 after = self.getDeltaMovement();
        Vec3 diff = after.subtract(bananaclient$beforeLerp);
        if (diff.lengthSqr() < 1.0E-9) return;

        if (Velocity.shouldScaleIncomingVelocity()) {
            float mult = Velocity.getMultiplier();
            self.setDeltaMovement(bananaclient$beforeLerp.add(diff.scale(mult)));
        }
    }
}