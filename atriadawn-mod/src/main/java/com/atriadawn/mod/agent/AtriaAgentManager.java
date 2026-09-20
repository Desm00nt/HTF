package com.atriadawn.mod.agent;

import com.atriadawn.mod.AtriaApiClient;
import com.atriadawn.mod.AtriaConfig;
import com.atriadawn.mod.AtriaDawnMod;
import com.atriadawn.mod.entity.AtriaCompanionEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Агентный цикл: превращает текстовую задачу игрока в последовательность
 * вызовов инструментов ({@link AtriaTools}) через модель Atria Dawn.
 *
 * <p>Поддерживаются оба режима работы модели: нативный tool use
 * (response_message.tool_calls, формат OpenAI) и текстовый fallback —
 * модель отвечает строго JSON-объектом {@code {"tool": ..., "args": ...}},
 * который парсится из content. После каждого действия результат уходит
 * модели, и цикл повторяется до вызова {@code finish}, лимита итераций
 * или таймаута задачи. Всё выполнение — асинхронно для сервера: ожидание
 * действий компаньона (ходьба/добыча/бой) через CompletableFuture.</p>
 */
public final class AtriaAgentManager {

    private static final Map<UUID, AtriaCompanionEntity> COMPANIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private AtriaAgentManager() {
    }

    // ---- реестр компаньонов ----

    public static void register(AtriaCompanionEntity entity) {
        if (entity.getOwnerUuid() != null) {
            COMPANIONS.put(entity.getOwnerUuid(), entity);
        }
    }

    public static AtriaCompanionEntity getCompanion(UUID ownerUuid) {
        AtriaCompanionEntity c = COMPANIONS.get(ownerUuid);
        return c != null && c.isAlive() && !c.isRemoved() ? c : null;
    }

    public static void onCompanionRemoved(AtriaCompanionEntity entity) {
        if (entity.getOwnerUuid() != null) {
            Session session = SESSIONS.get(entity.getOwnerUuid());
            if (session != null && session.companion == entity) {
                cancelSession(entity.getOwnerUuid());
            }
            COMPANIONS.remove(entity.getOwnerUuid(), entity);
        }
    }

    // ---- сессия задачи ----

    private static final class Session {
        final AtriaCompanionEntity companion;
        final UUID ownerUuid;
        final JsonArray messages = new JsonArray();
        final long startedAtMillis = System.currentTimeMillis();
        volatile boolean cancelled = false;
        int iterations = 0;

        Session(AtriaCompanionEntity companion, UUID ownerUuid) {
            this.companion = companion;
            this.ownerUuid = ownerUuid;
        }

        /** Свежая ссылка на владельца (null, если вышел с сервера). */
        ServerPlayer owner() {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            return server == null ? null : server.getPlayerList().getPlayer(ownerUuid);
        }
    }

    public static boolean isRunning(UUID ownerUuid) {
        return SESSIONS.containsKey(ownerUuid);
    }

