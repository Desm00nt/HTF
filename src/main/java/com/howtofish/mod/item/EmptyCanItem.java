package com.howtofish.mod.item;

import com.howtofish.mod.entity.BossFishEntity;
import com.howtofish.mod.registry.ModEntities;
import com.howtofish.mod.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;

/**
 * Empty beer can - the bait given by the Old Man after he drinks a Beer.
 * Throwing/using it on water summons the Spider Crab boss fish.
 */
public class EmptyCanItem extends Item {
    public EmptyCanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(6.0d, 0.0f, true);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return InteractionResultHolder.pass(stack);
        }
        FluidState fluid = level.getFluidState(blockHit.getBlockPos());
        if (!fluid.is(FluidTags.WATER)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            BossFishEntity boss = new BossFishEntity(ModEntities.BOSS_FISH.get(), level);
            boss.setPos(blockHit.getBlockPos().getX() + 0.5, blockHit.getBlockPos().getY() + 1.0, blockHit.getBlockPos().getZ() + 0.5);
            level.addFreshEntity(boss);
            level.playSound(null, boss.blockPosition(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.5f, 0.8f);
            player.sendSystemMessage(Component.translatable("message.howtofish.boss_summoned"));
            stack.shrink(1);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
