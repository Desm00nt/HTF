package com.howtofish.mod.event;

import com.howtofish.mod.economy.PlayerCurrency;
import com.howtofish.mod.economy.PlayerQuestData;
import com.howtofish.mod.network.SyncRadarPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Misc player lifecycle events (starting balance, radar sync, data persistence). */
public class ModEvents {

    @SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        com.howtofish.mod.command.ModCommands.register(event.getDispatcher());
    }

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

    /**
     * The custom HUD shows only hotbar cells 0-2, but slots 3-8 stay REAL
     * (visible in the inventory screen) - nothing is ever relocated, because
     * that race was what made items "vanish / show as dirt" before. The only
     * server-side guard left is clamping a bogus selection back to cell 0.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        Inventory inv = player.getInventory();
        if (inv.selected >= 3) {
            inv.selected = 0;
            player.connection.send(new ClientboundSetCarriedItemPacket(0));
        }
    }
}
