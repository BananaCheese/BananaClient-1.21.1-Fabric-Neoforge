package net.bananacheese.bananaclient.mixin.freecam;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    // Show Hands = false: skip rendering the held item/arms entirely.
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onRenderItemInHand(CallbackInfo ci) {
        if (Freecam.isActive() && !Freecam.shouldRenderHands()) {
            ci.cancel();
        }
    }

    // Don't highlight blocks under the crosshair while interactions are blocked.
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.isActive() && Freecam.shouldPreventInteractions()) {
            cir.setReturnValue(false);
        }
    }

    // When Allow Interactions is on, crosshair/click raytracing targets what
    // the real player is looking at, not what the freecam is looking at.
    @ModifyVariable(method = "pick(F)V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"))
    private Entity bananaclient$onGetHitTargetSource(Entity entity) {
        if (Freecam.isActive() && Freecam.allowInteractionsFromPlayer()) {
            return Minecraft.getInstance().player;
        }
        return entity;
    }
}
