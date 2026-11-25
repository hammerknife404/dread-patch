# Dread Patch: notes for the next agent

NeoForge 1.21.1 mod (Java 21, ModDevGradle) that adds Ice and Fire CE's unfinished Dread Queen to the game, with the Rimefrost (undead frost dragons of the Dread) and her own, Syndrigossa, for the owner's modpack. Hard dependencies on Ice and Fire CE and Iron's Spells 'n Spellbooks. It's the owner's largest mod so far.

This file holds the rules and the shape of the code. How each part works, what Ice and Fire already does, the owner's decisions and the test notes are in `docs/dev/` (index below): open the page for what you're changing before you change it.

## Where things stand

- **Version:** `mod_version` in `gradle.properties`.
- **Done:** the big drop (finished with 0.48.0) and the second (finished with 0.54.0; 0.54.1 paced its ice cracks). Paused there on 2026-11-22 at the owner's word; 0.55.0–0.59.0 came after, at the owner's requests. The Rimefrost dragonforge shipped in 0.59.0.
- **Left for later, the owner's call:** ready-built forges on some spires.
- **The owner's feature dump of 2026-11-23** (`Feature_Dump_11-23.md`, local, written by another assistant against 0.57.0), being built on `beta` (below, PRs) in the owner's order: 0.60.0 the music and quick fixes; 0.61.0 the Dreadlands basics (random arrival, dreadwood saplings, stray undead); 0.62.0 the post-defeat escalation; 0.63.0 risen Dread (miniature phantoms, water creatures; also stirring in caves); 0.64.0 the owner's notes from the dev client (sound filters, dread lights at 6, her fire, the crown); 0.65.0 dreadwood's block set and dread lights' cold; 0.66.0 the terrain's land (three biomes, caves, trenches); 0.66.1 her storm putting fire out every tick; 0.67.0 the sky (sunlight at most 6, a dim cyan sun) and the plains' grave patches. Still to do: Guard Villagers rising in her storm with their gear (the owner: "we also need to test against guard villagers"; add the pack's Guard Villagers as a test-only `localRuntime` and a smoke check); the owner's `Feature_Dump_11-24.md` (local, unread when this was written); and all of 0.60.0-0.67.0 in a client (`testing.md`). The owner's answers and notes for each are in `decisions.md`. Already done, so not to build: its forge gating (0.59.0), ghost villages (0.53.0), the shelter rule (any roof already shelters).
- **Owed:** the owner's own look at 0.52.0–0.54.1, 0.59.0 and 0.60.0's music in a client ([testing.md](docs/dev/testing.md), Client).
- The queen's only natural spawning is her encounter atop the Dreadlands' spires (0.47.0). Player riders sit properly on Ice and Fire's dragons since 0.49.0 (wanted for a public release).

## Keep in step

- **README:** it describes everything as a player sees it.
- **Wiki (since 2026-11-22):** `docs/wiki/` (one page per README part, plus Home, `_Sidebar`, Commands and a Settings table of every server config value), mirrored to the GitHub wiki by `.github/workflows/wiki.yml` (the owner's shared copy, catspells-nosumagro's) on each push to `main` that touches it. Edit pages there, never on github.com (the sync overwrites the wiki). Keep it in step with the README, and add any new config value to Settings.
- **`docs/dev/`:** change a part's page with its code. A new decision of the owner's goes in `decisions.md`, with its version. Keep feature detail out of this file. These pages aren't in `docs/wiki/`, so they never reach the wiki.

## The notes (`docs/dev/`)

