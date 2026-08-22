package net.bananacheese.bananaclient.fabric.client;

import net.bananacheese.bananaclient.BananaClient;
import net.bananacheese.bananaclient.hud.HudRenderer;
import net.bananacheese.bananaclient.modules.combat.Criticals;
import net.bananacheese.bananaclient.modules.movement.Velocity;
import net.bananacheese.bananaclient.modules.render.Freecam;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class BananaClientFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BananaClient.init();

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            HudRenderer.onRenderHud(guiGraphics);
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Freecam.onClientTick(mc);
            Criticals.onClientTick(mc);
            Velocity.onClientTick(mc);
        });
    }
}