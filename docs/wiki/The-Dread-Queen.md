# The Dread Queen

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
- **Dread phantoms:** 15 seconds into a fight, and every 45 seconds after, she calls one or two [dread phantoms](Dread-Phantoms) out of the air, never more than four of hers at once.
- **Whiteout Dominion:** every two minutes in a fight she calls down a blizzard 32 blocks round over the ground where she stands. It lasts 45 seconds and stays where it was raised; she ends it if she falls.
  - Inside it her enemies are slowed by a fifth, can't see past about 10 blocks through the snow-fog (mobs lose sight of anything past half their usual range, though a boss keeps its sight and is only slowed), and take a point of freezing damage every 3 seconds. Its cold puts out any fire inside it, hers too. From outside it shows as a wall of driven snow standing on the land.
  - Snow drifts over the ground under it and melts away as it clears. It's only drawn, so it never stays behind.
  - Its still water freezes over in patches as the snow drifts in, so a lake under it is ice to walk on. The ice holds while the storm stands and thaws as it clears. If the storm's chunks unload first, what's left melts in daylight like Frost Walker's.
  - Whatever dies in her storm rises where it fell as a dread mob of her court, up to 8 a storm: a thrall in its gear from a player or anything that stands upright (a villager, a skeleton, a creeper, an enderman), a ghoul from a zombie, a scuttler from a spider, a dread horse from a horse, a miniature dread phantom from a bat or a parrot ([Dread phantoms](Dread-Phantoms)), a Rime Eel from anything that lives in water ([Rime Eels](Rime-Eels)), and a dread beast its size from an animal or anything else. Golems stay dead. A thrall's armour looks as it did on the dead (a guard's iron, a player's diamond); only chainmail, which Ice and Fire's own thralls wear, takes their ragged look. What the dead carried still drops where they fell, as their own.

## Riding Syndrigossa

Spawned as the boss, she rises riding an ancient Syndrigossa. In the saddle she casts her bolt and crowns while her dragon flies and fights, her court rises on the ground below, and the two hunt the same prey.

- **Soul Tether:** while they're within 28 blocks of each other, everything her Withering Rime and the dragon's breath heal is shared between them half and half. Pull them apart and each keeps only its own.
- **Bringing her down:** she's hard to reach in the saddle, so it's her dragon you bring down. At half its health Syndrigossa is forced to land, the queen Barrow-Steps off and fights on foot for the rest of the fight, and the dragon can't take off again for 10 seconds. She also steps down if she falls to half her own health, or if her dragon dies under her.

## Her drops

- Her sword and her staff, each one time in four (Looting raises it: 30% at Looting I, 40% at Looting III).
- One epic or legendary ink.
- One scroll of her spells: Withering Rime-Bolt, Rimefire Crown, Barrow-Step, Whiteout Dominion, Summon Court, Create Rimefrost Dragon, Summon Dread Phantoms, Rimefire Blast or Sublimate. They're **Rimefrost** spells ([Spells](Spells)), and but for Rimefire Blast and Sublimate, found nowhere else.
- **A piece of her regalia,** when a player kills her (below).

## Her regalia

Her own outfit, to wear: the **Crown**, **Bodice**, **Robes** and **Greaves of the Dreaded Queen**. The crown is her ring of ice, the bodice her bodice with its necklace, cloak and silver pauldrons, the robes and greaves the rest, each drawn from her own model and texture.

- **Strength:** as dread dragonsteel's armour, and like the dragonscale and dragonsteel sets it takes 70% off dragon breath as a full set. It never breaks.
- **Each piece:** +150 max mana, 5% off the mana Ice and Rimefrost spells cost, 5% more Rimefrost damage dealt, and 5% less Rimefrost damage taken (Rimefrost spells, the Rimefrost dragons' breath and Shatter-Spine, Whiteout's cold, and Withering Rime's withering, counted as dealt by whoever laid the rime). The whole set: a fifth more dealt, a fifth less taken.
- **The whole set:** you can't freeze (powder snow, Withering Rime's chill and freezing damage don't touch you), the Dreadlands' weather slows you a step less (a heavy snowstorm as plain snow, plain snow not at all), and the Dread leave you be: they won't come for you unless you hurt them first, and then only that one, for half a minute. The queen herself, Syndrigossa and anything they command still do.
- **Her crown's sight:** the Dreadlands' storm fog doesn't close in on whoever wears her crown (snow or heavy snowstorm), and in any whiteout, the weather or a Whiteout Dominion's, they see 75% further, and a Whiteout Dominion's driven snow is that much thinner to them.
- **Collecting it:** each queen you kill drops a piece you don't have yet, so four kills complete the set. Looting gives a 10% chance a level of a second missing piece. Once you have the set, she drops a random piece one time in four (5% more a Looting level).
- **Advancements** keep count: one for slaying her, one for each piece you've held, and one for the whole set. A piece counts as yours once it's been in your inventory.
- **There Must Always Be a Dread Queen:** wear all four pieces with her blade and her staff in your hands.

**Armour trims** go on her regalia, Rimefrost dragonscale and dread dragonsteel armour at a smithing table, as on vanilla armour. Worn, a trim sits on vanilla armour's shape over the piece.

## What her defeat stirs

- **Round the Mausoleums:** once a Dread Queen has been defeated in a world (her death by anything but `/kill`, wherever she falls), the Dread rise for good round Ice and Fire's Mausoleums as they do in the Dreadlands on a clear day, and patrols cross there: within 128 blocks of one, in cold and snowy biomes, on snow, ice or dreadstone, never in a village. Everyone is told the first time. Off with `mausoleum_dread` at 0.
- **The defeat storm:** each defeat brings a dry storm over the cold lands for 30 to 60 seconds, then none for 6 real hours (`defeat_storm_hours`), whoever defeats her: out under the sky in a cold or snowy biome (or anywhere, with `defeat_storm_cold_only` off), an ash-grey fog closes in to about 48 blocks, rime blows along the ground, the Dreadlands' wind moans and its ice cracks, and a mild Rimefrost (10% slower) settles on you. No snow falls, nothing is hurt, frozen or raised, and sleeping doesn't end it. Get under a roof and it lifts; her full regalia keeps the Rimefrost off, and her crown the fog.

---
*Kept in step with the mod's [README](https://github.com/hammerknife404/dread-patch#readme); edits go in `docs/wiki/` in the repository.*
