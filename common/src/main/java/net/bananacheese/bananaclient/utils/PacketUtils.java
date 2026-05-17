package net.bananacheese.bananaclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

public class PacketUtils {

    /**
     * Sends a packet to the server from the client side.
     * Safe to call from the render/game thread.
     */
    public static void sendPacket(Packet<?> packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().send(packet);
        }
    }
}
