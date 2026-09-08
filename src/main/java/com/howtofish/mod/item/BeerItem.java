package com.howtofish.mod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A can of beer - the SPIDER CRAB's lure. Put it into the rod's bait slot
 * (press B holding the rod) and cast into the sea: after a long wait the
 * float plunges hard, and hooking THAT bite summons the boss. One beer =
 * one summoning attempt, exactly like in the reference game where you fish
 * the crab out with a can of brew.
 */
public class BeerItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.2f).alwaysEat().build();

    public BeerItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.howtofish.beer_bait"));
        tooltip.add(Component.translatable("tooltip.howtofish.bait_key"));
    }
}
