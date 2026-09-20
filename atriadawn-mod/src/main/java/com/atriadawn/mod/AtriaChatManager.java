package com.atriadawn.mod;

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
 * Оркестратор диалога с Atria Dawn.
 *
 * <p>Защита от «флуда печатанием»: если триггеры чата приходят чаще, чем
 * раз в {@link #DEBOUNCE_MS} мс (игрок ещё печатает, либо что-то шлёт
 * заготовки по нажатию клавиш), вопрос <b>не</b> отправляется сразу — текст
 * копируется в очередь и заменяется более новым. Запрос уходит, когда
 * наступает тишина ({@link #tickFlush()} вызывается из серверного тика),
 * причём уходит только <b>последний</b> вариант текста. Так мусорные фрагменты
 * («@п», «@при») не долетают до нейросети, а игрок всегда получает ответ на
 * своё полное сообщение.</p>
 *
 * <p>Дополнительно действует «период тишины» {@code cooldownSeconds} между
 * запросами одного игрока; вопрос, пришедший раньше, ждёт в той же очереди
 * и отправляется, когда период истечёт. Никакого спама предупреждениями —
 * сообщения об ошибках приходят только на реально отправленные запросы.</p>
 */
public final class AtriaChatManager {

    public static final String BOT_NAME = "Atria Dawn";
    /** Максимальная длина одного вопроса (защита от копипасты на 10 страниц). */
    public static final int MAX_QUESTION_CHARS = 2000;
    /** Минимальная длина вопроса — мусорные обрывки «@п» игнорируются молча. */
    public static final int MIN_QUESTION_CHARS = 3;
    /** Пока с последнего триггера прошло меньше этого времени, вопрос копится. */
    public static final long DEBOUNCE_MS = 1200L;

    /** Вопрос, ожидающий отправки (последний вариант текста побеждает). */
    private record QueuedQuestion(String text, long queuedAtMillis) {
    }

    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, QueuedQuestion> QUEUED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();
    /** Ограничитель частоты сообщений об ошибках на игрока (антиспам чата). */
    private static final Map<UUID, Long> LAST_ERROR_MESSAGE = new ConcurrentHashMap<>();

    private AtriaChatManager() {
    }

    /**
     * Триггер вопроса (из чата с префиксом или команды /atria).
     * Вызывается с серверного потока. Вопрос всегда сначала кладётся в
     * очередь: {@link #tickFlush()} отправит его, когда игрок закончит
     * печатать (тишина {@link #DEBOUNCE_MS} мс) — так обрывки слов при
     * наборе и ложные срабатывания не долетают до нейросети.
     */
    public static void ask(ServerPlayer player, String userText) {
        UUID playerId = player.getUUID();
        AtriaConfig cfg = AtriaConfig.get();

        if (cfg.resolveApiKey().isEmpty()) {
            // Важное сообщение — не глушим, но и не спамим: раз в 5 секунд.
            sendError(player, Component.translatable("atria.err_no_key"), cfg);
            return;
        }
        if (userText.length() > MAX_QUESTION_CHARS) {
            sendError(player, Component.translatable("atria.too_long", MAX_QUESTION_CHARS), cfg);
            return;
        }
        if (userText.length() < MIN_QUESTION_CHARS) {
            return; // обрывок при наборе — молча игнорируем
        }
        QUEUED.put(playerId, new QueuedQuestion(userText, System.currentTimeMillis()));
    }

    /**
     * Попытка реально отправить вопрос. Если игрок «в ожидании» ответа или
     * не истёк период тишины — вопрос кладётся в очередь (побеждает последний).
     */
    private static void startAsk(ServerPlayer player, String userText, long now) {
        UUID playerId = player.getUUID();
        AtriaConfig cfg = AtriaConfig.get();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        long cooldownMs = Math.max(0, cfg.cooldownSeconds) * 1000L;
        long lastRequest = LAST_REQUEST.getOrDefault(playerId, 0L);
        if (now - lastRequest < cooldownMs || !PENDING.add(playerId)) {
            QUEUED.put(playerId, new QueuedQuestion(userText, now));
            return;
        }
        LAST_REQUEST.put(playerId, now);

        // Эхо вопроса + индикатор набора текста — только у реально ушедшего запроса.
        sendMaybeBroadcast(player, cfg.broadcastReplies, questionComponent(player.getGameProfile().getName(), userText));
        if (cfg.showTypingIndicator) {
            sendMaybeBroadcast(player, cfg.broadcastReplies,
                    Component.translatable("atria.typing")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        List<AtriaConversation.Msg> history = AtriaConversation.snapshot(playerId);
        String playerName = player.getGameProfile().getName();

        AtriaApiClient.chat(cfg, history, playerName, userText).thenAccept(result -> server.execute(() -> {
            PENDING.remove(playerId);
            ServerPlayer target = MinecraftServerGetter.get().getPlayerList().getPlayer(playerId);
            if (target == null) {
                return; // игрок успел выйти — доставлять некому
            }
            if (!result.ok()) {
                sendError(target, Component.translatable(result.errorKey(), result.errorArgs()), cfg);
                return;
            }
            String answer = result.content();
            if (answer == null || answer.isBlank()) {
                sendError(target, Component.translatable("atria.err_bad_reply"), cfg);
                return;
            }
            AtriaConversation.rememberUser(playerId, userText, cfg.maxHistoryMessages);
            AtriaConversation.rememberAssistant(playerId, answer, cfg.maxHistoryMessages);
            for (String chunk : splitForChat(answer, Math.max(100, cfg.maxResponseChars))) {
                sendMaybeBroadcast(target, cfg.broadcastReplies, answerComponent(chunk));
            }
        }));
    }

    /**
     * Вызывай из серверного тика (раз в несколько тиков): отправляет
     * накопленные вопросы, когда наступила тишина и истёк период тишины.
     */
    public static void tickFlush() {
        if (QUEUED.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (Map.Entry<UUID, QueuedQuestion> entry : QUEUED.entrySet()) {
            QueuedQuestion queued = entry.getValue();
            if (now - queued.queuedAtMillis() < DEBOUNCE_MS) {
                continue; // игрок ещё «печатает»
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                QUEUED.remove(entry.getKey());
                continue;
            }
            if (PENDING.contains(entry.getKey())) {
                continue; // ждём текущий ответ — очередь уйдёт после него
            }
            if (QUEUED.remove(entry.getKey(), queued)) {
                startAsk(player, queued.text(), now);
            }
        }
    }

    /** Забыть всё про игрока (выход с сервера). */
    public static void forget(UUID playerId) {
        PENDING.remove(playerId);
        QUEUED.remove(playerId);
        LAST_REQUEST.remove(playerId);
        LAST_ERROR_MESSAGE.remove(playerId);
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

    /** Сообщение об ошибке не чаще, чем раз в 5 секунд на игрока. */
    private static void sendError(ServerPlayer player, Component message, AtriaConfig cfg) {
        long now = System.currentTimeMillis();
        Long last = LAST_ERROR_MESSAGE.get(player.getUUID());
        if (last != null && now - last < 5000L) {
            return;
        }
        LAST_ERROR_MESSAGE.put(player.getUUID(), now);
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
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
