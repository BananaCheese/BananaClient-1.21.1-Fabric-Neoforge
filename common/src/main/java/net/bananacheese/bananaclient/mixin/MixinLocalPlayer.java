package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.modules.movement.NoFall;
import net.bananacheese.bananaclient.modules.player.Reach;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStepHead(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer)(Object) this;

        // NoFall auto-place tick
        NoFall.onTick(self);

        // Note: freezing the real player while freecam is active, and the
        // freecam's own per-tick movement, are now handled by
        // MixinEntity (freecam mixin package) and FreeCamera itself.

        // Reach — apply attribute every tick
        if (Reach.isActive()) {
            var attr = self.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (attr != null) attr.setBaseValue(Reach.getDistance());
            var entityAttr = self.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (entityAttr != null) entityAttr.setBaseValue(Reach.getDistance());
        } else {
            var attr = self.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (attr != null && attr.getBaseValue() != 4.5) attr.setBaseValue(4.5);
            var entityAttr = self.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (entityAttr != null && entityAttr.getBaseValue() != 3.0)
                entityAttr.setBaseValue(3.0);
        }
    }
}