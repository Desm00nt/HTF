package com.howtofish.mod.item;

import com.howtofish.mod.entity.FishType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/**
 * Meat/loot obtained by killing a released fish with the Knife. Feed it to the
 * Old Man to earn Rubles (amount depends on {@link FishType#getPrice()}).
 */
public class FishMeatItem extends Item {
    private final FishType fishType;

    public FishMeatItem(Properties properties, FishType fishType) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(2).saturationMod(0.3f).build()));
        this.fishType = fishType;
    }

    public FishType getFishType() {
        return fishType;
    }
}
