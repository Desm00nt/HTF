package com.howtofish.mod.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Tracks small per-player quest flags, e.g. whether the boss trophy was turned in. */
public class PlayerQuestData {
    private static final String TAG_ROOT = "HowToFishData";
    private static final String TAG_NEXT_ISLAND = "NextIslandUnlocked";

    public static boolean hasUnlockedNextIsland(Player player) {
        CompoundTag persist = player.getPersistentData();
        return persist.contains(TAG_ROOT) && persist.getCompound(TAG_ROOT).getBoolean(TAG_NEXT_ISLAND);
    }

    public static void unlockNextIsland(Player player) {
        CompoundTag persist = player.getPersistentData();
        CompoundTag root = persist.contains(TAG_ROOT) ? persist.getCompound(TAG_ROOT) : new CompoundTag();
        root.putBoolean(TAG_NEXT_ISLAND, true);
        persist.put(TAG_ROOT, root);
    }
}
