# Dread Patch: notes for the next agent

NeoForge 1.21.1 mod (Java 21, ModDevGradle) that adds Ice and Fire CE's unfinished Dread Queen to the game, for the owner's modpack. Hard dependency on Ice and Fire CE; this is the owner's largest mod so far. As of 0.2.0 she's registered as `dreadpatch:dread_queen` with her original model; she doesn't spawn naturally yet and her loot is a placeholder.

## What Ice and Fire already has (2.1-beta.1)

Read Ice and Fire's source on the `1.21.1` branch of [IAFEnvoy/IceAndFire-CE](https://github.com/IAFEnvoy/IceAndFire-CE) (`master` is the 26.1 port). That branch is ahead of the pack's 2.1-beta.1 (its `mod_version` is 2.2, unreleased), so check the jar (`~/.gradle` caches it after a build) when exact behavior matters.

- `com.iafenvoy.iceandfire.entity.DreadQueenEntity extends DreadMobEntity`: complete class. Blue boss bar, attributes in `bakeAttributes()` (max health from Ice and Fire's config `misc.dreadQueenMaxHealth`, default 750; armor 30; follow range 256), melee and target goals, a 40-tick `ANIMATION_SPAWN` (Uranus `IAnimatedEntity`), spawns holding her sword and staff, persistent.
- Ice and Fire never registers her: no `EntityType` in `IafEntities`, no attributes, spawn placement, renderer, model or spawn egg. Checked against the 2.1-beta.1 jar (`javap`, JDK at `~/.jdks/java21`): `IafEntities` registers eight dread types (`dread_beast` … `dread_thrall`, `dread_lich_skull`) and no queen.
- Textures (128×64, the same sheet size as the dread knight and lich): `iceandfire:textures/entity/dread/dread_queen.png`, `_eyes`, `_inactive`, `_injured`. Lang `entity.iceandfire.dread_queen` = "Dread Queen" in 15 languages (unused by us: our entity's key is `entity.dreadpatch.dread_queen`).
- Items, registered: `iceandfire:dread_queen_sword` ("Blade of the Dreaded Queen", tier `IafTiers.DREAD_QUEEN`) and `iceandfire:dread_queen_staff` ("Staff of the Dreaded Queen", a plain `Item` marked "no usage at the moment").
- Dragon hooks: `DragonBaseEntity` (riding tick) and `IafDragonLogic` (hovering) treat a `DreadQueenEntity` controlling passenger specially; `DragonRiderFeatureRenderer` can exclude her. The comment there ("Original logic involves riding") suggests she was meant to ride one. They test `instanceof DreadQueenEntity`, so they work with our registration.
- **Her animation never advances**: unlike `DreadLichEntity`, she never calls `AnimationHandler.INSTANCE.updateAnimations(this)`, so after `finalizeSpawn` sets `ANIMATION_SPAWN` it stays at tick 0. Don't drive rendering from `getAnimation()` until that's fixed (in a subclass of hers).
- Ice and Fire tags its dread mobs `#minecraft:undead` (what makes a mob undead in 1.21.1: Smite, healing from Harming) and `#minecraft:freeze_immune_entity_types`.
- The other dread mobs render through hand-built models on `DreadBaseModel` (Uranus `HideableModelRenderer` parts), registered in `IafRenderers`. **Those can't be reused from here**: `DreadBaseModel` is package-private, and its public parent `BipedBaseModel` declares a package-private abstract `animate(...)`, so nothing outside `com.iafenvoy.iceandfire.render.model` can subclass either (and NeoForge won't load two mods sharing a package).

## What this mod adds

| Where | What |
|---|---|
| `registry/ModRegistries` | `dread_queen`: an `EntityType` for Ice and Fire's own `DreadQueenEntity` (0.6 × 1.95, the zombie's box; client tracking range 10, the Wither's); `dread_queen_spawn_egg` (`DeferredSpawnEggItem`, Ice and Fire's dread egg base `#E0E6E6` spotted with her eye glow `#00CBFF`) |
| `DreadPatch` | Her attributes (Ice and Fire's `bakeAttributes()`), the egg in the vanilla Spawn Eggs tab |
| `client/DreadQueenModel` | Ice and Fire's unused `ModelDreadQueen` (LGPL-3.0, branch 1.18.2 of AlexModGuy/Ice_and_Fire) rebuilt as a vanilla `HumanoidModel` layer: same boxes and texture offsets; extras (headware, chestplate, necklace, cloak, sleeves, robe) hang off the vanilla parts; empty `hat` because `HumanoidModel` requires one |
| `client/DreadQueenRenderer` | `HumanoidMobRenderer` (held sword and staff come with it) with Ice and Fire's texture and an `EyesLayer` for `dread_queen_eyes.png`. `_inactive` and `_injured` aren't used yet |
| `data/dreadpatch/loot_table/entities/dread_queen.json` | Placeholder until the owner decides: 8–16 dread shards and 2–6 bones (+ looting). Her held sword and staff drop at vanilla's 8.5% equipment chance; her XP is the `Monster` default |
| `data/.../tags/entity_type/` | `#minecraft:undead`, `#minecraft:freeze_immune_entity_types` (like Ice and Fire's dread mobs) and `#c:bosses` (other mods use it to treat her as a boss, e.g. not capturable) |

## Testing

- **Unit tests** (`./gradlew test`, part of `build`): `DreadQueenModelTest` bakes her model layer and builds the `HumanoidModel` from it. A missing or misnamed part otherwise only crashes a client while it loads, which neither the build nor a server boot reaches. Game classes are on the test classpath (`addModdingDependenciesTo(sourceSets.test)` in `build.gradle`), but there's no running game: only test code that needs none.
- **Dev server smoke test**: `tools/devserver/queen_smoke.py` (Python 3, stdlib) drives a running `./gradlew runServer` over RCON: summon, health/armor/held items, tags, 100 ticks of AI, loot, death, and the spawn egg fired from a dispenser. Needs `enable-rcon=true`, `rcon.password=<anything>` and `server-ip=127.0.0.1` in `run/server.properties` (gitignored; set on the owner's machine already). It stops the server when done (`--keep` to leave it up).
- **Client**: rendering has no automated check. On the owner's machine `./gradlew runClient` opens a window on their desktop (there's no Xvfb), so ask before launching it.

## Working here

- **Build:** `./gradlew build`. CI (`build.yml`) runs the same on every PR and push to `main` and uploads the jar, versioned `<mod_version>-dev.<commit>` (`-dev` locally); only the Release workflow builds a plain `<mod_version>` jar.
- **Dependencies:** Ice and Fire, Uranus, Jupiter and GeckoLib come from Modrinth's maven (`maven.modrinth:<slug>:<version id>`), pinned in `gradle.properties` to the exact files the pack ships. Use version ids, not numbers: Uranus 2.4.1 names both a NeoForge and a Fabric file. They're `implementation`, so the dev runs load them as mods, along with the jars bundled inside them (Architectury from Uranus, Integration from Ice and Fire). Optional test-only mods (JEI, Jade) go in `localRuntime`.
- **Dev runs:** `./gradlew runServer` boots to `Done` with all nine mods (the dev environment skips the EULA check); stop it with `stop` over RCON, or SIGTERM to the java process; both save the world. Expected noise: Ice and Fire's `iceandfire:delight/*` recipes fail to parse (they're for Farmer's Delight, absent here), Jupiter reports missing config files on a fresh `run/`, and GeckoLib's refmap warning. Grep the log for `FAILED during APPLY|was not applied` after changes.
- **Network:** a build needs `maven.neoforged.net`, the Mojang hosts, `api.modrinth.com` (+ `cdn.modrinth.com`), `maven.parchmentmc.org` and the foojay JDK download.
- **PRs:** into `main`. Bump `mod_version` in `gradle.properties` with user-visible changes; merging the bump releases it. Keep the README in step with behavior changes.
- **Release:** every push to `main` runs `release.yml`. If `v<mod_version>` isn't released yet, it builds with `RELEASE=true`, creates the tag on that commit and publishes the jar, with notes GitHub generates from the merged PRs (`.github/release-notes.md`, if present, goes above them); otherwise it stops after the check. To retry a failed release, run it by hand on `main` (GitHub MCP `actions_run_trigger`, `release.yml`, ref `main`). It never replaces an existing release. Sessions here can't push tags.
- **Shared CI:** `.github/workflows/` (`build.yml`, `release.yml`, `wiki.yml` where there's a wiki), `.github/dependabot.yml` and `.gitattributes` are identical in all the owner's mod repos (phantom_storage, golemarmor, friendlybeacons, catspells-nosumagro, gvs-noatk-players, dread-patch); phantom_storage's `CLAUDE.md` has the fullest notes. Change them in every repo at once, and keep repo-specific text out of them.
- **Modrinth:** the owner's rules: never publish anything to Modrinth (a version, the description, gallery images) without their consent, and no emoji in Modrinth text.
- **Modpack exports:** the owner drops `.mrpack` exports of their pack in the repo root for reference (gitignored). `modrinth.index.json` inside lists every mod with its Modrinth version id.

## The owner's pack

"The Living World" (1.3.6c, ~200 mods): NeoForge 21.1.265, Ice and Fire CE 2.1-beta.1 plus `iceandfire_create`, Uranus 2.4.1, Jupiter 2.3.7, GeckoLib 4.9.3, Architectury 13.0.12, JEI, Jade, L_Ender's Cataclysm, Iron's Spells. Iris 1.8.14-beta.1 + Sodium 0.8.13 and ImmediatelyFast 1.6.15; per phantom_storage's `CLAUDE.md` the owner plays with Complementary shaders and ImmediatelyFast's screen batching on, which broke screen overlays there. Read a mod's code rather than running downloaded jars when a report only happens in the pack.
