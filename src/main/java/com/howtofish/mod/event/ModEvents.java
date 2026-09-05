package com.howtofish.mod.event;

import com.howtofish.mod.economy.PlayerCurrency;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Misc player lifecycle events (starting balance, etc.). */
public class ModEvents {

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            int rubles = PlayerCurrency.get(event.getOriginal());
            PlayerCurrency.set(event.getEntity(), rubles);
        }
    }
}
