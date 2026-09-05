package com.howtofish.mod.item;

import com.howtofish.mod.economy.PlayerQuestData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Handheld Radar: right-click while riding the boat to reveal the coordinates
 * of the next fishing island, once the Old Man has been fed a boss trophy
 * (e.g. the Spider Crab Shell).
 */
public class RadarItem extends Item {
    public RadarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        boolean inBoat = player.getVehicle() instanceof Boat;
        boolean unlocked = PlayerQuestData.hasUnlockedNextIsland(player);

        if (!unlocked) {
            player.displayClientMessage(Component.translatable("message.howtofish.radar_locked"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!inBoat) {
            player.displayClientMessage(Component.translatable("message.howtofish.radar_need_boat"), true);
            return InteractionResultHolder.fail(stack);
        }

        var coords = com.howtofish.mod.world.IslandBuilder.SECOND_ISLAND_ORIGIN;
        player.sendSystemMessage(Component.translatable("message.howtofish.radar_coords", coords.getX(), coords.getY(), coords.getZ()));
        return InteractionResultHolder.consume(stack);
    }
}
