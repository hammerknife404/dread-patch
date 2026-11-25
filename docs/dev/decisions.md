# The owner's decisions

Part of the notes for agents working here: [CLAUDE.md](../../CLAUDE.md) has the rules and the index.

The design comes from the owner's two chats with another assistant, `proposed_stuff_1.txt` and `proposed_stuff_2.txt` (local only). Their numbers are that assistant's proposals, and some of their "facts" about the game are invented. Only what the owner adopted, listed below, holds. The version says when each decision shipped. Record each new one here, with its version and the owner's words where they gave them.

**The queen and her items**
- **Stats (0.12.0):** 300 health, 20 armor and 12 toughness. Only she heals from her Withering Rime.
- **Movement:**
  - She keeps her distance like Iron's necromancer (0.8.0).
  - She Barrow-Steps away when cornered (0.11.0, from the 2nd notes).
- **Figure (0.31.0):** her body should "reflect the female player variant", so she got a bust. Her arms were already Alex's 3 px.
- **The Rime siphon (0.14.0):**
  - She heals a flat 1/2/3 per target by difficulty.
  - 0.13.0 had her leech 1–2% of the victim's max health instead, and she soloed areas full of wardens, about 10 s each.
  - Since then every share-of-max-health hit is capped at 20 against everything. Before, the cap applied only against bosses.
