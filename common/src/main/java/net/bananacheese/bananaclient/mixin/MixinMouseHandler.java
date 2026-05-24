package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    /**
     * Intercepts the player rotation update.
     * When freecam is active we want the mouse to rotate the camera
     * entity, not the real player. We cancel vanilla rotation and
     * apply it to the FreeCamera instead.
     *
     * turnPlayer() in 1.21.1 takes no parameters — it reads the
     * accumulated deltas from its own fields internally.
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onTurnPlayer(CallbackInfo ci) {
        if (!Freecam.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        MouseHandler self = (MouseHandler)(Object) this;

        // Read accumulated mouse delta via accessor
        MouseHandlerAccessor acc = (MouseHandlerAccessor) self;
        double dx = acc.getAccumulatedDX();
        double dy = acc.getAccumulatedDY();

        // Consume the deltas so vanilla doesn't also process them
        acc.setAccumulatedDX(0.0);
        acc.setAccumulatedDY(0.0);

        // Calculate sensitivity scale — same formula Minecraft uses
        float sensitivity = mc.options.sensitivity().get().floatValue();
        float scale = sensitivity * 0.6f + 0.2f;
        scale = scale * scale * scale * 8.0f;

        var cam = Freecam.getCamera();
        if (cam == null) return;

        float newYaw   = cam.getYRot() + (float)(dx * scale * 0.15);
        float newPitch = cam.getXRot() + (float)(dy * scale * 0.15);
        // Clamp pitch between -90 and 90
        newPitch = Math.clamp(newPitch, -90.0f, 90.0f);

        cam.setYRot(newYaw);
        cam.setXRot(newPitch);
        // Update the "old" rotation values too so interpolation is correct
        cam.yRotO = newYaw;
        cam.xRotO = newPitch;

        // Cancel vanilla rotation so the real player doesn't move
        ci.cancel();
    }
}
