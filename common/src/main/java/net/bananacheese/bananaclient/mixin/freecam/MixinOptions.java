package net.bananacheese.bananaclient.mixin.freecam;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinOptions {

    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onSetCameraType(CallbackInfo ci) {
        if (Freecam.isActive()) {
            ci.cancel();
        }
    }
}
