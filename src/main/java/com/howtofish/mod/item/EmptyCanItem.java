package com.howtofish.mod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Empty beer can Old Sol hands over for the beer he never gets to finish.
 * <p>
 * It is a passive lure only: clicking the can (in hand or at water) does
 * nothing by design - it is loaded into the rod via the rod bait menu and
 * its magic works entirely through the bobber.
 */
public class EmptyCanItem extends Item {
    public EmptyCanItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.howtofish.empty_can.tooltip"));
        tooltip.add(Component.translatable("tooltip.howtofish.bait_key"));
    }
}
