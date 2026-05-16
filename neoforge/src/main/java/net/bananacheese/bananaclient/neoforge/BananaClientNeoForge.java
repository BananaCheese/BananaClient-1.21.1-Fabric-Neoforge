package net.bananacheese.bananaclient.neoforge;

import net.bananacheese.bananaclient.BananaClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;


@Mod(value = BananaClient.MOD_ID, dist = Dist.CLIENT)
public class BananaClientNeoForge {
    public BananaClientNeoForge(IEventBus modEventBus) {
        BananaClient.init();
        // keybinds and tick handled by mixin now
    }
}