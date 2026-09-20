package com.atriadawn.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP-клиент к OpenAI-совместимому Chat Completions API
 * (по умолчанию — официальный эндпоинт Atria Dawn, api.atria-asi.ai).
 *
 * <p>Запрос выполняется асинхронно ({@link HttpClient#sendAsync}),
 * поэтому сервер Minecraft никогда не блокируется ожиданием ответа
 * нейросети. Ответ парсится вручную через Gson, который уже есть
 * в Minecraft, — дополнительных библиотек мод не требует.</p>
 */
public final class AtriaApiClient {

    /**
     * Результат запроса: либо успешный текст {@link #content},
     * либо ключ перевода ошибки {@link #errorKey} с аргументами.
     */
    public record ApiResult(boolean ok, String content, String errorKey, Object[] errorArgs) {

        public static ApiResult success(String text) {
            return new ApiResult(true, text, null, null);
        }

        public static ApiResult failure(String translationKey, Object... args) {
            return new ApiResult(false, null, translationKey, args);
        }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private AtriaApiClient() {
    }

    /**
     * Отправить диалог модели. Возвращает future с ответом или
     * локализуемой ошибкой; вызывать можно с серверного потока —
     * сам вызов асинхронный.
     */
    public static CompletableFuture<ApiResult> chat(AtriaConfig cfg, List<AtriaConversation.Msg> history,
                                                    String playerName, String userText) {
        HttpRequest request;
        try {
            request = buildRequest(cfg, history, playerName, userText);
        } catch (Exception ex) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn: не удалось собрать запрос", ex);
            return CompletableFuture.completedFuture(
                    ApiResult.failure("atria.err_network", describe(ex)));
        }
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(AtriaApiClient::parseResponse)
                .exceptionally(ex -> {
                    Throwable cause = unwrap(ex);
                    if (cause instanceof HttpTimeoutException) {
                        return ApiResult.failure("atria.err_timeout");
                    }
                    AtriaDawnMod.LOGGER.warn("Atria Dawn: ошибка сети при запросе", ex);
                    return ApiResult.failure("atria.err_network", describe(ex));
                });
    }

    private static HttpRequest buildRequest(AtriaConfig cfg, List<AtriaConversation.Msg> history,
                                            String playerName, String userText) {
        String systemPrompt = cfg.systemPrompt == null ? "" : cfg.systemPrompt.replace("%player%", playerName);

        JsonObject payload = new JsonObject();
        payload.addProperty("model", cfg.model);

        JsonArray messages = new JsonArray();
        if (!systemPrompt.isBlank()) {
            messages.add(message("system", systemPrompt));
        }
        for (AtriaConversation.Msg msg : history) {
            messages.add(message(msg.role(), msg.content()));
        }
        messages.add(message("user", userText));
        payload.add("messages", messages);

        payload.addProperty("temperature", cfg.temperature);
        payload.addProperty("max_tokens", cfg.maxTokens);
        payload.addProperty("stream", false);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(cfg.apiUrl))
                .timeout(Duration.ofSeconds(Math.max(10, cfg.requestTimeoutSeconds)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "AtriaDawn-Minecraft-Mod/1.0 (Atria Dawn chat)")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8));
        String key = cfg.resolveApiKey();
        if (!key.isEmpty()) {
            builder.header("Authorization", "Bearer " + key);
        }
        return builder.build();
    }

    private static JsonObject message(String role, String content) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", role);
        obj.addProperty("content", content == null ? "" : content);
        return obj;
    }

    /** Разбор ответа API: HTTP-коды, JSON choices[0].message.content. */
    private static ApiResult parseResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body();

        if (status == 401 || status == 403) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn: API отклонил ключ (HTTP {}): {}", status, snippet(body));
            return ApiResult.failure("atria.err_auth", status);
        }
        if (status == 429) {
            return ApiResult.failure("atria.err_limited", snippet(body));
        }
        if (status < 200 || status >= 300) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn: неожиданный статус HTTP {}: {}", status, snippet(body));
            return ApiResult.failure("atria.err_api", status, snippet(body));
        }

        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return ApiResult.failure("atria.err_bad_reply");
            }
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject message = choice.has("message") && choice.get("message").isJsonObject()
                    ? choice.getAsJsonObject("message") : null;
            String content = null;
            if (message != null && message.has("content") && !message.get("content").isJsonNull()) {
                content = message.get("content").getAsString();
            }
            if (content == null || content.isBlank()) {
                return ApiResult.failure("atria.err_bad_reply");
            }
            return ApiResult.success(content.strip());
        } catch (Exception ex) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn: не удалось разобрать ответ API: {}", snippet(body), ex);
            return ApiResult.failure("atria.err_bad_reply");
        }
    }

    /** Раскрутить цепочку CompletionException → настоящая причина. */
    private static Throwable unwrap(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }

    /** Короткое человекочитаемое описание исключения для чата. */
    private static String describe(Throwable ex) {
        Throwable t = unwrap(ex);
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = t.getClass().getSimpleName();
        }
        msg = msg.replace('\n', ' ').strip();
        return msg.length() > 140 ? msg.substring(0, 137) + "..." : msg;
    }

    private static String snippet(String body) {
        String s = (body == null ? "" : body).replace('\n', ' ').strip();
        return s.length() > 120 ? s.substring(0, 117) + "..." : s;
    }

    // ---- Агентный режим: вызов модели с инструментами (tool use) ----

    /**
     * Результат агентного запроса: сырое сообщение ассистента (с возможными
     * tool_calls) + причина завершения, либо локализуемая ошибка.
     */
    public record AgentResult(boolean ok, JsonObject message, String finishReason,
                              String errorKey, Object[] errorArgs) {

        public static AgentResult success(JsonObject message, String finishReason) {
            return new AgentResult(true, message, finishReason, null, null);
        }

        public static AgentResult failure(String translationKey, Object... args) {
            return new AgentResult(false, null, null, translationKey, args);
        }
    }

    /**
     * Запрос к модели со списком инструментов. Сообщения передаются как есть
     * (JsonArray), чтобы агентный цикл мог возвращать raw-ответы ассистента
     * (включая tool_calls) обратно в историю.
     */
    public static CompletableFuture<AgentResult> agentChat(AtriaConfig cfg, JsonArray messages, JsonArray tools) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", cfg.model);
        payload.add("messages", messages);
        payload.addProperty("temperature", Math.min(cfg.temperature, 0.5D));
        payload.addProperty("max_tokens", Math.max(1024, cfg.maxTokens));
        payload.addProperty("stream", false);
        if (tools != null && tools.size() > 0) {
            payload.add("tools", tools);
            payload.addProperty("tool_choice", "auto");
        }

        HttpRequest request;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(cfg.apiUrl))
                    .timeout(Duration.ofSeconds(Math.max(10, cfg.requestTimeoutSeconds)))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "AtriaDawn-Minecraft-Mod/1.1 (agent loop)")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8));
            String key = cfg.resolveApiKey();
            if (!key.isEmpty()) {
                builder.header("Authorization", "Bearer " + key);
            }
            request = builder.build();
        } catch (Exception ex) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn agent: не удалось собрать запрос", ex);
            return CompletableFuture.completedFuture(
                    AgentResult.failure("atria.err_network", ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(AtriaApiClient::parseAgentResponse)
                .exceptionally(ex -> {
                    Throwable cause = unwrap(ex);
                    if (cause instanceof HttpTimeoutException) {
                        return AgentResult.failure("atria.err_timeout");
                    }
                    AtriaDawnMod.LOGGER.warn("Atria Dawn agent: ошибка сети", ex);
                    return AgentResult.failure("atria.err_network", describe(ex));
                });
    }

    private static AgentResult parseAgentResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body();
        if (status == 401 || status == 403) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn agent: ключ отклонён (HTTP {}): {}", status, snippet(body));
            return AgentResult.failure("atria.err_auth", status);
        }
        if (status == 429) {
            return AgentResult.failure("atria.err_limited", snippet(body));
        }
        if (status < 200 || status >= 300) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn agent: HTTP {}: {}", status, snippet(body));
            return AgentResult.failure("atria.err_api", status, snippet(body));
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return AgentResult.failure("atria.err_bad_reply");
            }
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject message = choice.has("message") && choice.get("message").isJsonObject()
                    ? choice.getAsJsonObject("message") : null;
            if (message == null) {
                return AgentResult.failure("atria.err_bad_reply");
            }
            String finish = choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()
                    ? choice.get("finish_reason").getAsString() : "";
            return AgentResult.success(message, finish);
        } catch (Exception ex) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn agent: не удалось разобрать ответ: {}", snippet(body), ex);
            return AgentResult.failure("atria.err_bad_reply");
        }
    }
}
