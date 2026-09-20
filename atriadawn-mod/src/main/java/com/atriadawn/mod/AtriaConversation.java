package com.atriadawn.mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кратковременная память диалога: у каждого игрока своя история
 * «вопрос → ответ», которая подставляется в контекст запроса к модели,
 * чтобы Atria Dawn понимала продолжение разговора («а почему?»).
 *
 * <p>История живёт, пока запущен сервер, и очищается при выходе игрока
 * либо по команде {@code /atria reset}. Ограничение размера —
 * {@link AtriaConfig#maxHistoryMessages}.</p>
 */
public final class AtriaConversation {

    /** Одна реплика диалога: role = "user" или "assistant". */
    public record Msg(String role, String content) {
    }

    private static final Map<UUID, Deque<Msg>> HISTORIES = new ConcurrentHashMap<>();

    private AtriaConversation() {
    }

    /** Копия истории игрока (без изменений оригинала). */
    public static List<Msg> snapshot(UUID playerId) {
        Deque<Msg> history = HISTORIES.get(playerId);
        if (history == null) {
            return List.of();
        }
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public static void rememberUser(UUID playerId, String text, int maxMessages) {
        append(playerId, new Msg("user", text), maxMessages);
    }

    public static void rememberAssistant(UUID playerId, String text, int maxMessages) {
        append(playerId, new Msg("assistant", text), maxMessages);
    }

    private static void append(UUID playerId, Msg msg, int maxMessages) {
        Deque<Msg> history = HISTORIES.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(msg);
            while (history.size() > maxMessages) {
                history.pollFirst();
            }
        }
    }

    /** Забыть диалог игрока (/atria reset, выход с сервера). */
    public static void clear(UUID playerId) {
        Deque<Msg> history = HISTORIES.remove(playerId);
        if (history != null) {
            synchronized (history) {
                history.clear();
            }
        }
    }
}
