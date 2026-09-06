package com.howtofish.mod.entity;

import com.howtofish.mod.registry.ModEntities;
import com.howtofish.mod.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom fishing float (bobber) implementing the "How To Fish" mini-game.
 *
 * States: FLYING -> BOBBING -> (NIBBLE -> BOBBING)* -> BITE -> caught/missed.
 * The player must retrieve during the BITE window (~1.5 s) to hook the fish;
 * otherwise the bobber returns to idle bobbing and the cycle repeats.
 * The live {@link CustomFishEntity} is spawned at the float and given a pull
 * velocity towards the player, so it visibly slides through the water.
 */
public class BobberEntity extends Projectile {

    public static final int STATE_FLYING = 0;
    public static final int STATE_BOBBING = 1;
    public static final int STATE_NIBBLE = 2;
    public static final int STATE_BITE = 3;
    public static final int STATE_GROUNDED = 4;

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.INT);

    /** Server-side registry: player UUID -> active bobber entity id. */
    private static final Map<UUID, Integer> ACTIVE_BOBBERS = new HashMap<>();

    private int biteCooldown = 100;
    private int nibblesLeft = 0;
    private int nibblePhaseTicks = 0;
    private int biteTicks = 0;
    private int groundTicks = 0;
    private int noOwnerTicks = 0;
    private int life = 0;

    public BobberEntity(EntityType<? extends BobberEntity> type, Level level) {
        super(type, level);
    }

    public BobberEntity(Player owner, Level level) {
        super(ModEntities.BOBBER.get(), level);
        this.setOwner(owner);
        this.moveTo(owner.getX(), owner.getEyeY() - 0.15, owner.getZ(), owner.getYRot(), owner.getXRot());
        this.setDeltaMovement(castVelocity(owner));
    }

    private static Vec3 castVelocity(Player owner) {
        float xRot = owner.getXRot() * ((float) Math.PI / 180F);
        float yRot = owner.getYRot() * ((float) Math.PI / 180F);
        double power = 0.9;
        double vx = -Mth.sin(yRot) * Mth.cos(xRot) * power;
        double vy = -Mth.sin(xRot) * power + 0.12;
        double vz = Mth.cos(yRot) * Mth.cos(xRot) * power;
        return new Vec3(vx, vy, vz);
    }

    public static void cast(Level level, Player player) {
        BobberEntity bobber = new BobberEntity(player, level);
        level.addFreshEntity(bobber);
        ACTIVE_BOBBERS.put(player.getUUID(), bobber.getId());
        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW,
                SoundSource.PLAYERS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    public static BobberEntity getForPlayer(Level level, Player player) {
        Integer id = ACTIVE_BOBBERS.get(player.getUUID());
        if (id == null) return null;
        Entity e = level.getEntity(id);
        if (e instanceof BobberEntity bobber && bobber.isAlive() && bobber.getOwner() == player) {
            return bobber;
        }
        ACTIVE_BOBBERS.remove(player.getUUID());
        return null;
    }

    private void deregister() {
        Entity owner = this.getOwner();
        if (owner != null) {
            ACTIVE_BOBBERS.remove(owner.getUUID(), this.getId());
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_STATE, STATE_FLYING);
    }

    public int getState() {
        return this.entityData.get(DATA_STATE);
    }

    private void setState(int state) {
        this.entityData.set(DATA_STATE, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            serverTick();
        }
    }

    private void serverTick() {
        this.life++;
        Entity owner = this.getOwner();
        if (!(owner instanceof Player) || !owner.isAlive()) {
            if (++this.noOwnerTicks > 100) {
                this.discardAndClean();
            }
            return;
        }
        this.noOwnerTicks = 0;
        if (this.life > 12000 || owner.distanceTo(this) > 48) {
            this.discardAndClean();
            return;
        }

        switch (getState()) {
            case STATE_FLYING -> {
                Vec3 motion = this.getDeltaMovement();
                this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
                this.setDeltaMovement(motion.scale(0.96).add(0, -0.045, 0));
                FluidState fluid = this.level.getFluidState(this.blockPosition());
                if (fluid.is(FluidTags.WATER)) {
                    this.setDeltaMovement(Vec3.ZERO);
                    this.setState(STATE_BOBBING);
                    this.biteCooldown = 80 + this.random.nextInt(120);
                } else if (this.onGround || !this.level.noCollision(this, this.getBoundingBox())) {
                    this.setState(STATE_GROUNDED);
                    this.playSplash(0.4f, 1.4f);
                }
            }
            case STATE_BOBBING -> {
                if (!this.level.getFluidState(this.blockPosition()).is(FluidTags.WATER)) {
                    this.setState(STATE_GROUNDED);
                    return;
                }
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(0, 0.02, 0));
                if (--this.biteCooldown <= 0) {
                    this.nibblesLeft = 1 + this.random.nextInt(3);
                    this.nibblePhaseTicks = 0;
                    this.setState(STATE_NIBBLE);
                }
            }
            case STATE_NIBBLE -> {
                this.nibblePhaseTicks++;
                if (this.nibblePhaseTicks % 10 == 0) {
                    this.nibblesLeft--;
                    if (this.nibblesLeft <= 0) {
                        this.biteTicks = 25 + this.random.nextInt(11);
                        this.setState(STATE_BITE);
                        this.playSplash(0.9f, 0.9f);
                        if (this.level instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.25, this.getZ(),
                                    10, 0.2, 0.05, 0.2, 0.1);
                        }
                    } else {
                        this.biteCooldown = 15 + this.random.nextInt(25);
                        this.setState(STATE_BOBBING);
                    }
                }
            }
            case STATE_BITE -> {
                if (--this.biteTicks <= 0) {
                    this.biteCooldown = 80 + this.random.nextInt(140);
                    this.setState(STATE_BOBBING);
                }
            }
            default -> { // STATE_GROUNDED
                this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
                if (++this.groundTicks > 60) {
                    this.discardAndClean();
                }
            }
        }
    }

    /**
     * Reel the line in. Returns the rod durability cost:
     * 1 when a fish was hooked, 0 for an empty retrieve.
     */
    public int retrieve(ItemStack rodStack, Player player) {
        boolean biteNow = getState() == STATE_BITE && this.biteTicks > 0;
        if (biteNow) {
            this.spawnCaughtFish(player);
        }
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                biteNow ? SoundEvents.FISHING_BOBBER_SPLASH : SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.PLAYERS, 0.8f, biteNow ? 0.8f : 1.0f);
        this.discardAndClean();
        return biteNow ? 1 : 0;
    }

    private void spawnCaughtFish(Player player) {
        FishType[] types = FishType.values();
        FishType type = types[this.random.nextInt(types.length)];
        CustomFishEntity fish = new CustomFishEntity(ModEntities.CUSTOM_FISH.get(), this.level);
        fish.setFishType(type);
        fish.setPos(this.getX(), this.getY(), this.getZ());
        Vec3 pull = player.position().add(0, 0.3, 0).subtract(this.position());
        pull = pull.lengthSqr() > 1.0e-3 ? pull.normalize().scale(0.42) : new Vec3(0.2, 0.2, 0.2);
        this.level.addFreshEntity(fish);
        fish.setDeltaMovement(pull.x, Math.max(0.18, pull.y), pull.z);
        fish.hurtMarked = true;
        if (this.level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.25, this.getZ(),
                    15, 0.25, 0.1, 0.25, 0.15);
        }
        this.level.playSound(null, fish.blockPosition(), ModSounds.FISH_FLOP.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void playSplash(float volume, float pitch) {
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, volume, pitch);
    }

    private void discardAndClean() {
        this.deregister();
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level.isClientSide) {
            this.deregister();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BobberState", getState());
        tag.putInt("BiteCooldown", this.biteCooldown);
        tag.putInt("NibblesLeft", this.nibblesLeft);
        tag.putInt("BiteTicks", this.biteTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_STATE, tag.getInt("BobberState"));
        this.biteCooldown = tag.getInt("BiteCooldown");
        this.nibblesLeft = tag.getInt("NibblesLeft");
        this.biteTicks = tag.getInt("BiteTicks");
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
