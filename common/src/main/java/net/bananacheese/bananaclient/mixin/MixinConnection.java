package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.modules.movement.NoFall;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Connection.class)
public class MixinConnection {

    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"), argsOnly = true)
    private Packet<?> onSend(Packet<?> packet) {
        if (!NoFall.shouldSendPacket()) return packet;
        if (!(packet instanceof ServerboundMovePlayerPacket move)) return packet;
        if (move.isOnGround()) return packet; // already grounded, skip to avoid recursion

        if (move instanceof ServerboundMovePlayerPacket.PosRot) {
            return new ServerboundMovePlayerPacket.PosRot(
                    move.getX(0), move.getY(0), move.getZ(0),
                    move.getYRot(0), move.getXRot(0), true);
        }
        if (move instanceof ServerboundMovePlayerPacket.Pos) {
            return new ServerboundMovePlayerPacket.Pos(
                    move.getX(0), move.getY(0), move.getZ(0), true);
        }
        if (move instanceof ServerboundMovePlayerPacket.Rot) {
            return new ServerboundMovePlayerPacket.Rot(
                    move.getYRot(0), move.getXRot(0), true);
        }
        return new ServerboundMovePlayerPacket.StatusOnly(true);
    }
}
