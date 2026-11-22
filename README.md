# Dread Patch

Patching in the Dread Queen for my own world.

[Ice and Fire: Community Edition](https://modrinth.com/mod/iceandfire-ce) ships a Dread Queen (her class, textures, name, sword and staff) but never puts her in the game. This NeoForge mod finishes her, and gives her the Rimefrost, undead frost dragons of the Dread, among them her own: Syndrigossa.

**Requires:** Minecraft 1.21.1, NeoForge 21.1.265+, Ice and Fire CE 2.1-beta.1+ (with Uranus, Jupiter and GeckoLib, which it requires anyway), and Iron's Spells 'n Spellbooks 3.x for 1.21.1.

**Status:** in development. She doesn't spawn naturally yet.

## Calling her

- **A Dread Gate** (below), with a Gravemaster's Key.
- **Spawn eggs**, in the Spawn Eggs tab: the Dread Queen; the Dread Queen and Syndrigossa (her riding it, as the boss); Syndrigossa; an Ancient Syndrigossa; and a Rimefrost Dragon.
- **Commands:** `/summon dreadpatch:dread_queen`, `/summon dreadpatch:dread_queen ~ ~ ~ {WithSyndrigossa:1b}` (riding her dragon), `/summon dreadpatch:syndrigossa`, and `/summon dreadpatch:syndrigossa ~ ~ ~ {Ancient:1b}` (full strength).

## The Dread Queen

A lean sovereign: 300 health behind 20 armor and 12 toughness (a full netherite set's), with a boss bar and her sword and staff. She has her original model from Ice and Fire, given a woman's figure (a bust in her chestplate's armor, as body mods give female player models), and she rises out of the ground when she spawns.

She fights like a necromancer. She keeps her distance, circling at casting range and giving ground when pressed, and saves her sword for whoever still reaches her.

**Withering Rime** is the mark of all her magic. It withers through armor, freezes and stops all healing, and it feeds her: each second it withers anyone she afflicted (by her bolt, her crown's shrapnel, her sword, her Decree or her husk), she heals 1 on Easy, 2 on Normal and 3 on Hard for each of them, while she's within 32 blocks. Her victims lose 1 a second, as under anyone's Withering Rime; a player's Withering Rime feeds no one. No share of anything's max health she takes, from any spell or her sword, is ever more than 20, so a warden or a dragon is a long fight for her too.

Her abilities:

- **Withering Rime-Bolt:** from range, a flame-sized streak of cold cyan dread fire and soot. It takes a share of the target's own life on top of its damage, and leaves Withering Rime.
- **Rimefire Crown:** a ring of ice spikes round her target, wreathed in cold cyan dread fire over cyan and black smoke. It roots everyone inside and drinks every other fire within it. She then shatters it with her bolt for a burst of shrapnel.
- **Barrow-Step:** when cornered, she steps 10 to 15 blocks away through the cold, leaving a husk of rime-glass in her shape where she stood. Whatever was hunting her turns on the husk, and when it's struck, or after 5 seconds, it shatters, rooting everyone near it for 2 seconds and touching them with Withering Rime.
- **Rimewalk:** still water freezes under her feet, 4 blocks round, as under Frost Walker II boots, and holds while she stands on it, so water won't keep you from her: she walks straight across lakes and rivers. If she's in water, the surface freezes round her and she climbs out onto it. It's vanilla's frosted ice, which melts back to water in the light once she's gone.
- **Court of the Dread:** she raises Ice and Fire's own thralls, ghouls, knights and scuttlers from the ground around her. Dread scuttlers, hers and any others, spit a weaker Withering Rime-Bolt from up to 12 blocks every 3 to 4 seconds, and climb walls like spiders.
- **Beater's Decree:** she burns one of her court from behind as it presses an enemy, a burst of rime that sends the rest of her court into a frenzy.
- **A Dread Lich:** half a minute into a fight, and at most every 90 seconds after, she raises one beside her, never two at once. It's Ice and Fire's own, with its skulls and the minions it raises itself (up to 5 over its life), fighting what she fights.
- **Create Rimefrost Dragon:** in a fight, once she's lost a quarter of her health, she raises the corpse of an ice dragon within 32 blocks as a Rimefrost dragon of hers, at most every 3 minutes and one at a time. It rises with half the health she's lost, and fights what she fights.
- **Whiteout Dominion:** every two minutes in a fight she calls down a blizzard 32 blocks round over the ground where she stands. It lasts 45 seconds and stays where it was raised; she ends it if she falls.
  - Inside it her enemies are slowed by a fifth, can't see past about 10 blocks through the snow-fog (mobs lose sight of anything past half their usual range), and take a point of freezing damage every 3 seconds. From outside it shows as a wall of driven snow standing on the land.
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
- One scroll of her spells: Withering Rime-Bolt, Rimefire Crown, Barrow-Step, Whiteout Dominion, Summon Court or Create Rimefrost Dragon. They're Ice spells (they count Ice spell power), found nowhere else.

## The Dread Gate and the Gravemaster's Key

1. Build a frame of Ice and Fire's dreadstone (any of its stone, bricks, tiles or chiseled skulls) round an opening 2 to 5 blocks wide and 3 to 7 tall, like a Nether portal's. The corners can be left out.
2. Right-click the frame with a **Gravemaster's Key**, Ice and Fire's Mausoleum key, which the gate uses up.
3. The opening fills with a dark, cold-fire gate, and 5 seconds later the Dread Queen comes through. The gate shatters, leaving its frame for another key.

Where there's room for her dragon in front of the gate, she comes as the full boss, riding an ancient Syndrigossa. Where there isn't, she rises out of the gate on foot.

- **In a Mausoleum:** every Mausoleum already has four gates. The arches of the shrine round the ice dragon forge in its far hall are dreadstone frames 3 wide and 6 tall. Its hall is too low for her dragon, so there she comes on foot.
- **Breaking the frame** during those 5 seconds collapses the gate, and the key is lost.
- **On Peaceful**, nothing comes through, and the key isn't used.

The Gravemaster's Key still opens a Mausoleum's sealed door too, and it can now be crafted: three Dread Shards, a Bone and two Gold Ingots in the shape of a key (shards and bone across the top, a shard below, the gold at the bottom right). The recipe unlocks when you pick up a dread shard.

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

## Rimefrost dragonscale and dread dragonsteel

Rimefrost dragons' scales make the Dread's answer to Ice and Fire's ice dragon gear, with Ice and Fire's ice sets' own numbers and models, recoloured soot, bone and cyan:

- **Rimefrost Dragon Blood:** Rimefrost Dragon Scales and a glass bottle in an ice dragonforge, as Ice and Fire makes ice dragon blood.
- **Dread Dragonsteel Ingot:** an iron ingot and Rimefrost Dragon Blood in an ice dragonforge.
- **Rimefrost dragonscale armour,** crafted from the scales like Ice and Fire's dragonscale armour.
- **Dread dragonsteel armour, sword, axe, pickaxe, shovel and hoe,** crafted from the ingots (the tools on witherbone handles) like Ice and Fire's dragonsteel. Where ice dragonsteel freezes what it hits, dread dragonsteel leaves 3 seconds of Withering Rime.
- **Against dragon breath,** each piece of either armour takes off as much as Ice and Fire's dragon armour does (10% helmet and boots, 30% chestplate, 20% leggings: 70% for a full set), against Ice and Fire's dragons and Rimefrost dragons alike. Ice and Fire's own dragonscale and dragonsteel armour now guards against Rimefrost breath too.

## Her staff and sword

**Her staff** is an Iron's staff now:

- It carries three of her spells at level 3 (Withering Rime-Bolt, Rimefire Crown and Summon Court), on your spell wheel while you hold it, cast with right-click.
- It gives +15% Ice Spell Power and +10% Cooldown Reduction in the main hand.
- **Permafrost Conduit:** Ice spells cost 20% less mana while you stand on snow or ice.

**Her sword** is unbreakable, with 11.5 attack damage at 1.6 speed and half a block more reach. She swings the same sword, so her own strikes leave Withering Rime and shatter too.

- **Thermodynamic Theft:** every strike leaves 4.5 seconds of Withering Rime.
- **Glass-Rime Shatter:** every third full strike on the same chilled or frozen foe, each within 5 seconds of the last, shatters its frost for an extra 10% of its max health past its armor (at most 20). It also leaves the foe **Brittle** (10% less armor) for 4 seconds. Hitting others in between doesn't reset the count, so it works with Better Combat's sweeping swings.
- **Fire-Drinking Guard:** it rises while you hold right-click, slowing you like a shield. It stops fire and magic from the front (Iron's spells and Ice and Fire's dragon fire included; arrows and blades still get through), and puts out fire on you and around you. A hit it stops charges the blade, which glints until its next full strike shatters at once. With the sword in your main hand, an off hand holding something with its own right-click (a shield, food, a bow, blocks, throwables, a staff) keeps that right-click, so raise the guard with that hand empty or holding something like a totem.

## Her spells, as a player

Her scrolls (from her drops) teach her spells to players:

- **Summon Court** raises her court round you, Ice and Fire's thralls, ghouls, knights and scuttlers rising out of the ground: 2 at level 1 up to 6 at level 5, standing 70 to 110 seconds, on a two-minute cooldown. They follow you and fall on hostile mobs that attack you or that you attack. They never fight the Dread (the queen and her court leave them be too), never turn on friendly mobs, villagers, golems, pets or other players, and don't fight back when those hit them; and friendly mobs leave them be in turn. You can't hurt them, nor can your spells. Casting it again dismisses the court you raised before. A server can change how they treat friendly mobs and players in `config/dreadpatch-server.toml` (`attack_friendly_mobs`, `retaliate`, `attack_players`, all off by default).
- **Create Rimefrost Dragon** raises the corpse of an Ice and Fire ice dragon you look at (within 24 blocks) as a Rimefrost dragon of the same age, gender and colour, tamed to you, to ride and command like any tamed Ice and Fire dragon. It rises with half the health you're missing (rounded down), so the closer you are to death, the stronger it rises; at full health it rises with 1. It costs 500 mana and then every drop you have left. You keep one at a time. Its cooldown is 3 minutes, and within them you can recast it once, for free, to send your dragon back and raise the corpse you're looking at instead; the cooldown still ends 3 minutes after the first cast. Like Iron's own Raise Dead, it can't be cast straight from its scroll: inscribe it in a spellbook.
- **Barrow-Step** takes you to where you look (11 to 15 blocks, by level) and leaves your own husk behind: rime-glass in your shape and skin.
- **Whiteout Dominion** (16 to 32 blocks round and 25 to 45 seconds, by level, on a two-minute cooldown) fogs, slows and freezes whoever you'd harm in it, and freezes its water over too, but only hers raises the dead. The Dread walk through anyone's storm untouched.

## Fixes to Ice and Fire

- **Dragon corpses:** Ice and Fire still counts a dead dragon as alive while its corpse lies, so a mob that was fighting a dragon when it died went on attacking its corpse forever. Now nothing fights a dragon's corpse, any Ice and Fire dragon's, and mobs from other mods that use Minecraft's usual targeting leave it be too. Harvesting a corpse is unchanged.

## Building

`./gradlew build` puts the jar in `build/libs/`. `./gradlew runClient` starts a dev client with Ice and Fire loaded.

## Credits

Her model is the geometry of Ice and Fire's unused `ModelDreadQueen` (Alexthe666 and contributors, LGPL-3.0). Her textures, name, sword, staff and behavior come from Ice and Fire CE at runtime.
