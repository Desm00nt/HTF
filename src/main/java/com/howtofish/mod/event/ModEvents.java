package com.howtofish.mod.event;

import com.howtofish.mod.economy.PlayerCurrency;
import com.howtofish.mod.economy.PlayerQuestData;
import com.howtofish.mod.network.SyncRadarPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
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

    /**
     * Keeps the visible hotbar to exactly THREE slots: any item that ends up
     * in hotbar slots 3-8 is silently relocated into the storage grid (9-35),
     * and the selected slot is clamped to 0-2 (the client is explicitly told
     * so the highlight never desyncs). Creative players are exempt.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        Inventory inv = player.getInventory();
        for (int i = 3; i <= 8; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            for (int j = 9; j < 36; j++) {
                if (inv.getItem(j).isEmpty()) {
                    inv.setItem(j, stack);
                    inv.setItem(i, ItemStack.EMPTY);
                    break;
                }
            }
        }
        if (inv.selected >= 3) {
            inv.selected = 0;
            player.connection.send(new ClientboundSetCarriedItemPacket(0));
        }
    }
}
