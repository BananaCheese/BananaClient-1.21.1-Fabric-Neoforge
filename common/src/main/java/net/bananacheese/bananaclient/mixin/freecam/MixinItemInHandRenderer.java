package net.bananacheese.bananaclient.mixin.freecam;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NOTE: the target method name here (renderHandsWithItems) matches the
 * pre-"submit" rendering pipeline used in 1.21.1. If your local mappings use
 * a different name for the method that iterates both hands each frame,
 * update the four @Redirect/@Inject/@ModifyVariable targets below to match.
 */
@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Unique
    private float bananaclient$tickDelta;

    @Redirect(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F"))
    private float bananaclient$getViewXRot(LocalPlayer player, float partialTick) {
        return Freecam.isActive() && Freecam.getFreeCamera() != null ? Freecam.getFreeCamera().getViewXRot(partialTick) : player.getViewXRot(partialTick);
    }

    @Redirect(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F"))
    private float bananaclient$getViewYRot(LocalPlayer player, float partialTick) {
        return Freecam.isActive() && Freecam.getFreeCamera() != null ? Freecam.getFreeCamera().getViewYRot(partialTick) : player.getViewYRot(partialTick);
    }

    @Redirect(method = "renderHandsWithItems", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;xBob:F", opcode = Opcodes.GETFIELD))
    private float bananaclient$getXBob(LocalPlayer player) {
        return Freecam.isActive() && Freecam.getFreeCamera() != null ? Freecam.getFreeCamera().xBob : player.xBob;
    }

    @Redirect(method = "renderHandsWithItems", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;xBobO:F", opcode = Opcodes.GETFIELD))
    private float bananaclient$getXBobO(LocalPlayer player) {
        return Freecam.isActive() && Freecam.getFreeCamera() != null ? Freecam.getFreeCamera().xBobO : player.xBobO;
    }

    @Redirect(method = "renderHandsWithItems", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;yBob:F", opcode = Opcodes.GETFIELD))
    private float bananaclient$getYBob(LocalPlayer player) {
        return Freecam.isActive() && Freecam.getFreeCamera() != null ? Freecam.getFreeCamera().yBob : player.yBob;
    }

    @Redirect(method = "renderHandsWithItems", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;yBobO:F", opcode = Opcodes.GETFIELD))
    private float bananaclient$getYBobO(LocalPlayer player) {
        return Freecam.isActive() && Freecam.getFreeCamera() != null ? Freecam.getFreeCamera().yBobO : player.yBobO;
    }

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void bananaclient$storeTickDelta(float partialTick,
                                             com.mojang.blaze3d.vertex.PoseStack poseStack,
                                             net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource,
                                             LocalPlayer player,
                                             int combinedLight,
                                             CallbackInfo ci) {
        this.bananaclient$tickDelta = partialTick;
    }

    @ModifyVariable(method = "renderHandsWithItems", at = @At("HEAD"), argsOnly = true)
    private int bananaclient$onSetLight(int lightCoords) {
        if (Freecam.isActive() && Freecam.getFreeCamera() != null) {
            return Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(Freecam.getFreeCamera(), bananaclient$tickDelta);
        }
        return lightCoords;
    }
}
