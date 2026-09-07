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
 * A can of beer. Drinkable by the player like normal food, but its main
 * purpose is being handed to the Old Man - he'll drink it (eye/mouth
 * animation) and give back an {@link EmptyCanItem}, the bait needed to
 * summon the Spider Crab boss.
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
