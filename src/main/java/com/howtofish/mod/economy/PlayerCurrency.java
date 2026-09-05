package com.howtofish.mod.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

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
        root.putInt(TAG_RUBLES, Math.max(0, value));
        persist.put(TAG_ROOT, root);
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
