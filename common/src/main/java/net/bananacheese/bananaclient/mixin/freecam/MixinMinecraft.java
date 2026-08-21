package net.bananacheese.bananaclient.mixin.freecam;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    // NOTE: startAttack returns boolean in 1.21.1 — adjust to plain
    // CallbackInfo if your mappings still use the old void signature.
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (bananaclient$disableInteract()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onPickBlock(CallbackInfo ci) {
        if (bananaclient$disableInteract()) {
            ci.cancel();
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onContinueAttack(CallbackInfo ci) {
        if (bananaclient$disableInteract()) {
            ci.cancel();
        }
    }

    private static boolean bananaclient$disableInteract() {
        return Freecam.isActive() && Freecam.shouldPreventInteractions();
    }
}
