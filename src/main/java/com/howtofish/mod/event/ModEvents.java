package com.howtofish.mod.event;

import com.howtofish.mod.economy.PlayerCurrency;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Misc player lifecycle events (starting balance, etc.). */
public class ModEvents {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Initialise the starting balance and push it to the client HUD.
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerCurrency.requestSync(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            int rubles = PlayerCurrency.get(event.getOriginal());
            PlayerCurrency.set(event.getEntity(), rubles);
        }
    }
}
