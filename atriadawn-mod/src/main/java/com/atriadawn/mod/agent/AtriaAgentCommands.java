package com.atriadawn.mod.agent;

import com.atriadawn.mod.entity.AtriaCompanionEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Команда {@code /atriaagent} — агентные задачи для компаньона:
 * <pre>
 *   /atriaagent &lt;задача&gt;  — дать нейросети задачу (она сама шагает, копает, строит)
 *   /atriaagent stop       — прервать текущую задачу
 * </pre>
 * (Призыв/удаление тела компаньона — {@code /atria summon} и {@code /atria remove},
 * они зарегистрированы вместе с остальными подкомандами /atria.)
 */
public final class AtriaAgentCommands {

    private AtriaAgentCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        // ВАЖНО: литерал stop зарегистрирован ДО greedy-аргумента task,
        // иначе brigadier направит "stop" в task.
        event.getDispatcher().register(
                com.mojang.brigadier.builder.LiteralArgumentBuilder
                        .<net.minecraft.commands.CommandSourceStack>literal("atriaagent")
                        .then(com.mojang.brigadier.builder.LiteralArgumentBuilder
                                .<net.minecraft.commands.CommandSourceStack>literal("stop")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    AtriaAgentManager.cancelSession(player.getUUID());
                                    return 1;
                                }))
                        .then(com.mojang.brigadier.builder.RequiredArgumentBuilder
                                .<net.minecraft.commands.CommandSourceStack, String>argument(
                                        "task", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    AtriaAgentManager.startTask(player,
                                            StringArgumentType.getString(ctx, "task").strip());
                                    return 1;
                                }))
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(Component.translatable("atria.agent_cmd_hint"), false);
                            return 1;
                        }));
    }

    /** Призвать тело-аватар рядом с игроком (вызывается и из /atria summon). */
    public static void summonPublic(ServerPlayer player) {
        AtriaCompanionEntity existing = AtriaAgentManager.getCompanion(player.getUUID());
        if (existing != null) {
            player.sendSystemMessage(Component.translatable("atria.agent_exists")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
            return;
        }
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        AtriaCompanionEntity companion = com.atriadawn.mod.registry.AtriaRegistry.ATRIA_COMPANION.get().create(level);
        if (companion == null) {
            return;
        }
        companion.moveTo(player.getX() + 1.0D, player.getY(), player.getZ(), player.getYRot(), 0.0F);
        companion.setOwner(player.getUUID());
        companion.setCustomName(Component.literal("Atria Dawn"));
        companion.setCustomNameVisible(true);
        level.addFreshEntity(companion);
        AtriaAgentManager.register(companion);
        player.sendSystemMessage(Component.translatable("atria.agent_spawned")
                .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
