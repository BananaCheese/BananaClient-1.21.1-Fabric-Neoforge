package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.modules.movement.NoFall;
import net.bananacheese.bananaclient.modules.render.Freecam;
import net.bananacheese.bananaclient.modules.render.freecam.FreeCamera;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    // Lets the freecam's horizontal flight speed be configured by the Speed
    // setting — vanilla creative flight only ties the "flying speed" ability
    // to vertical (ascend/descend) movement, not horizontal.
    @Inject(method = "getFrictionInfluencedSpeed", at = @At("HEAD"), cancellable = true)
    private void onGetFrictionInfluencedSpeed(float friction, CallbackInfoReturnable<Float> cir) {
        if (Freecam.isActive() && (Object) this instanceof FreeCamera freeCamera) {
            float base = Freecam.getSpeed() / 10f;
            cir.setReturnValue(freeCamera.isSprinting() ? base * 2f : base);
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void onFallDamage(float fallDistance, float multiplier,
                              net.minecraft.world.damagesource.DamageSource source,
                              CallbackInfoReturnable<Boolean> cir) {
        // Only cancel for the local player — LivingEntity covers all entities
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player == (Object) this) {
            if (NoFall.isActive()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"))
    private void onHurt(net.minecraft.world.damagesource.DamageSource source,
                        float amount,
                        CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player == (Object) this) {
            Freecam.onDamage();
        }
    }
}
