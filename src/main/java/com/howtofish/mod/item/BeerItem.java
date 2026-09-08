package com.howtofish.mod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A bottle of beer. NOT a bait and NOT a drink for the player: hand it to
 * Old Sol (right-click him) - he gulps it down, belches, and returns the
 * EMPTY CAN ({@link EmptyCanItem}), which is the real Spider Crab lure.
 * The beer's whole job is to get that can.
 */
public class BeerItem extends Item {

    public BeerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.howtofish.beer_give"));
        
    }
}
