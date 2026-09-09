package com.howtofish.mod.item;

import net.minecraft.world.item.ItemStack;

/**
 * THE single place that defines what a bait loaded into the rod actually DOES.
 * <p>
 * Fish species, the boss fight and the bite state machine never look at each
 * other - they only ask the bait for its {@link BaitKind}. To add a new bait
 * later (maggots, shiny spoon, ...), register the item, add one constant here
 * with its flags and it automatically works with the bobber, the rod menu and
 * the HUD. This keeps fish and boss logic decoupled from items.
 */
public enum BaitKind {
    /** Nothing loaded: standard bite cycle, common fish. */
    NONE(false, false),
    /** Golden Bait: valuable fish bite much sooner; survives many catches. */
    GOLDEN(true, false),
    /** Empty beer can from Old Sol: the Spider Crab's lure - ONE deep plunge, no nibbles, hooking it summons the boss. */
    CAN(false, true);

    public final boolean premiumFish;
    public final boolean summonBoss;

    BaitKind(boolean premiumFish, boolean summonBoss) {
        this.premiumFish = premiumFish;
        this.summonBoss = summonBoss;
    }

    public static BaitKind of(ItemStack bait) {
        if (bait.getItem() instanceof BaitItem) return GOLDEN;
        if (bait.getItem() instanceof EmptyCanItem) return CAN;
        return NONE;
    }

    /** Kind of the bait currently loaded into the rod the player holds. */
    public static BaitKind ofHeldRod(net.minecraft.world.entity.player.Player player) {
        for (ItemStack s : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (s.getItem() instanceof FishingRodCustomItem) {
                BaitKind kind = of(FishingRodCustomItem.getBait(s));
                if (kind != NONE) return kind;
            }
        }
        return NONE;
    }
}
