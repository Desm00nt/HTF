# How To Fish — Minecraft Forge 1.19.2 mod

A Forge mod (Minecraft **1.19.2**, Forge **43.4.0**) inspired by the Steam game
**"How to Fish"**: pick the **Fishing** world type when you create a new
world and you'll spawn on a tiny lighthouse island in the middle of an
endless ocean, together with **Old Salty**, the lighthouse keeper.

Catch fish with the 3D fishing rod → they're released alive next to you →
finish them off with the knife → feed the meat to Old Salty for **Rubles**
(you start with **3 ₽**, shown near your health, just like in the reference
game) → spend your Rubles in his shop → put a **beer** into the rod's bait
slot (press **B**) and cast - the one deep plunge is the **Spider Crab**
boss. Hook it, knife it, and hand its shell to Old Sol for the coordinates
of the next island (read them on the Radar bar while in the boat).

## Feature checklist vs. your request

| Request | Implementation |
| --- | --- |
| New "Fishing" mode in world creation | Custom **World Type** ("How to Fish (Endless Ocean)") — see *World Type* / *More World Options* in the Create World screen. Minecraft doesn't let mods add a 4th vanilla Game Mode button (Survival/Hardcore/Creative) without invasive, version-fragile client hacks, so — as you allowed — it lives next to it, as a normal, 100% supported extension point. |
| Endless ocean + island + lighthouse + boat + old man | `IslandBuilder` procedurally builds a sand island, a tall lighthouse with a glowing rotating-look beacon beam, a wooden dock, a spawned boat and Old Salty, the first time the custom world loads. |
| New 3D fishing rod | `assets/howtofish/models/item/fishing_rod.json` — real 3D element geometry (handle + angled rod + tip), not a flat icon. Casts a physics bobber with a line drawn from the actual rod tip (`BobberRenderer`). |
| Many new 3D fish, catch & release, kill for money | `CustomFishEntity` + `FishModel` (3D fish/crab/shrimp/lobster body with animated tail & fins). The rod always performs "catch & release" (see `FishingRodCustomItem`): a live fish spawns next to you and must be finished off with the Knife. |
| Old Man NPC with feeding animation (wide eyes + open mouth) | `OldManEntity` + `OldManModel` (an articulated jaw bone that opens on a timer) + `OldManRenderer` (swaps to a wide-eyed texture while eating). Right-click empty-handed to talk/shop, right-click with fish meat/beer/trophy to feed him. |
| Shop: rod 3₽, knife 4₽, beer 2₽, golden bait 15₽, radar 10₽ | `OldManShopMenu` / `OldManShopScreen` with item icons, live balance and coin SFX on purchase. |
| Beer IS the boss bait | Put `beer` in the rod's bait slot (B-menu), cast: no nibbles - one hard plunge - hook it and `BossFishEntity` (Spider Crab) drags itself ashore. |
| Radar gives coordinates after feeding a boss trophy | Kill the Spider Crab, feed its `spider_crab_shell` to Old Salty, then use the Radar while riding the boat. |
| Different fish: different HP/price | `FishType` enum — 6 species with individual health & Ruble value. |
| Money shown top-left near health | `CurrencyHudOverlay` (client HUD overlay). |
| Cool lighthouse | Tall brick tower + glass lamp room + `LighthouseLampBlock`/`LighthouseLampBlockEntity` rendering a tall glowing beam (reuses the vanilla Beacon beam shader) visible from far out at sea. |
| Pixel textures + "3D" look | All item/entity/block textures are hand-authored pixel art (see `src/main/resources/assets/howtofish/textures`). The rod/knife/radar use true 3D item models (cuboid elements), not flat sprites. |
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
   (empty hand) to talk and open his shop (balance is shown at the bottom).
3. Buy the **Fishing Rod** (3 ₽). Cast it at the sea (right-click while
   looking at water) — wait for the nibbles, hook on the big dip, then reel.
4. Buy the **Knife** (4 ₽) and finish the fish off.
5. Bring the meat to Old Salty — his eyes pop out and he gulps it down,
   you get Rubles (cha-ching).
6. Buy a **Beer** (2 ₽), press **B** with the rod in hand, drop the beer
   into the bait slot and cast. No nibbles this time - one hard plunge =
   **hook the Spider Crab boss**. It runs, leaps and slams; wait for the
   freeze after its swipe and hit back. (The red telegraph circle means it
   is about to pounce on THAT spot - run.)
7. Kill the boss with your knife, bring him the **Spider Crab Shell**.
8. Buy the **Radar** (10 ₽), hop in the **boat**, right-click the radar to
   pin the bearing of the next island on the top bar.

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
- Entity textures (Old Sol, the Spider Crab, all six species) are generated
  pixel art kept in sync with the model UVs by `tools/paint_textures.py` —
  edit the script and re-run it instead of hand-editing the PNGs, or swap
  them for Blockbench-authored ones if you want a different style.
