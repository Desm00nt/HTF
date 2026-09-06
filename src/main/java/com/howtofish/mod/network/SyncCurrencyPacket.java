package com.howtofish.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: keeps the player's Ruble balance in sync so the HUD and
 * the shop screen show the up-to-date value (persistent NBT is not synced
 * automatically).
 */
public class SyncCurrencyPacket {
    private final int rubles;

    public SyncCurrencyPacket(int rubles) {
        this.rubles = rubles;
    }

    public static void encode(SyncCurrencyPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.rubles);
    }

    public static SyncCurrencyPacket decode(FriendlyByteBuf buf) {
        return new SyncCurrencyPacket(buf.readVarInt());
    }

    public static void handle(SyncCurrencyPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                com.howtofish.mod.client.CurrencyHudOverlay.setClientBalance(packet.rubles);
            }
        });
        ctx.setPacketHandled(true);
    }
}