- **The staff (0.9.0):** the chat's numbers.
- **The sword (0.10.0):** the chat's profile and properties, with two gaps the owner filled:
  - A guard-blocked hit primes a shatter (the chat's "frost charge" did nothing defined).
  - The guard stops only fire and magic.
  - The shatter's 10% is ours.
- **Her drops (0.33.0):** with the craftable key letting players summon her at will, the sword and staff drop at 25% each (+5% per Looting level), with one ink and one scroll; until then all four were certain, with two inks and one or two scrolls.
- **Summon Court (0.33.0):** a court spell on her staff and as one of her scrolls. The owner's terms:
  - a player's court never fights the Dread (and the Dread leave it be);
  - it's "never hostile to other friendly mobs" and doesn't retaliate, with config options (`config/dreadpatch-server.toml`: `attack_friendly_mobs`, `retaliate`, `attack_players`, all off).
- **The Rimefire Crown (0.8.0):** it replaced the Barrow-Glass Tomb of 0.6.0–0.7.0, which the owner said looked like glass jars. Scrolls made before went blank.
- **Loot (0.4.0):** the owner's list ([queen.md](queen.md), Her loot table).

**The queen on her dragon (0.24.0–0.26.0)**
- As the boss, she rises riding it.
- The Soul Tether splits healing between them within 28 blocks. This is the 2nd notes' version, not the 1st notes' split damage.
- She can't be hit in the saddle, so the dragon's health is what counts. Once, at half its health, it's forced down, she gets off, and it's grounded for 10 s (0.26.0).

**Whiteout Dominion (0.27.0)**
- **Origin:** the 1st notes' one line (a zero-visibility night storm, glass-rime cones, 10% of max health a tick), remade to the owner's terms:
  - a large, localized weather event that cuts visibility and slows modestly;
  - a point of freezing damage every few seconds;
  - no share-of-max-health damage, and no cones;
  - anything killed in hers rises as a dread mob.
- **Numbers:** radius 32, 45 s, every 120 s, standing where it's cast.
- **The scroll:** a player's storm doesn't raise the dead.
- **0.28.0:** snow covers the ground for a while, and every mob rises except golems.
- **0.30.0:** its water freezes like Frost Walker's, in a player's storm too.

**Other additions**
- **Rimewalk (0.29.0):** "a frostwalker like ability to prevent water kiting".
- **Her Dread Lich (0.30.0):**
  - The owner: "its cooldown should be at least a minute or so for balance".
  - Ours: 30 s in, then at most every 90 s, one at a time.
- **The Dread Gate (0.32.0):**
  - The plan offered a summoning gate (A), a Dread realm (B) or her own rift (C).
  - The owner: "A, both, consume the key, recipe is fine". So it's buildable anywhere and in the Mausoleum, and it uses up the Gravemaster's Key.
  - The key got a recipe: 3 dread shards, a bone, 2 gold.

**Syndrigossa and the Rimefrost**
- **0.15.0:** an ice dragon of the Dread in a rotted skin.
- **0.16.0:** her Withering Rime breath and Shatter-Spine.
- **0.17.0:** Sovereign's Eclipse, with no closing in and no root, but a heavy slow.
- **0.18.0:** a wider breath cone, more air time, and strafing fly-bys.
- **0.20.0:** dread scuttlers spit bolts.
- **0.21.0:** Shatter-Spine finally seen (every 10 s in the air).
- **0.22.0:**
  - As the boss she's ancient and at full strength; otherwise she grows like an ice dragon.
  - She's undead, and her corpse is Dread.
- **0.23.0:** her egg comes only from the boss.
- **0.25.0:** her breath's particles, at normal size, for the queen's bolt too.
- **0.30.0:** health by difficulty: 500 Easy, 750 Normal, 1,000 Hard.
- **0.33.0:**
  - In the owner's taxonomy she's a unique, named dragon of the **Rimefrost**, a subtype of frost dragon (they offered "Rime" or "Rimefrost"). Lore only: no one-per-world limit. Her spawn eggs and egg say so in their tooltips.
  - Rimefrost dragons' bites inflict Withering Rime II: hers, 4.5 s.
- **0.34.0:** "Syndrigossa should have her own boss bar". Ours is white (the queen's is Ice and Fire's blue), and hidden while she's tame and once she's a corpse.
- **0.35.0:** "Dread mobs should ignore dragon corpses so they are not stuck attacking them forever", then "perhaps patch the corpse itself for broader compatibility": so it covers every mob and every Ice and Fire dragon's corpse.
- **0.35.1:** "Confirm the dragons breath particles match their distance". Checked by running vanilla's particle motion on them:
  - flame and smoke from her mouth carried 93–110% of their mark, so they matched;
  - vanilla's snowflakes flew 70–123%;
  - the particles set in the cone drifted 1.5–4.4 blocks on, past the end.
  - Now every particle ends within what the breath reaches (the cone and its splash), and `DragonBreathTest` holds it there.
- **0.36.0:** "Make the max distance on the breath 32. Adjust particle speeds accordingly, but keep the cutoff matching its max distance".
  - The cone's max is 32 (30 before), and it's a hard limit: the splash where it lands no longer hits past it, and no particle flies past it.
  - Then "apply this distance to rimefrost dragons too": like the Withering Rime II bite, it's a **Rimefrost trait**. Syndrigossa was then the only one; since 0.37.0 every Rimefrost dragon has both.
  - Particle speeds follow the cone's length on their own.
- **0.37.0, the Rimefrost subtype** (the owner, from their notes: "Create the Rimefrost subtype"):
  - the plan's split, which they didn't change: every Rimefrost dragon has the breath, the bite, Shatter-Spine, undeath, Dread alliance, the rotted look in its own ice dragon colour and a Dread corpse;
  - only Syndrigossa has Sovereign's Eclipse, the boss bar, health by difficulty, the ancient boss, the queen's saddle and tether, and her egg;
  - a plain Rimefrost dragon's health is Ice and Fire's for its age (their pick wasn't given; ours, as "same tiered").
- **Decided (2026-11-21):**
  - **Create Rimefrost Dragon**, a spell (scroll only): it raises an Ice and Fire ice dragon corpse as a Rimefrost dragon of the same age, owned by the caster. 3 minutes' cooldown at least; 500 mana and all the rest a player has (the queen pays none). It rises with health of half (rounded down) the caster's missing health: the queen's, or the casting player's. A player has one at a time. "Recasting Create Rimefrost Dragon within its cooldown unsummons the current one and allows the new target".
  - **Hatching her egg, B:** placed on dreadstone, it hatches after soaking up three full Whiteout Dominion storms cast over it, into a Rimefrost hatchling (white) tamed to whoever placed it.
- **0.38.0, Create Rimefrost Dragon** ([spells.md](spells.md); the owner raised one in the client, 2026-11-21, which is how they saw it sleep); ours: the queen raises one only once she's lost a quarter of her health (unhurt, hers would rise with 1), first 10 s in.
- **0.39.0, hatching her egg** (B, above; ours: the storm must rise with the egg already in it and run its whole course, not cut short; struck by a player, a laid egg pops back into an item and loses its storms).
- **0.40.0:** "Rimefrost dragons are undead, they do not sleep" (`RimefrostDragon`: the sleep flag never set, `isSleeping` false, `isTimeToWake` true).

**The two drops, and after (2026-11-21 on)**
- **The big drop (2026-11-21), the owner's answers to our questions** (built in five releases, in this order):
  1. **Scuttler climbing** (0.41.0, [queen.md](queen.md)): all dread scuttlers.
  2. **Rimefrost dragon scale** (0.42.0, [gear.md](gear.md); one item; the "dread dragon scale" and the phantom's "rimefrost dragon scales"): Rimefrost corpses drop it in place of Ice and Fire's coloured scales. **Dread dragonsteel** through Ice and Fire's ice dragonforge, as ice dragonsteel is made (scales + bottle → Rimefrost dragon blood; blood + iron ingot → dread dragonsteel). Full sets: dragonscale armour from the scales, dragonsteel armour and tools from the ingot, with their abilities (ours to propose: Withering Rime where ice dragonsteel freezes; the scale set resists Rimefrost breath), stats as Ice and Fire's ice sets.
  3. **Dread Queen armour** (0.43.0, [gear.md](gear.md)): "replicate as close as possible" her outfit; stats "in line with their respective scale and steel variants" (ours: the dread dragonsteel set's armour with the scale set's breath resistance, no spell bonuses); drops "tracked via achievements so each kill guarantees a needed piece to complete the set", Looting affecting (ours: a missing piece per kill by a player, per-piece advancements; complete, a random piece at 25%, Looting raising it).
  4. **Dread phantom** (0.45.0, [phantom.md](phantom.md); its natural spawning came with the Dreadlands): vanilla phantom's stats ("they're hated enough"), not burning by day, dread eyes, a look mixing both with more holes in its wing membranes showing bones, a sparse Rimefrost trail. Spawned by a spell scroll (players can learn it; not on her staff) and by the queen, a Dread Lich and Syndrigossa (not Rimefrost dragons): 1-2 every 45 s. They escort their summoner and aggressively take revenge; aggression scales with difficulty. Its attacks roll Rimefrost (the slow) and Withering Rime independently ("it would still be possible for both to apply"; ours: 35% and 15%). Drops our choice (ours: thrall drops + 1-3 phantom membrane + 1-3 Rimefrost dragon scales, Looting affecting). **Natural spawning (the owner, after 0.42.0): in the Dreadlands only, 1-2 at a time, at a low rate (the owner, after 0.43.0: "Keep natural dread phantom spawn rates low"), "not tied to the player's sanity mechanic"** (vanilla's insomnia, `TIME_SINCE_REST`). Sounds: "a bit of ghostlier sound on top of the normal phantom sounds". Ours: the spell 2 phantoms for 60 s, 90 s cooldown, a summoned court's rules; at most 4 a summoner.
  5. **The Dreadlands** ([dreadlands.md](dreadlands.md): 0.46.0 the dimension, its spires, the gate's way in and the way home, its phantoms; 0.47.0 the summit encounter and its return gate; 0.48.0 the storms, which finished the big drop): bleak frozen waste with rare tall spires (dreadstone, the ice types, snow, stone), each with a chance of the Queen and Syndrigossa encounter at its summit; per player, once, but resetting "after a lengthy time like several IRL hours" (ours: a server config, 6 h). Entered through the Dread Gate: its ritual 10 s; a player stepping in cancels the overworld summoning, and the gate stays open a random 15-30 s, its blocks fading in and out faster and faster over the last ~5 s. Arrival near a spire's base, not close enough to trigger it. Home: a return gate where the encounter's defeated, or a Dread Gate keyed there (ours). Weather follows the overworld's: in a storm, heavy fog (less than Whiteout Dominion's, "a warning"), no damage, and **Rimefrost** (a new effect: the slow, no withering; ours: −25% movement); lightning ignites a rimefire patch that heals dread mobs standing in it 0.25 a second.
- **The second drop (2026-11-22), the owner's "final feature draft"** (pasted as a pre-commit checklist; built in five releases in these groups, each its own branch, build, unit tests and smoke test before merging). It supersedes earlier decisions where they differ (the bolt's numbers, the regalia's "no spell bonuses", scrolls only from her loot: the owner, "we can bend that previous rule now that foundations are stable").
  1. **0.50.0, config and tooltips:** the server config in an in-game screen, applied live where safe; her staff's tooltip kept readable however many spells; and (asked alongside) dread phantoms' trails get "the occasional cyan type like the rimefrost breath just scaled down".
  2. **Spells and equipment** (0.51.0: [spells.md](spells.md) the school, Rimefire Blast, the bolt's shares; [gear.md](gear.md) the regalia's powers; [queen.md](queen.md) the staff):
     - **Rimefrost**, a new Iron's spell school, "a subtype of ice" (the owner's answer to "Rimefrost Spell Power").
     - **Rimefire Blast:** like Iron's Cone of Cold, Rimefire damage, low (like the Rimefrost breath), reliably leaving Withering Rime (the draft's "Withering Rimefrost": "keep as is currently"); made at Iron's **Scroll Forge** (the draft's "Spell Forge") from **dread shards** (the draft's "Dread Essence": "the standard dread mob drop that already exists") and ink.
     - **Withering Rime-Bolt:** its share of max health scales with level from 3% (ours, agreed: 3% at level 1, +1% a level, 7% at 5); the queen's 5% on Easy and Normal, 8% on Hard.
     - **Her regalia:** indestructible; each piece +150 max mana, −5% Ice spell mana cost, −5% Rimefrost damage taken (what that covers: ours), and (the owner, after seeing it built: the set "is to add rimefrost damage not lower it", then "unless you mean the wearer of the armor is taking less rimefire damage then thats ok too") +5% Rimefrost damage dealt, so both; the full set, freeze immunity and the Dread neutral to the wearer.
     - **Her staff:** +5% Rimefrost spell power.
  3. **Spawning, patrols and neutrality** (0.52.0, [dreadlands.md](dreadlands.md); the neutrality shipped with the regalia, 0.51.0): in the Dreadlands, snowfall raises Dread mob spawns (day and night), a little more in heavy storms; small Dread patrols spawn day and night; dread phantoms come more often round spires, day and night.
  4. **Worldgen, structures and Lootr** (0.53.0, [dreadlands.md](dreadlands.md)): spire tops flare a little wider for Syndrigossa; a 50/50 chance of a loot chest on a spire's top, now and then with a Dread Lich; rolling hills like wind-swept snowdrifts and dunes; abandoned (zombie) villages with Dread block replacements; every Dreadlands chest works with Lootr (in the owner's pack, 1.11.38: per-player loot).
  5. **Weather and ambience** (0.54.0, [dreadlands.md](dreadlands.md); 0.54.1 paced the ice cracking, the owner: "too quick"; this finished the second drop): a rare Whiteout Dominion weather in the Dreadlands (about one storm in six, the owner: "and never back to back"), never starting while the Dread Queen's alive, turning into a heavy thunderstorm if her fight starts during it; the ambience a little louder, with surface ice cracking and deeper underground rumbles, each sound's volume varying a little.
- **0.55.0, rimefire as a hazard** (the owner, 2026-11-22, after the second drop): their terms are quoted in [dreadlands.md](dreadlands.md), The Dreadlands' storms.
- **0.59.0, the Rimefrost dragonforge** (planned 2026-11-22; the owner, 2026-11-23, to our five recommendations: "Go with your recommendations on all five"):
  - its blocks (core, bricks, aperture) recoloured from Ice and Fire's ice forge, lit only by a Rimefrost dragon's breath on its aperture (any Rimefrost dragon's, Syndrigossa's in her fight too); a tame one is drawn to it as Ice and Fire's are to theirs;
  - recipes in Ice and Fire's `iceandfire:dragonforge` format with `"dragonType": "rimefrost"` (packs can add their own); **"Only the new forge"** (the owner, 2026-11-22): the dread recipes left Ice and Fire's ice forge;
  - the five: (1) a **block of Rimefrost dragon scales** (9 scales, and back), and bricks after Ice and Fire's (5 scale blocks round 4 **dreadstone bricks** make 4: about 360 scales a forge); (2) the core's heart is Ice and Fire's **ice dragon heart** (Rimefrost corpses have none); (3) its corners are Ice and Fire's **dragon bone blocks**; (4) a **core in spire summit chests at 5%**, ready-built forges on spires left for later; (5) **no recipes beyond the two** (blood, ingot);
  - ours: a JEI page for its recipes (Ice and Fire's pages show only fire, ice and lightning ones); its progress holds while unlit, as Ice and Fire's actually does (we'd told the owner it cools 2 a tick: that branch of Ice and Fire's can never run).
- **0.60.0, from the owner's feature dump** (2026-11-23, `Feature_Dump_11-23.md`, local; reviewed with them, then "start with the music disc and the quick fixes"):
  - **the music disc** "Entombed In Rimefrost": "not played too loudly", "only played ambiently in the dreadlands, no other normal music can", for a first arrival, "possibly track to the advancement of arring there"; ours: −17 LUFS (vanilla's −15 to −16.5), vanilla's 10–20 minutes between plays, the advancement "Into the Dreadlands" starting it, the disc from spire summit chests at 10%;
  - **no undead burn in the Dreadlands' sun** (the dump's "Dimension-Wide Daylight Undead Immunity");
  - **bosses keep their sight in Whiteout** (the dump asked to verify that bosses don't drop aggro in the storm; they did; ours: the slow stays);
  - **the NeoForge floor** lowered from the pack's 21.1.265 to Ice and Fire CE's 21.1.230 (the dump: "avoid unnecessarily locking players to the exact 21.1.265 pack build");
  - **the track is the owner's** ("I am the creator of the music, Generated with my Claude Pro sub account"), so it ships, credited to them in the README;
  - **`main` held for the beta** (the dump's "Freeze main"; the owner, asked: "hold for the beta"): 0.60.0 (#86) isn't merged.
- **The beta (2026-11-23), the owner's answers to our review of the dump:**
  1. a `beta` branch, "follow your recommendations": beta PRs into it, merged on CI; `beta` into `main` once, to release;
  2. **the arrival** (replaces the big drop's "near a spire's base"): "somewhere random, but never on a tower or within 48 blocks from one. And prefer the surface, but caves or similar are ok";
  3. **dread water beasts:** "intended for overworld only or places that have water and aquatic life" (not the Dreadlands);
  4. **mini dread phantoms** (from bats and parrots): "Same as the Dread water beasts basically";
  5. **the post-defeat storm:** "about 30-60 seconds varying";
  6. **dreadwood saplings only on graveyard soil:** "Dreadlands only. Farmin' 'aint easy."
  - **what turns a bat, parrot or water creature Dread:** "Dying to whiteout dominion or other conditions that would turn a corpse into a dread mob" (her storm's raising of the dead is the one so far);
  - **the order:** "begin 1-3" (the Dreadlands basics: 0.61.0; the post-defeat escalation; mini phantoms and water beasts), the worldgen last;
  - **for the worldgen, later:** "Caves should be much more sheered in the cracked biomes. Dim the sunlight to never brighter than 6, and the suns texture/brightness map reflects this. Save #4 for last."
- **0.61.0, the Dreadlands basics:** the random arrival (above; ours: within 256 of the gate's x/z, 48 counted past the most a spire reaches, facing anywhere); dreadwood saplings only on graveyard soil there (ours: graveyard soil into `#minecraft:dirt` so a sapling goes on it at all; dreadwood leaves drop saplings); vanilla's undead strayed in (ours: our spawner, one in 16 a player every 10 s, 1–2, stray 3 : zombie 2 : skeleton 2 : zombie villager 1, at most 4 near; config `undead_spawns`, `undead_cap`).
- **0.62.0, the post-defeat escalation** (the dump's section 6; the storm's length the owner's): the Dread round the Mausoleums after the first defeat; the defeat storm, 30-60 s, one per 6 real hours. Ours: what counts as a defeat (any death but `/kill`), within 128 of a Mausoleum's middle, one in 12, no village pieces, the first defeat told to everyone, Rimefrost I, fog to 48 in ash grey, snowflakes along the ground, the cooldown its own setting (`defeat_storm_hours`, by default her encounter's 6).
- **0.63.0, risen Dread** (the dump's mini phantoms and water beasts; the owner's answers above, and the cave spawning): bats and parrots rise as miniature dread phantoms, water creatures as themselves made Dread, where the Dread raise the dead; both stir rarely in caves and underground water. Ours: half size, 6 health, a 2 bite; ×2 health, ×1.6 speed, a bite of 1 + 15% of the kind's health; a frost tint; one in 40 a player every 10 s each, at most 3 near; underground water's glow squid 3 : squid 1 : cod 1 : salmon 1; the overworld and the Dreadlands.
- **0.64.0, the owner's notes from the dev client** (2026-11-23): the Dread's weather muffles distant sounds; our sounds' highs a little softened ("Music is fine, maybe clip the top end a tiny bit. Do that on all sounds actually"); dread phantoms' sounds 55% quieter; her storm puts out her fire; her crown thins her storm's drawn snow too (it "does not reduce the dread queen npc boss's whiteout dominion fog at all or should a little more"); every dread light at most 6 (asked earlier). Ours: the muffling curve and severities, 0.8 of our highs, all of a storm's fire put out.
- **Queued by the owner:** Dreadwood stairs, slabs, fences "etc."; dread torches and dread lanterns freezing water ("or compatible fluids") toward blue ice over about a Minecraft day, which sleeping can't skip; testing against Guard Villagers (do guards rise in her storm with their gear); then the terrain.
- **0.65.0, dreadwood's block set and dread lights' cold** (the owner, as above): ours: the eight blocks (no signs or boats), vanilla's dark oak door art recoloured, villages' wooden shapes turned; the cold's reach (2 across, 2 below, 1 above), a third of a day a stage, water's tag as "compatible fluids", lava untouched.
- **0.66.0, the terrain's land** (the dump's three biomes, its caves and lava pockets, spires grounded in the wastes; the owner's "Caves should be much more sheered in the cracked biomes"): ours: every number in `make_dreadlands_terrain.py`; sheer fissures as well as trenches in the chasma; dreadstone and packed ice blobs for the cave walls; no lakes (the dump's dry basins). Still to come with the sky: the sunlight never brighter than 6 and the sun's look ("Dim the sunlight to never brighter than 6, and the suns texture/brightness map reflects this"), the plains' wither roses and sparse dreadwood trees. Not done, the owner hadn't confirmed: spires half buried by dunes.
- **The dev start kit** (the owner, 2026-11-24: "make it so the dev clienet start me with the full dread queen set + a hydra heart - this is for dev only right now"): `DevKit`, only when `FMLEnvironment.production` is false. **Changed the same day:** "Instead give the player a full set of Dread dragonsteel armor and a diamond sword + hydra heart. Give each armor piece the general best enchantments for each piece and the sword." So each piece has Protection IV, Unbreaking III and Mending, plus Respiration III and Aqua Affinity on the helmet, Swift Sneak III on the leggings, and Feather Falling IV, Depth Strider III and Soul Speed III on the boots. The sword has Sharpness V, Looting III, Fire Aspect II, Sweeping Edge III, Unbreaking III and Mending. It's given once a player (the persisted `dreadpatch:dev_kit` flag), so a dev world that already had the old kit gets nothing new.
- **0.68.0, the Rime Chasma's floor** (the owner's two pictures, then: "No canyon walls. The floor is flat as shown with the hexegonal ridges and shattered deep cracks and ravines. These are below see level, up to y0 and cracks can go all the way to bedrock"; the floor "Mostly" at about y 30 down gentle slopes ("~y 30, gentle slopes"), "the cracks CAN drop down to bedrock. Rarely. But almost always deep enough to cause fall damage. Include pockets of water within the ice here at the basin to naturaly spawn some of the aquatic dread beast"; the floor's look "Mix of both" pictures; of the extras only "Tilted ice slabs", not frozen waterfalls, an expedition camp or layered crevasse walls). Ours: the floor's top at 30 and its ice 16 deep (bands of packed and blue ice, which the cracks show anyway); plates 18 across, a third of their edges cracks; cracks 7+ deep, most 10-30, rarely to bedrock; chasmas widened; the slabs' sizes; pockets 6-10 under the top; `chasma_dread_water` one in 8.
- **0.69.0, the owner's notes from the dev client** (2026-11-24, at 0.68.0; of the chasma, a picture: "Not nearly as sharply defined by as a hexagon"; another, of the ground: "Looks ok", "Biome ground gen look good"; "Make sure Bedrock is at least 1 layer thick throught the entire dimension"; "Could the Chasma biome be hazier a little bit. The other biome should have higher winds"; then "the music is not muffled by storms in the dreadlands. Tiny change and apply the muffling to juke boxes and noteblocks in dread weather?"). Ours: hexagonal plates, jittered a little; −64 never carved; the chasma's haze to 112 from 12; "the other biome" read as the Sastrugi Wastes (a gale: flakes in clear weather, twice as fast, the wind's sound); music, jukeboxes and note blocks muffled as though 8 blocks off, and every playing sound set again for the weather twice a second.
- **0.70.0, the rest of those notes:** of a guard risen in her storm, "Weapons and shields are left equiped. Armor drops on the ground" (ours: the thrall wore its iron all along, but Ice and Fire draws a thrall's armour as its own rags, and the guard drops its own as before: now any armour but chainmail is drawn as itself); "Ice and Fire's Liches Should sometimes spawn in dread villages" (ours: one village in three, `village_liches`, at its heart as it's made, kept for good); "Leaving a dread portal open no longer summons the Dread Queen or Syndragossa. It remains just a portal and does not despawn if players go in. It should slowly build a few layers of snow layers around it up to 3-4 blocks out" (ours: a keyed gate on either side stays open till its frame breaks, and lays a layer every 2 s at most, 3 deep beside it to 1 at 4 out; the gate home at her fall still closes after 5 minutes). So a gate no longer calls her: she's met atop the spires, or by egg or command. The owner, of all else at 0.68.0: "Otherwise everything looks ready to hit full beta release."
- **0.66.1:** her storm puts fire out every tick (the owner: "I observed her burning from fire aspect sword and continued burning").
- **0.67.0, the sky and the plains** (the owner's sunlight cap and the dump's roses and trees): ours: the lightmap capped for the sky's share only (block light unchanged), the sun at 0.42 in pale cyan, how it's drawn only (gameplay's sky light vanilla's); grave patches of graveyard soil carrying the roses and the plains' only wild dreadwood, one chunk in 6, a tree at 40%.
