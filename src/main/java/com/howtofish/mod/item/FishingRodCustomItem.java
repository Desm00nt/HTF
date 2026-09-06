package com.howtofish.mod.item;

import com.howtofish.mod.entity.BobberEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * "How To Fish" fishing rod with a real fishing mini-game:
 *
 * 1. Right click          - cast: a bobber flies out and floats on the water.
 * 2. Wait                 - the fish approaches: small nibbles dip the float.
 * 3. Big splash           - the BITE! The float sinks for ~1.5 seconds.
 * 4. Right click in time  - hook the fish: a live fish is pulled out of the
 *                           water and slides towards the player.
 * 5. Right click later    - just reels the empty line back in.
 *
 * The caught fish must be finished off with the {@link KnifeItem} to drop
 * meat - or released by right-clicking it with an empty hand.
 */
public class FishingRodCustomItem extends Item {

    public FishingRodCustomItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            BobberEntity bobber = BobberEntity.getForPlayer(level, player);
            if (bobber != null) {
                // Reel in: catch the fish if a bite is happening right now.
                int durabilityCost = bobber.retrieve(stack, player);
                if (durabilityCost > 0) {
                    stack.hurtAndBreak(durabilityCost, player, p -> p.broadcastBreakEvent(hand));
                }
            } else {
                // Cast the bobber.
                BobberEntity.cast(level, player);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
