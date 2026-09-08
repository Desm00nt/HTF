package com.howtofish.mod.item;

import com.howtofish.mod.entity.BobberEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * 4. Right click in time  - HOOK: the float starts to glide towards the
 *                           player and the fish visibly fights behind it.
 * 5. Right click repeatedly - strong pulls that reel the fish in faster.
 * 6. Reeled in            - the fish lands on the shore: kill it with the
 *                           knife or release it (empty hand right click).
 *
 * The rod also has a BAIT SLOT stored in its NBT ({@code HTFBait}). The
 * player opens the bait menu by pressing "B" with the rod in hand. Two baits
 * exist: the Golden Bait (15 catches - valuable fish bite faster) and BEER,
 * which is not for regular fish at all: with a beer on the line there are no
 * nibbles - one deep plunge - and hooking it drags out the Spider Crab boss.
 */
public class FishingRodCustomItem extends Item {

    public static final String TAG_BAIT = "HTFBait";

    public FishingRodCustomItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // Bait slot (stored in the rod's NBT)
    // ------------------------------------------------------------------

    /** The bait currently inserted into this rod (EMPTY if none). */
    public static ItemStack getBait(ItemStack rod) {
        if (rod.isEmpty() || !rod.hasTag() || !rod.getTag().contains(TAG_BAIT)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(rod.getTag().getCompound(TAG_BAIT));
    }

    public static void setBait(ItemStack rod, ItemStack bait) {
        if (bait.isEmpty()) {
            rod.removeTagKey(TAG_BAIT);
        } else {
            rod.getOrCreateTag().put(TAG_BAIT, bait.save(new CompoundTag()));
        }
    }

    /** Uses up one bait charge: beer vanishes after one catch, golden bait after {@link BaitItem#MAX_USES}. */
    public static void consumeBait(ItemStack rod, Player player) {
        ItemStack bait = getBait(rod);
        if (bait.isEmpty()) return;
        if (bait.getItem() instanceof BeerItem) {
            setBait(rod, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("message.howtofish.beer_used"), true);
            player.level.playSound(null, player.blockPosition(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6f, 1.2f);
        } else if (bait.getItem() instanceof BaitItem) {
            CompoundTag tag = bait.getOrCreateTag();
            int damage = tag.getInt("Damage") + 1;
            if (damage >= BaitItem.MAX_USES) {
                setBait(rod, ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("message.howtofish.bait_broke"), true);
                player.level.playSound(null, player.blockPosition(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
            } else {
                tag.putInt("Damage", damage);
            }
        }
    }

    // ------------------------------------------------------------------
    // Fishing
    // ------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            BobberEntity bobber = BobberEntity.getForPlayer(level, player);
            if (bobber != null) {
                // Reel in: hook during the bite, strong pull while a fish is hooked.
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
        return !getBait(stack).isEmpty();
    }
}
