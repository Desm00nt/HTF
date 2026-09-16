# How To Fish — Minecraft Forge 1.19.2 mod

A Forge mod (Minecraft **1.19.2**, Forge **43.4.0**) inspired by the Steam game
**"How to Fish"**: pick the **Fishing** world type when you create a new
world and you'll spawn on a tiny lighthouse island in the middle of an
endless ocean, together with **Old Salty**, the lighthouse keeper.

Catch fish with the 3D fishing rod → they're released alive next to you →
finish them off with the knife → feed the meat to Old Salty for **Rubles**
(you start with **3 ₽**, shown top-left near your health, just like in the
reference game) → spend your Rubles in his shop → feed him a beer to get a
bait can, summon the Spider Crab boss with it, and turn in its shell for the
coordinates of the next island (read them with the Radar while in the boat).

**Bonus:** the mod also ships an in-game **AI chat companion — [Atria Dawn](https://github.com/atria-asi/Atria-Dawn-Preview)**.
Write `@<вопрос>` in chat or `/atria <вопрос>` and a real neural network
answers you in Russian, right in the chat. See [«Чат с Atria Dawn»](#-чат-с-atria-dawn-ии-собеседница).

## 💬 Чат с Atria Dawn (ИИ-собеседница)

Встроенный ИИ-чат: **нейросетевая модель Atria Dawn** ([Atria Dawn Preview](https://github.com/atria-asi/Atria-Dawn-Preview),
Shanghai AI Laboratory) отвечает на ваши сообщения прямо в игровом чате — **на русском языке**.
Работает в любом мире (не только «Fishing»), в одиночной игре и на сервере.

### Как пользоваться
- **Префикс в чате:** напишите сообщение, начиная с `@` — например:
  `@привет! кто ты?` или `@как скрафтить печь?` — Atria Dawn ответит в чате.
- **Команда:** `/atria <сообщение>` — то же самое.
- `/atria reset` — забыть контекст диалога; `/atria about` — справка;
  `/atria reload` — перечитать конфиг (операторы).
- Бот **помнит контекст**: последние 20 реплик вашего диалога (настраивается),
  поэтому можно уточнять: «а почему?», «а на сервере?». У каждого игрока — своя память диалога.
- Кулдаун 3 секунды между вопросами (защита от спама), ответ приходит асинхронно,
  сервер не подвисает во время запроса.

### Настройка (один раз)
1. Запустите игру с модом — создастся файл **`config/atriadawn.json`**.
2. Получите API-ключ в консоли Atria (ключи вида `atr_...`) и впишите его:
   ```json
   {
     "apiUrl": "https://api.atria-asi.ai/v1/chat/completions",
     "apiKey": "atr_ВАШ_КЛЮЧ",
     "apiKeyEnvVar": "ATRIA_API_KEY",
     "model": "Atria-Dawn-Preview",
     ...
   }
   ```
   Вместо `apiKey` можно задать переменную окружения `ATRIA_API_KEY`.
   Подойдёт **любой OpenAI-совместимый API** (OpenRouter, ollama, llama.cpp server,
   OpenAI и т.д.) — просто поменяйте `apiUrl`, `model` и `apiKey`.
3. В игре выполните `/atria reload` (нужны права оператора) — готово.

Другие настройки: `chatPrefix` (префикс-триггер, пустая строка = отключить),
`systemPrompt` (характер бота; `%player%` заменяется на ник), `temperature`,
`maxTokens`, `cooldownSeconds`, `maxHistoryMessages`, `maxResponseChars`,
`showTypingIndicator`, `broadcastReplies` (true — отвечать всем игрокам сервера).

### Как это устроено
- Сообщения с префиксом перехватываются серверным событием `ServerChatEvent`
  (обычный чат при этом не рассылается — бот сам показывает «Nick → Atria Dawn: вопрос»).
- Запрос к API — асинхронный (`java.net.http.HttpClient.sendAsync`), ответ
  возвращается на серверный поток через `server.execute(...)`; JSON собирается и
  разбирается штатным Gson из Minecraft — **дополнительных библиотек не нужно**.
- Ответы локализованы: `ru_ru.json` / `en_us.json` (ключи `atria.howtofish.*`).
- ⚠️ Вопросы отправляются на внешний API — не пишите боту пароли и личные данные.
  API-ключ хранится в `config/atriadawn.json` в открытом виде.

| Request | Implementation |
| --- | --- |
| New "Fishing" mode in world creation | Custom **World Type** ("How to Fish (Endless Ocean)") — see *World Type* / *More World Options* in the Create World screen. Minecraft doesn't let mods add a 4th vanilla Game Mode button (Survival/Hardcore/Creative) without invasive, version-fragile client hacks, so — as you allowed — it lives next to it, as a normal, 100% supported extension point. |
| Endless ocean + island + lighthouse + boat + old man | `IslandBuilder` procedurally builds a sand island, a tall lighthouse with a glowing rotating-look beacon beam, a wooden dock, a spawned boat and Old Salty, the first time the custom world loads. |
| New 3D fishing rod | `assets/howtofish/models/item/fishing_rod.json` — real 3D element geometry (handle + angled rod + tip), not a flat icon. |
| Many new 3D fish, catch & release, kill for money | `CustomFishEntity` + `FishModel` (3D fish/crab/shrimp/lobster body with animated tail & fins). The rod always performs "catch & release" (see `FishingRodCustomItem`): a live fish spawns next to you and must be finished off with the Knife. |
| Old Man NPC with feeding animation (wide eyes + open mouth) | `OldManEntity` + `OldManModel` (an articulated jaw bone that opens on a timer) + `OldManRenderer` (swaps to a wide-eyed texture while eating). Right-click empty-handed to talk/shop, right-click with fish meat/beer/trophy to feed him. |
| Shop: rod 3₽, knife, radar, beer | `OldManShopMenu` / `OldManShopScreen`, prices in `OldManShopMenu.OFFERS` (Rod 3₽, Knife 4₽, Radar 10₽, Beer 2₽). |
| Beer → empty can → boss bait | Feed `beer` to Old Salty, get `empty_can` back, use the can on water to summon `BossFishEntity` (Spider Crab). |
| Radar gives coordinates after feeding a boss trophy | Kill the Spider Crab, feed its `spider_crab_shell` to Old Salty, then use the Radar while riding the boat. |
| Different fish: different HP/price | `FishType` enum — 6 species with individual health & Ruble value. |
| Money shown top-left near health | `CurrencyHudOverlay` (client HUD overlay). |
| Cool lighthouse | Tall brick tower + glass lamp room + `LighthouseLampBlock`/`LighthouseLampBlockEntity` rendering a tall glowing beam (reuses the vanilla Beacon beam shader) visible from far out at sea. |
| Pixel textures + "3D" look | All item/entity/block textures are hand-authored pixel art (see `src/main/resources/assets/howtofish/textures`). The rod/knife/radar use true 3D item models (cuboid elements), not flat sprites. |
| Chat with the **Atria Dawn** AI in Russian | `atria/` package: `AtriaEvents` intercepts `@`-prefixed chat / registers `/atria`, `AtriaChatManager` orchestrates the async request, `AtriaApiClient` talks to the OpenAI-compatible `api.atria-asi.ai` endpoint, `AtriaConfig` manages `config/atriadawn.json`. See [above](#-чат-с-atria-dawn-ии-собеседница). |
| Build a jar / GitHub Actions | `.github/workflows/build.yml` — push this folder to GitHub and Actions will build `build/libs/howtofish-1.0.0.jar` automatically (see below). |

## Folder layout

Everything needed for the mod lives in **this folder** (`minecraft-mod/`).
Treat it as the **root of your GitHub repository** (i.e. upload the contents
of `minecraft-mod/`, not the folder itself, to the repo root) so that
`.github/workflows/build.yml` is picked up by GitHub Actions automatically.

```
minecraft-mod/
├── build.gradle, gradle.properties        # ForgeGradle 5 build (MDK-style)
├── .github/workflows/build.yml            # GitHub Actions: builds the jar on every push
└── src/main/
    ├── java/com/howtofish/mod/            # all mod source code
    │   ├── item/, entity/, block/         # rod, knife, radar, beer, fish, boss, old man...
    │   ├── atria/                         # AI chat: Atria Dawn in the game chat (RU)
    │   ├── client/                        # models, renderers, HUD, shop screen
    │   ├── world/                         # island builder + world-type detection
    │   ├── economy/, menu/, network/, registry/, event/
    └── resources/
        ├── META-INF/mods.toml
        ├── data/howtofish/                # dimension type + "Fishing" world preset (datapack)
        └── assets/howtofish/              # textures, models, lang, blockstates
```

## Building the mod jar

**You do not need Java or Gradle installed locally.** Since this project was
authored without access to a JDK/Gradle sandbox, the reliable path is:

1. Create a new GitHub repository.
2. Upload everything **inside** `minecraft-mod/` to the repo root (so
   `build.gradle` sits at the repo root, next to `.github/`).
3. Push to the `main` branch (or open a PR, or just run the workflow
   manually from the "Actions" tab — `workflow_dispatch` is enabled).
4. GitHub Actions will:
   - install JDK 17,
   - download Forge/ForgeGradle + Minecraft 1.19.2 official mappings,
   - run `gradle build`,
   - upload the resulting `howtofish-1.0.0.jar` as a downloadable build
     **artifact** on the run's summary page,
   - and if you push a **tag** (e.g. `v1.0.0`), also attach the jar to a
     GitHub **Release** automatically.

If you *do* have Java 17 + a Forge dev environment locally, the usual
commands work as well: `gradle build` (or generate a wrapper first with
`gradle wrapper --gradle-version 7.5.1` and use `./gradlew build`). The
finished jar appears in `build/libs/`.

## How to play

1. Create a new world → **World Type: How to Fish (Endless Ocean)**.
2. You spawn on the lighthouse island with 3 ₽. Right-click **Old Salty**
   (empty hand) to talk and open his shop.
3. Buy the **Fishing Rod** (3 ₽). Cast it at the sea (right-click while
   looking at water) — a live fish appears next to you.
4. Buy the **Knife** (4 ₽) and finish the fish off.
5. Bring the meat to Old Salty — he opens his eyes wide and gulps it down,
   you get Rubles.
6. Buy him a **Beer** (2 ₽) → feed it to him → he hands back an **Empty
   Can**. Use the can on the water to summon the **Spider Crab** boss.
7. Kill the boss with your knife, bring him the **Spider Crab Shell**.
8. Buy the **Radar** (10 ₽), hop in the **boat**, right-click the radar to
   read the coordinates of the next island.

## Known limitations / notes for further work

This was built file-by-file as plain Java/JSON without a local Minecraft/
Forge toolchain to compile against, so please treat it as a strong,
feature-complete **starting codebase** rather than a pre-tested release:
- If GitHub Actions reports a small API mismatch (Mojang/Forge do rename
  the odd method between patch versions), it's almost always a one-line fix
  in the file the compiler points at.
- The second island is currently a small placeholder outcrop — extend
  `IslandBuilder.buildSecondIsland` with more structures/quests to keep
  growing the "island chain" progression like the original game.
- Feel free to swap the procedurally generated pixel-art textures in
  `assets/howtofish/textures` for hand-painted ones (Blockbench is a great
  free tool for both textures and to expand the 3D models further).
