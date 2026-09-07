package com.howtofish.mod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Golden Bait - a REUSABLE lure sold by the Old Man. Insert it into the
 * rod's bait slot via the rod menu ("B" key with the rod in hand). While
 * equipped, valuable fish bite much more often and start biting faster.
 * It survives {@value #MAX_USES} catches and then breaks.
 */
public class BaitItem extends Item {

    public static final int MAX_USES = 15;

    public BaitItem(Properties properties) {
        super(properties);
    }

    /** Remaining catches, stored in the bait stack's Damage tag (only used while in the rod). */
    public static int getBaitDamage(ItemStack stack) {
        return stack.getOrCreateTag().getInt("Damage");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getBaitDamage(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (1.0f - (float) getBaitDamage(stack) / MAX_USES));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFD700;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.howtofish.bait"));
        tooltip.add(Component.translatable("tooltip.howtofish.bait_key"));
    }
}
