package net.bananacheese.bananaclient.fabric.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.bananacheese.bananaclient.BananaClient;

import net.bananacheese.bananaclient.hud.HudRenderer;
import net.fabricmc.api.ClientModInitializer;

public class BananaClientFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BananaClient.init();
        ClientGuiEvent.RENDER_HUD.register((gfx, tickDelta) -> {
            HudRenderer.onRenderHud(gfx);
        });
    }
}