package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class MixinCamera {

    /**
     * After vanilla sets up the camera, override position and rotation
     * with the freecam values interpolated by partialTick.
     *
     * Using @At("RETURN") ensures vanilla has fully initialized the camera
     * (frustum, block occlusion checks, etc.) before we override position.
     * We then replace position and rotation — vanilla's interpolation
     * from prevPos/prevRot is bypassed and we supply our own lerp.
     */
    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetup(BlockGetter level, Entity entity,
                         boolean thirdPerson, boolean inverseView,
                         float partialTick, CallbackInfo ci) {
        if (!Freecam.isActive()) return;

        CameraAccessor self = (CameraAccessor)(Object) this;

        // Use our lerped positions — this is what prevents the shaking
        // Previous shaking was caused by setting integer-tick positions
        // without interpolation between frames
        self.invokeSetPosition(
                Freecam.getX(partialTick),
                Freecam.getY(partialTick),
                Freecam.getZ(partialTick)
        );
        self.invokeSetRotation(
                Freecam.getYaw(partialTick),
                Freecam.getPitch(partialTick)
        );
    }
}
