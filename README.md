# Dread Patch

Patching in the Dread Queen for my own world.

[Ice and Fire: Community Edition](https://modrinth.com/mod/iceandfire-ce) ships a Dread Queen (her class, textures, name, sword and staff) but never puts her in the game. This NeoForge mod finishes her, and gives her the Rimefrost, undead frost dragons of the Dread, among them her own: Syndrigossa.

**Requires:** Minecraft 1.21.1, NeoForge 21.1.230+, Ice and Fire CE 2.1-beta.1+ (with Uranus, Jupiter and GeckoLib, which it requires anyway), and Iron's Spells 'n Spellbooks 3.x for 1.21.1.

**Status:** in development. In the wild she's met only atop the Dreadlands' spires.

## Calling her

- **Spawn eggs**, in the Spawn Eggs tab: the Dread Queen; the Dread Queen and Syndrigossa (her riding it, as the boss); Syndrigossa; an Ancient Syndrigossa; a Rimefrost Dragon; and a Dread Phantom.
- **Commands:** `/summon dreadpatch:dread_queen`, `/summon dreadpatch:dread_queen ~ ~ ~ {WithSyndrigossa:1b}` (riding her dragon), `/summon dreadpatch:syndrigossa`, and `/summon dreadpatch:syndrigossa ~ ~ ~ {Ancient:1b}` (full strength).

## The Dread Queen

A lean sovereign: 300 health behind 20 armor and 12 toughness (a full netherite set's), with a boss bar and her sword and staff. She has her original model from Ice and Fire, given a woman's figure (a bust in her chestplate's armor, as body mods give female player models), and she rises out of the ground when she spawns.

**Her theme**, "Beneath the Permafrost", plays once for everyone within 96 blocks when she or Syndrigossa spawns, cutting off whatever music was playing. It never starts over or plays twice at once (she spawns with her dragon), and no other music cuts it off. Once it ends, the usual music comes back after the usual wait.

She fights like a necromancer. She keeps her distance, circling at casting range and giving ground when pressed, and saves her sword for whoever still reaches her.

**Withering Rime** is the mark of all her magic. It withers through armor, freezes and stops all healing, and it feeds her: each second it withers anyone she afflicted (by her bolt, her crown's shrapnel, her sword, her Decree or her husk), she heals 1 on Easy, 2 on Normal and 3 on Hard for each of them, while she's within 32 blocks. Her victims lose 1 a second, as under anyone's Withering Rime; a player's Withering Rime feeds no one. No share of anything's max health she takes, from any spell or her sword, is ever more than 20, so a warden or a dragon is a long fight for her too.

**The Dread's own hits** carry it too. A dread scuttler's bite leaves Withering Rime II now and then (8%), as Syndrigossa's does, and one bite in three a brief 2-second Rimefrost, which a fresh bite renews but never deepens. A dread knight's blows leave Withering Rime now and then (8%), and 3 seconds of Rimefrost a little more often (13%). Every other Dread mob (thralls, ghouls, beasts, liches and the risen) leaves 3 seconds of Withering Rime with 5% of its hits and 5 seconds of Rimefrost with another 5%, rolled apart.

**One source at a time:** on a player, Withering Rime and Rimefrost each come from one source at a time. While one mob's lasts, another's doesn't land, so a pack can't keep piling it on; the same mob can renew its own. The queen and Syndrigossa always land theirs, taking it over. The weather's Rimefrost is no one's, so it never blocks anyone's. When her Withering Rime is on several targets at once, each feeds her in full unless the server caps how many do (`siphon_targets`) or shares her heal among them (`siphon_split`).

Her abilities:

- **Withering Rime-Bolt:** from range, a flame-sized streak of cold cyan dread fire and soot. It takes a share of the target's own life on top of its damage (5% of their max health, 8% on Hard), and leaves Withering Rime.
- **Rimefire Crown:** a ring of ice spikes round her target, wreathed in cold cyan dread fire over cyan and black smoke. It roots everyone inside and drinks every other fire within it. She then shatters it with her bolt for a burst of shrapnel.
- **Barrow-Step:** when cornered, she steps 10 to 15 blocks away through the cold, leaving a husk of rime-glass in her shape where she stood. Whatever was hunting her turns on the husk, and when it's struck, or after 5 seconds, it shatters, rooting everyone near it for 2 seconds and touching them with Withering Rime.
- **Rimewalk:** still water freezes under her feet, 4 blocks round, as under Frost Walker II boots, and holds while she stands on it, so water won't keep you from her: she walks straight across lakes and rivers. If she's in water, the surface freezes round her and she climbs out onto it. It's vanilla's frosted ice, which melts back to water in the light once she's gone.
- **Court of the Dread:** she raises Ice and Fire's own thralls, ghouls, knights and scuttlers from the ground around her. Dread scuttlers, hers and any others, spit a weaker Withering Rime-Bolt from up to 12 blocks every 3 to 4 seconds, and climb walls like spiders. They notice what's up to 24 blocks off, move a tenth faster than Ice and Fire made them, and every 5 to 8 seconds spit a web at whatever they hunt 4 to 24 blocks off (never at someone already caught in one): where it lands it leaves a **Dread Web**, which holds less than a cobweb and melts away after 3 seconds, drops nothing, and lets the Dread through. They don't spit webs where mobs can't change blocks (`mobGriefing` off).
- **Dread knights,** hers and any others, stand half again as large as Ice and Fire made them (2.7 blocks tall), walk an eighth faster, and step up a full block. Where a passage is lower than they are, they crouch like a player to squeeze in, down to 1.8 blocks, so a 2-block hallway won't keep one out; it still has to see you to strike, and its box is its real size, so it never hits through a wall.
- **Beater's Decree:** she burns one of her court from behind as it presses an enemy, a burst of rime that sends the rest of her court into a frenzy.
- **A Dread Lich:** half a minute into a fight, and at most every 90 seconds after, she raises one beside her, never two at once. It's Ice and Fire's own, with its skulls and the minions it raises itself (up to 5 over its life), fighting what she fights.
- **Create Rimefrost Dragon:** in a fight, once she's lost a quarter of her health, she raises the corpse of an ice dragon within 32 blocks as a Rimefrost dragon of hers, at most every 3 minutes and one at a time. It rises with half the health she's lost, and fights what she fights.
- **Dread phantoms:** 15 seconds into a fight, and every 45 seconds after, she calls one or two dread phantoms out of the air (below), never more than four of hers at once.
- **Whiteout Dominion:** every two minutes in a fight she calls down a blizzard 32 blocks round over the ground where she stands. It lasts 45 seconds and stays where it was raised; she ends it if she falls.
  - Inside it her enemies are slowed by a fifth, can't see past about 10 blocks through the snow-fog (mobs lose sight of anything past half their usual range, though a boss keeps its sight and is only slowed), and take a point of freezing damage every 3 seconds. Its cold puts out any fire inside it, hers too. From outside it shows as a wall of driven snow standing on the land.
  - Snow drifts over the ground under it and melts away as it clears. It's only drawn, so it never stays behind.
  - Its still water freezes over in patches as the snow drifts in, so a lake under it is ice to walk on. The ice holds while the storm stands and thaws as it clears. If the storm's chunks unload first, what's left melts in daylight like Frost Walker's.
  - Whatever dies in her storm rises where it fell as a dread mob of her court, up to 8 a storm: a thrall in its gear from a player or anything that stands upright (a villager, a skeleton, a creeper, an enderman), a ghoul from a zombie, a scuttler from a spider, a dread horse from a horse, a miniature dread phantom from a bat or a parrot (below, Dread phantoms), a Rime Eel from anything that lives in water (below, Rime Eels), and a dread beast its size from an animal or anything else. Golems stay dead. A thrall's armour looks as it did on the dead (a guard's iron, a player's diamond); only chainmail, which Ice and Fire's own thralls wear, takes their ragged look. What the dead carried still drops where they fell, as their own.

### Riding Syndrigossa

Spawned as the boss, she rises riding an ancient Syndrigossa. In the saddle she casts her bolt and crowns while her dragon flies and fights, her court rises on the ground below, and the two hunt the same prey.

- **Soul Tether:** while they're within 28 blocks of each other, everything her Withering Rime and the dragon's breath heal is shared between them half and half. Pull them apart and each keeps only its own.
- **Bringing her down:** she's hard to reach in the saddle, so it's her dragon you bring down. At half its health Syndrigossa is forced to land, the queen Barrow-Steps off and fights on foot for the rest of the fight, and the dragon can't take off again for 10 seconds. She also steps down if she falls to half her own health, or if her dragon dies under her.

### Her drops

- Her sword and her staff, each one time in four (Looting raises it: 30% at Looting I, 40% at Looting III).
- One epic or legendary ink.
- One scroll of her spells: Withering Rime-Bolt, Rimefire Crown, Barrow-Step, Whiteout Dominion, Summon Court, Create Rimefrost Dragon, Summon Dread Phantoms, Rimefire Blast or Sublimate. They're **Rimefrost** spells (below), and but for Rimefire Blast and Sublimate, found nowhere else.
- **A piece of her regalia,** when a player kills her (below).

### Her regalia

Her own outfit, to wear: the **Crown**, **Bodice**, **Robes** and **Greaves of the Dreaded Queen**. The crown is her ring of ice, the bodice her bodice with its necklace, cloak and silver pauldrons, the robes and greaves the rest, each drawn from her own model and texture.

- **Strength:** as dread dragonsteel's armour, and like the dragonscale and dragonsteel sets it takes 70% off dragon breath as a full set. It never breaks.
- **Each piece:** +150 max mana, 5% off the mana Ice and Rimefrost spells cost, 5% more Rimefrost damage dealt, and 5% less Rimefrost damage taken (Rimefrost spells, the Rimefrost dragons' breath and Shatter-Spine, Whiteout's cold, and Withering Rime's withering, counted as dealt by whoever laid the rime). The whole set: a fifth more dealt, a fifth less taken.
- **The whole set:** you can't freeze (powder snow, Withering Rime's chill and freezing damage don't touch you), the Dreadlands' weather slows you a step less (a heavy snowstorm as plain snow, plain snow not at all), and the Dread leave you be: they won't come for you unless you hurt them first, and then only that one, for half a minute. The queen herself, Syndrigossa and anything they command still do.
- **Her crown's sight:** the Dreadlands' storm fog doesn't close in on whoever wears her crown (snow or heavy snowstorm), and in any whiteout, the weather or a Whiteout Dominion's, they see 75% further, and a Whiteout Dominion's driven snow is that much thinner to them.
- **Collecting it:** each queen you kill drops a piece you don't have yet, so four kills complete the set. Looting gives a 10% chance a level of a second missing piece. Once you have the set, she drops a random piece one time in four (5% more a Looting level).
- **Advancements** keep count: one for slaying her, one for each piece you've held, and one for the whole set. A piece counts as yours once it's been in your inventory.
- **There Must Always Be a Dread Queen:** wear all four pieces with her blade and her staff in your hands.

**Armour trims** go on her regalia, Rimefrost dragonscale and dread dragonsteel armour at a smithing table, as on vanilla armour. Worn, a trim sits on vanilla armour's shape over the piece.

### What her defeat stirs

- **Round the Mausoleums:** once a Dread Queen has been defeated in a world (her death by anything but `/kill`, wherever she falls), the Dread rise for good round Ice and Fire's Mausoleums as they do in the Dreadlands on a clear day, and patrols cross there: within 128 blocks of one, in cold and snowy biomes, on snow, ice or dreadstone, never in a village. Everyone is told the first time. Off with `mausoleum_dread` at 0.
- **The defeat storm:** each defeat brings a dry storm over the cold lands for 30 to 60 seconds, then none for 6 real hours (`defeat_storm_hours`), whoever defeats her: out under the sky in a cold or snowy biome (or anywhere, with `defeat_storm_cold_only` off), an ash-grey fog closes in to about 48 blocks, rime blows along the ground, the Dreadlands' wind moans and its ice cracks, and a mild Rimefrost (10% slower) settles on you. No snow falls, nothing is hurt, frozen or raised, and sleeping doesn't end it. Get under a roof and it lifts; her full regalia keeps the Rimefrost off, and her crown the fog.

## The Dread Gate and the Gravemaster's Key

1. Build a frame of Ice and Fire's dreadstone (any of its stone, bricks, tiles or chiseled skulls) round an opening 2 to 21 blocks wide and 3 to 21 tall, as a Nether portal's can be. The corners can be left out.
2. Right-click the frame with a **Gravemaster's Key**, Ice and Fire's Mausoleum key, which the gate uses up.
3. The opening fills with a dark, cold-fire gate: a way to **the Dreadlands** (below). Step into it and it takes you there. Anyone who follows through the same gate arrives where you did.
4. It stays open until its frame is broken. Over the minutes it's open, snow drifts up round its foot, up to three layers deep beside it and thinning out to a single layer 3 to 4 blocks out.
5. **The Dread come out of it.** Now and then one of the Dread (a thrall, ghoul, knight or scuttler) steps out at its foot, about twice as often as zombified piglins come out of a Nether portal, and more often on harder difficulties. None come on Peaceful, with `doMobSpawning` off, or from a gate in the Dreadlands.

- **In a Mausoleum:** every Mausoleum already has four gates. The arches of the shrine round the ice dragon forge in its far hall are dreadstone frames 3 wide and 6 tall.
- **Breaking the frame** closes the gate, and the key is lost.

The Gravemaster's Key still opens a Mausoleum's sealed door too, and it can now be crafted: three Dread Shards, a Bone and two Gold Ingots in the shape of a key (shards and bone across the top, a shard below, the gold at the bottom right). The recipe unlocks when you pick up a dread shard.

## The Dreadlands

A dimension of bleak frozen waste: gently rolling snowfields, carved here and there by the wind into fields of long drifts like dunes, over packed and blue ice, black dreadstone breaking through here and there, under a grey sky with snow drifting and the wind moaning, ice cracking somewhere out on the waste, something rumbling deep below, and now and then, far off and never from the same quarter twice, a dragon's roar. Its day, night and weather are the overworld's.

- **Its lands:** three kinds, side by side, each stretching hundreds of blocks.
  - **The plains:** the snowfields above, with stretches of hill country: broad snowy swells and frozen ridgelines rising to about y 120-130 over ground that lies about y 80.
  - **The Sastrugi Wastes:** a sunken, dry seabed about 35 blocks lower, its tall smooth dunes (up to about 17 blocks over and under its middle) making deep blind valleys, reached down long shelves or sheer bluffs of packed ice and dreadstone. A gale blows across them, driving snow past even in clear weather, and harder in their storms. Spires rise there too.
  - **The Rime Chasma:** a frozen lakebed sunk well below the wastes, reached down gentle slopes. Its floor is a flat sheet of ice, clear blue in places and snow-covered in others, broken into hexagonal plates outlined by low ridges of snow. A faint haze hangs over it in any weather. Some plate edges have split into cracks too deep to fall into safely, and wider ravines cut through it, pinching shut here and there into snow bridges. Now and then one drops all the way to bedrock. Slabs of ice lie heaved up at the cracks' edges, and pockets of water are sealed in the ice beneath, where Rime Eels stir.
  - **Crags:** now and then, on the plains and in the wastes, a cluster of jagged spikes of bare dreadstone and packed ice juts up to about 30 blocks over the land, all ledges and overhangs. Steep ground anywhere shows rock and ice, not snow.
  - **Below:** caves run down to bedrock (a floor of it everywhere, never holed) through stone, dreadstone and packed ice, and very rarely a pocket of lava lies sealed in a crust of ice, basalt, dreadstone and obsidian. The basins stay dry, but the deeps are flooded: every open space from bedrock up to y −52 is black, still water, so the lowest caves and the cracks that reach the bottom end in underground lakes.
  - **The sky:** dim. However high the sun, the open ground looks no brighter than light 6, and the sun itself burns small, pale and cyan. Torches and lanterns still light as they do. Only how it's drawn changes; with a shader pack on, the pack's own sky likely shows instead.
  - **Grave patches:** here and there on the plains, a patch of graveyard soil with a wither rose or three, and now and then a dead dreadwood tree, a stub of a branch and a leaf or two at most, the only dreadwood growing wild.
  - **Dragon bones:** now and then, in any of its lands, the skeleton of an ice dragon lies out on the ground, as they lie in the overworld's snow but far more often: about one chunk in a hundred. They're Ice and Fire's own, with its bones to take.
  - Ground generated before this keeps its old shape, so a seam can show where old meets new.
- **The Dread walk there:** Ice and Fire's thralls, ghouls, knights and scuttlers rise out of the snow in ones, twos and threes, by day and by night, 24 to 48 blocks off: now and then in clear weather, more often while it snows, and a little more often again in a heavy snowstorm (thunder). No more than 8 of them gather round any one player.
- **Patrols:** now and then a small Dread patrol (a dread knight on foot leading two to four more) crosses the waste, its first leg passing by you, then on from waypoint to waypoint. They fight what they meet, then walk on; if their knight falls, another leads.

- **Its storms:** when it rains in the overworld, it snows there, and that's a storm. Out under the open sky the fog closes in to about 56 blocks, and to about 24 when it thunders (a warning; a Whiteout Dominion's is 10), snow blows past on the wind, and **Rimefrost** settles on you: the Dread's cold without the withering, 10% slower in snow and 25% in a heavy snowstorm (thunder), for as long as you stay out in it. It does no damage, and the Dread don't feel it. Get under a roof and it lifts.
- **Whiteout storms:** about one storm in six (never two in a row) is a Whiteout Dominion, like the Dread Queen's own storm across the whole land: out under the open sky the fog closes in to 10 blocks, the wind howls and the snow drives past, you're slowed further, and the cold does half a heart of damage every 3 seconds unless you can't freeze (leather, her full regalia). Get under a roof. It never starts while a Dread Queen is about there, and if she comes during one, it breaks into a heavy thunderstorm. How often is a server setting (`whiteout_storms`; 0 for never).
- **Its lightning:** when the overworld thunders, lightning strikes round you every 20 seconds or so (Minecraft's own never strikes ground that cold). It hurts what it hits, as lightning does, but sets no fire: it leaves a patch of **rimefire**, cold dread fire burning cyan for 30 to 60 seconds. One of the Dread standing in it heals a quarter of a heart a second; anyone else is chilled (Rimefrost), put out, and burned by the cold, half a heart a second. Rimefire is a hazard only: it gives nothing and can't be lit by players. It glows dimly (light 6), too little to melt the snow and ice round it. Rimefrost dragons' breath lights patches of it where it lands (with `mobGriefing` on), and a Withering Rime-Bolt that strikes the ground leaves a few seconds of its embers.

- **Getting there:** step into an open Dread Gate (above). You arrive somewhere random within about 256 blocks of where the gate stands, never on a spire or within 48 blocks of one, on open ground (or, if there's none, lower down); anyone who follows through the same gate arrives at the same spot.
- **The spires:** rare and tall (64 to 100 blocks), standing far apart on the waste. They're dreadstone and dreadstone bricks over stone at the foot, banded with packed and blue ice, giving way to packed ice and snow, flaring out again near the top to a broad, flat summit (room for Syndrigossa) crowned with ice spikes. A ledge of dreadstone bricks winds up the outside from the ground to the summit.
- **Summit chests:** half the spires keep a chest on the summit: dread shards, bones, Rimefrost scales, iron, gold, sometimes diamonds or ink, sometimes a Gravemaster's Key (a way home), now and then a scroll of one of her lesser spells, rarely a Rimefrost Dragonforge Core, and now and then the music disc **Entombed In Rimefrost**. Now and then a Dread Lich guards it.
- **Abandoned villages:** snowy villages overrun by the dead, their houses rebuilt in dreadwood and dreadstone among bare dead trees, lit by dread lanterns and torches burning cyan, zombie villagers still shambling through them. About one in three is kept by a Dread Lich, which raises the dead to guard it. Their chests hold what any village's would.
- **Lootr:** with Lootr installed, every chest in the Dreadlands (summits and villages) gives each player their own loot.
- **The Queen and Syndrigossa:** half the spires hold her, and you can tell which from below: a dreadstone dais at the summit's middle, with dread torches burning cyan round it. Reach such a summit and she comes, as the full boss, riding an ancient Syndrigossa out of the air beside it (on foot on the summit, if there's no room for her dragon), and turns on you. Each spire's encounter comes to each player once; it comes to you there again only after 6 real hours (a server setting, `encounter_reset_hours` in `config/dreadpatch-server.toml`), and never while that spire's queen is still about. Anyone who reaches the summit while she's there has met it too. Not on Peaceful.
- **Her defeat:** when she falls, a Dread Gate rises where she fell, open home for 5 minutes (time to take what she dropped, and her dragon's corpse), fading out over its last seconds. Its frame stays for a key.
- **Getting home:** beat a spire's queen (above), or build a Dread Gate there and key it. It stays open until its frame is broken, and takes whoever steps in back to the gate they came in by (or to the world's spawn). A key takes three dread shards (dread phantoms drop them), a bone and two gold ingots, so bring the gold; the frame takes any dreadstone, the spires' included. Dying sends you to your usual spawn, as anywhere.
- **Flames die cold there:** a lantern or torch you place in the Dreadlands fades, a step every 30 to 45 seconds, to its soul kind and then to the Dread's: a **dread lantern** (a soul lantern burning her cyan; break it with a pickaxe to keep it) or a dread torch.
- **Its music:** "Entombed In Rimefrost", the only music that plays there, and only there, coming now and then as Minecraft's own music does (every 10 to 20 minutes). The first time you arrive (the advancement **Into the Dreadlands**) it starts at once. Its disc (from summit chests) plays it in any jukebox.
- **The weather muffles:** in its snow and storms (and in any whiteout, or the defeat storm) distant sounds are quieter and duller, the further off the more so, and the worse the weather the more. Music, jukeboxes and note blocks are muffled too, even close by, and every sound follows the weather as it comes and goes. Its own sounds and music are a little softened at the top.
- **Dread lights:** every dread light, a dread torch, a dread lantern, a Dread Gate's face, rimefire, a lit Rimefrost dragonforge, glows at light 6 at most, too dim to melt snow or ice. A dread torch or lantern is cold, too, anywhere: still water within 2 blocks of it (2 below, 1 above) turns to ice, then packed ice, then blue ice over about a Minecraft day, a third of it each, and sleeping doesn't hurry it (`dread_light_freeze_minutes`).
- **The sun burns no undead there:** zombies, skeletons and the rest walk its snow by day unharmed, the abandoned villages' zombie villagers among them.
- **Stray undead:** now and then one or two of Minecraft's own undead (strays mostly, zombies, skeletons, zombie villagers) rise there too, by day or night, never more than 4 round any one player. The Dread fall on them as on anything living.
- **Dreadwood:** there a dreadwood sapling takes root only in graveyard soil (Ice and Fire's, which raises ghosts at night); on anything else it won't go down, and one already standing elsewhere never grows. Farming isn't easy.
- **Dreadwood's block set:** stairs, slabs, fences, fence gates, doors, trapdoors, buttons and pressure plates of dreadwood planks, crafted as oak's are. The abandoned villages' wooden ones are dreadwood's.
- **Beds** explode there, as in the Nether, and respawn anchors don't work.
- **Dread phantoms** come down out of its sky now and then, one or two at a time, whether or not you've slept: about once every 4 minutes for each player out under the sky, and about every minute and a half near a spire, never while 4 are already about.
- **All these rates** are server settings (Config, in the mods list, or `config/dreadpatch-server.toml`).

## Rimefrost dragons

The Rimefrost are, in the lore, a subtype of frost dragon: undead dragons of the Dread, risen from Ice and Fire's ice dragons and rotted to the bone (soot-black scales, bone showing through, torn wings) in their own colour, white, sapphire, silver or blue. They're allied with the queen and her court, and they grow, and their health grows, as an ice dragon's does. Spawn one with the **Rimefrost Dragon Spawn Egg** (Spawn Eggs tab) or `/summon dreadpatch:rimefrost_dragon`. Tamed, one can be ridden like any Ice and Fire dragon.

What every Rimefrost dragon has:

- **Breath of Withering Rime**, in a wide cone (20 degrees either side) rather than Ice and Fire's thin stream, reaching 2 blocks past what it breathes at, up to 32 blocks from its head; nothing of it, splash or particles, reaches further. It hits everyone in the cone and where it lands as hard as an ice dragon's breath of its size, leaves Withering Rime, and drinks fire instead of freezing the ground. Each hit siphons half a heart more, past armor, into it.
- **Its bite** leaves Withering Rime II, which withers twice as fast, for 4.5 seconds. Tail and wing strikes don't.
- **Shatter-Spine**, its charge: a core of bone and dread-glass that bursts 9 blocks above its target into 5 to 7 shards raining over the ground below. Each cuts everyone within a block and a half and leaves Withering Rime.
- **In the air:** it takes off far more than other dragons (with a target on the ground it's up in about 3 seconds), favours strafing fly-bys, breathing as it passes, and looses Shatter-Spine every 10 seconds at whatever it's hunting. It can't hunt players in creative or spectator. Its breath is big cold dread-fire flames and soot clouds you can see from the ground, Shatter-Spine flies as a burr of dark ice spikes and bursts in a ring, and its wings keep beating while it breathes on a fly-by.
- **Undead:** it never sleeps. Smite hits it harder, Healing hurts it and Harming heals it, and Poison, Regeneration and Wither don't take. Like ice dragons, it can't freeze or drown.
- **Players:** it hunts them even when Ice and Fire's `neutralToPlayer` setting makes other wild dragons leave them alone.
- **Its corpse:** harvested like any dragon's, empty-handed, but it's Dread. It gives **Rimefrost Dragon Scales** (more the older it was) or dread shards (no flesh, and no blood for a bottle), a handful of dread shards and witherbones where a dragon's heart would be, no ice dragon egg, then dragon bones and an ice dragon skull.

## Syndrigossa

The unique, named Rimefrost dragon, risen as the queen's mount; still in progress. She has everything above, always in white, and what's hers alone:

- **Sovereign's Eclipse:** every 30 seconds, once she's grown, her own Rimefire Crown round her target, 16 blocks across with dark spires and indigo fire, standing 10 seconds. It doesn't root and doesn't close in, but everyone inside is heavily slowed (Slowness IV) and kept under Withering Rime, and it drinks the fire inside it.
- **Dread phantoms:** wild, she calls them as the queen does (below).
- **Her own boss bar,** white beside the queen's blue (none while she's tamed).
- **Health:** set by difficulty, not by Ice and Fire's dragon health setting: 500 at full strength on Easy, 750 on Normal and 1,000 on Hard. A younger one has the same share of that as an ice dragon her age. Change the difficulty and she keeps the same share of her health.
- **Growth:** from her spawn egg or `/summon` she's Ice and Fire's random age (1 to 80 days), with an ice dragon's bite and armor for it.
- **The boss:** as the boss she's ancient, at full strength: 125 days, so on Hard 1,000 health, and in a pack with Ice and Fire's dragon damage at 20, 21 bite and 20 armor.
- **Her theme's disc:** one Syndrigossa in four leaves the music disc **Beneath the Permafrost** with the heart's haul. It plays a little quieter than other discs.
- **Her egg:** only the boss, an ancient Syndrigossa, also leaves her **Syndrigossa Egg** with the heart's haul, male or female. It hatches by a Dread rite:
  1. Lay it on top of dreadstone (any of the blocks a Dread Gate's frame takes) by right-clicking.
  2. Let three Whiteout Dominion storms pass over it, start to end; yours or the queen's both count. Each storm must rise with the egg already inside it and run its full course; one cut short, or recast over, doesn't count. You're told how many it has soaked up.
  3. The third hatches it into a white Rimefrost dragon hatchling, tamed to whoever laid it. Syndrigossa is unique, so her eggs hatch Rimefrost dragons. Name the egg with a name tag and the hatchling keeps the name.

  Strike a laid egg to pick it back up; its storms are lost. Nothing else harms it.

  As with Ice and Fire's own hatchlings, right-click it with an empty hand while it's small to carry it on your shoulder, and sneak to set it down.

## Dread phantoms

Phantoms of the Dread: as strong as Minecraft's own phantoms, with a phantom's 20 health and bite, but they don't burn by day. They look like a phantom gone to the Dread, its hide soot and slate still faintly blue, its bones pale, its wings torn through to the finger bones, its eyes the Dread's cyan, and they leave a sparse trail of rime, snowflakes and now and then a small cyan flame. Under a phantom's screech, each of their sounds carries something ghostlier. Spawn one with the **Dread Phantom Spawn Egg** (Spawn Eggs tab) or `/summon dreadpatch:dread_phantom`.

- **Who calls them:** the Dread Queen, any Dread Lich, and a wild Syndrigossa, each one or two at a time while they have something to fight: the first 15 seconds in, then every 45 seconds, never more than four of their own at once. Players can call them with the Summon Dread Phantoms scroll (below).
- **What they do:** they circle above whoever called them, take up their fights, and turn on whatever hurts them or the one who called them, diving on it at once. A dive is committed: hitting a diving phantom doesn't turn it aside, as it does a phantom; only its bite or a wall ends the dive. The harder the difficulty, the more often they dive: every 8 to 11 seconds on Easy (a phantom's pace), 6 to 8 on Normal, 4 to 6 on Hard. With no one to answer to, they hunt players as phantoms do. They're of the Dread: allied with the queen and her court, and untouched by Withering Rime, Whiteout and Rimefrost.
- **Their sounds:** a phantom's, under something ghostlier, all of them quieter than a phantom's (45%).
- **Their bite:** a phantom's, and one bite in three (35%) leaves 5 seconds of **Rimefrost**, the Dread's cold without the withering (25% slower). One in seven (15%), rolled apart, leaves 3 seconds of Withering Rime, so a bite can leave both.
- **Their drops:** a dread thrall's (bones and dread shards), and when a player kills one, 1 to 3 phantom membranes and 1 to 3 Rimefrost Dragon Scales, each with up to one more a Looting level. The ones a player calls leave nothing.
- **Natural spawning:** in the Dreadlands, and only there, one or two at a time, rarely, whether or not you've slept (above).
- **Miniature dread phantoms:** a bat or a parrot that dies where the Dread raise the dead (the queen's storm) rises as a dread phantom half the size, with 6 health, a weaker bite (2) with the same chances of Rimefrost and Withering Rime, and a higher voice. Now and then one stirs in a dark cave too, in the overworld or the Dreadlands. When a player kills one, it may drop a phantom membrane.
- **Dread water creatures:** now and then a squid, a glow squid, a cod or a salmon stirs in the overworld's underground water turned Dread: frost-grey, twice its health, much faster, and hunting players, its bite sized by its health with the dread phantom's chances of Rimefrost and Withering Rime. The Dread's rime and storms don't touch it. Rare (`cave_dread_phantoms`, `cave_dread_water`). (Until 0.71.0 anything living in water rose this way in the queen's storm; now it rises a Rime Eel, below.)

## Rime Eels

Serpentine dread anglerfish of the Dreadlands' dark water: a big, bone-pale anglerfish head with a cold cyan cast, its jaw hanging open on long fangs, black eyes burning cyan, fanned bony fins and a jagged dorsal fin, and a thick, ribbed serpent's body curling round behind it into a ring. About one in six has a lure: a tall stalk reaching over its snout with a lantern hanging from it, which sparkles while lit, hangs straight down whichever way the eel turns, reaches out while the eel hunts or swims about, and draws in over its head while it's still. The rest have no lure at all. Each comes in its own size, 0.6 to 2.5 blocks across, with 10 to 18 health and a bite of 2 to 4 to match, and 4 armour; one in twenty is an alpha or matron, 2.5 to 4 blocks across, with up to 24.5 health and a bite of 5.5. Spawn one with the **Rime Eel Spawn Egg** (Spawn Eggs tab) or `/summon dreadpatch:rime_eel`.

- **Where:** any dark water in the Dreadlands (two blocks deep at least, light 6 or less), even the scattered pockets sealed in the Rime Chasma's ice, rarely (`dreadlands_rime_eels`, `cave_dread_water`, `chasma_dread_water`), never more than four near you. And anything that lives in water and dies in the queen's storm rises as one of hers.
- **In water and out:** it swims fast and climbs single blocks; out of water it slithers like a snake, never drowns or dries out, and is as keen to get back to water as to chase its prey, whichever it sets out on first.
- **What it hunts:** players and any mob not of the Dread (not dragons or bosses). It's of the Dread: allied with the queen and her court, untouched by Withering Rime, Whiteout and Rimefrost.
- **Its lure:** flickers erratically, off far more often than on. In a fight it pulses every 3 to 5 seconds, laying 4 seconds of Rimefrost (25% slower) or Whiteout on everything within 4 blocks that isn't of the Dread, and lighting up (Glowing, 4 seconds) everything within it but Rime Eels, the Dread included. If the pulse catches anything, the eel goes into a **Frenzy** for 4 seconds and bites twice as fast. Eels without a lure never pulse.
- **Glowing prey:** any eel whose prey is Glowing (from a lure's pulse, a spectral arrow, anything) goes into a Frenzy while it glows, so one lantern eel sets the eels round it on whatever it lights.
- **Its bite:** by its size, once a second (twice in a Frenzy), with a 10% chance of 3 seconds of Withering Rime. On land it lunges at its target from up to 5 blocks, then waits 2 seconds before the next.
- **It eats:** dropped food (anything edible but golden apples and golden carrots, plus ink sacs), one item at a time, healing 0.25 to 1 health each (rotten flesh 0.5, a steak 1), so it clears out a mob farm's leftovers.
- **Its drops:** 1 to 2 bones and 1 to 2 dread shards (up to one more of each a Looting level), now and then a rotten flesh (10%), and rarely (5%) 1 to 2 Rimefrost Dragon Scales.

## Rimefrost dragonscale and dread dragonsteel

Rimefrost dragons' scales make the Dread's answer to Ice and Fire's ice dragon gear, with Ice and Fire's ice sets' own numbers and models, recoloured soot, bone and cyan:

- **Rimefrost Dragon Scales:** from a Rimefrost dragon's corpse (above), from dread phantoms a player kills, or now and then from a Rime Eel.
- **Rimefrost Dragon Blood:** Rimefrost Dragon Scales and a glass bottle in the Rimefrost dragonforge (below), as Ice and Fire makes ice dragon blood.
- **Dread Dragonsteel Ingot:** an iron ingot and Rimefrost Dragon Blood in the Rimefrost dragonforge.
- **Rimefrost dragonscale armour,** crafted from the scales like Ice and Fire's dragonscale armour.
- **Dread dragonsteel armour, sword, axe, pickaxe, shovel and hoe,** crafted from the ingots (the tools on witherbone handles) like Ice and Fire's dragonsteel. Where ice dragonsteel freezes what it hits, dread dragonsteel leaves 3 seconds of Withering Rime.
- **Against dragon breath,** each piece of either armour takes off as much as Ice and Fire's dragon armour does (10% helmet and boots, 30% chestplate, 20% leggings: 70% for a full set), against Ice and Fire's dragons and Rimefrost dragons alike. Ice and Fire's own dragonscale and dragonsteel armour now guards against Rimefrost breath too.
- **Against webs,** each piece of dread armour (Rimefrost dragonscale, dread dragonsteel or her regalia) takes an eighth of a web's hold off, half for a full set: cobwebs and the scuttlers' webs still slow you, just less. Powder snow and berry bushes hold as ever.

## The Rimefrost dragonforge

Dread dragonsteel is forged in the Rimefrost dragonforge, and only there: Ice and Fire's ice dragonforge no longer makes Rimefrost dragon blood or dread dragonsteel. It's Ice and Fire's ice dragonforge made Dread, built and worked the same way:

- **Building it:** a 3×3×3 cube. Its core sits in the middle, with Rimefrost Dragonforge Bricks round it, above it and below it. The 8 corners of the top and bottom layers are Ice and Fire's blocks of dragon bones, and the aperture takes the place of one side's brick. Once it stands whole, its core glows her cyan and the bricks beside it show their grill.
- **Its blocks:**
  - **Block of Rimefrost Dragon Scales:** 9 Rimefrost Dragon Scales, and back.
  - **Rimefrost Dragonforge Brick:** 5 blocks of Rimefrost Dragon Scales and 4 dreadstone bricks (scale blocks on the corners and in the middle) make 4.
  - **Rimefrost Dragonforge Core:** 8 bricks round an ice dragon heart. Now and then one turns up in a spire's summit chest.
  - **Rimefrost Dragonforge Aperture:** 4 bricks on the corners and 4 iron ingots on the sides.
- **Lighting it:** only a Rimefrost dragon's breath lights it, landing on its aperture (an older dragon's breath needn't land quite so close). A whole forge with something to make draws a tame Rimefrost dragon within 50 blocks that can see the aperture, and it breathes on it until the work is done, as Ice and Fire's tame dragons tend its forges. A wild one's breath lights it too, Syndrigossa's in her fight included. While the breath is on it the aperture glows; once the breath stops, the work holds where it is until the breath comes back.
- **Using it:** right-click the core, a brick or the aperture to open it. As in Ice and Fire's, what's being made goes in the first slot and blood in the second, and the work comes out on the right. Hoppers on the top brick feed the first slot, hoppers on a side brick feed the blood slot, and a hopper under the bottom brick takes out what it makes.
- **What it makes:**
  - **Rimefrost Dragon Blood:** Rimefrost Dragon Scales and a glass bottle (37.5 seconds of breath).
  - **Dread Dragonsteel Ingot:** an iron ingot and Rimefrost Dragon Blood (50 seconds of breath).
  - Packs can add their own: any Ice and Fire `iceandfire:dragonforge` recipe with `"dragonType": "rimefrost"`.
- **JEI:** with JEI installed, its recipes have a page of their own.

## Her staff and sword

**Her staff** is an Iron's staff now:

- It carries three of her spells at level 3 (Withering Rime-Bolt, Rimefire Crown and Summon Court), on your spell wheel while you hold it, cast with right-click. Its tooltip lists them a line each; hold Shift for each one's details.
- It gives +15% Ice Spell Power, +5% Rimefrost Spell Power and +10% Cooldown Reduction in the main hand.
- **Permafrost Conduit:** Ice spells cost 20% less mana while you stand on snow or ice.

**Her sword** is unbreakable, with 11.5 attack damage at 1.6 speed and half a block more reach. She swings the same sword, so her own strikes leave Withering Rime and shatter too.

- **Thermodynamic Theft:** every strike leaves 4.5 seconds of Withering Rime.
- **Glass-Rime Shatter:** every third full strike on the same chilled or frozen foe, each within 5 seconds of the last, shatters its frost for an extra 10% of its max health past its armor (at most 20). It also leaves the foe **Brittle** (10% less armor) for 4 seconds. Hitting others in between doesn't reset the count, so it works with Better Combat's sweeping swings.
- **Fire-Drinking Guard:** it rises while you hold right-click, slowing you like a shield. It stops fire and magic from the front (Iron's spells and Ice and Fire's dragon fire included; arrows and blades still get through), and puts out fire on you and around you. A hit it stops charges the blade, which glints until its next full strike shatters at once. With the sword in your main hand, an off hand holding something with its own right-click (a shield, food, a bow, blocks, throwables, a staff) keeps that right-click, so raise the guard with that hand empty or holding something like a totem.

## Her spells, as a player

Her scrolls (from her drops) teach her spells to players. They're **Rimefrost** spells, a school of their own and a kind of Ice magic: Ice spell power strengthens them as well as Rimefrost spell power, Ice magic resistance guards against them as well as Rimefrost's, and anything that makes Ice spells cheaper does them too.

- **Rimefire Blast** (levels 1 to 10, 12-second cooldown), held like Cone of Cold: a cone of cold dread fire pours from your hand, burning little but leaving 3 seconds of Withering Rime on everything it touches. Unlike her other spells it can be made: put a dread shard in Iron's Scroll Forge as the focus, with ink and paper.
- **Sublimate** (levels 1 to 5, 8-second cooldown), cast in an instant: a sudden cone of rime turning to vapour from your eyes, 8 blocks long at level 1 to 12 at level 5 and a few blocks wide at its end. It pierces everyone in it you'd harm (walls stop it), striking with Rimefrost damage (spell power: 4 at level 1, 8 at level 5) and leaving Rimefrost I, the mild slow, for 3 to 7 seconds by level (casting it again renews the slow but never deepens it), and a moment's **Whiteout**, 1 to 2 seconds by level: snow-fog for a player, half its sight for a mob (not a boss's). It can be made at Iron's Scroll Forge from Epic ink up (a dread shard as the focus), and it's found on Dread Liches killed by a player (one in ten), in spire summit chests and in her drops.
- **Withering Rime-Bolt**'s share of the target's life grows with its level: 3% at level 1 to 7% at level 5.

- **Summon Court** raises her court round you, Ice and Fire's thralls, ghouls, knights and scuttlers rising out of the ground: 2 at level 1 up to 6 at level 5, standing 70 to 110 seconds, on a two-minute cooldown. They follow you and fall on hostile mobs that attack you or that you attack. They never fight the Dread (the queen and her court leave them be too), never turn on friendly mobs, villagers, golems, pets or other players, and don't fight back when those hit them; and friendly mobs leave them be in turn. You can't hurt them, nor can your spells. Casting it again dismisses the court you raised before. A server can change how they treat friendly mobs and players in `config/dreadpatch-server.toml` (`attack_friendly_mobs`, `retaliate`, `attack_players`, all off by default).
- **Summon Dread Phantoms** calls two dread phantoms down over you for a minute, on a 90-second cooldown. They circle above you and dive on hostile mobs that attack you or that you attack, under the same rules as Summon Court's court: never the Dread, never friendly mobs, villagers, golems, pets or other players unless the server allows it, and you can't hurt them. They leave nothing behind. Casting it again dismisses the ones you called before.
- **Create Rimefrost Dragon** raises the corpse of an Ice and Fire ice dragon you look at (within 24 blocks) as a Rimefrost dragon of the same age, gender and colour, tamed to you, to ride and command like any tamed Ice and Fire dragon. It rises with half the health you're missing (rounded down), so the closer you are to death, the stronger it rises; at full health it rises with 1. It costs 500 mana and then every drop you have left. You keep one at a time. Its cooldown is 3 minutes, and within them you can recast it once, for free, to send your dragon back and raise the corpse you're looking at instead; the cooldown still ends 3 minutes after the first cast. Like Iron's own Raise Dead, it can't be cast straight from its scroll: inscribe it in a spellbook.
- **Barrow-Step** takes you to where you look (11 to 15 blocks, by level) and leaves your own husk behind: rime-glass in your shape and skin.
- **Whiteout Dominion** (16 to 32 blocks round and 25 to 45 seconds, by level, on a two-minute cooldown) fogs, slows and freezes whoever you'd harm in it (a boss is slowed but keeps its sight, so it won't lose you in the snow), and freezes its water over too, but only hers raises the dead. The Dread walk through anyone's storm untouched.

## Fixes to Ice and Fire

- **Riding a dragon:** a player steering any Ice and Fire dragon (fire, ice, lightning, or a tamed Rimefrost dragon) is drawn sitting upright in the saddle on its back, moving with it as it walks, flies and pitches. Ice and Fire drew them upside down under the dragon's belly. As before, you don't see yourself in first person. Prey shaken in a dragon's jaws is still drawn as Ice and Fire draws it.
- **Dragon corpses:** Ice and Fire still counts a dead dragon as alive while its corpse lies, so a mob that was fighting a dragon when it died went on attacking its corpse forever. Now nothing fights a dragon's corpse, any Ice and Fire dragon's, and mobs from other mods that use Minecraft's usual targeting leave it be too. Harvesting a corpse is unchanged.
- **Dreadwood leaves:** Ice and Fire gives them no loot, so they dropped nothing. Now they drop dreadwood saplings and sticks as oak leaves drop theirs (the leaves themselves with shears or Silk Touch).

## Commands

For operators and the server console, under `/dreadpatch`:

| Command | What it does |
|---|---|
| `/dreadpatch weather` | Tells you the Dreadlands' weather: clear, snow, a heavy snowstorm or a whiteout. |
| `/dreadpatch weather clear\|snow\|storm\|whiteout [seconds]` | Sets it, for 10 minutes unless you say. The Dreadlands share the overworld's weather, so the overworld's changes too. `whiteout` makes the storm a Whiteout Dominion whatever its odds; `snow` and `storm` are plain. |
| `/dreadpatch encounters reset [players]` | Forgets who has met which spire's Queen, so her encounter comes again (everyone's, unless you name players). |
| `/dreadpatch dreadlands enter [players]` | Sends players into the Dreadlands, where a gate would set them down, remembering where they came from. |
| `/dreadpatch dreadlands home [players]` | Sends players in the Dreadlands home again. |
| `/dreadpatch dreadlands arrival [x z]` | Where a gate there (or where you stand) would set someone down in the Dreadlands, and how far that is from the nearest spire. Arrivals are random, so each try may differ. |
| `/dreadpatch escalation [storm [seconds] \| reset]` | Whether a Dread Queen has been defeated in this world (so the Dread stir round the Mausoleums) and the defeat storm: on, waiting, or due; `storm` brings one now (30-60 s, or the seconds given), whatever the wait; `reset` forgets every defeat and the storm. |
| `/dreadpatch config [setting [value]]` | Lists every Dread Patch setting, shows one, or sets one (saved to `config/dreadpatch-server.toml`, applied at once). |
| `/dreadpatch patrol [pos]` | Raises a Dread patrol there (a dread knight leading 2 to 4 more). |
| `/dreadpatch rise [count] [pos]` | Raises 1 to 8 of the Dread there (3 by default). |

## Settings

Dread Patch's server settings (how a summoned court behaves, how long until a spire's queen comes back for you, how often the Dread rise, patrol and fly in the Dreadlands, how often its storms are whiteouts) are in the mods list: select Dread Patch and press **Config**. In singleplayer you can change them there as you play, and they apply at once. On a server they're in `config/dreadpatch-server.toml` (a copy in the world's `serverconfig` folder overrides it).

## Building

`./gradlew build` puts the jar in `build/libs/`. `./gradlew runClient` starts a dev client with Ice and Fire loaded.

## Credits

Her model is the geometry of Ice and Fire's unused `ModelDreadQueen` (Alexthe666 and contributors, LGPL-3.0). Her textures, name, sword, staff and behavior come from Ice and Fire CE at runtime. The Dreadlands' music, "Entombed In Rimefrost", and her theme, "Beneath the Permafrost", are hammerknife404's own.
