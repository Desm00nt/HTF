package com.howtofish.mod.entity;

import com.howtofish.mod.economy.PlayerCurrency;
import com.howtofish.mod.economy.PlayerQuestData;
import com.howtofish.mod.item.BeerItem;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The old Lighthouse Keeper. Right-click with an empty hand to talk / open the
 * shop. Right-click while holding fish meat or a boss trophy to feed him:
 * - Fish meat  -&gt; earns the player Rubles, triggers the "eat" animation.
 * - Spider Crab Shell -&gt; unlocks the Radar coordinates for the next island.
 * - Beer       -&gt; he gulps it down (drinking animation) and hands over the
 *                EMPTY CAN - the only thing the Spider Crab answers to. The
 *                can itself stays a passive lure: the player loads it into
 *                the rod via the bait menu; clicking water with it does nothing.
 * While a player stands next to him holding raw fish or a beer his eyes
 * physically bulge out of their sockets (model animation, see OldManModel)
 * and his nose swells.
 */
public class OldManEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> EATING =
            SynchedEntityData.defineId(OldManEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> EAT_TICKS =
            SynchedEntityData.defineId(OldManEntity.class, EntityDataSerializers.INT);
    /** True when a player holding raw fish meat is standing close by. */
    private static final EntityDataAccessor<Boolean> EYES_POPPING =
            SynchedEntityData.defineId(OldManEntity.class, EntityDataSerializers.BOOLEAN);

    /** Client-side smooth 0..1 amount used by the model to bulge the eyes. */
    public float eyePopAmount;

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
        // Sol NEVER wanders off his stool: float safety + watching players.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Head-tracking is client-side cosmetics (see tick() below) - no goal,
        // so NOTHING can ever turn his body away from the sea or walk him off
        // the stool.
    }

    /** Old Sol is a fixture of the island: nothing can hurt or move him. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;   // Sol never despawns
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EATING, false);
        this.entityData.define(EAT_TICKS, 0);
        this.entityData.define(EYES_POPPING, false);
    }

    public boolean isEyesPopping() {
        return this.entityData.get(EYES_POPPING);
    }

    /** Smoothed eye-pop amount for the renderer, including partial ticks. */
    public float getEyePopAmount(float partialTicks) {
        float target = isEyesPopping() ? 1.0f : 0.0f;
        return Mth.clamp(this.eyePopAmount + (target - this.eyePopAmount) * partialTicks, 0.0f, 1.0f);
    }

    private void updateEyePopping() {
        Player nearest = null;
        for (Player player : this.level.players()) {
            if (player.isAlive() && this.distanceToSqr(player) < 5.0 * 5.0
                    && isTempting(player)) {
                nearest = player;
                break;
            }
        }
        boolean pop = nearest != null;
        if (pop != isEyesPopping()) {
            this.entityData.set(EYES_POPPING, pop);
            if (pop) {
                this.level.playSound(null, this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.GLASS_PLACE, SoundSource.NEUTRAL, 0.7f, 1.6f);
            }
        }
    }

    /** Raw fish meat or beer in hands: both make his eyes bulge. */
    private static boolean isTempting(Player player) {
        return isFishOrBeer(player.getMainHandItem()) || isFishOrBeer(player.getOffhandItem());
    }

    private static boolean isFishOrBeer(ItemStack stack) {
        return stack.getItem() instanceof FishMeatItem || stack.getItem() instanceof BeerItem;
    }

    public boolean isEating() {
        return this.entityData.get(EATING);
    }

    public int getEatTicks() {
        return this.entityData.get(EAT_TICKS);
    }

    private void startEating() {
        startEating(20);
    }

    /** Plays the chew animation for {@code duration} ticks (beer is gulped longer). */
    private void startEating(int duration) {
        this.eatDuration = duration;
        this.entityData.set(EATING, true);
        this.entityData.set(EAT_TICKS, 0);
        this.level.playSound(null, this.blockPosition(), ModSounds.OLD_MAN_EAT.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    private int eatDuration = 20;

    @Override
    public void tick() {
        super.tick();
        // Eating countdown runs ONLY server-side (EAT_TICKS/EATING are synced
        // entity data; a client-side countdown would fight the server).
        if (!this.level.isClientSide && isEating()) {
            int ticks = getEatTicks() + 1;
            if (ticks > this.eatDuration) {
                this.entityData.set(EATING, false);
                this.entityData.set(EAT_TICKS, 0);
            } else {
                this.entityData.set(EAT_TICKS, ticks);
            }
        }
        if (!this.level.isClientSide) {
            // Refresh the "player with fish nearby" detection a few times a second.
            if (this.tickCount % 8 == 0) {
                updateEyePopping();
            }
            // Immovable prop: kill any stray horizontal drift (shoves, water).
            if (this.tickCount % 4 == 0
                    && Math.abs(this.getDeltaMovement().x) + Math.abs(this.getDeltaMovement().z) > 0.001) {
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            }
        } else {
            // Smooth animation towards the synced target on the client.
            float target = isEyesPopping() ? 1.0f : 0.0f;
            this.eyePopAmount += (target - this.eyePopAmount) * 0.22f;
            this.eyePopAmount = Mth.clamp(this.eyePopAmount, 0.0f, 1.0f);

            // HEAD-ONLY tracking: every player within 16 blocks gets watched,
            // but only the head turns (yHeadRot; the renderer feeds it into
            // the model as netHeadYaw) - the seated body keeps facing the sea.
            Player nearest = null;
            double best = 16.0 * 16.0;
            for (Player pl : this.level.players()) {
                if (pl.isAlive() && !pl.isSpectator()) {
                    double d = pl.distanceToSqr(this);
                    if (d < best) {
                        best = d;
                        nearest = pl;
                    }
                }
            }
            float want = this.getYRot();
            if (nearest != null) {
                double dx = nearest.getX() - this.getX();
                double dz = nearest.getZ() - this.getZ();
                float full = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
                float delta = Mth.wrapDegrees(full - this.getYRot());
                delta = Mth.clamp(delta, -75.0F, 75.0F);
                want = this.getYRot() + delta;
            }
            this.yHeadRot += Mth.wrapDegrees(want - this.yHeadRot) * 0.12F;
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
            this.level.playSound(null, this.blockPosition(), ModSounds.COIN.get(),
                    SoundSource.PLAYERS, 1.0f, 1.35f);
            player.displayClientMessage(Component.translatable("message.howtofish.fed_fish", reward), true);
            return InteractionResult.SUCCESS;
        }

        if (item instanceof BeerItem) {
            // He gulps the whole bottle down, belches, and returns the empty can:
            // the ONE thing the Spider Crab answers to as a rod bait.
            held.shrink(1);
            startEating(44);
            this.level.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS,
                    1.0f, 0.7f);
            this.level.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_BURP, SoundSource.PLAYERS,
                    0.9f, 0.8f);
            ItemStack can = new ItemStack(ModItems.EMPTY_CAN.get());
            if (!player.getInventory().add(can)) {
                player.drop(can, false);
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
            this.level.playSound(null, this.blockPosition(), ModSounds.OLD_MAN_TALK.get(),
                    SoundSource.NEUTRAL, 1.0f, 0.9f + this.random.nextFloat() * 0.2f);
            player.sendSystemMessage(Component.translatable("message.howtofish.old_man_greeting" + (this.random.nextInt(3))));
            net.minecraft.server.level.ServerPlayer sp = (net.minecraft.server.level.ServerPlayer) player;
            net.minecraftforge.network.NetworkHooks.openScreen(sp,
                    new SimpleMenuProvider((id, inv, p) -> new OldManShopMenu(id, inv), Component.translatable("menu.howtofish.old_man_shop")));
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }
}
