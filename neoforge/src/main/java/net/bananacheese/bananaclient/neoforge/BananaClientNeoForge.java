package net.bananacheese.bananaclient.neoforge;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.bananacheese.bananaclient.BananaClient;
import net.bananacheese.bananaclient.hud.HudRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;


@Mod(value = BananaClient.MOD_ID, dist = Dist.CLIENT)
public class BananaClientNeoForge {

    public BananaClientNeoForge(IEventBus modEventBus) {
        BananaClient.init();
        ClientGuiEvent.RENDER_HUD.register((gfx, tickDelta) -> {
            HudRenderer.onRenderHud(gfx);
        });
    }
}