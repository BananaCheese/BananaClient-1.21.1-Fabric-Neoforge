package net.bananacheese.bananaclient.mixin.freecam;

import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NOTE: if your local mappings don't have Gui#getCameraPlayer(), the HUD
 * simply isn't wired through that indirection in your version — safe to
 * delete this mixin entirely; nothing else depends on it.
 */
@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
    private void bananaclient$onGetCameraPlayer(CallbackInfoReturnable<Player> cir) {
        if (Freecam.isActive() && Minecraft.getInstance().player != null) {
            cir.setReturnValue(Minecraft.getInstance().player);
        }
    }
}
