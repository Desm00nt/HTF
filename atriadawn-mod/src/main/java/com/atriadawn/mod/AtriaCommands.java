package com.atriadawn.mod;

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
 *   /atria key &lt;ключ&gt;   — вписать API-ключ прямо в игре и сохранить его
 *                         в config/atriadawn.json (операторы)
 *   /atria key status   — проверить, задан ли ключ (показывается маскированным)
 *   /atria key clear    — удалить ключ из конфига (операторы)
 *   /atria reload       — перечитать config/atriadawn.json (операторы)
 * </pre>
 */
public final class AtriaCommands {

    /** Минимальная «разумная» длина API-ключа (защита от опечаток). */
    public static final int MIN_KEY_LENGTH = 10;

    private AtriaCommands() {
    }

    /** Похож ли текст на API-ключ: без пробелов и достаточно длинный. */
    private static boolean isValidKey(String key) {
        return key.length() >= MIN_KEY_LENGTH && !key.matches(".*\\s.*");
    }

    /** Строка статуса ключа: из конфига (маскирован), из env или не задан. */
    private static Component keyStatus() {
        AtriaConfig cfg = AtriaConfig.get();
        if (cfg.apiKey != null && !cfg.apiKey.isBlank()) {
            return Component.translatable("atria.cmd_key_status_cfg", cfg.maskedKey());
        }
        String envVar = cfg.apiKeyEnvVar == null ? "" : cfg.apiKeyEnvVar.strip();
        if (!envVar.isEmpty() && !cfg.resolveApiKey().isEmpty()) {
            return Component.translatable("atria.cmd_key_status_env", envVar);
        }
        return Component.translatable("atria.cmd_key_status_none").withStyle(ChatFormatting.YELLOW);
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
                            player.sendSystemMessage(Component.translatable("atria.cmd_reset")
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
                                    ? "atria.cmd_reload"
                                    : "atria.cmd_reload_nokey"), true);
                            return 1;
                        }))
                .then(Commands.literal("key")
                        // /atria key (без аргументов) — краткая справка
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    Component.translatable("atria.cmd_key_hint"), false);
                            return 1;
                        })
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(keyStatus(), false);
                                    return 1;
                                }))
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    AtriaConfig.get().clearApiKey();
                                    ctx.getSource().sendSuccess(
                                            Component.translatable("atria.cmd_key_cleared"), true);
                                    return 1;
                                }))
                        .then(Commands.argument("apikey", StringArgumentType.greedyString())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    String raw = StringArgumentType.getString(ctx, "apikey").strip();
                                    // убираем случайные кавычки вокруг скопированного ключа
                                    if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
                                        raw = raw.substring(1, raw.length() - 1).strip();
                                    }
                                    if (!isValidKey(raw)) {
                                        ctx.getSource().sendFailure(Component.translatable(
                                                "atria.cmd_key_invalid", MIN_KEY_LENGTH));
                                        return 0;
                                    }
                                    AtriaConfig cfg = AtriaConfig.get();
                                    cfg.setApiKey(raw);
                                    ctx.getSource().sendSuccess(Component.translatable(
                                            "atria.cmd_key_set", cfg.maskedKey()), true);
                                    return 1;
                                })))
                // аргумент регистрируется последним: точные литералы выше имеют приоритет
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String text = StringArgumentType.getString(ctx, "message").strip();
                            if (text.isEmpty()) {
                                player.sendSystemMessage(Component.translatable("atria.cmd_empty")
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
        source.sendSuccess(Component.translatable("atria.about_1", AtriaChatManager.BOT_NAME), false);
        source.sendSuccess(Component.translatable("atria.about_2", prefix), false);
        source.sendSuccess(Component.translatable("atria.about_3"), false);
        source.sendSuccess(Component.translatable("atria.about_4",
                Math.max(1, cfg.maxHistoryMessages / 2)), false);
        return 1;
    }
}
