package com.howtofish.mod.entity;

import com.howtofish.mod.item.BaitKind;
import com.howtofish.mod.registry.ModEntities;
import com.howtofish.mod.registry.ModSounds;
import net.minecraft.core.BlockPos;
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
 * States: FLYING -> BOBBING -> (NIBBLE -> BOBBING)* -> BITE -> HOOKED.
 * During the BITE window (~1.5 s) the player must use the rod again to hook
 * the fish. A hooked fish is then VISIBLY pulled out of the water: the float
 * glides towards the player and the live fish follows right behind it, so the
 * whole "fight" is visible. Once the float reaches the player the fish is set
 * down on the shore where it can be finished with the knife or released with
 * an empty hand.
 * <p>
 * BEER-CAN BAIT (BaitKind.CAN): with an empty can loaded there are no nibbles
 * at all - after a long pause the float plunges HARD once. Hooking THAT bite
 * summons the Spider Crab boss out of the deep.
 */
public class BobberEntity extends Projectile {

    public static final int STATE_FLYING = 0;
    public static final int STATE_BOBBING = 1;
    public static final int STATE_NIBBLE = 2;
    public static final int STATE_BITE = 3;
    public static final int STATE_GROUNDED = 4;
    public static final int STATE_HOOKED = 5;

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.INT);
    /** Owner entity id, synced so the CLIENT can render the line from the rod. */
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.INT);
    /** Hooked fish entity id, synced so the client renders a second line float->fish. */
    private static final EntityDataAccessor<Integer> DATA_FISH_ID =
            SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.INT);
    /** Distance the fish was hooked at, used by the HUD progress bar. */
    private static final EntityDataAccessor<Float> DATA_HOOK_DISTANCE =
            SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.FLOAT);
    /** True while the hooked fish makes an escape dash - the HUD flashes "CLICK!". */
    private static final EntityDataAccessor<Boolean> DATA_STRUGGLE =
            SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.BOOLEAN);

    /** Server-side registry: player UUID -> active bobber entity id. */
    private static final Map<UUID, Integer> ACTIVE_BOBBERS = new HashMap<>();

    private int biteCooldown = 100;
    private int nibblesLeft = 0;
    private int nibblePhaseTicks = 0;
    private int biteTicks = 0;
    private int groundTicks = 0;
    private int noOwnerTicks = 0;
    private int life = 0;

    /** Server: uuid of the fish currently hooked. */
    private UUID hookedFishUuid;
    /** Ticks since the fish was hooked - drives the dash rhythm. */
    private int hookedTicks = 0;
    /** Extra reeling speed applied while > 0 (re-filling by pressing use again). */
    /** Round 7: BLOCKS of line still owed to the player by queued reel
        strokes. Every LMB click deposits PULL_PER_STROKE and it is always
        paid out in full - progress can never be rolled back. */
    private double reelCredit = 0.0;
    /** Guaranteed shortening of the line per LMB stroke, blocks. */
    public static final double PULL_PER_STROKE = 1.4;
    /** RMB within this horizontal distance LANDS the fish; beyond it RMB is
        a deliberate LET-GO. Those are the only two endings - the line never
        snaps and the fight never decides anything on its own. */
    public static final double LAND_DISTANCE = 3.4;
    private int lastReelStroke = -100;
    private int struggleTimer = 0;

    public BobberEntity(EntityType<? extends BobberEntity> type, Level level) {
        super(type, level);
    }

    public BobberEntity(Player owner, Level level) {
        super(ModEntities.BOBBER.get(), level);
        this.setOwner(owner);
        // Sync the owner id so the CLIENT renderer can draw the line from the rod.
        this.entityData.set(DATA_OWNER_ID, owner.getId());
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
        this.entityData.define(DATA_OWNER_ID, 0);
        this.entityData.define(DATA_FISH_ID, -1);
        this.entityData.define(DATA_HOOK_DISTANCE, 0.0f);
        this.entityData.define(DATA_STRUGGLE, false);
    }

    /** Client: is the fish dashing right now (HUD flashing cue)? */
    public boolean isFishStruggling() {
        return this.entityData.get(DATA_STRUGGLE);
    }

    public int getState() {
        return this.entityData.get(DATA_STATE);
    }

    private void setState(int state) {
        this.entityData.set(DATA_STATE, state);
    }

    /** Client-side owner resolution: vanilla Projectile#getOwner returns null on the client. */
    public Entity getSyncedOwner() {
        if (this.level.isClientSide) {
            int id = this.entityData.get(DATA_OWNER_ID);
            return id != 0 ? this.level.getEntity(id) : null;
        }
        return this.getOwner();
    }

    /** Client-side: the fish entity currently hooked to this float (may be null). */
    public CustomFishEntity getSyncedFish() {
        int id = this.entityData.get(DATA_FISH_ID);
        if (id <= 0) return null;
        return this.level.getEntity(id) instanceof CustomFishEntity fish ? fish : null;
    }

    public float getHookDistance() {
        return this.entityData.get(DATA_HOOK_DISTANCE);
    }

    /** The bait kind loaded in the player's held rod - the only lever fish/boss behavior uses. */
    public static BaitKind baitKind(Player player) {
        return BaitKind.ofHeldRod(player);
    }

    /** Server: consume one use of the bait in the held rod (can = 1 use, golden = 15). */
    private static void consumeBait(Player player) {
        for (ItemStack s : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (s.getItem() instanceof com.howtofish.mod.item.FishingRodCustomItem) {
                com.howtofish.mod.item.FishingRodCustomItem.consumeBait(s, player);
                return;
            }
        }
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
        if (!(owner instanceof Player player) || !owner.isAlive()) {
            if (++this.noOwnerTicks > 100) {
                this.discardAndClean();
            }
            return;
        }
        this.noOwnerTicks = 0;
        if (this.life > 12000 || owner.distanceTo(this) > 64) {
            this.discardAndClean();
            return;
        }

        switch (getState()) {
            case STATE_FLYING -> tickFlying();
            case STATE_BOBBING -> tickBobbing(player);
            case STATE_NIBBLE -> tickNibble(player);
            case STATE_BITE -> tickBite();
            case STATE_HOOKED -> tickHooked(player);
            default -> tickGrounded();
        }
    }

    private void tickFlying() {
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        this.setDeltaMovement(motion.scale(0.96).add(0, -0.045, 0));
        FluidState fluid = this.level.getFluidState(this.blockPosition());
        if (fluid.is(FluidTags.WATER)) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setState(STATE_BOBBING);
            Player player = this.getOwner() instanceof Player p ? p : null;
            BaitKind kind = player == null ? BaitKind.NONE : baitKind(player);
            switch (kind) {
                case CAN -> {
                    // The crab takes its time... then commits. One plunge, no nibbles.
                    this.biteCooldown = 110 + this.random.nextInt(110);
                }
                // Golden bait attracts fish roughly twice as fast.
                case GOLDEN -> this.biteCooldown = 40 + this.random.nextInt(60);
                default -> this.biteCooldown = 80 + this.random.nextInt(120);
            }
        } else if (this.onGround || !this.level.noCollision(this, this.getBoundingBox())) {
            this.setState(STATE_GROUNDED);
            this.playSplash(0.4f, 1.4f);
        }
    }

    private void tickBobbing(Player player) {
        if (!this.level.getFluidState(this.blockPosition()).is(FluidTags.WATER)) {
            this.setState(STATE_GROUNDED);
            return;
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(0, 0.02, 0));
        if (--this.biteCooldown <= 0) {
            if (baitKind(player) == BaitKind.CAN) {
                // No teasing nibbles with a beer can - one massive plunge. LONGER window.
                this.biteTicks = 45 + this.random.nextInt(15);
                this.setState(STATE_BITE);
                this.playSplash(1.4f, 0.5f);
                if (this.level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.2, this.getZ(),
                            22, 0.4, 0.15, 0.4, 0.2);
                }
                return;
            }
            this.nibblesLeft = 1 + this.random.nextInt(3);
            this.nibblePhaseTicks = 0;
            this.setState(STATE_NIBBLE);
        }
    }

    private void tickNibble(Player player) {
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

    private void tickBite() {
        if (--this.biteTicks <= 0) {
            // Missed the window: the fish escapes with a splash.
            this.biteCooldown = 80 + this.random.nextInt(140);
            this.setState(STATE_BOBBING);
            this.playSplash(0.7f, 1.3f);
        }
    }

    /**
     * Reel the line in. During a BITE this hooks the fish; while a fish is
     * HOOKED this gives an extra pull; otherwise it's an empty retrieve.
     * Returns the rod durability cost.
     */
    public int retrieve(ItemStack rodStack, Player player) {
        int state = getState();
        if (state == STATE_BITE && this.biteTicks > 0) {
            hookFish(player);
            return 1;
        }
        if (state == STATE_HOOKED) {
            // THE ONLY ENDING THERE IS. RMB (the "let go" button) decides:
            //   fish pulled close  -> land it at your feet (that IS the catch);
            //   fish still far away -> unhook and set it free, unharmed.
            // Nothing else can end a fight - no line snap, no auto-land, no
            // timer. You pull with LMB, you finish with RMB.
            double rdx = player.getX() - this.getX();
            double rdz = player.getZ() - this.getZ();
            double horiz = Math.sqrt(rdx * rdx + rdz * rdz);
            CustomFishEntity fish = this.level.getEntity(this.entityData.get(DATA_FISH_ID))
                    instanceof CustomFishEntity f ? f : null;
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.9f,
                    horiz <= LAND_DISTANCE ? 0.85f : 1.4f);
            if (horiz <= LAND_DISTANCE) {
                if (fish != null && fish.isAlive()) {
                    releaseFish(player, fish);
                }
            } else {
                if (fish != null) {
                    fish.setHooked(false);
                    fish.setInvulnerable(false);
                    fish.setNoAi(false);
                    fish.setDeltaMovement(new Vec3(this.random.nextGaussian() * 0.12, 0.05,
                            this.random.nextGaussian() * 0.12));
                    fish.hurtMarked = true;
                }
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.howtofish.fight_released"), true);
            }
            this.discardAndClean();
            return 0;
        }
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8f, 1.0f);
        this.discardAndClean();
        return 0;
    }

    /**
     * One LMB stroke of the reel (sent by ReelClickPacket while a fish is
     * hooked). Short cooldown so frantic spam is capped to real pulling.
     */
    public void applyReelStroke() {
        if (this.level.isClientSide || getState() != STATE_HOOKED) return;
        if (this.tickCount - this.lastReelStroke < 5) return;
        this.lastReelStroke = this.tickCount;
        // Deterministic: this click SHORTENS THE LINE by a fixed pull, every
        // single time, no rolls, no fail states, no timers. That is the whole
        // fight - trade clicks for distance and finish it with RMB yourself.
        this.reelCredit = Math.min(this.reelCredit + PULL_PER_STROKE, 6.0);
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.85f,
                1.15f + this.random.nextFloat() * 0.15f);
    }

    /** Hook: the fish that bit is now attached and will be pulled out of the water. */
    private void hookFish(Player player) {
        if (baitKind(player) == BaitKind.CAN && noBossNearby(player)) {
            consumeBait(player);
            summonBoss(player);
            return;
        }
        FishType type = FishType.roll(this.random, baitKind(player).premiumFish);
        consumeBait(player);
        CustomFishEntity fish = new CustomFishEntity(ModEntities.CUSTOM_FISH.get(), this.level);
        fish.setFishType(type);
        // Starts right below the float, under water - it will be dragged out.
        fish.setPos(this.getX(), this.getY() - 0.8, this.getZ());
        fish.setHooked(true);
        fish.setInvulnerable(true);
        fish.setNoAi(true);
        this.level.addFreshEntity(fish);
        this.hookedFishUuid = fish.getUUID();
        this.entityData.set(DATA_FISH_ID, fish.getId());
        this.entityData.set(DATA_HOOK_DISTANCE, (float) player.distanceTo(this));
        this.setState(STATE_HOOKED);
        this.reelCredit = 0.35;
        this.struggleTimer = 0;

        this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 1.0f, 0.7f);
        if (this.level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.3, this.getZ(),
                    25, 0.3, 0.2, 0.3, 0.2);
            sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2, this.getZ(),
                    6, 0.2, 0.1, 0.2, 0.01);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.howtofish.hooked", net.minecraft.network.chat.Component.translatable(
                        "fish.howtofish." + type.getId())), true);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.howtofish.reel_now"), true);
    }

    /**
     * The fish visibly follows the float while the float is reeled towards the
     * player. The float stays on the WATER SURFACE and glides horizontally, so
     * the fight is slow and readable: the fish trails behind it, splashing and
     * struggling, and only lands once it reaches the shore.
     */
    private void tickHooked(Player player) {
        CustomFishEntity fish = this.level.getEntity(this.entityData.get(DATA_FISH_ID)) instanceof CustomFishEntity f ? f : null;
        if (fish == null || !fish.isAlive()) {
            // Killed mid-fight or lost: the line snaps.
            this.discardAndClean();
            return;
        }

        Vec3 diff = player.position().subtract(this.position());
        double horiz = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        // THE FIGHT, ROUND 7 - STABLE BY CONSTRUCTION:
        //  * the line can NEVER snap and this method NEVER ends the fight
        //    (only RMB does - see retrieve(), or the fish dying);
        //  * every queued reel stroke pays out a guaranteed pull, so LMB
        //    always equals shorter line / closer fish;
        //  * the fish's struggle drags the float back only within the bounds
        //    it was hooked at - you can lose some ground, never the catch.
        this.hookedTicks++;
        double weight = Mth.clamp(fish.getMaxHealth() * 0.006f, 0.02f, 0.11f);
        boolean dash = (this.hookedTicks % 90) < 28;   // cosmetic burst rhythm
        if (this.entityData.get(DATA_STRUGGLE) != dash) {
            this.entityData.set(DATA_STRUGGLE, dash);
        }
        if (dash && this.hookedTicks % 90 == 0 && this.level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.2, this.getZ(),
                    10, 0.3, 0.15, 0.3, 0.12);          // visible "it pulled!" beat
        }

        double step = 0.0;
        if (this.reelCredit > 1.0E-4) {
            // Pay out the clicks - glide them over ticks so the line visibly
            // shortens instead of teleporting. Stops at "kiss distance".
            step = Math.min(this.reelCredit, Math.max(0.0, horiz - 1.0));
            this.reelCredit = Math.max(0.0, this.reelCredit - Math.max(step, 0.14));
        } else {
            // No input: the fish tests the line, but the line holds.
            step = dash ? -weight * 1.3 : -weight * 0.15;
        }
        double cap = Math.max(1.2, this.entityData.get(DATA_HOOK_DISTANCE));
        double newHoriz = Mth.clamp(horiz - step, 1.0, cap);
        if (horiz > 0.001) {
            Vec3 dirH = new Vec3(diff.x / horiz, 0, diff.z / horiz);
            this.setPos(this.getX() + dirH.x * (horiz - newHoriz),
                    findSurfaceY(), this.getZ() + dirH.z * (horiz - newHoriz));
        }
        horiz = newHoriz;
        diff = player.position().subtract(this.position());
        Vec3 dirH = horiz > 0.001
                ? new Vec3(diff.x / horiz, 0, diff.z / horiz)
                : new Vec3(0, 0, 0);
        if (horiz <= LAND_DISTANCE && this.hookedTicks % 40 == 0) {
            // They've arrived - announce that the finish is THEIRS to press.
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.howtofish.land_prompt"), true);
        }
        this.setYRot((float) (Mth.atan2(dirH.z, dirH.x) * (180F / Math.PI)) - 90.0f);

        // The fish swims BEHIND the float with a lively sideways struggle.
        this.struggleTimer++;
        Vec3 behind = this.position().add(0, -0.25, 0).subtract(dirH.scale(0.7));
        Vec3 toFish = behind.subtract(fish.position());
        double fd = toFish.length();
        if (fd > 0.01) {
            double pull = Mth.clamp(fd * 0.18, 0.07, 0.34) + (this.reelCredit > 0.05 ? 0.05 : 0.0);
            double perp = Math.sin(this.struggleTimer * 0.4) * 0.05;
            Vec3 dir = toFish.normalize();
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            Vec3 vel = dir.scale(pull).add(side.scale(perp));
            if (!fish.isInWater()) {
                vel = vel.add(0, -0.08, 0);
            }
            fish.setDeltaMovement(vel);
            // Manual server-side drag (the fish has no AI while hooked).
            fish.setPos(fish.getX() + vel.x, fish.getY() + vel.y, fish.getZ() + vel.z);
            // Keep the fish right at the surface so the fight is visible.
            double fishSurface = findSurfaceYAt(fish.getX(), fish.getY(), fish.getZ());
            if (fish.getY() > fishSurface + 0.2) {
                fish.setPos(fish.getX(), fishSurface + 0.2, fish.getZ());
            }
        }
        fish.hurtMarked = true;
        fish.setYRot((float) (Mth.atan2(vel(fish).z, vel(fish).x) * (180F / Math.PI)) - 90.0f);

        // Splashes while the fish fights on the surface.
        if (this.struggleTimer % 10 == 0) {
            this.level.playSound(null, fish.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH,
                    SoundSource.PLAYERS, 0.5f, 0.8f + this.random.nextFloat() * 0.4f);
            if (this.level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SPLASH, fish.getX(), fish.getY() + 0.3, fish.getZ(),
                        8, 0.25, 0.1, 0.25, 0.1);
            }
        }
    }

    private boolean noBossNearby(Player player) {
        return this.level.getEntitiesOfClass(BossFishEntity.class,
                player.getBoundingBox().inflate(48.0), BossFishEntity::isAlive).isEmpty();
    }

    /**
     * The can bite is not a fish: hooking it triggers the BOSS. All crab
     * internals (sound, FX, music, targeting) live in
     * {@link BossFishEntity#summonAt} - the fishing code only says
     * "summon it HERE", which keeps fish and boss systems decoupled.
     */
    private void summonBoss(Player player) {
        BossFishEntity boss = BossFishEntity.summonAt(this.level, this.position(), player);
        if (boss != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.howtofish.boss_bite"), true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.howtofish.boss_summoned"));
        }
        this.discardAndClean();
    }

    /** Water surface height at the float's position, searching up/down a little. */
    private double findSurfaceY() {
        return findSurfaceYAt(this.getX(), this.getY(), this.getZ());
    }

    private double findSurfaceYAt(double x, double y, double z) {
        BlockPos base = new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        // Already at a water surface?
        if (this.level.getFluidState(base).is(FluidTags.WATER)) {
            return base.getY() + (float) (Math.sin((this.tickCount) * 0.25) * 0.02);
        }
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos up = base.above(dy);
            if (this.level.getFluidState(up).is(FluidTags.WATER)) return up.getY();
        }
        for (int dy = 1; dy <= 4; dy++) {
            BlockPos down = base.below(dy);
            if (this.level.getFluidState(down).is(FluidTags.WATER)) return down.getY();
        }
        return y;
    }

    private static Vec3 vel(Entity e) {
        return e.getDeltaMovement();
    }

    /** Set the caught fish down on the shore next to the player. */
    private void releaseFish(Player player, CustomFishEntity fish) {
        Vec3 look = player.getLookAngle();
        double x = player.getX() + look.x * 1.4;
        double z = player.getZ() + look.z * 1.4;
        // Try to place on solid ground near the player, otherwise at feet.
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos((int) Math.floor(x),
                (int) Math.floor(player.getY() + 1), (int) Math.floor(z));
        if (!this.level.getBlockState(mpos.below()).isSolidRender(this.level, mpos.below())) {
            x = player.getX();
            z = player.getZ();
        }
        fish.setPos(x, player.getY() + 0.4, z);
        fish.setNoAi(false);
        fish.setHooked(false);
        fish.setInvulnerable(false);
        fish.setDeltaMovement(look.scale(0.25).add(0, 0.18, 0));
        fish.hurtMarked = true;
        this.entityData.set(DATA_FISH_ID, -1);

        this.level.playSound(null, fish.blockPosition(), ModSounds.FISH_FLOP.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        if (this.level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH, fish.getX(), fish.getY() + 0.4, fish.getZ(),
                    15, 0.3, 0.2, 0.3, 0.15);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.howtofish.landed", net.minecraft.network.chat.Component.translatable(
                        "fish.howtofish." + fish.getFishType().getId())), true);
    }

    private void tickGrounded() {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
        if (++this.groundTicks > 60) {
            this.discardAndClean();
        }
    }

    private void playSplash(float volume, float pitch) {
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, volume, pitch);
    }

    private void discardAndClean() {
        // If a hooked fish is left dangling, free it.
        if (getState() == STATE_HOOKED) {
            Entity fish = this.level.getEntity(this.entityData.get(DATA_FISH_ID));
            if (fish instanceof CustomFishEntity f && f.isAlive()) {
                f.setNoAi(false);
                f.setHooked(false);
                f.setInvulnerable(false);
            }
        }
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
        if (this.hookedFishUuid != null) {
            tag.putUUID("HookedFish", this.hookedFishUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_STATE, tag.getInt("BobberState"));
        this.biteCooldown = tag.getInt("BiteCooldown");
        this.nibblesLeft = tag.getInt("NibblesLeft");
        this.biteTicks = tag.getInt("BiteTicks");
        if (tag.hasUUID("HookedFish")) {
            this.hookedFishUuid = tag.getUUID("HookedFish");
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
