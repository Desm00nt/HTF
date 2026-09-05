package com.howtofish.mod.item;

import net.minecraft.world.item.Item;

/**
 * Physical Ruble coin item - mainly for decoration/trading with other players;
 * the HUD balance used for the shop is tracked separately via PlayerCurrency capability.
 */
public class RubleCoinItem extends Item {
    public RubleCoinItem(Properties properties) {
        super(properties);
    }
}
