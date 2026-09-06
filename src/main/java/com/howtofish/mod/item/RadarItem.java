package com.howtofish.mod.item;

import com.howtofish.mod.economy.PlayerQuestData;
import com.howtofish.mod.network.SyncRadarPacket;
import com.howtofish.mod.world.IslandBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Handheld Radar: once the Old Man has been fed a boss trophy, right-clicking
 * switches the radar into its always-on mode - a bossbar-style heading strip
 * appears at the top of the screen showing the direction of (and distance to)
 * the second island.
 */
public class RadarItem extends Item {
    public RadarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!PlayerQuestData.hasUnlockedNextIsland(player)) {
            player.displayClientMessage(Component.translatable("message.howtofish.radar_locked"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!PlayerQuestData.isRadarActive(player)) {
            PlayerQuestData.setRadarActive(player, true);
            if (player instanceof ServerPlayer sp) {
                SyncRadarPacket.sync(sp, true);
            }
            var coords = IslandBuilder.SECOND_ISLAND_ORIGIN;
            player.sendSystemMessage(Component.translatable("message.howtofish.radar_on", coords.getX(), coords.getZ()));
        } else {
            PlayerQuestData.setRadarActive(player, false);
            if (player instanceof ServerPlayer sp) {
                SyncRadarPacket.sync(sp, false);
            }
            player.displayClientMessage(Component.translatable("message.howtofish.radar_off"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
