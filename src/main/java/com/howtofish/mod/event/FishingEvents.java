package com.howtofish.mod.event;

import com.howtofish.mod.entity.BossFishEntity;
import com.howtofish.mod.entity.CustomFishEntity;
import com.howtofish.mod.item.KnifeItem;
import com.howtofish.mod.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handles the "catch -&gt; kill -&gt; sell" combat loop: the Knife deals bonus
 * damage to released fish/boss fish, and killing a fish always drops the meat
 * item matching its {@link com.howtofish.mod.entity.FishType}.
 */
public class FishingEvents {

    @SubscribeEvent
    public void onDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof CustomFishEntity) && !(target instanceof BossFishEntity)) return;
        if (!(event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        ItemStack weapon = player.getMainHandItem();
        if (weapon.getItem() instanceof KnifeItem) {
            // Fish die in one clean slice; the boss only gets a modest boost
            // so a knife never trivialises the fight (separate tuning knobs).
            float mult = target instanceof BossFishEntity ? 1.5f : 2.2f;
            event.setAmount(event.getAmount() * mult);
        }
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof CustomFishEntity fish) {
            event.getDrops().clear();
            ItemStack meat = getMeatFor(fish);
            if (!meat.isEmpty()) {
                event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                        fish.level, fish.getX(), fish.getY(), fish.getZ(), meat));
            }
        }
    }

    private ItemStack getMeatFor(CustomFishEntity fish) {
        return switch (fish.getFishType()) {
            case ANCHOVY -> new ItemStack(ModItems.FISH_MEAT_ANCHOVY.get());
            case HERRING -> new ItemStack(ModItems.FISH_MEAT_HERRING.get());
            case CRAB -> new ItemStack(ModItems.FISH_MEAT_CRAB.get());
            case SHRIMP -> new ItemStack(ModItems.FISH_MEAT_SHRIMP.get());
            case LOBSTER -> new ItemStack(ModItems.FISH_MEAT_LOBSTER.get());
            case PUFFERFISH -> new ItemStack(ModItems.FISH_MEAT_PUFFERFISH.get());
        };
    }
}
