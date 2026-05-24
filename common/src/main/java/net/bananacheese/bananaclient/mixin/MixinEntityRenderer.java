package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void onShouldRender(T entity,
                                                   net.minecraft.client.renderer.culling.Frustum frustum,
                                                   double x, double y, double z,
                                                   CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        // Always render the real player body when freecam is active
        // and showPlayer is enabled
        if (entity == mc.player && Freecam.isActive()) {
            cir.setReturnValue(Freecam.shouldRenderPlayer());
        }
    }
}
