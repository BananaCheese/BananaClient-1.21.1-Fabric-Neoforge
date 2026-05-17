package net.bananacheese.bananaclient.fabric.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.bananacheese.bananaclient.BananaClient;

import net.bananacheese.bananaclient.hud.HudRenderer;
import net.bananacheese.bananaclient.modules.movement.NoFall;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class BananaClientFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BananaClient.init();
        ClientGuiEvent.RENDER_HUD.register((gfx, tickDelta) -> {
            HudRenderer.onRenderHud(gfx);
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                NoFall.onTick();
            });
        });
    }
}
