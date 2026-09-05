package com.howtofish.mod.item;

import com.howtofish.mod.entity.CustomFishEntity;
import com.howtofish.mod.entity.FishType;
import com.howtofish.mod.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;

/**
 * "How To Fish" fishing rod: instead of reeling the catch into the inventory,
 * it always performs a catch &amp; release - a live, thrashing fish (or, rarely,
 * the boss) is spawned right in front of the player in the water. The player
 * then has to finish it off with the {@link KnifeItem} and bring the meat to
 * the Old Man for Rubles.
 */
public class FishingRodCustomItem extends Item {

    public FishingRodCustomItem(Properties properties) {
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
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.howtofish.no_water"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            RandomSource random = level.getRandom();
            FishType type = randomFishType(random);
            CustomFishEntity fish = new CustomFishEntity(com.howtofish.mod.registry.ModEntities.CUSTOM_FISH.get(), level);
            fish.setFishType(type);
            fish.setPos(blockHit.getBlockPos().getX() + 0.5, blockHit.getBlockPos().getY() + 1.0, blockHit.getBlockPos().getZ() + 0.5);
            level.addFreshEntity(fish);
            level.playSound(null, fish.blockPosition(), ModSounds.FISH_FLOP.get(), SoundSource.PLAYERS, 1.0f, 1.0f + (random.nextFloat() - 0.5f) * 0.4f);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.howtofish.caught", type.getId()), true);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }

        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private FishType randomFishType(RandomSource random) {
        FishType[] values = FishType.values();
        return values[random.nextInt(values.length)];
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
