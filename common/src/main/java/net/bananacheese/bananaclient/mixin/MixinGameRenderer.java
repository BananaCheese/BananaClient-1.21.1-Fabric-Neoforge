package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    /**
     * renderItemInHand renders the first-person hand/item overlay.
     * Cancel it entirely when freecam is active so the player
     * doesn't see their hand floating in camera space.
     */
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void onRenderItemInHand(CallbackInfo ci) {
        if (Freecam.isActive()) {
            ci.cancel();
        }
    }
}