    /** Запустить задачу (команда /atriaagent). Вызывать с серверного потока. */
    public static void startTask(ServerPlayer owner, String task) {
        AtriaConfig cfg = AtriaConfig.get();
        if (!cfg.agentEnabled) {
            owner.sendSystemMessage(Component.translatable("atria.agent_disabled").withStyle(ChatFormatting.RED));
            return;
        }
        if (task == null || task.isBlank()) {
            owner.sendSystemMessage(Component.translatable("atria.agent_empty_task").withStyle(ChatFormatting.YELLOW));
            return;
        }
        AtriaCompanionEntity companion = getCompanion(owner.getUUID());
        if (companion == null) {
            owner.sendSystemMessage(Component.translatable("atria.agent_none").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (SESSIONS.containsKey(owner.getUUID())) {
            owner.sendSystemMessage(Component.translatable("atria.agent_running").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (cfg.resolveApiKey().isEmpty()) {
            owner.sendSystemMessage(Component.translatable("atria.err_no_key").withStyle(ChatFormatting.RED));
            return;
        }

        Session session = new Session(companion, owner.getUUID());
        SESSIONS.put(owner.getUUID(), session);

        session.messages.add(systemMessage(buildSystemPrompt(cfg)));
        session.messages.add(userMessage("ЗАДАЧА от игрока " + session.companion.getOwnerUuid()
                + ": " + task.strip() + "\nТвоё текущее состояние: " + AtriaTools.execute(companion, "get_status", null).join()));
        owner.sendSystemMessage(Component.translatable("atria.agent_started").withStyle(ChatFormatting.DARK_AQUA));
        iterate(session);
    }

    /** Остановить текущую задачу (/atriaagent stop). */
    public static void cancelSession(UUID ownerUuid) {
        Session session = SESSIONS.remove(ownerUuid);
        if (session != null) {
            session.cancelled = true;
            session.companion.cancelAction();
            ServerPlayer owner = ServerLifecycleHooks.getCurrentServer() != null
                    ? ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(ownerUuid) : null;
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("atria.agent_stopped").withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    // ---- цикл ----

    private static void iterate(Session session) {
        AtriaConfig cfg = AtriaConfig.get();
        if (session.cancelled || session.owner() == null) {
            SESSIONS.remove(session.ownerUuid);
            return;
        }
        long elapsed = System.currentTimeMillis() - session.startedAtMillis;
        if (elapsed > cfg.agentTaskTimeoutSeconds * 1000L) {
            finish(session, Component.translatable("atria.agent_failed_timeout").withStyle(ChatFormatting.RED));
            return;
        }
        if (session.iterations >= cfg.agentMaxIterations) {
            finish(session, Component.translatable("atria.agent_failed_limit").withStyle(ChatFormatting.RED));
            return;
        }
        session.iterations++;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        AtriaApiClient.agentChat(cfg, session.messages, AtriaTools.buildToolSpecs())
                .thenAccept(result -> server.execute(() -> processResponse(session, result)));
    }

    private static void processResponse(Session session, AtriaApiClient.AgentResult result) {
        if (session.cancelled || SESSIONS.get(session.ownerUuid) != session) {
            return;
        }
        if (!result.ok()) {
            finish(session, Component.translatable(result.errorKey(), result.errorArgs()).withStyle(ChatFormatting.RED));
            return;
        }
        JsonObject message = result.message();

        // 1) Нативный tool use (OpenAI format)
        if (message.has("tool_calls") && message.get("tool_calls").isJsonArray()
                && message.getAsJsonArray("tool_calls").size() > 0) {
            session.messages.add(message); // ассистента с tool_calls возвращаем как есть
            JsonArray toolCalls = message.getAsJsonArray("tool_calls");
            java.util.List<java.util.concurrent.CompletableFuture<String>> futures = new java.util.ArrayList<>();
            java.util.List<String> callIds = new java.util.ArrayList<>();
            for (int i = 0; i < toolCalls.size(); i++) {
                JsonObject call = toolCalls.get(i).getAsJsonObject();
                String callId = call.has("id") ? call.get("id").getAsString() : "call_" + i;
                String name = "";
                JsonObject args = new JsonObject();
                try {
                    JsonObject function = call.getAsJsonObject("function");
                    name = function.get("name").getAsString();
                    if (function.has("arguments") && function.get("arguments").isJsonPrimitive()) {
                        JsonObject parsed = JsonParser.parseString(function.get("arguments").getAsString()).getAsJsonObject();
                        args = parsed;
                    }
                } catch (Exception ex) {
                    AtriaDawnMod.LOGGER.warn("Atria agent: некорректный tool_call", ex);
                }
                callIds.add(callId);
                AtriaDawnMod.LOGGER.info("Atria agent[{}]: tool '{}' {}", session.companion.getOwnerUuid(), name, args);
                if ("finish".equals(name)) {
                    futures.add(java.util.concurrent.CompletableFuture.completedFuture(
                            "FINISH: " + safeArg(args, "summary")));
                } else {
                    futures.add(AtriaTools.execute(session.companion, name, args));
                }
            }
            java.util.concurrent.CompletableFuture
                    .allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .thenRun(() -> MinecraftServerGetter.server().execute(() -> {
                        if (session.cancelled || SESSIONS.get(session.ownerUuid) != session) {
                            return;
                        }
                        for (int i = 0; i < callIds.size(); i++) {
                            String outcome = futures.get(i).getNow("нет данных");
                            if (outcome.startsWith("FINISH:")) {
                                finish(session,
                                        Component.translatable("atria.agent_finished", outcome.substring(7).strip())
                                                .withStyle(ChatFormatting.GREEN));
                                return;
                            }
                            session.messages.add(toolMessage(callIds.get(i), outcome));
                        }
                        iterate(session);
                    }));
            return;
        }

        // 2) Текстовый fallback: {"tool": ..., "args": {...}} внутри content
        String content = message.has("content") && !message.get("content").isJsonNull()
                ? message.get("content").getAsString() : "";
        JsonObject inline = extractJsonObject(content);
        if (inline != null && inline.has("tool") && inline.get("tool").isJsonPrimitive()) {
            session.messages.add(message);
            String name = inline.get("tool").getAsString();
            JsonObject args = inline.has("args") && inline.get("args").isJsonObject()
                    ? inline.getAsJsonObject("args") : new JsonObject();
            AtriaDawnMod.LOGGER.info("Atria agent[{}]: inline tool '{}' {}", session.companion.getOwnerUuid(), name, args);
            java.util.concurrent.CompletableFuture<String> future = "finish".equals(name)
                    ? java.util.concurrent.CompletableFuture.completedFuture("FINISH: " + safeArg(args, "summary"))
                    : AtriaTools.execute(session.companion, name, args);
            future.thenRun(() -> MinecraftServerGetter.server().execute(() -> {
                if (session.cancelled || SESSIONS.get(session.ownerUuid) != session) {
                    return;
                }
                String outcome = future.getNow("нет данных");
                if (outcome.startsWith("FINISH:")) {
                    finish(session, Component.translatable("atria.agent_finished", outcome.substring(7).strip())
                            .withStyle(ChatFormatting.GREEN));
                    return;
                }
                session.messages.add(userMessage("РЕЗУЛЬТАТ ИНСТРУМЕНТА '" + name + "': " + outcome
                        + "\nПродолжай выполнение задачи. Ответь следующим действием (JSON) или вызови finish."));
                iterate(session);
            }));
            return;
        }

        // 3) Обычный текст — считаем итоговым ответом
        if (!content.isBlank()) {
            session.companion.say(content.strip());
        }
        finish(session, Component.translatable("atria.agent_finished",
                content.isBlank() ? "модель закончила без итога" : content.strip()).withStyle(ChatFormatting.GREEN));
    }

    private static void finish(Session session, Component finalMessage) {
        SESSIONS.remove(session.ownerUuid);
        session.companion.cancelAction();
        ServerPlayer owner = session.owner();
        if (owner != null) {
            owner.sendSystemMessage(finalMessage);
        }
    }

    // ---- сообщения и парсинг ----

    private static JsonObject systemMessage(String text) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", "system");
        obj.addProperty("content", text);
        return obj;
    }

    private static JsonObject userMessage(String text) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", "user");
        obj.addProperty("content", text);
        return obj;
    }

    private static JsonObject toolMessage(String callId, String content) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", "tool");
        obj.addProperty("tool_call_id", callId);
        obj.addProperty("content", content);
        return obj;
    }

    private static String buildSystemPrompt(AtriaConfig cfg) {
        return "Ты — Atria Dawn, ИИ-компаньон в мире Minecraft. У тебя есть физическое тело-аватар "
                + "в мире, которым ты управляешь через инструменты. Выполняй задачу игрока пошагово.\n"
                + "ПРАВИЛА ОТВЕТА:\n"
                + "1. Если API поддерживает вызовы функций — вызывай инструменты напрямую.\n"
                + "2. Иначе отвечай СТРОГО одним JSON-объектом без какого-либо другого текста: "
                + "{\"tool\": \"<имя>\", \"args\": {\"параметр\": значение}}\n"
                + "3. Никогда не выдумывай результаты действий — результат каждого инструмента приходит следующим сообщением.\n"
                + "4. Когда задача выполнена — вызови {\"tool\": \"finish\", \"args\": {\"summary\": \"краткий итог\"}}.\n"
                + "5. Действуй по шагам: сначала scan_area (осмотреться), затем walk_to рядом, затем действие. "
                + "Координаты — целые числа x,y,z. Точка place_block должна быть в 5 блоках от тела.\n"
                + "6. Не ломай сундуки и блоки рядом с игроком. Добывай только то, что нужно для задачи.\n"
                + "Лимит шагов: " + cfg.agentMaxIterations + ". Планируй экономно.";
    }

    /** Вытащить первый сбалансированный JSON-объект из текста (fallback-режим). */
    static JsonObject extractJsonObject(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        while (start >= 0) {
            int depth = 0;
            boolean inString = false;
            for (int i = start; i < text.length(); i++) {
                char c = text.charAt(i);
                if (inString) {
                    if (c == '\\') {
                        i++;
                    } else if (c == '"') {
                        inString = false;
                    }
                    continue;
                }
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        try {
                            return JsonParser.parseString(text.substring(start, i + 1)).getAsJsonObject();
                        } catch (Exception ex) {
                            break;
                        }
                    }
                }
            }
            start = text.indexOf('{', start + 1);
        }
        return null;
    }

    private static String safeArg(JsonObject args, String key) {
        try {
            if (args != null && args.has(key) && args.get(key).isJsonPrimitive()) {
                return args.get(key).getAsString();
            }
        } catch (Exception ignored) {
        }
        return "задача выполнена";
    }

    public static MutableComponent agentLine(String text) {
        return Component.literal("Atria Dawn").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
                .append(Component.literal(" \u00bb ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    /** Обёртка доступа к серверу внутри цепочек. */
    private static final class MinecraftServerGetter {
        static MinecraftServer server() {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                throw new IllegalStateException("Server stopped");
            }
            return server;
        }
    }
}
