package com.howtofish.mod.event;

import com.howtofish.mod.economy.PlayerCurrency;
import com.howtofish.mod.economy.PlayerQuestData;
import com.howtofish.mod.network.SyncRadarPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Misc player lifecycle events (starting balance, radar sync, data persistence). */
public class ModEvents {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Initialise the starting balance and push it to the client HUD.
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerCurrency.requestSync(player);
            SyncRadarPacket.sync(player, PlayerQuestData.isRadarActive(player));
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            int rubles = PlayerCurrency.get(event.getOriginal());
            PlayerCurrency.set(event.getEntity(), rubles);
            // Keep the mod's quest flags (next island, radar) across death.
            CompoundTag oldRoot = event.getOriginal().getPersistentData()
                    .getCompound(PlayerQuestData.TAG_ROOT_DATA());
            if (!oldRoot.isEmpty()) {
                event.getEntity().getPersistentData().put(PlayerQuestData.TAG_ROOT_DATA(), oldRoot.copy());
            }
            if (event.getEntity() instanceof ServerPlayer sp) {
                SyncRadarPacket.sync(sp, PlayerQuestData.isRadarActive(event.getEntity()));
            }
        }
    }
}
