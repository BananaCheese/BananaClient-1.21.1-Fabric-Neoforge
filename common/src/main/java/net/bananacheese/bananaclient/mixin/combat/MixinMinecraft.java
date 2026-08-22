package net.bananacheese.bananaclient.mixin.combat;

import net.bananacheese.bananaclient.modules.combat.Criticals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// NOTE: startAttack returns boolean in 1.21.1 — adjust to plain
// CallbackInfo if your mappings still use the old void signature.
@Mixin(net.minecraft.client.Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Criticals.onStartAttack()) {
            cir.setReturnValue(false);
        }
    }
}