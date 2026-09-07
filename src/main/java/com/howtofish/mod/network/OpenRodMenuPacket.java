package com.howtofish.mod.network;

import com.howtofish.mod.item.FishingRodCustomItem;
import com.howtofish.mod.menu.RodBaitMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent when the player presses "B" in game. If the fishing rod is in the main
 * hand, the server opens the rod's bait menu (one bait slot stored in the
 * rod's NBT + the player inventory).
 */
public class OpenRodMenuPacket {

    public OpenRodMenuPacket() {
    }

    public static void encode(OpenRodMenuPacket packet, FriendlyByteBuf buf) {
    }

    public static OpenRodMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenRodMenuPacket();
    }

    public static void handle(OpenRodMenuPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ItemStack rod = player.getMainHandItem();
            if (!(rod.getItem() instanceof FishingRodCustomItem)) {
                player.displayClientMessage(Component.translatable("message.howtofish.need_rod"), true);
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new RodBaitMenu(id, inv, rod),
                    Component.translatable("menu.howtofish.rod_bait")));
        });
        ctx.setPacketHandled(true);
    }
}
