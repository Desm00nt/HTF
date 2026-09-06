package com.howtofish.mod.economy;

import com.howtofish.mod.network.ModNetwork;
import com.howtofish.mod.network.SyncCurrencyPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/**
 * Very small helper that stores each player's Ruble balance directly in their
 * persistent NBT data (howtofish.rubles). Starting balance is 3, as requested.
 */
public class PlayerCurrency {
    private static final String TAG_ROOT = "HowToFishData";
    private static final String TAG_RUBLES = "Rubles";
    public static final int STARTING_BALANCE = 3;

    public static int get(Player player) {
        CompoundTag persist = player.getPersistentData();
        if (!persist.contains(TAG_ROOT)) {
            CompoundTag root = new CompoundTag();
            root.putInt(TAG_RUBLES, STARTING_BALANCE);
            persist.put(TAG_ROOT, root);
            return STARTING_BALANCE;
        }
        CompoundTag root = persist.getCompound(TAG_ROOT);
        if (!root.contains(TAG_RUBLES)) {
            root.putInt(TAG_RUBLES, STARTING_BALANCE);
        }
        return root.getInt(TAG_RUBLES);
    }

    public static void set(Player player, int value) {
        CompoundTag persist = player.getPersistentData();
        CompoundTag root = persist.contains(TAG_ROOT) ? persist.getCompound(TAG_ROOT) : new CompoundTag();
        int clamped = Math.max(0, value);
        root.putInt(TAG_RUBLES, clamped);
        persist.put(TAG_ROOT, root);
        sync(player, clamped);
    }

    /** Pushes the current balance to the owning client (HUD + shop display). */
    public static void requestSync(Player player) {
        sync(player, get(player));
    }

    private static void sync(Player player, int value) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncCurrencyPacket(value));
        }
    }

    public static void add(Player player, int amount) {
        set(player, get(player) + amount);
    }

    public static boolean spend(Player player, int amount) {
        int balance = get(player);
        if (balance < amount) return false;
        set(player, balance - amount);
        return true;
    }
}
