package com.howtofish.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Syncs the "radar active" flag to the client HUD. */
public class SyncRadarPacket {

    private final boolean active;

    public SyncRadarPacket(boolean active) {
        this.active = active;
    }

    public static void encode(SyncRadarPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.active);
    }

    public static SyncRadarPacket decode(FriendlyByteBuf buf) {
        return new SyncRadarPacket(buf.readBoolean());
    }

    public static void handle(SyncRadarPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                com.howtofish.mod.client.RadarBarOverlay.clientRadarActive = packet.active;
            }
        });
        ctx.setPacketHandled(true);
    }

    /** Server helper: push the flag to the player's HUD. */
    public static void sync(ServerPlayer player, boolean active) {
        com.howtofish.mod.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SyncRadarPacket(active));
    }
}
