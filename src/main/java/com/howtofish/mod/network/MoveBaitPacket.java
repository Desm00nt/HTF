package com.howtofish.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent when the player presses the "Move bait" key (B).
 * If the Golden Bait is in the inventory and the offhand slot is free, the
 * bait moves there; pressing B with the bait in the offhand puts it back into
 * the hotbar. The bait is never consumed.
 */
public class MoveBaitPacket {

    public MoveBaitPacket() {
    }

    public static void encode(MoveBaitPacket packet, FriendlyByteBuf buf) {
    }

    public static MoveBaitPacket decode(FriendlyByteBuf buf) {
        return new MoveBaitPacket();
    }

    public static void handle(MoveBaitPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            var inventory = player.getInventory();
            ItemStack offhand = inventory.offhand.get(0);

            // Case 1: bait already in offhand -> move it back to the hotbar.
            if (offhand.getItem() instanceof com.howtofish.mod.item.BaitItem) {
                for (int i = 0; i < 9; i++) {
                    if (inventory.getItem(i).isEmpty()) {
                        inventory.setItem(i, offhand.copy());
                        inventory.offhand.set(0, ItemStack.EMPTY);
                        player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
                        return;
                    }
                }
                return;
            }

            // Case 2: bait anywhere in the inventory -> put it into the offhand
            // (swap if the offhand is occupied).
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).getItem() instanceof com.howtofish.mod.item.BaitItem) {
                    inventory.offhand.set(0, inventory.getItem(i).copy());
                    inventory.setItem(i, offhand);
                    player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
                    return;
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
