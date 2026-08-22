package net.bananacheese.bananaclient.neoforge;

import net.bananacheese.bananaclient.BananaClient;
import net.bananacheese.bananaclient.hud.HudRenderer;
import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;


@Mod(value = BananaClient.MOD_ID, dist = Dist.CLIENT)
public class BananaClientNeoForge {

    public BananaClientNeoForge(IEventBus modEventBus) {
        BananaClient.init();

        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
                HudRenderer.onRenderHud(event.getGuiGraphics()));

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) ->
                Freecam.onClientTick(Minecraft.getInstance()));
    }
}