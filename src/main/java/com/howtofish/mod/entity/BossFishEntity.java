package com.howtofish.mod.entity;

import com.howtofish.mod.registry.ModItems;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.EntityDimensions;

import javax.annotation.Nullable;

/**
 * The "Spider Crab" style boss: summoned with an Empty Beer Can, much tankier
 * and hits back. On death it drops the {@code spider_crab_shell} trophy that
 * must be handed to the Old Man for the coordinates of the next island.
 */
public class BossFishEntity extends Monster {

    public BossFishEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0d)
                .add(Attributes.ATTACK_DAMAGE, 6.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.32d)
                .add(Attributes.ARMOR, 4.0d);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15d, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0d));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!level.isClientSide) {
            this.spawnAtLocation(new ItemStack(ModItems.SPIDER_CRAB_SHELL.get()));
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }
}
