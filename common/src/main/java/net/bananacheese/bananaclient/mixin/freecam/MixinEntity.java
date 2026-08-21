package net.bananacheese.bananaclient.mixin.freecam;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NOTE: freezing the real player in place is now handled by
 * Freecam#onClientTick (position pinned after each tick) rather than by
 * cancelling low-level movement calls here — cancelling setDeltaMovement/
 * moveRelative/setPos fights the player entity's own tick logic and was
 * causing a stuck-jump-loop glitch when freecam was toggled mid-air.
 */
@Mixin(Entity.class)
public class MixinEntity {

    // Makes mouse input rotate the freecam instead of the real player.
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onTurn(double yRot, double xRot, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (Freecam.isActive() && freecam$this() == mc.player && Freecam.getFreeCamera() != null) {
            Freecam.getFreeCamera().turn(yRot, xRot);
            ci.cancel();
        }
    }

    // Prevents the freecam entity from pushing, or being pushed by, other entities.
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onPush(Entity entity, CallbackInfo ci) {
        if (Freecam.isActive() && (entity == Freecam.getFreeCamera() || freecam$this() == Freecam.getFreeCamera())) {
            ci.cancel();
        }
    }

    @Unique
    private Entity freecam$this() {
        return (Entity) (Object) this;
    }
}