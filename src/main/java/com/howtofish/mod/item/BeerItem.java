package com.howtofish.mod.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/**
 * A can of beer. Drinkable by the player like normal food, but its main
 * purpose is being handed to the Old Man - he'll drink it (eye/mouth
 * animation) and give back an {@link EmptyCanItem}, the bait needed to
 * summon the Spider Crab boss.
 */
public class BeerItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.2f).alwaysEdible().build();

    public BeerItem(Properties properties) {
        super(properties.food(FOOD));
    }
}
