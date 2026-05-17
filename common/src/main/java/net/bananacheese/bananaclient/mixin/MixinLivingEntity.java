package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.modules.movement.NoFall;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

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
}
