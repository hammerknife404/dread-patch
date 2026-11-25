# Testing

Part of the notes for agents working here: [CLAUDE.md](../../CLAUDE.md) has the rules and the index.

## Unit tests

`./gradlew test`, part of `build`:
- **Rules:** one test per class in `magic/`.
- **Client and data:**
  - `UndeadSkinTest`;
  - `IceSpikeTest` (spike geometry, through a `VertexConsumer`);
  - `RisePoseTest`;
  - `DreadPhantomSkinTest` and `QueenArmorModelsTest` (bakes each regalia slot's model);
  - `SaddleSeatTest` and `SyndrigossaAnimationsTest` (against Ice and Fire's files on the test classpath).
- **`DreadQueenModelTest`:**
  - bakes her layer and builds the `HumanoidModel` from it, so a missing part fails here, not in a client;
  - checks that her bust's texels are opaque.
- **Limits:** game classes are on the test classpath (`addModdingDependenciesTo(sourceSets.test)`), but there's no running game.

## Dev server smoke test

**Running it**
- `tools/devserver/queen_smoke.py` (Python 3, stdlib) drives `./gradlew runServer` over RCON, and stops the server when done (`--keep` leaves it up).
- It needs `enable-rcon=true`, `rcon.password=<anything>` and `server-ip=127.0.0.1` in `run/server.properties` (gitignored; already set on the owner's machine).

**What it covers**
- **The queen:** her stats, items and loot, and her rise.
- **Magic:** Withering Rime and the siphon, and each spell, cast through Iron's `/cast` by armor stands.
- **Fighting:** the sword, her spacing and Barrow-Step, her court, decree, storm and lich.
- **Summon Court:** cast by a NoAI villager (an armor stand records no attacker, and the court defends its caster from what hurts it). The count by level; leaving a cow and a dread thrall be when they hit the caster or a member; the caster's hit cancelled; a husk that hits the caster attacked; a recast replacing the court; expiry (set through `data merge ... {NeoForgeData:{...}}`). With `attack_friendly_mobs` and `retaliate` switched on by hand, the court killed the cow within 4 s, so the check does catch the filter missing.
- **Scuttler climbing:** a scuttler hit by a golem standing on a solid block 4 high is on top within 10 s.
- **Guard Villagers** (the pack's 2.4.12, a dev-run mod since 2026-11-24): a guard given an iron helmet, chestplate, sword and shield (`item replace`, which lands in its own `Inventory`) and killed in her storm rises a thrall of hers wearing all four, each at drop chance 0. Its `getItemBySlot` reads that inventory, so our slot copy needs nothing of its own. The guard drops its gear itself at `chanceToDropEquipment` (100 in the pack, so always), so nothing is duplicated.
- **Patrols:** in the Dreadlands' patch, a thrall with patrol data (its AI on) walks 6+ blocks toward its waypoint in 10 s, a ghoul following it stays within 10, and with the thrall killed the ghoul takes a waypoint of its own.
- **The storms:** in the Dreadlands' own patch, in clear weather, rimefire (set by hand) heals a slowed dread ghoul 0.7–1.6 in 4.5 s and chills, puts out and (0.55.0) hurts a slowed husk; a Withering Rime-Bolt summoned falling onto the pad leaves a `brief=true` rimefire block, gone 4.5 s on; then with `weather rain`, a NoAI husk under the open sky gets Rimefrost within 3 s and one under a stone roof doesn't. **The whiteout weather (0.54.0)**, with `whiteout_storms` set to 2 in `run/config/dreadpatch-server.toml` (NeoForge re-reads it on a change; the script sets it to 0 for the rest of the run, so a random whiteout can't put Whiteout on what another check reads, and puts the old value back as it exits): over three storms (`weather clear`, 2.5 s, `weather rain`, 3.5 s) whiteouts alternate; in one, a husk outside gets Whiteout, loses health and has Rimefrost too, one under a roof gets no Whiteout; a NoAI queen summoned there brings thunder (`execute if predicate {condition:"minecraft:weather_check",thundering:true}` in the Dreadlands, polled: a level's `isThundering` waits for its thunder level, rising 0.01 a tick, to pass 0.9, so 91 ticks; read at 4.5 s it failed) and the Whiteout lifts; with her there, two storms bring none.
- **The encounter:** a queen marked as a spire's, killed in the Dreadlands, leaves a gate where she fell, still open after 31 s (past a keyed gate's 30); a queen risen in the open air with her dragon is still aloft 3 s on (its `Hovering` or `Flying` 1b, its feet well over the platform: it sinks from 260 to 242 and holds). Summoned outside the loaded chunks (x 40), she never ticked and rose with no dragon.
- **The Dreadlands:** the biome locates; a spire locates, and with its chunks forceloaded its summit is 50+ blocks over the ground 30 out; the ground's top block is snow or ice (or dreadstone, gravel); a gate built and summoned there opens, brings no queen, is still open at 11 s, fades (an absent pane while it lives) and closes within 30 s, its frame left. The overworld gate's ritual is checked to bring no queen 8 s in. **Its structures (0.53.0)**, far out (a random 8,000–40,000 blocks each run, so on new ground: the world is kept between runs, and the first run's village kept the conversion it was made with): a located village, forceloaded, has 50+ dreadwood blocks and no spruce planks, spruce logs or cobblestone left, and no plain chest (its chests Lootr's); two spires' summits have no plain chest (and the check prints their Lootr chests and dread torches). Chests and torches are counted by swapping them to sponge and back with `/fill` (it counts no same-block change).
- **The dread phantom:** at noon a phantom (the control) burns and a dread phantom doesn't; undead; refuses Withering Rime, Whiteout and Rimefrost; Rimefrost slows a zombie by a quarter; its table gives a thrall's drops and, without a player, no membrane or scales. Summon Dread Phantoms (a villager casting): 2, its caster's, court-marked, circling within 28 blocks of it, unhurt by it, diving on a husk that hits it (within 20 s), leaving nothing when killed, replaced by a recast, gone when their time's up. The queen's AI fight: 1-2 of hers by the time her storm comes. A Dread Lich with its countdown set short (`NeoForgeData:{"dreadpatch:phantom_call":2}`) calls 1-2 of its own at a golem.
- **Hatching:** an egg summoned on a dreadstone block with an `Owner`, and another on plain stone:
  - a full summoned storm (`Lifetime` 40) gives the first 1 and the second 0;
  - a storm killed early gives nothing;
  - two more hatch the first into a white Rimefrost hatchling (`AgeTicks` under 400) owned by the egg's owner.
- **Dragon corpses:** a dread thrall, then an iron golem (one at a time: a golem goes for a thrall), takes an ice dragon for its target. The dragon is killed into a corpse; the corpse is invulnerable, and the fighter has no target 1 s and 4 s on. With no target, `execute ... on target` prints nothing at all.
- **Sleep:** at midnight, an Ice and Fire ice dragon summoned `Sleeping:1b` sleeps; a Rimefrost dragon summoned so is awake.
- **Create Rimefrost Dragon:**
  - a NoAI villager (Resistance V, at 6 of 20) casts it at a silver ice dragon corpse: the corpse is gone, and a silver Rimefrost of its age rises there with 7 health, `RaisedBy` the villager;
  - cast at a second corpse, the first is sent back and the second (blue) rises;
  - a queen at 100 of 300 with a golem target raises one of hers from a corpse 8 blocks off within 20 s, not whole.
  - The raised dragon is set NoAI right after the cast: with its AI it heals as it fights (it read 87 once) and wanders into the next cast's line of sight.
- **Her regalia's powers (0.51.0):** worn on a zombie, +600 max mana over dread dragonsteel's, all four pieces unbreakable, its frozen ticks gone (dread dragonsteel's kept), 20% less Rimefrost magic; her breath 76% off (70%, then 20%). Her staff in an armor stand's hand: Rimefrost spell power 1.05. A bare zombie takes 20% more Rimefrost magic from the one in her regalia than from the one in dread dragonsteel; a golem 20% more from a Rimefire Blast cone (its hit and its rime's withering) owned by a zombie in her regalia. A slowed dread thrall takes a plain zombie for its target, not one in her regalia, until that one hits it. Rimefire Blast's cone, summoned owned by a villager, hits a golem for its 4.5 and leaves Withering Rime.
- **The Rimefrost dragonforge (0.59.0):** built by command (core at 8 241 7, its aperture east), its core lit and a grill showing; a brick on a bone corner unlights it and the bone back relights it; a hopper on its top brick fills what goes in and one on a side brick its blood; a tame stage-3 Rimefrost dragon (its AI on, `Owner` set) 9 blocks east lights it within 20 s and the aperture glows; with `CookTime` set to 700 it makes Rimefrost dragon blood, scale and bottle used; Ice and Fire's ice forge, lit by a tame Ice and Fire ice dragon, makes dragon ice of our scale and no blood.
- **0.60.0:** at noon, clear, a NoAI zombie on the Dreadlands pad stays unlit and whole while one in the overworld's sun (the control) burns (summoned with NBT, so no random helmet); in a summoned storm a zombie's follow range is halved and Iron's Dead King's (`#c:bosses`) isn't, though it has Whiteout (vanilla's Wither and Ender Dragon override `addEffect` to take none, so they can't show it); the disc in a jukebox (`item replace block ... container.0`) sets `has_record` and `ticks_since_song_started` (a jukebox plays only a song its registry knows); 60 rolls of the summit table give 1–20 discs.
- **0.61.0:** `/dreadpatch dreadlands arrival` from a spire's middle (twice) and the origin: each on dry ground with room to stand, within 256, 69+ from any spire's middle; bone-mealed by a dispenser beside it (24 pulses), a dreadwood sapling grows on dirt in the overworld (the control) and on graveyard soil in the Dreadlands, not on dirt there; 400 hand-breaks of dreadwood leaves (`loot spawn ... mine`) give saplings and never the leaves.
- **0.62.0:** a NoAI queen `/kill`ed is no defeat; one killed by `/damage` is (the world's first: `escalation` says so) and brings a 25-60 s storm; another within the cooldown leaves it running down, not renewed; `escalation reset` forgets it all.
- **0.63.0:** in her storm (a NoAI queen's), a bat and a parrot killed rise as miniature dread phantoms with her as `Summoner`; a cod killed in water (stone over it, or her storm freezes it) rises a cod marked `dreadpatch:risen`, with 6 max health, refusing Withering Rime. (The bat rose a dread beast before.)
- **0.64.0:** a dread lantern and a Dread Gate's face, each alone in a stone box, light it to exactly 6 (a dread torch, Ice and Fire's, to 5) (`location_check` light); a NoAI queen set burning (`Fire:300s`) inside her own storm is put out within 1.5 s.
- **0.65.0:** each dreadwood block sets; an abandoned village has dreadwood stairs, slabs, fences or doors and no spruce ones; with `dread_light_freeze_minutes` at 0.05 (a second a stage), water beside a dread torch and under a dread lantern (set by `/setblock`, tracked by its neighbour update) is blue ice within 40 s, and water 3 over them stays water.
- **0.66.0:** the Sastrugi Wastes and the Rime Chasma locate; a crusted lava pocket placed (`/place feature`) in a block of stone has lava at its heart and none past it.
- **0.67.0:** a grave patch placed (`/place feature`) on a stone pad turns some of it to graveyard soil and stands a wither rose or more on it.
- **Her regalia:** worn, its armour and toughness equal dread dragonsteel's; all twelve armour pieces of ours are `#minecraft:trimmable_armor`; it's in the breath check below; a queen not killed by a player (her table rolled, `/kill`) drops none.
- **Gear:** two zombies in the same armour (100 health), one hit by 40 Rimefrost breath, one by 40 `mob_attack` (armour cuts both; vanilla's `generic` goes straight past armour, which once made it look like a 95% cut): the breath lands at 30% of the attack, for dread dragonsteel and for Ice and Fire's ice dragonsteel; a husk's hit with a dread dragonsteel sword rimes an iron golem (no Resistance: a hit brought to nothing carries nothing). Corpses give Rimefrost dragon scales.
- **Rimefrost dragons:** a summoned one keeps its colour (sapphire), is undead with the ice dragon's tags, refuses Withering Rime and Wither, starts whole from a tag at Ice and Fire's health for its age (unchanged on Hard, unlike Syndrigossa's), and its corpse gives its own colour's scales and shards, its heart never her egg. Summoned without NBT it's a random age and ice colour (a summon with any NBT skips `finalizeSpawn`: a colourless hatchling).
- **Her drops:** at most one sword and one staff, one ink and one scroll a kill; over 40 rolls each of sword and staff 3–20 times; over four deaths of a queen saved with `HandDropChances` 2.0, not all eight.
- **Ice:** Rimewalk, and the storm's ice.
- **Syndrigossa:**
  - stats by difficulty, tags and effects;
  - corpse tables;
  - attacks, without her AI.
- **The pair:** the queen on her dragon (the tether, getting down).
- **The Dread Gate:** summoned in its frame, which it finds as the key would.

**What it learned**
- **`/reload` first:** Iron's builds its spell config on datapack sync (a player joining, or `/reload`). Before that, every spell reads max level 1.
- **World setup:** it sets noon, because dragons sleep at night and monsters spawn on dark platforms. It sets Easy, and puts the difficulty back at the end.
- **Command pitfalls:**
  - Commands under `execute` need `run`.
  - A check on a failed summon passes vacuously, so summons are checks too.
  - NBT tests go inside a selector's brackets.
  - `has_effect` stops the run on a malformed command, rather than reading it as a fail.
  - With no vehicle, `execute ... on vehicle` prints nothing at all.
- **Timing:**
  - An instant effect from `/effect` lands on the next tick, and so do held items' attributes.
  - NoAI mobs keep the `OnGround` they're summoned with.
  - A wild dragon ages a tick each tick.
- **Cleanup:** every queen's court is cleared with her (`clear_court`), or strays turn up in later checks.
- **The crown check** polls every 0.1 s. Her next bolt comes within 1.25 s of a crown, sometimes within a few ticks, and shatters it.
- **Shards:** once, a core's burst counted 8 shards (it makes 5–7), never explained. That check prints where they were.
- **The seat:** once (0.37.0), "she sits on its back" read her 14.7 blocks above her dragon's feet. The same setup repeated eight times seated her at 9.24, its dragon on the ground, and the next full run passed: likely her dragon stirring in the 0.3 s it has its AI before the test sets NoAI.
- **The queen's raising:** once (0.52.0) she was gone altogether by the check (her position unreadable), alone she raised at 10 s untouched; the golem now has no AI and her summon is a check. Once (0.40.0) she raised nothing within 20 s in the full run; alone it passed 3 runs of 3, each at 10 s, and the next full run passed. The check now prints her position, health and target, and whether the corpse is there, if it fails.
- **Entity UUIDs in the script must differ:** a killed lich lingers in its death animation, so a villager summoned with the same UUID right after was never added, and the Create Rimefrost Dragon checks after it all failed (0.45.0's first run). The UUIDs in use: 2 (Rimefire Blast's caster), 3 (the phantom-calling lich), 5 (Create Rimefrost Dragon's caster), 6 (the wounded queen raising a dragon), 7 (the court's and phantoms' caster), 9 (a queen), 21 (a patrol's leader), 23 (the whiteout's queen), 24 (the burning queen).
- **Dread mobs with their AI go for any living thing in reach** (0.48.0's first run): a slowed dread ghoul on rimefire walked off it to kill a husk 10 blocks off; alone, it still wandered off once. Test them one at a time, and wall the ghoul into a one-block cell over the fire.
- **Her phantoms come 15 s after her first target, her storm 15 s after she spawns:** a single look right after the storm once found none (0.48.0's first run); the check waits up to 10 s, after the storm's golem check (put before it, the wait made that one fail).
- **The clock runs, and the weather turns:** `time set noon` and `weather clear` are at the start, but minutes in it's evening, or raining; a check that needs the sun sets both again (the dread phantom's: once the rain came and the vanilla phantom, the control, didn't burn; the forge's (0.59.0): by its turn it was night, and Ice and Fire's ice dragon, its control, slept and never breathed on its forge; ours never sleep).
- **Ice and Fire wakes its dragons by day on their first tick,** NoAI or not: the sleep check read one at noon right after its summon and failed when a tick came first (0.60.0's run on NeoForge 21.1.230); it runs at midnight now.
- **Vanilla's Wither and Ender Dragon take no effects at all** (each overrides `addEffect`): a check that needs a boss under an effect uses Iron's Dead King (`#c:bosses` too).
- **Random ticks come only near a player** (a forceloaded chunk gets none, whatever `randomTickSpeed` is): the sapling check grew nothing that way (0.61.0's first run); it bone-meals with a dispenser, pulsed by a redstone block set and cleared (scheduled ticks run in forceloaded chunks).
- **Far-out structure checks pick fresh ground each run:** the village check's 32 spots on one diagonal came round again (0.61.0) onto a village made before 0.57.0, all of whose lights were still vanilla's; it picks anywhere 8,000–200,000 out on each axis, either way, now (the spire-summit check, its mirror images).
- **Run it on the dev world (`run/`), not a fresh one:** a fresh world (0.67.0's, in a worktree) has another seed and spawn, so the chunks round the origin it doesn't forceload never tick; "holds still while rising" and "risen in the air, her dragon takes wing" failed there twice and passed on the dev world, 408/408.
- **Worldgen probes:** `forceload` refuses more than 256 chunks, and a corner off a chunk's edge adds a row; forceloading 256 new chunks can hold the server thread past the RCON client's timeout (lengthen it); old chunks keep what made them, so a terrain change is checked on ground never generated (0.66.0).
- **Water washes a torch away:** the cold check's first layout let its water flow into the dread torch's block (0.65.0); each water is a sealed cell in stone now, the lights on the stone over it.
- **A zombie in full sun catches fire about 4% of ticks:** 4 s once left the sun check's control unlit; it waits 8.
- **A court member:** "its caster can't hurt it" once read two different members (`limit=1` alone may pick another between reads); it now tags one and reads `/damage`'s own output.

**What it doesn't cover**
- **Needs a player:**
  - the key on a frame, its hint, Peaceful, the recipe and tooltips;
  - the Permafrost Conduit and the sword's guard;
  - Syndrigossa's corpse harvest and egg; laying an egg with a right-click, its hint, picking it back up, its look;
  - her bite's Withering Rime II (a bite needs her AI and a target in reach);
  - a summoned court following a moving player, and an iron golem leaving it be (the owner saw both in 0.33.0: "the court follows and ignores golems");
  - her hunting a player in the owner's pack;
  - her regalia: its drop on a player's kill, the advancements (There Must Always Be a Dread Queen too), how it looks worn;
  - trims: at the smithing table, on the icons, worn;
  - the sky (0.67.0): the dimmed lightmap by day, the pale cyan sun, its swap back on leaving, under a shader pack; grave patches and their trees as seen;
  - the terrain as seen (0.66.0): the wastes' dunes and shores, the chasma's trenches, bridges and fissures, the caves, a lava pocket, seams with older ground;
  - dreadwood's blocks crafted, placed and looking right (the door's and trapdoor's recoloured art), a dread light's cold over a real day;
  - the sound filters (0.64.0): the muffling, the softened highs, the phantoms' volume;
  - risen Dread (0.63.0): their look (the frost tint, a miniature phantom's size), their hunting and bites, stirring in caves and underground water;
  - the escalation (0.62.0): the Dread round a Mausoleum, the defeat storm's fog, rime, wind, cracking and Rimefrost on a player, the first-defeat message;
  - the Dreadlands' stray undead rising (0.61.0), a player refused placing a dreadwood sapling off graveyard soil there, and stepping through a gate to a random arrival;
  - the Dreadlands' music (0.60.0): its loudness against vanilla's, that nothing else plays there (creative too) and it stops on leaving, its start on a first arrival, the advancement, the disc's look and a jukebox's fall-off;
  - the Rimefrost dragonforge's screen, shift-clicking, its recipes, its JEI page, its look (the animated core and grill, the recoloured screen), a wild dragon's breath lighting it;
  - the dread phantom's look (skin, eyes, trail), its sounds, its bite's effects on a player, its drops on a player's kill, and Summon Dread Phantoms from a scroll or spellbook.
  - the Dreadlands: stepping into a gate, the arrival, the way home, beds, its phantoms coming down; a spire's encounter coming to a summit, its reset, its dais and torches; a storm's fog and blown snow, its lightning and rimefire's look;
  - its spawning (0.52.0): the Dread rising round a player by the weather, patrols rising and crossing, phantoms by the spires;
  - its worldgen as seen (0.53.0): a spire's flared summit, its chest and lich, the dune fields and the flatter ground between, a village's look, Lootr's per-player chests opened;
  - its weather and sound (0.54.0): a whiteout's fog, wind and snow, its freezing on a player; the ambience (its loop, the ice cracks' pacing and bursts, the rumbles).
- **Needs her AI in flight:** the forced landing from the air.

## Client

- **Rules** (ask before launching it; creative skips mana and cooldowns): in CLAUDE.md, Testing.
- **The owner's checks:** they've checked each release in the dev client or their pack as it shipped, through 0.35.0 (2026-11-21: at 0.33.0 "everything works, the court follows and ignores golems"; at 0.34.0 "the boss bar works"; at 0.35.0 "everything works, they ignore the corpse now"). Their reports are behind the decisions above.
- **Since 0.35.0:** at 0.44.1 (2026-11-22), "the achievements work" (the regalia's and There Must Always Be a Dread Queen); at 0.45.0, "phantoms work" (summoning, sounds and their AI, in the dev client); at 0.46.0, "dreadlands work"; at 0.47.0, "encounter works"; at 0.48.0, "storms work!" (so the whole big drop has been tried in the dev client); at 0.49.0, riding an Ice and Fire dragon, "camera and pov's are fine now"; at 0.51.0 they found two faults (her Rimefrost scrolls drawn as the missing texture, and a dread shard refused by the Scroll Forge: 0.51.1), and at 0.51.1 "scroll forge works". 0.50.0 went unreported. **0.52.0–0.53.0 are untried, and 0.59.0 (the forge's look, screen, recipes and JEI page)** (the owner: "I'll test this alongside the next batch"); of 0.54.0 they reported "The cracking loop is too quick" (0.54.1), nothing else yet. From the dev client, 2026-11-22: the gate size and storm fog (0.55.1), the weather slowing (0.56.0), and at 0.57.0 lanterns and torches "Both change exactly as it should".
- **Never checked:** Fabulous graphics and Iris shader packs, for the storm and its fog.
- **Seen once, not looked into** (0.39.0, the owner: "Seems to be a dysync issue, reloadign seemed to work, likely after shift+right clicking. Moving on"): something went out of step between client and server while trying the egg's hatchling, and a reload put it right. The hatchling had been riding the player's shoulder (Ice and Fire's), and sneaking gets it down.
