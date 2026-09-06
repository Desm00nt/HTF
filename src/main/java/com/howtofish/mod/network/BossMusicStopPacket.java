package com.howtofish.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-bound: stops the looping boss music (boss died or was removed). */
public class BossMusicStopPacket {

    public BossMusicStopPacket() {
    }

    public static void encode(BossMusicStopPacket packet, FriendlyByteBuf buf) {
    }

    public static BossMusicStopPacket decode(FriendlyByteBuf buf) {
        return new BossMusicStopPacket();
    }

    public static void handle(BossMusicStopPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                mc.getSoundManager().stop(null, net.minecraft.sounds.SoundSource.RECORDS);
            }
        });
        ctx.setPacketHandled(true);
    }
}
