package com.howtofish.mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.WaterAnimal;
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
    public int getMaxAirSupply() {
        return 6000;
    }
}
