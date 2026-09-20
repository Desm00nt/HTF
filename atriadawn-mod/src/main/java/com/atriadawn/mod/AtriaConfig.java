package com.atriadawn.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Конфиг Atria Dawn — {@code config/atriadawn.json}.
 *
 * <p>Файл создаётся автоматически с настройками по умолчанию при первом
 * обращении (когда игрок впервые пишет боту). После правки файла вручную
 * выполните {@code /atria reload} (нужны права оператора).</p>
 *
 * <p>Эндпоинт по умолчанию — официальный OpenAI-совместимый API модели
 * Atria Dawn Preview (api.atria-asi.ai). Подойдёт любой совместимый сервис
 * (OpenRouter, ollama, llama.cpp server и т.п.) — просто поменяйте apiUrl,
 * model и apiKey.</p>
 */
public final class AtriaConfig {

    /** Официальный Chat Completions эндпоинт Atria Dawn. */
    public static final String DEFAULT_API_URL = "https://api.atria-asi.ai/v1/chat/completions";
    /** Имя модели на "проводе" (регистр важен). */
    public static final String DEFAULT_MODEL = "Atria-Dawn-Preview";

    /** Личность бота. %player% заменяется на ник игрока. */
    public static final String DEFAULT_SYSTEM_PROMPT =
            "Ты — Atria Dawn, дружелюбная ИИ-спутница игроков Minecraft. "
            + "Всегда отвечай на русском языке. Пиши кратко: 1-3 предложения, "
            + "без Markdown, без эмодзи и без переносов строк — твой текст показывается "
            + "в игровом чате Minecraft. Ты отлично знаешь Minecraft (крафты, механики, "
            + "выживание, биомы, мобов) и помогаешь советами. Если спрашивают, кто ты — "
            + "ты нейросетевая модель Atria Dawn. Общайся тепло, живо и с лёгким юмором. "
            + "Игрока зовут %player%.";

    private static final String FILE_NAME = "atriadawn.json";
    private static final Object LOCK = new Object();
    private static volatile AtriaConfig instance;

    // ---- настройки (сохраняются в JSON в порядке объявления) ----

    /** URL Chat Completions эндпоинта. */
    public String apiUrl = DEFAULT_API_URL;
    /** API-ключ (atr_...). Если пусто — берётся из переменной окружения apiKeyEnvVar. */
    public String apiKey = "";
    /** Имя переменной окружения с ключом (запасной вариант). */
    public String apiKeyEnvVar = "ATRIA_API_KEY";
    /** Имя модели. */
    public String model = DEFAULT_MODEL;
    /** Префикс в чате, включающий бота ("@" → "@как скрафтить печь?"). Пустая строка = выключено. */
    public String chatPrefix = "@";
    /** Системный промпт (характер и правила бота). */
    public String systemPrompt = DEFAULT_SYSTEM_PROMPT;
    /** Температура сэмплирования (0 = сухо, 2 = бред). */
    public double temperature = 0.8;
    /** max_tokens для ответа (у Atria допустимо 1..65536). */
    public int maxTokens = 1024;
    /** Таймаут ожидания ответа, секунд. */
    public int requestTimeoutSeconds = 120;
    /** Минимальная пауза между запросами одного игрока, секунд (антиспам). */
    public int cooldownSeconds = 3;
    /** Сколько последних реплик (вопрос+ответ) держать в контексте каждого игрока. */
    public int maxHistoryMessages = 20;
    /** Максимальная длина одного сообщения-ответа в чате (длинные режутся на части). */
    public int maxResponseChars = 700;
    /** Показывать ли сообщение "Atria Dawn печатает…". */
    public boolean showTypingIndicator = true;
    /** true — отвечать всем игрокам на сервере, false — только автору вопроса. */
    public boolean broadcastReplies = false;
    /** true — писать в лог каждый триггер бота (диагностика ложных срабатываний). */
    public boolean logTriggers = false;

