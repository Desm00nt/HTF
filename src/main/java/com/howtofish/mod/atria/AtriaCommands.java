package com.howtofish.mod.atria;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Команда {@code /atria} — общение с нейросетью Atria Dawn:
 * <pre>
 *   /atria &lt;сообщение&gt; — задать вопрос (можно и просто «@вопрос» в чате)
 *   /atria reset        — очистить контекст своего диалога
 *   /atria about        — справка
 *   /atria reload       — перечитать config/atriadawn.json (операторы)
 * </pre>
 */
public final class AtriaCommands {

    private AtriaCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("atria")
                .executes(ctx -> sendAbout(ctx.getSource()))
                .then(Commands.literal("about")
                        .executes(ctx -> sendAbout(ctx.getSource())))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            AtriaConversation.clear(player.getUUID());
                            player.sendSystemMessage(Component.translatable("atria.howtofish.cmd_reset")
                                    .withStyle(ChatFormatting.GREEN));
                            return 1;
                        }))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            AtriaConfig.reload();
                            AtriaConfig cfg = AtriaConfig.get();
                            boolean hasKey = !cfg.resolveApiKey().isEmpty();
                            ctx.getSource().sendSuccess(Component.translatable(hasKey
                                    ? "atria.howtofish.cmd_reload"
                                    : "atria.howtofish.cmd_reload_nokey"), true);
                            return 1;
                        }))
                // аргумент регистрируется последним: точные литералы выше имеют приоритет
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String text = StringArgumentType.getString(ctx, "message").strip();
                            if (text.isEmpty()) {
                                player.sendSystemMessage(Component.translatable("atria.howtofish.cmd_empty")
                                        .withStyle(ChatFormatting.YELLOW));
                                return 0;
                            }
                            AtriaChatManager.ask(player, text);
                            return 1;
                        })));
    }

    private static int sendAbout(CommandSourceStack source) {
        AtriaConfig cfg = AtriaConfig.get();
        String prefix = cfg.chatPrefix == null || cfg.chatPrefix.strip().isEmpty()
                ? "/" : cfg.chatPrefix.strip();
        source.sendSuccess(Component.translatable("atria.howtofish.about_1", AtriaChatManager.BOT_NAME), false);
        source.sendSuccess(Component.translatable("atria.howtofish.about_2", prefix), false);
        source.sendSuccess(Component.translatable("atria.howtofish.about_3"), false);
        source.sendSuccess(Component.translatable("atria.howtofish.about_4",
                Math.max(1, cfg.maxHistoryMessages / 2)), false);
        return 1;
    }
}
