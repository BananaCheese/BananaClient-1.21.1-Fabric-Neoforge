package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.modules.misc.Fullbright;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class MixinLightTexture {

    @Inject(
            method = "updateLightTexture",
            at = @At("HEAD")
    )
    private void onUpdateLightTexture(float partialTicks, CallbackInfo ci) {
        if (Fullbright.isActive()) {
            System.out.println("[BananaClient] Mixin firing - fullbright active");
        }
    }
}
