package com.howtofish.mod.network;

import com.howtofish.mod.economy.PlayerCurrency;
import com.howtofish.mod.menu.OldManShopMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sent client -&gt; server when the player clicks a buy button in the Old Man shop. */
public class BuyItemPacket {
    private final String offerId;

    public BuyItemPacket(String offerId) {
        this.offerId = offerId;
    }

    public static void encode(BuyItemPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.offerId);
    }

    public static BuyItemPacket decode(FriendlyByteBuf buf) {
        return new BuyItemPacket(buf.readUtf());
    }

    public static void handle(BuyItemPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            OldManShopMenu.OFFERS.stream()
                    .filter(o -> o.id().equals(packet.offerId))
                    .findFirst()
                    .ifPresent(offer -> {
                        if (PlayerCurrency.spend(player, offer.price())) {
                            var stack = offer.display().copy();
                            if (!player.getInventory().add(stack)) {
                                player.drop(stack, false);
                            }
                            player.displayClientMessage(Component.translatable("message.howtofish.bought", offer.price()), true);
                        } else {
                            player.displayClientMessage(Component.translatable("message.howtofish.not_enough_money"), true);
                        }
                    });
        });
        ctx.setPacketHandled(true);
    }
}
