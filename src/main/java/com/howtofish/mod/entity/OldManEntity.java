package com.howtofish.mod.entity;

import com.howtofish.mod.economy.PlayerCurrency;
import com.howtofish.mod.economy.PlayerQuestData;
import com.howtofish.mod.item.BeerItem;
import com.howtofish.mod.item.EmptyCanItem;
import com.howtofish.mod.item.FishMeatItem;
import com.howtofish.mod.menu.OldManShopMenu;
import com.howtofish.mod.registry.ModItems;
import com.howtofish.mod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The old Lighthouse Keeper. Right-click with an empty hand to talk / open the
 * shop. Right-click while holding fish meat, beer or a boss trophy to feed him:
 * - Fish meat -&gt; earns the player Rubles, triggers the "eat" animation.
 * - Beer -&gt; he drinks it (eat animation) and returns an Empty Can (boss bait).
 * - Spider Crab Shell -&gt; unlocks the Radar coordinates for the next island.
 */
public class OldManEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> EATING =
            SynchedEntityData.defineId(OldManEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> EAT_TICKS =
            SynchedEntityData.defineId(OldManEntity.class, EntityDataSerializers.INT);

    public OldManEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.3d)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0d);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7d));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EATING, false);
        this.entityData.define(EAT_TICKS, 0);
    }

    public boolean isEating() {
        return this.entityData.get(EATING);
    }

    public int getEatTicks() {
        return this.entityData.get(EAT_TICKS);
    }

    private void startEating() {
        this.entityData.set(EATING, true);
        this.entityData.set(EAT_TICKS, 0);
        this.level.playSound(null, this.blockPosition(), ModSounds.OLD_MAN_EAT.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (isEating()) {
            int ticks = getEatTicks() + 1;
            if (ticks > 20) {
                this.entityData.set(EATING, false);
                this.entityData.set(EAT_TICKS, 0);
            } else {
                this.entityData.set(EAT_TICKS, ticks);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getItemInHand(hand);
        Item item = held.getItem();

        if (item instanceof FishMeatItem meat) {
            int reward = meat.getFishType().getPrice();
            PlayerCurrency.add(player, reward);
            startEating();
            held.shrink(1);
            player.displayClientMessage(Component.translatable("message.howtofish.fed_fish", reward), true);
            return InteractionResult.SUCCESS;
        }

        if (item instanceof BeerItem) {
            startEating();
            held.shrink(1);
            if (!player.getInventory().add(new ItemStack(ModItems.EMPTY_CAN.get()))) {
                player.drop(new ItemStack(ModItems.EMPTY_CAN.get()), false);
            }
            player.displayClientMessage(Component.translatable("message.howtofish.gave_beer"), true);
            return InteractionResult.SUCCESS;
        }

        if (item == ModItems.SPIDER_CRAB_SHELL.get()) {
            startEating();
            held.shrink(1);
            PlayerQuestData.unlockNextIsland(player);
            var coords = com.howtofish.mod.world.IslandBuilder.SECOND_ISLAND_ORIGIN;
            player.sendSystemMessage(Component.translatable("message.howtofish.trophy_turned_in", coords.getX(), coords.getY(), coords.getZ()));
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            player.sendSystemMessage(Component.translatable("message.howtofish.old_man_greeting" + (this.random.nextInt(3))));
            net.minecraft.server.level.ServerPlayer sp = (net.minecraft.server.level.ServerPlayer) player;
            net.minecraftforge.network.NetworkHooks.openScreen(sp,
                    new SimpleMenuProvider((id, inv, p) -> new OldManShopMenu(id, inv), Component.translatable("menu.howtofish.old_man_shop")));
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }
}
