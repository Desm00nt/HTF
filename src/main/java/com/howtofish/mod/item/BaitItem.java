package com.howtofish.mod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Golden Bait - a REUSABLE lure sold by the Old Man. Put it into the bait
 * slot (the offhand slot) by pressing the "B" key or by hand: while equipped,
 * valuable fish bite much more often and start biting faster. It is never
 * consumed.
 */
public class BaitItem extends Item {

    public BaitItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.howtofish.bait"));
        tooltip.add(Component.translatable("tooltip.howtofish.bait_key"));
    }
}
