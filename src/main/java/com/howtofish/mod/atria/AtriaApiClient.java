package com.howtofish.mod.atria;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.howtofish.mod.HowToFishMod;

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
            HowToFishMod.LOGGER.warn("Atria Dawn: не удалось собрать запрос", ex);
            return CompletableFuture.completedFuture(
                    ApiResult.failure("atria.howtofish.err_network", describe(ex)));
        }
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(AtriaApiClient::parseResponse)
                .exceptionally(ex -> {
                    Throwable cause = unwrap(ex);
                    if (cause instanceof HttpTimeoutException) {
                        return ApiResult.failure("atria.howtofish.err_timeout");
                    }
                    HowToFishMod.LOGGER.warn("Atria Dawn: ошибка сети при запросе", ex);
                    return ApiResult.failure("atria.howtofish.err_network", describe(ex));
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
                .header("User-Agent", "HowToFish-Minecraft-Mod/1.0 (Atria Dawn chat)")
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
            HowToFishMod.LOGGER.warn("Atria Dawn: API отклонил ключ (HTTP {}): {}", status, snippet(body));
            return ApiResult.failure("atria.howtofish.err_auth", status);
        }
        if (status == 429) {
            return ApiResult.failure("atria.howtofish.err_limited", snippet(body));
        }
        if (status < 200 || status >= 300) {
            HowToFishMod.LOGGER.warn("Atria Dawn: неожиданный статус HTTP {}: {}", status, snippet(body));
            return ApiResult.failure("atria.howtofish.err_api", status, snippet(body));
        }

        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return ApiResult.failure("atria.howtofish.err_bad_reply");
            }
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject message = choice.has("message") && choice.get("message").isJsonObject()
                    ? choice.getAsJsonObject("message") : null;
            String content = null;
            if (message != null && message.has("content") && !message.get("content").isJsonNull()) {
                content = message.get("content").getAsString();
            }
            if (content == null || content.isBlank()) {
                return ApiResult.failure("atria.howtofish.err_bad_reply");
            }
            return ApiResult.success(content.strip());
        } catch (Exception ex) {
            HowToFishMod.LOGGER.warn("Atria Dawn: не удалось разобрать ответ API: {}", snippet(body), ex);
            return ApiResult.failure("atria.howtofish.err_bad_reply");
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
}
