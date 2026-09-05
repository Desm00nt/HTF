package com.howtofish.mod.economy;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.entity.player.Player;

/** Makes sure freshly joining players get their starting balance initialised. */
public class CurrencyEvents {
    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerCurrency.get(player); // lazily initialises the balance to the starting amount
        }
    }
}
