package net.bananacheese.bananaclient.fabric.client;

import net.bananacheese.bananaclient.BananaClient;

import net.fabricmc.api.ClientModInitializer;

public class BananaClientFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BananaClient.init();
        // keybinds and tick handled by mixin now
    }
}
