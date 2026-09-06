package com.howtofish.mod.entity;

import com.howtofish.mod.network.BossMusicStopPacket;
import com.howtofish.mod.network.ModNetwork;
import com.howtofish.mod.registry.ModItems;
import com.howtofish.mod.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The "Spider Crab" BOSS - a real crab now, not an oversized fish.
 *
 * Behaviour:
 * - climbs out of the water onto the island and constantly chases the player,
 * - periodically LEAPS at the player (aggressive jumps),
 * - melee swipes knock the player back, after every swipe the crab FREEZES
 *   for ~2 seconds - the window to hit it back,
 * - special attack: the crab freezes & shakes, a RED CIRCLE appears on the
 *   ground and follows the player for ~3.5 seconds, then the crab JUMPS onto
 *   that spot dealing heavy area damage,
 * - an epic royalty-free boss track starts on summon and stops on death.
 *
 * Summoned by throwing an Empty Beer Can into the water.
 */
public class BossFishEntity extends Monster {

    public static final int STATE_CHASE = 0;
    public static final int STATE_STUNNED = 1;
    public static final int STATE_TELEGRAPH = 2;
    public static final int STATE_LEAPING = 3;

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(BossFishEntity.class, EntityDataSerializers.INT);
    /** Remaining telegraph ticks - drives the red circle animation client-side. */
    private static final EntityDataAccessor<Integer> DATA_TELE_TICKS =
            SynchedEntityData.defineId(BossFishEntity.class, EntityDataSerializers.INT);

    public static final int TELEGRAPH_TIME = 70;
    private static final int STUN_TIME = 45;
    private static final int BIG_STUN_TIME = 70;

    private int stateTimer = 0;
    private int jumpCooldown = 40;
    private int attackCooldown = 0;
    private int specialCooldown = 160;
    private int waterTicks = 0;

