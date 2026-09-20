package com.atriadawn.mod.entity;

import com.atriadawn.mod.AtriaAgentManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Тело-аватар Atria Dawn: гуманоидный компаньон, которым управляет
 * нейросеть через «инструменты» ({@link com.atriadawn.mod.agent.AtriaTools}).
 *
 * <p>Серверная логика построена как простой автомат состояний
 * (WALK / MINE / ATTACK + флаг «следовать за владельцем» в простое),
 * а не как набор Goal — так управляемое поведение детерминировано и его
 * легко ждать из агентного цикла: каждый инструмент возвращает
 * {@link CompletableFuture}, который сущность завершает по факту
 * выполнения или таймауту.</p>
 */
public class AtriaCompanionEntity extends PathfinderMob {

    private UUID ownerUuid;
    private boolean followOwner = true;

    private enum Action { NONE, WALK, MINE, ATTACK }

    private Action action = Action.NONE;
    private BlockPos actionPos;
    private LivingEntity attackTarget;
    private CompletableFuture<String> activeFuture;
    private int timeoutTicks;
    private int breakProgress;
    private int attackCooldown;
    private int ambientCooldown;
    /** Счётчик шагов добычи для диагностики владельцу. */
    private int minedCount;

    /** Простой инвентарь компаньона (9 слотов) — куда падает добыча и откуда берутся блоки. */
    private final List<ItemStack> inventory = new ArrayList<>();

