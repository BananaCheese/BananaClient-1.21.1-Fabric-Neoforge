package net.bananacheese.bananaclient.mixin.freecam;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.bananacheese.bananaclient.modules.render.freecam.FreeCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class MixinCamera {

    @Shadow private Entity entity;
    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;

    // Snap the camera's eye height instantly (no smoothing) whenever we swap
    // onto or off of the freecam entity, since the two eye heights can differ
    // wildly (swimming pose vs standing) and a smoothed transition looks wrong.
    @Inject(method = "setup", at = @At("HEAD"))
    private void bananaclient$onSetup(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float partialTick, CallbackInfo ci) {
        if (entity == null || this.entity == null || entity == this.entity) return;
        if (entity instanceof FreeCamera || this.entity instanceof FreeCamera) {
            this.eyeHeightOld = this.eyeHeight = entity.getEyeHeight();
        }
    }

    // Removes the red/blue submersion overlay while freecam is active — the
    // camera isn't really underwater/in lava even if it's positioned there.
    @Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onGetFluidInCamera(CallbackInfoReturnable<FogType> cir) {
        if (Freecam.isActive()) {
            cir.setReturnValue(FogType.NONE);
        }
    }
}