    // ---- агент-компаньон ----
    /** Включён ли агентный режим (/atriaagent, /atria summon). */
    public boolean agentEnabled = true;
    /** Максимум шагов (вызовов модели) на одну задачу. */
    public int agentMaxIterations = 12;
    /** Радиус (в блоках от владельца), в котором агенту разрешено действовать. */
    public int agentActionRadius = 48;
    /** Разрешено ли агенту ломать блоки. */
    public boolean agentCanBreakBlocks = true;
    /** Разрешено ли агенту ставить блоки. */
    public boolean agentCanPlaceBlocks = true;
    /** Таймаут всей задачи, секунд. */
    public int agentTaskTimeoutSeconds = 300;
    /** Компаньон сам ест еду из инвентаря при потере здоровья. */
    public boolean agentAutoEat = true;
    /** Телепортироваться к владельцу, если отстала дальше ~40 блоков. */
    public boolean agentStuckTeleport = true;

    private AtriaConfig() {
    }

    /** Текущий конфиг (лениво читает/создаёт файл при первом обращении). */
    public static AtriaConfig get() {
        AtriaConfig local = instance;
        if (local == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = load();
                }
                local = instance;
            }
        }
        return local;
    }

    /** Перечитать config/atriadawn.json с диска (команда /atria reload). */
    public static void reload() {
        synchronized (LOCK) {
            instance = load();
        }
    }

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static AtriaConfig load() {
        AtriaConfig cfg = new AtriaConfig();
        Path file = configPath();
        try {
            if (!Files.exists(file)) {
                cfg.save();
                AtriaDawnMod.LOGGER.info("Atria Dawn: создан конфиг {} — впишите apiKey и выполните /atria reload", file);
                return cfg;
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Object parsed = JsonParser.parseReader(reader);
                if (parsed instanceof JsonObject obj) {
                    cfg.readFrom(obj);
                    cfg.save(); // дописываем отсутствующие поля значениями по умолчанию
                } else {
                    AtriaDawnMod.LOGGER.warn("Atria Dawn: {} не является JSON-объектом, используются значения по умолчанию", file);
                    cfg.save();
                }
            }
            return cfg;
        } catch (Exception ex) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn: не удалось прочитать {}, используются значения по умолчанию", file, ex);
            return cfg;
        }
    }

    private void readFrom(JsonObject obj) {
        apiUrl = readString(obj, "apiUrl", apiUrl);
        apiKey = readString(obj, "apiKey", apiKey);
        apiKeyEnvVar = readString(obj, "apiKeyEnvVar", apiKeyEnvVar);
        model = readString(obj, "model", model);
        chatPrefix = readString(obj, "chatPrefix", chatPrefix);
        systemPrompt = readString(obj, "systemPrompt", systemPrompt);
        temperature = readDouble(obj, "temperature", temperature);
        maxTokens = readInt(obj, "maxTokens", maxTokens);
        requestTimeoutSeconds = readInt(obj, "requestTimeoutSeconds", requestTimeoutSeconds);
        cooldownSeconds = readInt(obj, "cooldownSeconds", cooldownSeconds);
        maxHistoryMessages = readInt(obj, "maxHistoryMessages", maxHistoryMessages);
        maxResponseChars = readInt(obj, "maxResponseChars", maxResponseChars);
        showTypingIndicator = readBool(obj, "showTypingIndicator", showTypingIndicator);
        broadcastReplies = readBool(obj, "broadcastReplies", broadcastReplies);
        logTriggers = readBool(obj, "logTriggers", logTriggers);

        agentEnabled = readBool(obj, "agentEnabled", agentEnabled);
        agentMaxIterations = readInt(obj, "agentMaxIterations", agentMaxIterations);
        agentActionRadius = readInt(obj, "agentActionRadius", agentActionRadius);
        agentCanBreakBlocks = readBool(obj, "agentCanBreakBlocks", agentCanBreakBlocks);
        agentCanPlaceBlocks = readBool(obj, "agentCanPlaceBlocks", agentCanPlaceBlocks);
        agentTaskTimeoutSeconds = readInt(obj, "agentTaskTimeoutSeconds", agentTaskTimeoutSeconds);
        agentAutoEat = readBool(obj, "agentAutoEat", agentAutoEat);
        agentStuckTeleport = readBool(obj, "agentStuckTeleport", agentStuckTeleport);

        agentMaxIterations = Math.max(1, Math.min(40, agentMaxIterations));
        agentActionRadius = Math.max(8, Math.min(128, agentActionRadius));
        agentTaskTimeoutSeconds = Math.max(30, Math.min(1800, agentTaskTimeoutSeconds));

        // sanity-ограничения
        maxTokens = Math.max(1, Math.min(65536, maxTokens));
        requestTimeoutSeconds = Math.max(10, Math.min(600, requestTimeoutSeconds));
        maxHistoryMessages = Math.max(2, Math.min(200, maxHistoryMessages));
        maxResponseChars = Math.max(100, Math.min(4000, maxResponseChars));
        temperature = Math.max(0.0, Math.min(2.0, temperature));
    }

    /** Сохранить текущие значения в config/atriadawn.json. */
    public void save() {
        Path file = configPath();
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            JsonObject obj = new JsonObject();
            obj.addProperty("apiUrl", apiUrl);
            obj.addProperty("apiKey", apiKey);
            obj.addProperty("apiKeyEnvVar", apiKeyEnvVar);
            obj.addProperty("model", model);
            obj.addProperty("chatPrefix", chatPrefix);
            obj.addProperty("systemPrompt", systemPrompt);
            obj.addProperty("temperature", temperature);
            obj.addProperty("maxTokens", maxTokens);
            obj.addProperty("requestTimeoutSeconds", requestTimeoutSeconds);
            obj.addProperty("cooldownSeconds", cooldownSeconds);
            obj.addProperty("maxHistoryMessages", maxHistoryMessages);
            obj.addProperty("maxResponseChars", maxResponseChars);
            obj.addProperty("showTypingIndicator", showTypingIndicator);
            obj.addProperty("broadcastReplies", broadcastReplies);
            obj.addProperty("logTriggers", logTriggers);
            obj.addProperty("agentEnabled", agentEnabled);
            obj.addProperty("agentMaxIterations", agentMaxIterations);
            obj.addProperty("agentActionRadius", agentActionRadius);
            obj.addProperty("agentCanBreakBlocks", agentCanBreakBlocks);
            obj.addProperty("agentCanPlaceBlocks", agentCanPlaceBlocks);
            obj.addProperty("agentTaskTimeoutSeconds", agentTaskTimeoutSeconds);
            obj.addProperty("agentAutoEat", agentAutoEat);
            obj.addProperty("agentStuckTeleport", agentStuckTeleport);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                gson.toJson(obj, writer);
            }
        } catch (IOException ex) {
            AtriaDawnMod.LOGGER.warn("Atria Dawn: не удалось сохранить {}", file, ex);
        }
    }

    /**
     * Ключ авторизации: явный apiKey, иначе переменная окружения apiKeyEnvVar.
     * Пустая строка = ключ не задан.
     */
    public String resolveApiKey() {
        String key = apiKey == null ? "" : apiKey.strip();
        if (!key.isEmpty()) {
            return key;
        }
        if (apiKeyEnvVar != null && !apiKeyEnvVar.isBlank()) {
            try {
                String env = System.getenv(apiKeyEnvVar);
                if (env != null && !env.isBlank()) {
                    return env.strip();
                }
            } catch (SecurityException ignored) {
                // нет доступа к переменным окружения — считаем ключ не заданным
            }
        }
        return "";
    }

    /**
     * Сохранить новый API-ключ (команда {@code /atria key}) и сразу записать
     * конфиг на диск. Так как метод меняет живой singleton, перезагрузка
     * конфига не требуется — следующий же запрос уйдёт с новым ключом.
     */
    public void setApiKey(String key) {
        this.apiKey = key == null ? "" : key.strip();
        save();
    }

    /** Удалить ключ из конфига ({@code /atria key clear}). */
    public void clearApiKey() {
        setApiKey("");
    }

    /** Замаскированный ключ для показа в чате: {@code atr_3f...9c2d}. */
    public String maskedKey() {
        String key = apiKey == null ? "" : apiKey.strip();
        if (key.isEmpty()) {
            return "";
        }
        if (key.length() <= 10) {
            return key.substring(0, 2) + "...";
        }
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }

    // ---- безопасные читатели полей JSON ----

    private static String readString(JsonObject obj, String key, String fallback) {
        try {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsString();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static int readInt(JsonObject obj, String key, int fallback) {
        try {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsInt();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static double readDouble(JsonObject obj, String key, double fallback) {
        try {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsDouble();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static boolean readBool(JsonObject obj, String key, boolean fallback) {
        try {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsBoolean();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
