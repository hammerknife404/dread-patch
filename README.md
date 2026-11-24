# Dread Patch

Patching in the Dread Queen for my own world.

[Ice and Fire: Community Edition](https://modrinth.com/mod/iceandfire-ce) ships a Dread Queen (her class, textures, name, sword and staff) but never puts her in the game. This NeoForge mod finishes her, and gives her the Rimefrost, undead frost dragons of the Dread, among them her own: Syndrigossa.

**Requires:** Minecraft 1.21.1, NeoForge 21.1.230+, Ice and Fire CE 2.1-beta.1+ (with Uranus, Jupiter and GeckoLib, which it requires anyway), and Iron's Spells 'n Spellbooks 3.x for 1.21.1.

**Status:** in development. In the wild she's met only atop the Dreadlands' spires.

## Calling her

- **A Dread Gate** (below), with a Gravemaster's Key.
- **Spawn eggs**, in the Spawn Eggs tab: the Dread Queen; the Dread Queen and Syndrigossa (her riding it, as the boss); Syndrigossa; an Ancient Syndrigossa; a Rimefrost Dragon; and a Dread Phantom.
- **Commands:** `/summon dreadpatch:dread_queen`, `/summon dreadpatch:dread_queen ~ ~ ~ {WithSyndrigossa:1b}` (riding her dragon), `/summon dreadpatch:syndrigossa`, and `/summon dreadpatch:syndrigossa ~ ~ ~ {Ancient:1b}` (full strength).

## The Dread Queen

A lean sovereign: 300 health behind 20 armor and 12 toughness (a full netherite set's), with a boss bar and her sword and staff. She has her original model from Ice and Fire, given a woman's figure (a bust in her chestplate's armor, as body mods give female player models), and she rises out of the ground when she spawns.

She fights like a necromancer. She keeps her distance, circling at casting range and giving ground when pressed, and saves her sword for whoever still reaches her.

**Withering Rime** is the mark of all her magic. It withers through armor, freezes and stops all healing, and it feeds her: each second it withers anyone she afflicted (by her bolt, her crown's shrapnel, her sword, her Decree or her husk), she heals 1 on Easy, 2 on Normal and 3 on Hard for each of them, while she's within 32 blocks. Her victims lose 1 a second, as under anyone's Withering Rime; a player's Withering Rime feeds no one. No share of anything's max health she takes, from any spell or her sword, is ever more than 20, so a warden or a dragon is a long fight for her too.

Her abilities:

- **Withering Rime-Bolt:** from range, a flame-sized streak of cold cyan dread fire and soot. It takes a share of the target's own life on top of its damage (5% of their max health, 8% on Hard), and leaves Withering Rime.
- **Rimefire Crown:** a ring of ice spikes round her target, wreathed in cold cyan dread fire over cyan and black smoke. It roots everyone inside and drinks every other fire within it. She then shatters it with her bolt for a burst of shrapnel.
- **Barrow-Step:** when cornered, she steps 10 to 15 blocks away through the cold, leaving a husk of rime-glass in her shape where she stood. Whatever was hunting her turns on the husk, and when it's struck, or after 5 seconds, it shatters, rooting everyone near it for 2 seconds and touching them with Withering Rime.
- **Rimewalk:** still water freezes under her feet, 4 blocks round, as under Frost Walker II boots, and holds while she stands on it, so water won't keep you from her: she walks straight across lakes and rivers. If she's in water, the surface freezes round her and she climbs out onto it. It's vanilla's frosted ice, which melts back to water in the light once she's gone.
- **Court of the Dread:** she raises Ice and Fire's own thralls, ghouls, knights and scuttlers from the ground around her. Dread scuttlers, hers and any others, spit a weaker Withering Rime-Bolt from up to 12 blocks every 3 to 4 seconds, and climb walls like spiders.
- **Beater's Decree:** she burns one of her court from behind as it presses an enemy, a burst of rime that sends the rest of her court into a frenzy.
- **A Dread Lich:** half a minute into a fight, and at most every 90 seconds after, she raises one beside her, never two at once. It's Ice and Fire's own, with its skulls and the minions it raises itself (up to 5 over its life), fighting what she fights.
- **Create Rimefrost Dragon:** in a fight, once she's lost a quarter of her health, she raises the corpse of an ice dragon within 32 blocks as a Rimefrost dragon of hers, at most every 3 minutes and one at a time. It rises with half the health she's lost, and fights what she fights.
- **Dread phantoms:** 15 seconds into a fight, and every 45 seconds after, she calls one or two dread phantoms out of the air (below), never more than four of hers at once.
- **Whiteout Dominion:** every two minutes in a fight she calls down a blizzard 32 blocks round over the ground where she stands. It lasts 45 seconds and stays where it was raised; she ends it if she falls.
  - Inside it her enemies are slowed by a fifth, can't see past about 10 blocks through the snow-fog (mobs lose sight of anything past half their usual range, though a boss keeps its sight and is only slowed), and take a point of freezing damage every 3 seconds. From outside it shows as a wall of driven snow standing on the land.
  - Snow drifts over the ground under it and melts away as it clears. It's only drawn, so it never stays behind.
  - Its still water freezes over in patches as the snow drifts in, so a lake under it is ice to walk on. The ice holds while the storm stands and thaws as it clears. If the storm's chunks unload first, what's left melts in daylight like Frost Walker's.
  - Whatever dies in her storm rises where it fell as a dread mob of her court, up to 8 a storm: a thrall in its gear from a player or anything that stands upright (a villager, a skeleton, a creeper, an enderman), a ghoul from a zombie, a scuttler from a spider, a dread horse from a horse, and a dread beast its size from an animal or anything else. Golems stay dead.

### Riding Syndrigossa

Spawned as the boss, she rises riding an ancient Syndrigossa. In the saddle she casts her bolt and crowns while her dragon flies and fights, her court rises on the ground below, and the two hunt the same prey.

- **Soul Tether:** while they're within 28 blocks of each other, everything her Withering Rime and the dragon's breath heal is shared between them half and half. Pull them apart and each keeps only its own.
- **Bringing her down:** she's hard to reach in the saddle, so it's her dragon you bring down. At half its health Syndrigossa is forced to land, the queen Barrow-Steps off and fights on foot for the rest of the fight, and the dragon can't take off again for 10 seconds. She also steps down if she falls to half her own health, or if her dragon dies under her.

### Her drops

- Her sword and her staff, each one time in four (Looting raises it: 30% at Looting I, 40% at Looting III).
- One epic or legendary ink.
- One scroll of her spells: Withering Rime-Bolt, Rimefire Crown, Barrow-Step, Whiteout Dominion, Summon Court, Create Rimefrost Dragon, Summon Dread Phantoms or Rimefire Blast. They're **Rimefrost** spells (below), and but for Rimefire Blast, found nowhere else.
- **A piece of her regalia,** when a player kills her (below).

### Her regalia

Her own outfit, to wear: the **Crown**, **Bodice**, **Robes** and **Greaves of the Dreaded Queen**. The crown is her ring of ice, the bodice her bodice with its necklace, cloak and silver pauldrons, the robes and greaves the rest, each drawn from her own model and texture.

- **Strength:** as dread dragonsteel's armour, and like the dragonscale and dragonsteel sets it takes 70% off dragon breath as a full set. It never breaks.
- **Each piece:** +150 max mana, 5% off the mana Ice and Rimefrost spells cost, 5% more Rimefrost damage dealt, and 5% less Rimefrost damage taken (Rimefrost spells, the Rimefrost dragons' breath and Shatter-Spine, Whiteout's cold, and Withering Rime's withering, counted as dealt by whoever laid the rime). The whole set: a fifth more dealt, a fifth less taken.
- **The whole set:** you can't freeze (powder snow, Withering Rime's chill and freezing damage don't touch you), the Dreadlands' weather slows you a step less (a heavy snowstorm as plain snow, plain snow not at all), and the Dread leave you be: they won't come for you unless you hurt them first, and then only that one, for half a minute. The queen herself, Syndrigossa and anything they command still do.
- **Her crown's sight:** the Dreadlands' storm fog doesn't close in on whoever wears her crown (snow or heavy snowstorm), and in any whiteout, the weather or a Whiteout Dominion's, they see 75% further.
- **Collecting it:** each queen you kill drops a piece you don't have yet, so four kills complete the set. Looting gives a 10% chance a level of a second missing piece. Once you have the set, she drops a random piece one time in four (5% more a Looting level).
- **Advancements** keep count: one for slaying her, one for each piece you've held, and one for the whole set. A piece counts as yours once it's been in your inventory.
- **There Must Always Be a Dread Queen:** wear all four pieces with her blade and her staff in your hands.

**Armour trims** go on her regalia, Rimefrost dragonscale and dread dragonsteel armour at a smithing table, as on vanilla armour. Worn, a trim sits on vanilla armour's shape over the piece.

## The Dread Gate and the Gravemaster's Key

1. Build a frame of Ice and Fire's dreadstone (any of its stone, bricks, tiles or chiseled skulls) round an opening 2 to 21 blocks wide and 3 to 21 tall, as a Nether portal's can be. The corners can be left out.
2. Right-click the frame with a **Gravemaster's Key**, Ice and Fire's Mausoleum key, which the gate uses up.
3. The opening fills with a dark, cold-fire gate, and 10 seconds later the Dread Queen comes through. The gate shatters, leaving its frame for another key.
4. **Or step into it** before she comes, and it takes you to **the Dreadlands** (below) instead. She doesn't come, and the gate stays open 15 to 30 seconds more for anyone to follow you, flickering faster and faster over its last 5 seconds before it closes.

Where there's room for her dragon in front of the gate, she comes as the full boss, riding an ancient Syndrigossa. Where there isn't, she rises out of the gate on foot.

- **In a Mausoleum:** every Mausoleum already has four gates. The arches of the shrine round the ice dragon forge in its far hall are dreadstone frames 3 wide and 6 tall. Its hall is too low for her dragon, so there she comes on foot.
- **Breaking the frame** while it's open collapses the gate, and the key is lost.
- **On Peaceful** the gate still opens, as a way to the Dreadlands, but the queen doesn't come through.

The Gravemaster's Key still opens a Mausoleum's sealed door too, and it can now be crafted: three Dread Shards, a Bone and two Gold Ingots in the shape of a key (shards and bone across the top, a shard below, the gold at the bottom right). The recipe unlocks when you pick up a dread shard.

## The Dreadlands

A dimension of bleak frozen waste: gently rolling snowfields, carved here and there by the wind into fields of long drifts like dunes, over packed and blue ice, black dreadstone breaking through here and there, frozen lakes in the hollows, under a grey sky with snow drifting and the wind moaning, ice cracking somewhere out on the waste, something rumbling deep below, and now and then, far off and never from the same quarter twice, a dragon's roar. Its day, night and weather are the overworld's.

- **The Dread walk there:** Ice and Fire's thralls, ghouls, knights and scuttlers rise out of the snow in ones, twos and threes, by day and by night, 24 to 48 blocks off: now and then in clear weather, more often while it snows, and a little more often again in a heavy snowstorm (thunder). No more than 8 of them gather round any one player.
- **Patrols:** now and then a small Dread patrol (a dread knight on foot leading two to four more) crosses the waste, its first leg passing by you, then on from waypoint to waypoint. They fight what they meet, then walk on; if their knight falls, another leads.

- **Its storms:** when it rains in the overworld, it snows there, and that's a storm. Out under the open sky the fog closes in to about 56 blocks, and to about 24 when it thunders (a warning; a Whiteout Dominion's is 10), snow blows past on the wind, and **Rimefrost** settles on you: the Dread's cold without the withering, 10% slower in snow and 25% in a heavy snowstorm (thunder), for as long as you stay out in it. It does no damage, and the Dread don't feel it. Get under a roof and it lifts.
- **Whiteout storms:** about one storm in six (never two in a row) is a Whiteout Dominion, like the Dread Queen's own storm across the whole land: out under the open sky the fog closes in to 10 blocks, the wind howls and the snow drives past, you're slowed further, and the cold does half a heart of damage every 3 seconds unless you can't freeze (leather, her full regalia). Get under a roof. It never starts while a Dread Queen is about there, and if she comes during one, it breaks into a heavy thunderstorm. How often is a server setting (`whiteout_storms`; 0 for never).
- **Its lightning:** when the overworld thunders, lightning strikes round you every 20 seconds or so (Minecraft's own never strikes ground that cold). It hurts what it hits, as lightning does, but sets no fire: it leaves a patch of **rimefire**, cold dread fire burning cyan for 30 to 60 seconds. One of the Dread standing in it heals a quarter of a heart a second; anyone else is chilled (Rimefrost), put out, and burned by the cold, half a heart a second. Rimefire is a hazard only: it gives nothing and can't be lit by players. It glows dimly (light 6), too little to melt the snow and ice round it. Rimefrost dragons' breath lights patches of it where it lands (with `mobGriefing` on), and a Withering Rime-Bolt that strikes the ground leaves a few seconds of its embers.

- **Getting there:** step into a Dread Gate during its ritual (above). You arrive near the foot of the nearest spire, 30 to 40 blocks from it, facing it; anyone who follows through the same gate arrives at the same spot.
- **The spires:** rare and tall (64 to 100 blocks), standing far apart on the waste. They're dreadstone and dreadstone bricks over stone at the foot, banded with packed and blue ice, giving way to packed ice and snow, flaring out again near the top to a broad, flat summit (room for Syndrigossa) crowned with ice spikes. A ledge of dreadstone bricks winds up the outside from the ground to the summit.
- **Summit chests:** half the spires keep a chest on the summit: dread shards, bones, Rimefrost scales, iron, gold, sometimes diamonds or ink, sometimes a Gravemaster's Key (a way home), now and then a scroll of one of her lesser spells, rarely a Rimefrost Dragonforge Core, and now and then the music disc **Entombed In Rimefrost**. Now and then a Dread Lich guards it.
- **Abandoned villages:** snowy villages overrun by the dead, their houses rebuilt in dreadwood and dreadstone among bare dead trees, lit by dread lanterns and torches burning cyan, zombie villagers still shambling through them. Their chests hold what any village's would.
- **Lootr:** with Lootr installed, every chest in the Dreadlands (summits and villages) gives each player their own loot.
- **The Queen and Syndrigossa:** half the spires hold her, and you can tell which from below: a dreadstone dais at the summit's middle, with dread torches burning cyan round it. Reach such a summit and she comes, as the full boss, riding an ancient Syndrigossa out of the air beside it (on foot on the summit, if there's no room for her dragon), and turns on you. Each spire's encounter comes to each player once; it comes to you there again only after 6 real hours (a server setting, `encounter_reset_hours` in `config/dreadpatch-server.toml`), and never while that spire's queen is still about. Anyone who reaches the summit while she's there has met it too. Not on Peaceful.
- **Her defeat:** when she falls, a Dread Gate rises where she fell, open home for 5 minutes (time to take what she dropped, and her dragon's corpse), fading out over its last seconds. Its frame stays for a key.
- **Getting home:** beat a spire's queen (above), or build a Dread Gate there and key it. It opens at once (no queen comes), stays open 15 to 30 seconds, and takes whoever steps in back to the gate they came in by (or to the world's spawn). A key takes three dread shards (dread phantoms drop them), a bone and two gold ingots, so bring the gold; the frame takes any dreadstone, the spires' included. Dying sends you to your usual spawn, as anywhere.
- **Flames die cold there:** a lantern or torch you place in the Dreadlands fades, a step every 30 to 45 seconds, to its soul kind and then to the Dread's: a **dread lantern** (a soul lantern burning her cyan; break it with a pickaxe to keep it) or a dread torch.
- **Its music:** "Entombed In Rimefrost", the only music that plays there, and only there, coming now and then as Minecraft's own music does (every 10 to 20 minutes). The first time you arrive (the advancement **Into the Dreadlands**) it starts at once. Its disc (from summit chests) plays it in any jukebox.
- **The sun burns no undead there:** zombies, skeletons and the rest walk its snow by day unharmed, the abandoned villages' zombie villagers among them.
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
- **Their bite:** a phantom's, and one bite in three (35%) leaves 5 seconds of **Rimefrost**, the Dread's cold without the withering (25% slower). One in seven (15%), rolled apart, leaves 3 seconds of Withering Rime, so a bite can leave both.
- **Their drops:** a dread thrall's (bones and dread shards), and when a player kills one, 1 to 3 phantom membranes and 1 to 3 Rimefrost Dragon Scales, each with up to one more a Looting level. The ones a player calls leave nothing.
- **Natural spawning:** in the Dreadlands, and only there, one or two at a time, rarely, whether or not you've slept (above).

## Rimefrost dragonscale and dread dragonsteel

Rimefrost dragons' scales make the Dread's answer to Ice and Fire's ice dragon gear, with Ice and Fire's ice sets' own numbers and models, recoloured soot, bone and cyan:

- **Rimefrost Dragon Scales:** from a Rimefrost dragon's corpse (above), or from dread phantoms a player kills.
- **Rimefrost Dragon Blood:** Rimefrost Dragon Scales and a glass bottle in the Rimefrost dragonforge (below), as Ice and Fire makes ice dragon blood.
- **Dread Dragonsteel Ingot:** an iron ingot and Rimefrost Dragon Blood in the Rimefrost dragonforge.
- **Rimefrost dragonscale armour,** crafted from the scales like Ice and Fire's dragonscale armour.
- **Dread dragonsteel armour, sword, axe, pickaxe, shovel and hoe,** crafted from the ingots (the tools on witherbone handles) like Ice and Fire's dragonsteel. Where ice dragonsteel freezes what it hits, dread dragonsteel leaves 3 seconds of Withering Rime.
- **Against dragon breath,** each piece of either armour takes off as much as Ice and Fire's dragon armour does (10% helmet and boots, 30% chestplate, 20% leggings: 70% for a full set), against Ice and Fire's dragons and Rimefrost dragons alike. Ice and Fire's own dragonscale and dragonsteel armour now guards against Rimefrost breath too.

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
- **Withering Rime-Bolt**'s share of the target's life grows with its level: 3% at level 1 to 7% at level 5.

- **Summon Court** raises her court round you, Ice and Fire's thralls, ghouls, knights and scuttlers rising out of the ground: 2 at level 1 up to 6 at level 5, standing 70 to 110 seconds, on a two-minute cooldown. They follow you and fall on hostile mobs that attack you or that you attack. They never fight the Dread (the queen and her court leave them be too), never turn on friendly mobs, villagers, golems, pets or other players, and don't fight back when those hit them; and friendly mobs leave them be in turn. You can't hurt them, nor can your spells. Casting it again dismisses the court you raised before. A server can change how they treat friendly mobs and players in `config/dreadpatch-server.toml` (`attack_friendly_mobs`, `retaliate`, `attack_players`, all off by default).
- **Summon Dread Phantoms** calls two dread phantoms down over you for a minute, on a 90-second cooldown. They circle above you and dive on hostile mobs that attack you or that you attack, under the same rules as Summon Court's court: never the Dread, never friendly mobs, villagers, golems, pets or other players unless the server allows it, and you can't hurt them. They leave nothing behind. Casting it again dismisses the ones you called before.
- **Create Rimefrost Dragon** raises the corpse of an Ice and Fire ice dragon you look at (within 24 blocks) as a Rimefrost dragon of the same age, gender and colour, tamed to you, to ride and command like any tamed Ice and Fire dragon. It rises with half the health you're missing (rounded down), so the closer you are to death, the stronger it rises; at full health it rises with 1. It costs 500 mana and then every drop you have left. You keep one at a time. Its cooldown is 3 minutes, and within them you can recast it once, for free, to send your dragon back and raise the corpse you're looking at instead; the cooldown still ends 3 minutes after the first cast. Like Iron's own Raise Dead, it can't be cast straight from its scroll: inscribe it in a spellbook.
- **Barrow-Step** takes you to where you look (11 to 15 blocks, by level) and leaves your own husk behind: rime-glass in your shape and skin.
- **Whiteout Dominion** (16 to 32 blocks round and 25 to 45 seconds, by level, on a two-minute cooldown) fogs, slows and freezes whoever you'd harm in it (a boss is slowed but keeps its sight, so it won't lose you in the snow), and freezes its water over too, but only hers raises the dead. The Dread walk through anyone's storm untouched.

## Fixes to Ice and Fire

- **Riding a dragon:** a player steering any Ice and Fire dragon (fire, ice, lightning, or a tamed Rimefrost dragon) is drawn sitting upright in the saddle on its back, moving with it as it walks, flies and pitches. Ice and Fire drew them upside down under the dragon's belly. As before, you don't see yourself in first person. Prey shaken in a dragon's jaws is still drawn as Ice and Fire draws it.
- **Dragon corpses:** Ice and Fire still counts a dead dragon as alive while its corpse lies, so a mob that was fighting a dragon when it died went on attacking its corpse forever. Now nothing fights a dragon's corpse, any Ice and Fire dragon's, and mobs from other mods that use Minecraft's usual targeting leave it be too. Harvesting a corpse is unchanged.

## Commands

For operators and the server console, under `/dreadpatch`:

| Command | What it does |
|---|---|
| `/dreadpatch weather` | Tells you the Dreadlands' weather: clear, snow, a heavy snowstorm or a whiteout. |
| `/dreadpatch weather clear\|snow\|storm\|whiteout [seconds]` | Sets it, for 10 minutes unless you say. The Dreadlands share the overworld's weather, so the overworld's changes too. `whiteout` makes the storm a Whiteout Dominion whatever its odds; `snow` and `storm` are plain. |
| `/dreadpatch encounters reset [players]` | Forgets who has met which spire's Queen, so her encounter comes again (everyone's, unless you name players). |
| `/dreadpatch dreadlands enter [players]` | Sends players into the Dreadlands, by the nearest spire, remembering where they came from. |
| `/dreadpatch dreadlands home [players]` | Sends players in the Dreadlands home again. |
| `/dreadpatch config [setting [value]]` | Lists every Dread Patch setting, shows one, or sets one (saved to `config/dreadpatch-server.toml`, applied at once). |
| `/dreadpatch patrol [pos]` | Raises a Dread patrol there (a dread knight leading 2 to 4 more). |
| `/dreadpatch rise [count] [pos]` | Raises 1 to 8 of the Dread there (3 by default). |

## Settings

Dread Patch's server settings (how a summoned court behaves, how long until a spire's queen comes back for you, how often the Dread rise, patrol and fly in the Dreadlands, how often its storms are whiteouts) are in the mods list: select Dread Patch and press **Config**. In singleplayer you can change them there as you play, and they apply at once. On a server they're in `config/dreadpatch-server.toml` (a copy in the world's `serverconfig` folder overrides it).

## Building

`./gradlew build` puts the jar in `build/libs/`. `./gradlew runClient` starts a dev client with Ice and Fire loaded.

## Credits

Her model is the geometry of Ice and Fire's unused `ModelDreadQueen` (Alexthe666 and contributors, LGPL-3.0). Her textures, name, sword, staff and behavior come from Ice and Fire CE at runtime.
