package com.howtofish.mod.network;

import com.howtofish.mod.HowToFishMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HowToFishMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, BuyItemPacket.class, BuyItemPacket::encode, BuyItemPacket::decode, BuyItemPacket::handle);
        CHANNEL.registerMessage(id++, SyncCurrencyPacket.class, SyncCurrencyPacket::encode, SyncCurrencyPacket::decode, SyncCurrencyPacket::handle);
    }
}
