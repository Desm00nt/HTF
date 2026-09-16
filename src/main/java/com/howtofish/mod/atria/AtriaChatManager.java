package com.howtofish.mod.atria;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Оркестратор диалога с Atria Dawn: антиспам-кулдаун, «одно сообщение
 * за раз» на игрока, показ вопроса и индикатора «печатает…», вызов
 * {@link AtriaApiClient} и доставка ответа в чат.
 *
 * <p>Запрос выполняется асинхронно; обратно на серверный поток
 * возвращаемся через {@link MinecraftServer#execute(Runnable)},
 * как того требует потоковая модель Minecraft.</p>
 */
public final class AtriaChatManager {

    public static final String BOT_NAME = "Atria Dawn";
    /** Максимальная длина одного вопроса (защита от копипасты на 10 страниц). */
    public static final int MAX_QUESTION_CHARS = 2000;

    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    private AtriaChatManager() {
    }

    /**
     * Задать вопрос нейросети от лица игрока. Вызывается с серверного потока
     * (обработчик чата или команда {@code /atria}).
     */
    public static void ask(ServerPlayer player, String userText) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        UUID playerId = player.getUUID();
        AtriaConfig cfg = AtriaConfig.get();

        if (cfg.resolveApiKey().isEmpty()) {
            send(player, Component.translatable("atria.howtofish.err_no_key").withStyle(ChatFormatting.RED));
            return;
        }
        if (userText.length() > MAX_QUESTION_CHARS) {
            send(player, Component.translatable("atria.howtofish.too_long", MAX_QUESTION_CHARS)
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(0, cfg.cooldownSeconds) * 1000L;
        long last = LAST_REQUEST.getOrDefault(playerId, 0L);
        if (cooldownMs > 0 && now - last < cooldownMs) {
            long secondsLeft = (cooldownMs - (now - last) + 999) / 1000;
            send(player, Component.translatable("atria.howtofish.cooldown", secondsLeft)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!PENDING.add(playerId)) {
            send(player, Component.translatable("atria.howtofish.busy").withStyle(ChatFormatting.YELLOW));
            return;
        }
        LAST_REQUEST.put(playerId, now);

        // Эхо вопроса + индикатор набора текста.
        sendMaybeBroadcast(player, cfg.broadcastReplies, questionComponent(player.getGameProfile().getName(), userText));
        if (cfg.showTypingIndicator) {
            sendMaybeBroadcast(player, cfg.broadcastReplies,
                    Component.translatable("atria.howtofish.typing")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        List<AtriaConversation.Msg> history = AtriaConversation.snapshot(playerId);
        String playerName = player.getGameProfile().getName();

        AtriaApiClient.chat(cfg, history, playerName, userText).thenAccept(result -> server.execute(() -> {
            PENDING.remove(playerId);
            ServerPlayer target = server.getPlayerList().getPlayer(playerId);
            if (target == null) {
                return; // игрок успел выйти — доставлять некому
            }
            if (!result.ok()) {
                send(target, Component.translatable(result.errorKey(), result.errorArgs())
                        .withStyle(ChatFormatting.RED));
                return;
            }
            String answer = result.content();
            if (answer == null || answer.isBlank()) {
                send(target, Component.translatable("atria.howtofish.err_bad_reply").withStyle(ChatFormatting.RED));
                return;
            }
            AtriaConversation.rememberUser(playerId, userText, cfg.maxHistoryMessages);
            AtriaConversation.rememberAssistant(playerId, answer, cfg.maxHistoryMessages);
            for (String chunk : splitForChat(answer, Math.max(100, cfg.maxResponseChars))) {
                sendMaybeBroadcast(target, cfg.broadcastReplies, answerComponent(chunk));
            }
        }));
    }

    /** Забыть всё про игрока (выход с сервера). */
    public static void forget(UUID playerId) {
        PENDING.remove(playerId);
        LAST_REQUEST.remove(playerId);
        AtriaConversation.clear(playerId);
    }

    // ---- форматирование сообщений чата ----

    /** «Nick → Atria Dawn: вопрос» — эхо отправленного запроса. */
    public static MutableComponent questionComponent(String playerName, String text) {
        return Component.literal(playerName).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" \u2192 ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(BOT_NAME).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD))
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    /** «Atria Dawn » ответ» — сообщение бота. */
    public static MutableComponent answerComponent(String chunk) {
        return Component.literal(BOT_NAME).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
                .append(Component.literal(" \u00bb ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(chunk).withStyle(ChatFormatting.WHITE));
    }

    /**
     * Нарезать ответ на куски по {@code maxChars} символов (по границам слов,
     * длинные слова — жёстко). Длинный ответ не должен заваливать чат, поэтому
     * максимум 8 кусков + "[...]".
     */
    static List<String> splitForChat(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.isEmpty()) {
            return chunks;
        }
        for (String paragraph : normalized.split("\n+")) {
            String p = paragraph.strip();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() <= maxChars) {
                chunks.add(p);
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String rawWord : p.split(" ")) {
                String word = rawWord.strip();
                if (word.isEmpty()) {
                    continue;
                }
                if (word.length() > maxChars) {
                    if (current.length() > 0) {
                        chunks.add(current.toString());
                        current = new StringBuilder();
                    }
                    for (int i = 0; i < word.length(); i += maxChars) {
                        chunks.add(word.substring(i, Math.min(word.length(), i + maxChars)));
                    }
                    continue;
                }
                if (current.length() == 0) {
                    current.append(word);
                } else if (current.length() + 1 + word.length() <= maxChars) {
                    current.append(' ').append(word);
                } else {
                    chunks.add(current.toString());
                    current = new StringBuilder(word);
                }
            }
            if (current.length() > 0) {
                chunks.add(current.toString());
            }
        }
        if (chunks.size() > 8) {
            chunks = new ArrayList<>(chunks.subList(0, 8));
            chunks.add("[...]");
        }
        return chunks;
    }

    private static void send(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
    }

    private static void sendMaybeBroadcast(ServerPlayer player, boolean broadcast, Component message) {
        if (broadcast) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                    online.sendSystemMessage(message);
                }
                return;
            }
        }
        player.sendSystemMessage(message);
    }
}
