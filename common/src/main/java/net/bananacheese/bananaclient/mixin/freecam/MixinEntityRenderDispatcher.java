package net.bananacheese.bananaclient.mixin.freecam;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.bananacheese.bananaclient.modules.render.freecam.FreeCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onShouldRender(Entity entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof FreeCamera) {
            // Never render the freecam's own (invisible) body.
            cir.setReturnValue(false);
        } else if (entity == Minecraft.getInstance().player && Freecam.isActive()) {
            // Force an explicit answer (rather than only conditionally
            // hiding) so this isn't at the mercy of vanilla's distance/
            // behind-camera culling heuristics once the camera detaches.
            cir.setReturnValue(Freecam.shouldRenderPlayer());
        }
    }
}