| Page | What's in it |
|---|---|
| [decisions.md](docs/dev/decisions.md) | The owner's decisions, by topic and version, quoted where they gave words. **Read the entry before changing how something behaves.** |
| [ice-and-fire.md](docs/dev/ice-and-fire.md) | What Ice and Fire CE 2.1-beta.1 already has: its unregistered queen, its dragons (stats, age, sleep, targeting, riding, breath, attacks, corpses), its dragonforge, the Dread Lich, the Mausoleum and key, necromancy. |
| [queen.md](docs/dev/queen.md) | `DreadQueen`: casting, movement and Barrow-Step's spot, Withering Rime and the siphon, Rimewalk, her court, decree and lich, scuttler climbing and bolts, her model, her staff and sword (Better Combat), mounting Syndrigossa and getting down, her loot table. |
| [spells.md](docs/dev/spells.md) | The Rimefrost school and her spells: Rimefire Blast, the bolt, the crown, Barrow-Step and its husk, Summon Court, Create Rimefrost Dragon, Whiteout Dominion (its storm, raising the dead, its ice, how it's drawn). |
| [dragons.md](docs/dev/dragons.md) | Rimefrost dragons and Syndrigossa (skin, age, health, undeath, corpse, breath, Shatter-Spine, Sovereign's Eclipse, animation), riders (`SaddleLayer`), her egg, dragon corpses, the Rimefrost dragonforge. |
| [phantom.md](docs/dev/phantom.md) | The dread phantom: its flight, targets, bite, calling, Summon Dread Phantoms, drops, sounds, skin, trail. |
| [gear.md](docs/dev/gear.md) | Rimefrost dragonscale and dread dragonsteel, her regalia (model, drops, advancements, powers, crown sight), armour trims. |
| [dreadlands.md](docs/dev/dreadlands.md) | The Dread Gate and key; the Dreadlands: dimension, terrain, spires, arrival and home, flames, villages, Lootr, spawning and patrols, the summit encounter, storms and rimefire, the whiteout weather, ambience; `/dreadpatch` commands. |
| [testing.md](docs/dev/testing.md) | The unit tests, what the smoke test covers and what it learned, what needs a player, the owner's client checks. |

Design proposals: the owner's two chats with another assistant, `proposed_stuff_1.txt` and `proposed_stuff_2.txt` (local only). Their numbers are that assistant's, and some of their "facts" about the game are invented. Only what `decisions.md` records holds.

## How the code is built

**Layout:** everything is under `io.github.hammerknife404.dreadpatch`: `block`, `client`, `command`, `compat/jei`, `effect`, `entity`, `item`, `loot`, `registry`, `spell`, `world`, plus `DreadPatch` (attributes, setup) and `DreadPatchConfig`. `magic/` holds the rules that don't need a running game, each with a unit test; put new game-free logic there.

**Changing other mods from outside:** no mixins. Ice and Fire's and Iron's behaviour is changed through events, subclasses where their classes allow, and default components (`ModifyDefaultComponentsEvent`, so stacks already made get them too: a stack stores only changes). Code that has to run on Ice and Fire's own entities hangs off `EntityTickEvent` / `EntityJoinLevelEvent` (goals aren't saved, so they're re-set on every join).

**Ice and Fire:**
- Read its source on the `1.21.1` branch of [IAFEnvoy/IceAndFire-CE](https://github.com/IAFEnvoy/IceAndFire-CE) (`master` is the 26.1 port). That branch is ahead of the pack's 2.1-beta.1 (its `mod_version` is 2.2, unreleased), so when exact behaviour matters, check the jar: `~/.gradle` caches it after a build; use `javap` from the JDK at `~/.jdks/java21`.
- Its `instanceof` checks count our subclasses: our `DreadQueen` is its `DreadQueenEntity`, our `RimefrostDragon` its `IceDragonEntity` (so its ice forge drew ours).
- Its tags don't carry to a subclass's own entity type: tag ours ourselves.
- Its dread mobs' models can't be reused: `DreadBaseModel` and an abstract method of its parent are package-private, and NeoForge won't load two mods that share a package.
- Don't add a `DragonColor`: its constructor adds it to every dragon of its type, and armour gets made for it.
- An attribute it adds only to its own dragon types (`dragon_forge_speed`) must be added to ours in `DreadPatch`: reading one an entity lacks throws, and once crashed the dev server's tick.

**Dread lights:** none brighter than 6 (`registry/DreadLights.LIGHT`, the owner's); give a new one that.

**Dev only:** `DevKit` gives a player joining a dev run enchanted dread dragonsteel, an enchanted diamond sword and a hydra heart, once; it registers only when `FMLEnvironment.production` is false, so never in a built jar.

**Spawning:** mobs we raise go through `EventHooks.finalizeMobSpawn`, so they get their own rise and gear and other mods see the spawn.

**Network:** one payload so far (`world/DreadlandsMusic.FirstArrival`, 0.60.0), registered in common code on `RegisterPayloadHandlersEvent`, its handler a lambda that calls into a client class (only a client ever runs it, so a dedicated server never loads that class). Do the same for another.

**Assets:** nothing of Ice and Fire's, Iron's or vanilla's ships.
- Item and block sprites are recoloured as they load by our sprite source `dreadpatch:dread_recolor` (listed in `assets/minecraft/atlases/blocks.json`; the colours in the game-free `magic/DreadRecolor`). Entity, armour and screen textures are `DynamicTexture`s made the first time they're drawn and remade on a resource reload.
- Every sound event points at vanilla's or Ice and Fire's files, but the Dreadlands' music: the owner's own track, made by `tools/music/make_music.py` (edit and re-run it as the icons' script).
- Block JSON copied from vanilla's (dreadwood's set) comes from `tools/datagen/make_dreadwood.py`: edit and re-run it.
- Spell icons, effect icons, Syndrigossa's egg, the dread fire and the gate's texture come from `tools/sprites/make_icons.py` (deterministic; needs Pillow). Edit and re-run it rather than editing the PNGs.

**Config** (`DreadPatchConfig`, a NeoForge server config, `config/dreadpatch-server.toml`):
- It shows in NeoForge's `ConfigurationScreen` from the mods list (registered by `DreadPatchClient`; editable in singleplayer, refused while online or open to LAN) and through `/dreadpatch config`.
- Every value is read where it's used, never cached, so a change applies at once. Give a new value the same treatment, a comment (the screen's tooltip), lang `dreadpatch.configuration.<key>` (sections too; the file's title is `dreadpatch.configuration.section.dreadpatch.server.toml[.title]`), and a row on the wiki's Settings page.

**Her spells** (details in `spells.md`):
- They're of our **Rimefrost** school, a subtype of Ice. Iron's per-world spell config stores each spell's school, so each spell of hers overrides `getSchoolType`.
- They stay out of random loot (`allowLooting()` false) and, but for Rimefire Blast, the Scroll Forge. **Add each new one to her loot table's `spell_filter` list** (`data/dreadpatch/loot_table/entities/dread_queen.json`), and a lesser one (not a storm, court or dragon) to the spire summit chest's (`chests/spire_summit`).
- A spell that deals damage under its own id needs a death message: lang `death.attack.<spell id>`, plus the `.item` form Iron's own spells carry.
- Iron's wants item models for every school (`<ns>:item/scroll_<school>`, `affinity_ring_<school>`); after a client run, grep its log for `Unable to load model: 'dreadpatch:`.

**Damage and effects:**
- Withering Rime is always applied with a source: `RimeSiphon` records the last to lay it, and only a queen's feeds her.
- Any share-of-max-health hit is capped at 20, against everything (`magic/DreadDamage`).
- `#dreadpatch:rimefrost` is what counts as Rimefrost damage, for her regalia both ways. Put a new Rimefrost damage type in it.
- The Dread (`IDreadMob`) refuse Withering Rime, Whiteout and Rimefrost (`MobEffectEvent.Applicable`).

**Rimefrost dragons:** every Rimefrost trait goes in `RimefrostDragon`; only what the owner gave Syndrigossa alone goes in `Syndrigossa` (the split: `decisions.md`, 0.37.0).

## Testing

- **Unit tests:** `./gradlew test`, part of `build`. Game classes are on the test classpath, but there's no running game.
- **The smoke test:** `tools/devserver/queen_smoke.py` (Python 3, stdlib) drives `./gradlew runServer` over RCON and stops the server when done (`--keep` leaves it up). It needs `enable-rcon=true`, `rcon.password=<anything>` and `server-ip=127.0.0.1` in `run/server.properties` (gitignored; already set on the owner's machine). Give each new feature a check. Before writing one, read `testing.md`'s "What it learned": `/reload` first, a summon is a check too, entity UUIDs must differ, and the clock and weather drift during a run.
- **Client: ask first.** Rendering has no automated check, and on the owner's machine `./gradlew runClient` opens a window on their desktop (there's no Xvfb). **Ask before launching it.**
- **Creative mode:** Iron's spends no mana and sets no cooldowns in creative (`CREATIVE_MANA_COST` / `CREATIVE_COOLDOWN`), so check those in survival.

## Working here

### Building and dependencies

**Build**
- `./gradlew build`.
- CI (`build.yml`) runs the same on every PR and every push to `main`, and uploads the jar, versioned `<mod_version>-dev.<commit>` (`-dev` locally).
- Only the Release workflow builds a plain `<mod_version>` jar.

**Dependencies**
- **Sources:** Ice and Fire, Uranus, Jupiter and GeckoLib come from Modrinth's maven (`maven.modrinth:<slug>:<version id>`).
  - They're pinned in `gradle.properties` to the exact files the pack ships.
  - Use version ids, not version numbers: Uranus 2.4.1 names both a NeoForge and a Fabric file.
- **As mods:** they're `implementation`, so the dev runs load them as mods, along with the jars bundled inside them (Architectury from Uranus, Integration from Ice and Fire).
- **Iron's Spells:** it and its required libraries (Iron's Lib, playerAnimator, Curios) come the same way.
- **NeoForge:** `neo_version` is the pack's, which the dev runs use; `neo_version_range` is the floor the jar declares (0.60.0: `[21.1.230,)`, Ice and Fire CE's own; 0.59.0 and before declared the pack's build).
- **JEI** (0.59.0): the pack's file (19.70.0.469, `jei_version`, Modrinth version id `1vC6z8wV`), `compileOnly` for our plugin and `localRuntime` for the dev runs. Never required: JEI finds the plugin itself, and nothing else of ours touches JEI's classes.
- **Test-only mods** go in `localRuntime`: Better Combat 2.4.0, Better Mob Combat Reimagined 1.1.22, Cloth Config 15.0.140, and (0.53.0) Lootr 1.11.38.127, the pack's file (`lootr_version`, its Modrinth version id from the pack export).
  - They're the owner's files, from the untracked `compat_mods_testing_better_combat/`, pinned by the Modrinth version ids their SHA-1s resolve to.
  - The owner's pack export lists Reimagined 1.1.21.
  - The compat jars there for Cataclysm, Cataclysm: Spellbooks and Entity Model Features need mods the dev runs don't have, so they're left out.

### Dev runs and network

**Dev runs**
- **Server:** `./gradlew runServer` boots to `Done`, its mod list 20 long (Minecraft and NeoForge among them; Lootr made it 18 in 0.53.0, JEI and its bundled MezzConfig 20 in 0.59.0); the dev environment skips the EULA check.
- **Stopping:** `stop` over RCON, or SIGTERM to the java process. Both save the world.
- **Expected noise:**
  - after a `mod_version` change, a warning that the world's `dreadpatch` version changed;
  - Ice and Fire's `iceandfire:delight/*` recipes failing to parse (they need Farmer's Delight);
  - missing Jupiter config files on a fresh `run/`;
  - refmap warnings from GeckoLib, Iron's Spells, Iron's Lib and Curios;
  - Lootr's mixin looking for Create's `SchematicPrinter`;
  - `Not a list: null` whenever an ice dragon is summoned.
- **After changes:** grep the log for `FAILED during APPLY|was not applied`.

**Network:** a build needs:
- `maven.neoforged.net` and the Mojang hosts;
- `api.modrinth.com` and `cdn.modrinth.com`;
- `maven.parchmentmc.org`;
- the foojay JDK download.

### Pull requests and releases

**PRs**
- Into `main`, based on `main`. A PR based on another PR's branch merges into that branch, not `main` (#2 did).
- Bump `mod_version` with user-visible changes; merging the bump releases it. Keep the README in step.
- **Before committing (the owner's checklist, pasted 2026-11-22 for the second drop, followed since):**
  - one branch for each group of work;
  - stage files by name (never `git add .` or `-A`; `compat_mods_testing_better_combat/` and the `.mrpack`s stay untracked), after checking the diff for debug leftovers;
  - `./gradlew build` (unit tests included) and the smoke test pass before the merge, or a failure is understood and flagged;
  - the PR says what changed, what was assumed, and what still needs testing by hand;
  - no commit or PR until all that's done, or a blocker is resolved or clearly flagged.
- **Merging (since 2026-11-14):**
  - the owner wants each PR merged as soon as its CI passes, without waiting for their review;
  - with merge commits, as they merged #1 and #2.
- **The beta (since 2026-11-23,** the owner: "hold for the beta", then "Create a beta branch and follow your recommendations"**):** `main` stays at the last release. Beta work branches from `beta` and its PRs go into `beta`, merged there (merge commits) once CI passes; each still bumps `mod_version`. `beta` merges into `main` once, when the owner says the beta's ready, which releases the version it's at. Pushes to `beta` release nothing and don't sync the wiki (`release.yml` and `wiki.yml` run on `main` only); PR builds still run.
- **One release at a time:** merge PRs that each bump the version one at a time, letting each Release run finish first. `release.yml`'s concurrency group cancels a pending run when another queues behind it, which would skip that version's release.

**Release**
- **Trigger:** every push to `main` runs `release.yml`.
- **What it does:** if `v<mod_version>` isn't released yet, it:
  - builds with `RELEASE=true`;
  - creates the tag on that commit;
  - publishes the jar, with notes GitHub generates from the merged PRs (`.github/release-notes.md`, if present, goes above them).
- **Otherwise** it stops after the check.
- **Retrying a failed release:** run it by hand on `main` (GitHub MCP `actions_run_trigger`, `release.yml`, ref `main`).
- **Safety:** it never replaces an existing release.
- **Tags:** sessions here can't push tags.

### The owner's other rules

**Shared CI**
- These files are identical in all the owner's mod repos (phantom_storage, golemarmor, friendlybeacons, catspells-nosumagro, gvs-noatk-players, dread-patch):
  - `.github/workflows/`: `build.yml`, `release.yml`, and `wiki.yml` where there's a wiki;
  - `.github/dependabot.yml`;
  - `.gitattributes`.
- phantom_storage's `CLAUDE.md` has the fullest notes.
- Change them in every repo at once, and keep repo-specific text out of them.

**Modrinth:** never publish anything there (a version, the description, gallery images) without the owner's consent, and no emoji in Modrinth text.

## The owner's pack

"The Living World" (1.3.6c, about 200 mods), on NeoForge 21.1.265. The owner drops `.mrpack` exports of it in the repo root for reference (gitignored); the `modrinth.index.json` inside lists every mod with its Modrinth version id.

- **Ice and Fire** CE 2.1-beta.1, plus `iceandfire_create`, on Uranus 2.4.1, Jupiter 2.3.7, GeckoLib 4.9.3 and Architectury 13.0.12. Its `config/iceandfire/iaf-common.json` (in the export's overrides): dragon `maxHealth` 1,000 and `attackDamage` 20; breath 1.8 / 2.1 / 3.5; **`neutralToPlayer` true** and `griefing` false. So a stage-5 ice dragon at 100 days has 808 health, 17 bite and 16.2 armor. The dev run keeps the defaults: `maxHealth` 500, `attackDamage` 17, breath 2.0 / 2.5 / 3.5.
- **Iron's Spells 'n Spellbooks** 1.21.1-3.16.3, with Iron's Lib 2.3.0, playerAnimator 2.0.4, Curios 9.5.1, and addons such as Cataclysm: Spellbooks.
- **Others:** JEI and Jade; L_Ender's Cataclysm 3.33; Better Combat 2.4.0 with `better_mob_combat_reimagined` (which gives mobs its attacks) and Cloth Config; AttributeFix (it raises attribute caps, but vanilla's armor math still caps the cut at 80%).
- **Graphics:** Iris 1.8.14-beta.1, Sodium 0.8.13 and ImmediatelyFast 1.6.15. Per phantom_storage's `CLAUDE.md`, the owner plays with Complementary shaders and with ImmediatelyFast's screen batching on, which broke screen overlays there.
- **Debugging pack-only reports:** read a mod's code rather than running downloaded jars when a report only happens in the pack.
