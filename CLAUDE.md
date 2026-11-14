# Dread Patch: notes for the next agent

NeoForge 1.21.1 mod (Java 21, ModDevGradle) that adds Ice and Fire CE's unfinished Dread Queen to the game, for the owner's modpack. Hard dependency on Ice and Fire CE; this is the owner's largest mod so far. As of the foundation (0.1.0) it loads and does nothing yet.

## What Ice and Fire already has (2.1-beta.1)

Read Ice and Fire's source on the `1.21.1` branch of [IAFEnvoy/IceAndFire-CE](https://github.com/IAFEnvoy/IceAndFire-CE) (`master` is the 26.1 port). That branch is ahead of the pack's 2.1-beta.1 (its `mod_version` is 2.2, unreleased), so check the jar (`~/.gradle` caches it after a build) when exact behavior matters.

- `com.iafenvoy.iceandfire.entity.DreadQueenEntity extends DreadMobEntity`: complete class. Blue boss bar, attributes in `bakeAttributes()` (max health from Ice and Fire's config `misc.dreadQueenMaxHealth`, default 750; armor 30; follow range 256), melee and target goals, a 40-tick `ANIMATION_SPAWN` (Uranus `IAnimatedEntity`), spawns holding her sword and staff, persistent.
- **Not registered**: there's no `EntityType` in `IafEntities`, no attributes, no spawn placement, no renderer, no model, no spawn egg. That's this mod's job.
- Textures (128×64, the same sheet size as the dread knight and lich): `iceandfire:textures/entity/dread/dread_queen.png`, `_eyes`, `_inactive`, `_injured`. Lang `entity.iceandfire.dread_queen` = "Dread Queen".
- Items, registered: `iceandfire:dread_queen_sword` ("Blade of the Dreaded Queen", tier `IafTiers.DREAD_QUEEN`) and `iceandfire:dread_queen_staff` ("Staff of the Dreaded Queen", a plain `Item` marked "no usage at the moment").
- Dragon hooks: `DragonBaseEntity` (riding tick) and `IafDragonLogic` (hovering) treat a `DreadQueenEntity` controlling passenger specially; `DragonRiderFeatureRenderer` can exclude her. The original design had her riding a dragon.
- The other dread mobs render through hand-built models on `DreadBaseModel` (Uranus `HideableModelRenderer` parts), registered in `IafRenderers`.

## Working here

- **Build:** `./gradlew build`. CI (`build.yml`) runs the same on every PR and push to `main` and uploads the jar, versioned `<mod_version>-dev.<commit>` (`-dev` locally); only the Release workflow builds a plain `<mod_version>` jar.
- **Dependencies:** Ice and Fire, Uranus, Jupiter and GeckoLib come from Modrinth's maven (`maven.modrinth:<slug>:<version id>`), pinned in `gradle.properties` to the exact files the pack ships. Use version ids, not numbers: Uranus 2.4.1 names both a NeoForge and a Fabric file. They're `implementation`, so the dev runs load them as mods, along with the jars bundled inside them (Architectury from Uranus, Integration from Ice and Fire). Optional test-only mods (JEI, Jade) go in `localRuntime`.
- **Dev runs:** `./gradlew runServer` boots to `Done` with all nine mods (the dev environment skips the EULA check); stop it with SIGTERM to the java process, which saves the world. Expected noise: Ice and Fire's `iceandfire:delight/*` recipes fail to parse (they're for Farmer's Delight, absent here), Jupiter reports missing config files on a fresh `run/`, and GeckoLib's refmap warning. Grep the log for `FAILED during APPLY|was not applied` after changes.
- **Network:** a build needs `maven.neoforged.net`, the Mojang hosts, `api.modrinth.com` (+ `cdn.modrinth.com`), `maven.parchmentmc.org` and the foojay JDK download.
- **PRs:** into `main`. Bump `mod_version` in `gradle.properties` with user-visible changes; merging the bump releases it. Keep the README in step with behavior changes.
- **Release:** every push to `main` runs `release.yml`. If `v<mod_version>` isn't released yet, it builds with `RELEASE=true`, creates the tag on that commit and publishes the jar, with notes GitHub generates from the merged PRs (`.github/release-notes.md`, if present, goes above them); otherwise it stops after the check. To retry a failed release, run it by hand on `main` (GitHub MCP `actions_run_trigger`, `release.yml`, ref `main`). It never replaces an existing release. Sessions here can't push tags.
- **Shared CI:** `.github/workflows/` (`build.yml`, `release.yml`, `wiki.yml` where there's a wiki), `.github/dependabot.yml` and `.gitattributes` are identical in all the owner's mod repos (phantom_storage, golemarmor, friendlybeacons, catspells-nosumagro, gvs-noatk-players, dread-patch); phantom_storage's `CLAUDE.md` has the fullest notes. Change them in every repo at once, and keep repo-specific text out of them.
- **Modrinth:** the owner's rules: never publish anything to Modrinth (a version, the description, gallery images) without their consent, and no emoji in Modrinth text.
- **Modpack exports:** the owner drops `.mrpack` exports of their pack in the repo root for reference (gitignored). `modrinth.index.json` inside lists every mod with its Modrinth version id.

## The owner's pack

"The Living World" (1.3.6c, ~200 mods): NeoForge 21.1.265, Ice and Fire CE 2.1-beta.1 plus `iceandfire_create`, Uranus 2.4.1, Jupiter 2.3.7, GeckoLib 4.9.3, Architectury 13.0.12, JEI, Jade, L_Ender's Cataclysm, Iron's Spells. Iris 1.8.14-beta.1 + Sodium 0.8.13 and ImmediatelyFast 1.6.15; per phantom_storage's `CLAUDE.md` the owner plays with Complementary shaders and ImmediatelyFast's screen batching on, which broke screen overlays there. Read a mod's code rather than running downloaded jars when a report only happens in the pack.
