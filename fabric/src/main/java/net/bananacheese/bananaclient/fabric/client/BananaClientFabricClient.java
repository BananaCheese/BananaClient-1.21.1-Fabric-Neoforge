package net.bananacheese.bananaclient.fabric.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.bananacheese.bananaclient.BananaClient;

import net.bananacheese.bananaclient.hud.HudRenderer;
import net.bananacheese.bananaclient.modules.render.Freecam;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class BananaClientFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BananaClient.init();
        ClientGuiEvent.RENDER_HUD.register((gfx, tickDelta) -> {
            HudRenderer.onRenderHud(gfx);
        });

        // Fabric-specific fix for Freecam's "Show Player" setting: unlike
        // NeoForge, Fabric's (unmodified-vanilla) render pipeline excludes
        // the local player from the level's entity render pass entirely —
        // not just via EntityRenderDispatcher#shouldRender — to avoid the
        // classic "see your own head in first person" bug. That means once
        // the camera detaches, forcing shouldRender=true (see
        // MixinEntityRenderDispatcher) isn't enough here: the player never
        // reaches that check in the first place. So on Fabric we render it
        // back in manually, right after the normal entity pass.
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || context.consumers() == null) return;
            if (!Freecam.isActive() || !Freecam.shouldRenderPlayer()) return;

            float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(true);
            int light = mc.getEntityRenderDispatcher().getPackedLightCoords(player, partialTick);
            mc.getEntityRenderDispatcher().render(
                    player,
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(),
                    partialTick,
                    context.matrixStack(),
                    context.consumers(),
                    light
            );
        });
    }
}