    public AtriaCompanionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        for (int i = 0; i < 9; i++) {
            inventory.add(ItemStack.EMPTY);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // ---- владелец / NBT ----

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwner(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public ServerPlayer getOwnerPlayer() {
        if (ownerUuid == null || level.isClientSide) {
            return null;
        }
        return level.getServer() != null ? level.getServer().getPlayerList().getPlayer(ownerUuid) : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) {
            tag.putUUID("AtriaOwner", ownerUuid);
        }
        tag.putBoolean("AtriaFollow", followOwner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("AtriaOwner")) {
            ownerUuid = tag.getUUID("AtriaOwner");
        }
        followOwner = tag.getBoolean("AtriaFollow");
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        if (!level.isClientSide) {
            AtriaAgentManager.onCompanionRemoved(this);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level.isClientSide) {
            ServerPlayer owner = getOwnerPlayer();
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("atria.agent_dead").withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && player.getUUID().equals(ownerUuid)) {
            followOwner = !followOwner;
            player.sendSystemMessage(Component.translatable(followOwner ? "atria.agent_follow_on" : "atria.agent_follow_off")
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    // ---- тик ----

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            serverAiTick();
        }
    }

    private void serverAiTick() {
        vacuumNearbyItems();
        if (timeoutTicks > 0) {
            timeoutTicks--;
        }
        switch (action) {
            case WALK -> tickWalk();
            case MINE -> tickMine();
            case ATTACK -> tickAttack();
            case NONE -> tickAmbient();
        }
    }

    /** Простой режим: следование за владельцем. */
    private void tickAmbient() {
        if (!followOwner) {
            return;
        }
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null || owner.level != level) {
            return;
        }
        double distSq = distanceToSqr(owner);
        if (distSq > 49.0D && --ambientCooldown <= 0) {
            ambientCooldown = 20;
            getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), 1.05D);
        } else if (distSq <= 49.0D) {
            ambientCooldown = 0;
        }
    }

    private void tickWalk() {
        if (actionPos == null || activeFuture == null) {
            stopAction();
            return;
        }
        double distSq = distanceToSqr(actionPos.getX() + 0.5, actionPos.getY() + 0.5, actionPos.getZ() + 0.5);
        if (distSq < 3.24D) { // в пределах ~1,8 блока от центра
            complete("прибыл в точку " + posStr(actionPos));
            return;
        }
        if (timeoutTicks <= 0) {
            complete("НЕ СМОГ дойти до " + posStr(actionPos) + " за отведённое время (путь заблокирован?)");
            return;
        }
        if (getNavigation().isDone() && --ambientCooldown <= 0) {
            ambientCooldown = 20;
            getNavigation().moveTo(actionPos.getX() + 0.5, actionPos.getY(), actionPos.getZ() + 0.5, 1.1D);
        }
    }

    private void tickMine() {
        if (actionPos == null || activeFuture == null) {
            stopAction();
            return;
        }
        if (timeoutTicks <= 0) {
            complete("НЕ СМОГ добыть блок " + posStr(actionPos) + " за отведённое время");
            return;
        }
        BlockState state = level.getBlockState(actionPos);
        if (state.isAir()) {
            complete("блок в " + posStr(actionPos) + " уже отсутствует");
            return;
        }
        double distSq = distanceToSqr(actionPos.getX() + 0.5, actionPos.getY() + 0.5, actionPos.getZ() + 0.5);
        if (distSq > 20.25D) { // дальше ~4,5 блоков — сначала подойти
            if (getNavigation().isDone()) {
                getNavigation().moveTo(actionPos.getX() + 0.5, actionPos.getY(), actionPos.getZ() + 0.5, 1.1D);
            }
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(actionPos.getX() + 0.5, actionPos.getY() + 0.5, actionPos.getZ() + 0.5);
        float hardness = state.getDestroySpeed(level, actionPos);
        int needTicks = (int) Math.max(16, Math.min(120, hardness * 18.0F));
        if (breakProgress % 6 == 0) {
            swing(InteractionHand.MAIN_HAND);
        }
        breakProgress++;
        if (breakProgress >= needTicks) {
            level.destroyBlock(actionPos, true, this);
            minedCount++;
            swing(InteractionHand.MAIN_HAND);
            complete("добыл " + Registry.BLOCK.getKey(state.getBlock()) + " в " + posStr(actionPos));
        }
    }

    private void tickAttack() {
        if (attackTarget == null || activeFuture == null) {
            stopAction();
            return;
        }
        if (!attackTarget.isAlive() || attackTarget.isRemoved()) {
            complete("цель уничтожена: " + targetName(attackTarget));
            return;
        }
        if (timeoutTicks <= 0) {
            complete("НЕ СМОГ уничтожить " + targetName(attackTarget) + " за отведённое время");
            return;
        }
        double distSq = distanceToSqr(attackTarget);
        if (distSq > 4.84D) { // ~2,2 блока
            if (getNavigation().isDone()) {
                getNavigation().moveTo(attackTarget, 1.15D);
            }
        } else if (--attackCooldown <= 0) {
            attackCooldown = 15;
            getLookControl().setLookAt(attackTarget);
            swing(InteractionHand.MAIN_HAND);
            attackTarget.hurt(DamageSource.mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    // ---- публичный API инструментов (вызывать с серверного потока) ----

    public synchronized CompletableFuture<String> beginWalk(BlockPos pos, int timeoutTicks) {
        cancelAction();
        action = Action.WALK;
        actionPos = pos.immutable();
        this.timeoutTicks = timeoutTicks;
        ambientCooldown = 0;
        getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.1D);
        return startFuture();
    }

    public synchronized CompletableFuture<String> beginMine(BlockPos pos, int timeoutTicks) {
        cancelAction();
        action = Action.MINE;
        actionPos = pos.immutable();
        this.timeoutTicks = timeoutTicks;
        breakProgress = 0;
        getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.1D);
        return startFuture();
    }

    public synchronized CompletableFuture<String> beginAttack(LivingEntity target, int timeoutTicks) {
        cancelAction();
        action = Action.ATTACK;
        attackTarget = target;
        this.timeoutTicks = timeoutTicks;
        attackCooldown = 0;
        getNavigation().moveTo(target, 1.15D);
        return startFuture();
    }

    public synchronized void cancelAction() {
        action = Action.NONE;
        actionPos = null;
        attackTarget = null;
        timeoutTicks = 0;
        breakProgress = 0;
        getNavigation().stop();
        if (activeFuture != null) {
            activeFuture.complete("отменено");
            activeFuture = null;
        }
    }

    private synchronized CompletableFuture<String> startFuture() {
        activeFuture = new CompletableFuture<>();
        return activeFuture;
    }

    private synchronized void complete(String result) {
        action = Action.NONE;
        actionPos = null;
        attackTarget = null;
        getNavigation().stop();
        if (activeFuture != null) {
            activeFuture.complete(result);
            activeFuture = null;
        }
    }

    private synchronized void stopAction() {
        action = Action.NONE;
    }

    // ---- инвентарь и «магнит» дропа ----

    private void vacuumNearbyItems() {
        if (this.tickCount % 10 != 0) {
            return;
        }
        AABB box = getBoundingBox().inflate(1.5D);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (item.isRemoved()) {
                continue;
            }
            ItemStack stack = item.getItem();
            if (!stack.isEmpty() && addStack(stack)) {
                item.discard();
            }
        }
    }

    /** Сложить предмет в инвентарь (со слиянием стаков). true — если поместилось целиком. */
    public boolean addStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        for (int i = 0; i < inventory.size() && !copy.isEmpty(); i++) {
            ItemStack slot = inventory.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, copy)) {
                int space = Math.min(slot.getMaxStackSize(), 64) - slot.getCount();
                if (space > 0) {
                    int moved = Math.min(space, copy.getCount());
                    slot.grow(moved);
                    copy.shrink(moved);
                }
            }
        }
        for (int i = 0; i < inventory.size() && !copy.isEmpty(); i++) {
            if (inventory.get(i).isEmpty()) {
                inventory.set(i, copy.split(copy.getCount()));
            }
        }
        return copy.isEmpty();
    }

    /** Найти и изъять один предмет-блок (опционально с фильтром по имени). */
    public synchronized ItemStack takeBlockItem(String nameFilter) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slot = inventory.get(i);
            if (slot.isEmpty() || !(slot.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            ResourceLocation id = Registry.ITEM.getKey(slot.getItem());
            if (nameFilter == null || nameFilter.isBlank()
                    || id.getPath().contains(nameFilter.toLowerCase().strip())) {
                ItemStack one = slot.split(1);
                if (slot.isEmpty()) {
                    inventory.set(i, ItemStack.EMPTY);
                }
                return new ItemStack(blockItem.getBlock(), 1);
            }
        }
        return ItemStack.EMPTY;
    }

    public String inventorySummary() {
        StringBuilder sb = new StringBuilder();
        for (ItemStack slot : inventory) {
            if (!slot.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(slot.getCount()).append("x ").append(Registry.ITEM.getKey(slot.getItem()).getPath());
            }
        }
        return sb.isEmpty() ? "пусто" : sb.toString();
    }

    // ---- поиск (инструмент scan_area) ----

    public List<BlockPos> findBlocks(int radius, String filter) {
        BlockPos min = blockPosition().offset(-radius, -Math.min(radius, 12), -radius);
        BlockPos max = blockPosition().offset(radius, Math.min(radius, 12), radius);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (filter != null && !filter.isBlank()) {
                String id = Registry.BLOCK.getKey(state.getBlock()).getPath();
                if (!id.contains(filter.toLowerCase().strip())) {
                    continue;
                }
            }
            found.add(pos.immutable());
            if (found.size() >= 400) {
                break;
            }
        }
        found.sort(Comparator.comparingDouble(p -> p.distSqr(blockPosition())));
        return found;
    }

    public List<Monster> findHostiles(int radius) {
        return level.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(radius),
                m -> m.isAlive() && !m.isRemoved());
    }

    public LivingEntity findAttackTarget(int radius, String filter) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius),
                e -> e.isAlive() && !e.isRemoved() && e != this && !(e instanceof AtriaCompanionEntity)
                        && !e.getUUID().equals(ownerUuid));
        candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
        String f = filter == null ? "" : filter.toLowerCase().strip();
        for (LivingEntity e : candidates) {
            boolean matches = switch (f) {
                case "", "hostile", "враждебный" -> e instanceof Monster;
                case "any", "любой" -> true;
                default -> EntityType.getKey(e.getType()).getPath().contains(f);
            };
            if (matches) {
                return e;
            }
        }
        return null;
    }

    public void say(String text) {
        ServerPlayer owner = getOwnerPlayer();
        Component msg = AtriaAgentManager.agentLine(text);
        if (owner != null) {
            owner.sendSystemMessage(msg);
        } else if (level.getServer() != null) {
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(msg);
            }
        }
    }

    public void setFollowOwner(boolean follow) {
        this.followOwner = follow;
    }

    public int getMinedCount() {
        return minedCount;
    }

    private static String posStr(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String targetName(LivingEntity e) {
        return EntityType.getKey(e.getType()).getPath();
    }
}
