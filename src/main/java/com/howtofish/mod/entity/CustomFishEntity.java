package com.howtofish.mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/**
 * A live fish/crustacean that was released from the fishing rod. It flops
 * around near the water and must be finished off with the Knife - it does not
 * despawn quickly so the player has time to grab a weapon.
 */
public class CustomFishEntity extends WaterAnimal {

    private static final EntityDataAccessor<String> FISH_TYPE =
            SynchedEntityData.defineId(CustomFishEntity.class, EntityDataSerializers.STRING);

    public CustomFishEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new net.minecraft.world.entity.ai.control.MoveControl(this);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.7d);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.5d));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0d));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FISH_TYPE, FishType.ANCHOVY.getId());
    }

    public void setFishType(FishType type) {
        this.entityData.set(FISH_TYPE, type.getId());
        this.setHealth((float) type.getHealth());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.getHealth());
    }

    public FishType getFishType() {
        String id = this.entityData.get(FISH_TYPE);
        for (FishType t : FishType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return FishType.ANCHOVY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("FishType", getFishType().getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FishType")) {
            for (FishType t : FishType.values()) {
                if (t.getId().equals(tag.getString("FishType"))) {
                    setFishType(t);
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Out of water the fish flops around, just like vanilla fish.
        if (!this.level.isClientSide && !this.isInWater() && this.onGround) {
            if (this.random.nextInt(18) == 0) {
                this.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.3,
                        0.35 + this.random.nextDouble() * 0.1,
                        (this.random.nextDouble() - 0.5) * 0.3);
                this.hasImpulse = true;
                if (this.random.nextInt(3) == 0) {
                    this.level.playSound(null, this.blockPosition(), SoundEvents.COD_FLOP,
                            SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
            }
        }
    }

    /**
     * Catch \u0026 release: right-click the landed fish with an empty hand to let it go -
     * it escapes with a splash and no meat is dropped.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).isEmpty()) {
            if (!this.level.isClientSide) {
                if (this.level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            this.getX(), this.getY() + 0.4, this.getZ(), 12, 0.3, 0.15, 0.3, 0.1);
                }
                this.level.playSound(null, this.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH,
                        SoundSource.NEUTRAL, 1.0f, 1.0f);
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        // Released fish swim away eventually, unless the player named one.
        return !this.hasCustomName();
    }

    @Override
    public int getMaxAirSupply() {
        return 6000;
    }
}