    public BossFishEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0d)
                .add(Attributes.ATTACK_DAMAGE, 7.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.30d)
                .add(Attributes.ARMOR, 6.0d)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8d)
                .add(Attributes.FOLLOW_RANGE, 40.0d);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STATE, STATE_CHASE);
        this.entityData.define(DATA_TELE_TICKS, 0);
    }

    public int getBossState() {
        return this.entityData.get(DATA_STATE);
    }

    private void setBossState(int state) {
        this.entityData.set(DATA_STATE, state);
    }

    public int getTelegraphTicks() {
        return this.entityData.get(DATA_TELE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            return;
        }

        LivingEntity target = getTarget();
        int state = getBossState();
        if (attackCooldown > 0) attackCooldown--;

        switch (state) {
            case STATE_CHASE -> tickChase(target);
            case STATE_STUNNED -> tickStunned();
            case STATE_TELEGRAPH -> tickTelegraph(target);
            case STATE_LEAPING -> tickLeaping(target);
        }

        // Swim upwards in water so it can climb back onto the island.
        if (isInWater()) {
            this.waterTicks++;
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.06, 0));
            this.hurtMarked = true;
        } else {
            this.waterTicks = 0;
        }
    }

    private void tickChase(@Nullable LivingEntity target) {
        this.entityData.set(DATA_TELE_TICKS, 0);
        if (target == null || !target.isAlive()) {
            getNavigation().stop();
            return;
        }
        double dist = this.distanceTo(target);

        // Pathfind towards the player.
        if (this.onGround) {
            getNavigation().moveTo(target, 1.25d);
        }

        // Aggressive jump towards the player.
        if (this.onGround && --this.jumpCooldown <= 0 && dist < 14.0) {
            Vec3 dir = target.position().subtract(this.position());
            Vec3 horiz = new Vec3(dir.x, 0, dir.z);
            if (horiz.lengthSqr() > 0.01) {
                horiz = horiz.normalize();
                this.setDeltaMovement(horiz.scale(0.55).add(0, 0.58, 0));
                this.hurtMarked = true;
                this.level.broadcastEntityEvent(this, (byte) 4); // leg animation kick
                playBossSound(SoundEvents.SPIDER_AMBIENT, 0.9f, 0.7f);
            }
            this.jumpCooldown = 30 + this.random.nextInt(30);
        }

        // Melee swipe -> then freeze.
        if (dist < 2.9 && attackCooldown <= 0) {
            doSwipe(target);
            enterStun(STUN_TIME);
            return;
        }

        // Special attack setup.
        if (--this.specialCooldown <= 0 && dist < 16.0 && this.onGround) {
            enterTelegraph();
        }
    }

    private void doSwipe(LivingEntity target) {
        double dmg = this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.hurt(DamageSource.mobAttack(this), (float) dmg);
        Vec3 kb = target.position().subtract(this.position()).normalize();
        target.push(kb.x * 0.9, 0.45, kb.z * 0.9);
        target.hurtMarked = true;
        playBossSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);
    }

    private void enterStun(int time) {
        setBossState(STATE_STUNNED);
        this.stateTimer = time;
        getNavigation().stop();
    }

    private void tickStunned() {
        // Frozen & vulnerable: no movement, no attacks.
        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        if (--this.stateTimer <= 0) {
            setBossState(STATE_CHASE);
            this.jumpCooldown = 10;
        }
    }

    private void enterTelegraph() {
        setBossState(STATE_TELEGRAPH);
        this.stateTimer = TELEGRAPH_TIME;
        this.entityData.set(DATA_TELE_TICKS, TELEGRAPH_TIME);
        getNavigation().stop();
        playBossSound(SoundEvents.SPIDER_AMBIENT, 1.6f, 0.55f);
    }

    private void tickTelegraph(@Nullable LivingEntity target) {
        // Frozen but SHAKING (client shakes the model); the red circle follows
        // the player until the very last moment.
        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.3));
        this.entityData.set(DATA_TELE_TICKS, Math.max(0, this.stateTimer));

        if (--this.stateTimer <= 0 || target == null || !target.isAlive()) {
            startLeap(target);
        }
    }

    private void startLeap(@Nullable LivingEntity target) {
        setBossState(STATE_LEAPING);
        this.stateTimer = 35;
        Vec3 landing = target != null ? target.position() : this.position();
        int flight = 16;
        double dx = landing.x - this.getX();
        double dy = landing.y - this.getY() + 0.5;
        double dz = landing.z - this.getZ();
        double vy = dy / flight + 0.04 * flight;
        this.setDeltaMovement(new Vec3(dx / flight * 1.2, vy, dz / flight * 1.2));
        this.hurtMarked = true;
        playBossSound(ModSounds.BOSS_ROAR.get(), 1.4f, 1.0f);
    }

    private void tickLeaping(@Nullable LivingEntity target) {
        // Airborne - let physics do the work, then slam down.
        if (this.onGround || --this.stateTimer <= 0) {
            landSlam();
        }
    }

    private void landSlam() {
        // Area damage around the landing spot.
        for (Entity e : this.level.getEntities(this, this.getBoundingBox().inflate(3.0),
                ent -> ent instanceof LivingEntity && ent != this && ent.isAlive())) {
            LivingEntity living = (LivingEntity) e;
            living.hurt(DamageSource.mobAttack(this), 9.0f);
            Vec3 kb = living.position().subtract(this.position()).normalize();
            living.push(kb.x * 1.4, 0.65, kb.z * 1.4);
            living.hurtMarked = true;
        }
        this.level.broadcastEntityEvent(this, (byte) 62); // custom client event: land FX
        if (this.level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.4, this.getZ(), 3, 0.2, 0.1, 0.2, 0);
            sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 25, 2.2, 0.3, 2.2, 0.05);
        }
        playBossSound(SoundEvents.GENERIC_EXPLODE, 1.4f, 0.75f);
        enterStun(BIG_STUN_TIME);
        this.specialCooldown = 300 + this.random.nextInt(200);
    }

    private void playBossSound(net.minecraft.sounds.SoundEvent sound, float vol, float pitch) {
        this.level.playSound(null, this.blockPosition(), sound, SoundSource.HOSTILE, vol, pitch);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 62 && this.level.isClientSide) {
            // Landing FX handled client-side via particles in server tick as well.
        }
        super.handleEntityEvent(id);
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!level.isClientSide) {
            this.spawnAtLocation(new ItemStack(ModItems.SPIDER_CRAB_SHELL.get()));
            // Stop the boss music for everyone nearby.
            if (level instanceof ServerLevel sl) {
                ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.DIMENSION.with(() -> sl.dimension()),
                        new BossMusicStopPacket());
            }
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public Component getName() {
        return Component.translatable("entity.howtofish.boss_fish");
    }
}
