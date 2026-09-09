package com.howtofish.mod.network;

import com.howtofish.mod.entity.BobberEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -&gt; server: the player pressed ATTACK (LMB) while a fish is hooked.
 * Left click is the reel stroke - RMB stays cast/hook only, so holding or
 * mashing RMB can never yank the line back by accident.
 */
public class ReelClickPacket {

    public static void encode(ReelClickPacket packet, FriendlyByteBuf buf) {
    }

    public static ReelClickPacket decode(FriendlyByteBuf buf) {
        return new ReelClickPacket();
    }

    public static void handle(ReelClickPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player == null) return;
            BobberEntity bobber = BobberEntity.getForPlayer(player.level, player);
            if (bobber != null) {
                bobber.applyReelStroke();
            }
        });
        ctx.setPacketHandled(true);
    }
}
