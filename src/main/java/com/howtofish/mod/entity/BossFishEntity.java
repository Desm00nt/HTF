package com.howtofish.mod.entity;

import com.howtofish.mod.network.BossMusicStopPacket;
import com.howtofish.mod.network.ModNetwork;
import com.howtofish.mod.registry.ModItems;
import com.howtofish.mod.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import com.mojang.math.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The "Spider Crab" BOSS - a real crab now, not an oversized fish.
 *
 * Behaviour:
 * - climbs out of the water onto the island and chases the player MANIACALLY
 *   (fast, weaving scuttle; no ground-navigation pathfinding needed),
 * - leaps constantly - lunges on land and splash-hops through the shallows,
 * - melee swipes knock the player back; after every swipe the crab FREEZES
 *   for a moment - the window to hit it back,
 * - special attack: the crab crouches & shakes, a growing RED CIRCLE trails
 *   the target for ~3.5 seconds, then the crab JUMPS onto that spot dealing
 *   heavy area damage and stuns itself on landing,
 * - an epic royalty-free boss track starts on summon and stops on death.
 *
 * Summoned by catching it: put a BEER into the rod's bait slot (press B) and
 * cast into the sea - the bite is the boss.
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
    private static final int STUN_TIME = 32;
    private static final int BIG_STUN_TIME = 55;

    private int stateTimer = 0;
    private int jumpCooldown = 20;
    private int specialCooldown = 90;
    private int waterTicks = 0;
    /** Server-side boss health bar (vanilla renders it at the top of the screen). */
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.howtofish.boss_fish"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    public BossFishEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        this.setPersistenceRequired();
        // Crabs climb: generous step height so it can scramble onto the shore.
        this.maxUpStep = 1.15f;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0d)
                .add(Attributes.ATTACK_DAMAGE, 7.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.34d)
                .add(Attributes.ARMOR, 6.0d)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8d)
                .add(Attributes.FOLLOW_RANGE, 56.0d);
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
        // Keep the boss bar in sync with the health.
        this.bossBar.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0f, 1.0f));
        if (this.level.isClientSide) {
            return;
        }

        LivingEntity target = getTarget();
        int state = getBossState();

        switch (state) {
            case STATE_CHASE -> tickChase(target);
            case STATE_STUNNED -> tickStunned();
            case STATE_TELEGRAPH -> tickTelegraph(target);
            case STATE_LEAPING -> tickLeaping(target);
        }
    }

    /**
     * Manual crab locomotion. Ground navigation cannot path a monster out of
     * the sea onto an island, which left the boss frozen in the water - so the
     * crab now scuttles with direct velocity: paddles while swimming, walks on
     * land, and JUMPS whenever it bumps into an obstacle (climbing the shore).
     */
    private void moveTowards(LivingEntity target, double speed) {
        Vec3 diff = target.position().subtract(this.position());
        double horiz = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (horiz < 0.05) return;
        Vec3 dir = new Vec3(diff.x / horiz, 0, diff.z / horiz);

        double yPull = 0.0;
        if (isInWater()) {
            // Paddle up to the surface while far below it.
            if (this.getY() < target.getY() - 0.5) yPull = 0.05;
            else yPull = 0.015;
        }
        // Erratic sideways weave while closing in - hard to kite, fun to fight.
        Vec3 perp = new Vec3(-dir.z, 0, dir.x);
        double zig = Math.sin(this.tickCount * 0.16 + this.getId()) * 0.35 * Math.min(1.0, horiz / 8.0);
        this.setDeltaMovement(this.getDeltaMovement().scale(0.6)
                .add(dir.scale(speed)).add(perp.scale(zig * speed)).add(0, yPull, 0));
        this.hasImpulse = true;
        this.hurtMarked = true;

        // Face the target.
        float targetYaw = (float) (Mth.atan2(diff.z, diff.x) * (180F / Math.PI)) - 90.0f;
        this.setYRot(this.yRotO + Mth.wrapDegrees(targetYaw - this.yRotO) * 0.4f);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();

        // Jump over obstacles (and onto the shore).
        if (this.onGround && this.horizontalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.42, 0).add(dir.scale(0.12)));
            this.hurtMarked = true;
        } else if (isInWater() && this.horizontalCollision) {
            // Climbing out of the water onto the beach.
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.35, 0));
            this.hurtMarked = true;
        }
    }

    private void tickChase(@Nullable LivingEntity target) {
        this.entityData.set(DATA_TELE_TICKS, 0);
        if (target == null || !target.isAlive()) {
            getNavigation().stop();
            return;
        }
        double dist = this.distanceTo(target);

        // FRENZIED scuttle (manual locomotion - navigation often cannot path
        // out of water, which froze the old boss in place). Faster from far
        // away so it never stops coming at you.
        double speed = dist > 10.0 ? 0.30 : 0.22;
        moveTowards(target, speed);

        // It jumps like a maniac - lunges on land, splash-hops through the shallows.
        this.jumpCooldown--;
        if (this.jumpCooldown <= 0 && dist > 2.6 && dist < 24.0) {
            if (!this.isInWater() && this.onGround) {
                Vec3 dir = target.position().subtract(this.position());
                Vec3 horiz = new Vec3(dir.x, 0, dir.z);
                if (horiz.lengthSqr() > 0.01) {
                    horiz = horiz.normalize();
                    float power = 0.62f + (float) Math.min(dist, 16.0) * 0.015f;
                    this.setDeltaMovement(horiz.scale(power).add(0, 0.62, 0));
                    this.hurtMarked = true;
                    playBossSound(SoundEvents.SPIDER_AMBIENT, 0.9f, 0.7f);
                }
                this.jumpCooldown = 10 + this.random.nextInt(14);
            } else if (this.isInWater()) {
                Vec3 dir = target.position().subtract(this.position());
                Vec3 horiz = new Vec3(dir.x, 0, dir.z);
                if (horiz.lengthSqr() > 0.01) {
                    horiz = horiz.normalize();
                    this.setDeltaMovement(horiz.scale(0.45).add(0, 0.38, 0));
                    this.hurtMarked = true;
                }
                this.jumpCooldown = 16 + this.random.nextInt(12);
                if (this.level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.5, this.getZ(),
                            10, 0.5, 0.2, 0.5, 0.1);
                }
            } else {
                this.jumpCooldown = 4;
            }
        }

        // Melee swipe -> then freeze (punish window, shorter than before).
        if (dist < 3.1) {
            doSwipe(target);
            enterStun(STUN_TIME);
            return;
        }

        // Special attack setup.
        if (--this.specialCooldown <= 0 && dist < 18.0 && this.onGround) {
            enterTelegraph();
        } else if (this.specialCooldown < 40 && dist >= 18.0) {
            this.specialCooldown = 60 + this.random.nextInt(40);
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
            this.jumpCooldown = 6;
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

        // Server-side red circle particles as well (reliable for all clients):
        // a glowing ring of red dust around the target, pulsing each few ticks.
        if (this.tickCount % 2 == 0 && target != null && target.isAlive()) {
            double px = target.getX();
            double pz = target.getZ();
            double py = target.getY() + 0.1;
            float progress = 1.0f - this.stateTimer / (float) TELEGRAPH_TIME;
            double radius = 0.6 + 2.2 * progress;
            if (this.level instanceof ServerLevel sl) {
                DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 1.4f);
                for (int i = 0; i < 22; i++) {
                    double angle = Math.PI * 2 * i / 22.0 + this.tickCount * 0.08;
                    sl.sendParticles(red,
                            px + Math.cos(angle) * radius, py, pz + Math.sin(angle) * radius,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }

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
        if (this.level instanceof ServerLevel sl && this.tickCount % 2 == 0) {
            sl.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.5, this.getZ(),
                    2, 0.2, 0.1, 0.2, 0.01);
        }
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
        this.specialCooldown = 170 + this.random.nextInt(140);
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
        this.bossBar.setProgress(0.0f);
        this.bossBar.removeAllPlayers();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level.isClientSide) {
            this.bossBar.removeAllPlayers();
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (this.bossBar != null && name != null) {
            this.bossBar.setName(name);
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
