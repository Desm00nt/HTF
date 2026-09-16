# Atria Dawn Chat — Minecraft Forge 1.19.2 mod

Полностью **автономный мод** (никак не зависит от других модов): общайтесь с
нейросетевой моделью **[Atria Dawn](https://github.com/atria-asi/Atria-Dawn-Preview)**
прямо в игровом чате — **на русском языке**. Работает в одиночной игре и на
сервере, в любом мире.

## Возможности

| Способ | Пример |
| --- | --- |
| Префикс в чате | `@как скрафтить печь?` — Atria Dawn ответит в чате |
| Команда | `/atria привет, кто ты?` |
| Сброс контекста | `/atria reset` |
| Справка | `/atria about` |
| Ввод API-ключа в игре | `/atria key atr_ВАШ_КЛЮЧ` |
| Проверка ключа | `/atria key status` (показывается замаскированным) |
| Удаление ключа | `/atria key clear` |
| Перечитать конфиг | `/atria reload` (операторы) |

- **Память диалога:** бот помнит 20 последних реплик (настраивается), у каждого
  игрока — своя история; можно уточнять: «а почему?», «а на сервере?».
- **Кулдаун** 3 секунды между вопросами, индикатор «Atria Dawn печатает...».
- **Асинхронные запросы** — сервер не подвисает во время ответа нейросети;
  длинные ответы аккуратно режутся на части.
- **Локализация:** русский и английский (`assets/atriadawn/lang`).
- Префикс-триггер и всё поведение настраиваются в `config/atriadawn.json`.

## Установка

1. Положите `atriadawn-1.0.0.jar` в папку `mods/` (Minecraft **1.19.2** +
   Forge **43.4.0** и новее в пределах 1.19.x).
2. Запустите игру (в одиночном мире включите читы, чтобы пользоваться
   командами оператора).

## Настройка (один раз)

1. Получите API-ключ в консоли Atria (ключи вида `atr_...`).
2. В игре выполните:
   ```
   /atria key atr_ВАШ_КЛЮЧ
   ```
   Ключ сохранится в `config/atriadawn.json` и применится сразу — перезагрузка
   не нужна. Можно и просто отредактировать файл руками, затем `/atria reload`.
3. Пишите боту: `@привет! кто ты?` или `/atria <сообщение>`.

По умолчанию мод обращается к официальному OpenAI-совместимому API
`https://api.atria-asi.ai/v1/chat/completions` (модель `Atria-Dawn-Preview`).
Подойдёт **любой совместимый сервис** (OpenRouter, ollama, llama.cpp server,
OpenAI и т.п.) — поменяйте `apiUrl`, `model` и `apiKey` в конфиге:

```json
{
    "apiUrl": "https://api.atria-asi.ai/v1/chat/completions",
    "apiKey": "",
    "apiKeyEnvVar": "ATRIA_API_KEY",
    "model": "Atria-Dawn-Preview",
    "chatPrefix": "@",
    "systemPrompt": "Ты — Atria Dawn, дружелюбная ИИ-спутница игроков Minecraft. ...",
    "temperature": 0.8,
    "maxTokens": 1024,
    "requestTimeoutSeconds": 120,
    "cooldownSeconds": 3,
    "maxHistoryMessages": 20,
    "maxResponseChars": 700,
    "showTypingIndicator": true,
    "broadcastReplies": false
}
```

| Параметр | Описание |
| --- | --- |
| `apiUrl` | URL Chat Completions эндпоинта |
| `apiKey` | API-ключ (можно задать и командой `/atria key`) |
| `apiKeyEnvVar` | Переменная окружения с ключом (запасной способ) |
| `chatPrefix` | Префикс-триггер в чате; пустая строка = отключить |
| `systemPrompt` | Характер бота; `%player%` заменяется на ник игрока |
| `temperature`, `maxTokens` | Параметры сэмплирования модели |
| `cooldownSeconds` | Антиспам-пауза между вопросами |
| `maxHistoryMessages` | Сколько реплик диалога помнить |
| `maxResponseChars` | Максимальная длина одного сообщения в чате |
| `broadcastReplies` | `true` — отвечать всем игрокам сервера |

## Сборка jar

**Вручную:** нужен JDK 17 — затем `./gradlew build` (готовый jar появится в
`build/libs/atriadawn-1.0.0.jar`).

**Без локальной сборки:** корневой CI этого репозитория собирает мод вместе с
How To Fish — готовые `atriadawn-1.0.0.jar` и `howtofish-1.0.0.jar` лежат в
артефакте `howtofish-mod` каждой зелёной сборки (вкладка **Actions** → запуск →
**Artifacts**). Кроме того, jar прикреплён к GitHub Release (тег `atriadawn-v1.0.0`).

> Если вынесете мод в отдельный репозиторий — скопируйте **содержимое** этой
> папки в его корень: внутри уже лежит собственный workflow
> `atriadawn-mod/.github/workflows/build.yml`, а хук в корневом `build.gradle`
> монорепозитория сам перестанет срабатывать (у него есть проверка наличия папки).

## Структура

```
atriadawn-mod/
├── build.gradle, gradle.properties      # ForgeGradle 5 (Minecraft 1.19.2)
├── gradlew, gradle/wrapper/             # Gradle wrapper 7.5.1
├── .github/workflows/build.yml          # CI (когда папка — корень своего репозитория)
└── src/main/
    ├── java/com/atriadawn/mod/          # весь код мода (7 классов)
    │   ├── AtriaDawnMod.java            # точка входа (@Mod "atriadawn")
    │   ├── AtriaEvents.java             # перехват чата (@) + регистрация /atria
    │   ├── AtriaCommands.java           # /atria, /atria key, /atria reset ...
    │   ├── AtriaChatManager.java        # кулдауны, «печатает...», доставка ответов
    │   ├── AtriaApiClient.java          # асинхронный HTTP-клиент (java.net.http)
    │   ├── AtriaConversation.java       # память диалога по игрокам
    │   └── AtriaConfig.java             # config/atriadawn.json
    └── resources/
        ├── META-INF/mods.toml, pack.mcmeta
        └── assets/atriadawn/lang/       # ru_ru.json, en_us.json
```

Дополнительных библиотек не требуется — JSON обрабатывается штатным Gson из
Minecraft, HTTP — стандартным `java.net.http` (Java 17).

## Замечания

- ⚠️ Вопросы отправляются на внешний API — не пишите боту пароли и личные
  данные. API-ключ хранится в `config/atriadawn.json` в открытом виде.
- Вводить ключ могут только операторы (уровень прав 2); в одиночной игре
  включите читы в настройках мира.
- `/atria key status` никогда не показывает ключ целиком — только маску
  вида `atr_3f...9c2d`.
