# Features

Numbers are as of 0.32.1.

## The Dread Queen

`dreadpatch:dread_queen`. Ice and Fire CE has her class, textures, sword and staff but never registers her; this mod does.

- **Stats:** 300 health, 20 armor, 12 armor toughness (a full netherite set's). Blue boss bar.
- **Look:** Ice and Fire's unused original model, with a bust added to her chestplate. Her eyes glow.
- **Rise:** she rises out of the ground when she spawns, and can't move or attack until she's up.
- **Spawning:** not natural yet. Use a [Dread Gate](#the-dread-gate), the **Dread Queen Spawn Egg** or `/summon dreadpatch:dread_queen`.
- **Undead:** tagged undead and freeze immune, like Ice and Fire's dread mobs, and `#c:bosses`.

### How she fights

She fights like Iron's necromancer: she keeps her distance and lets her magic and court do the work.

| Range from her target | What she does |
|---|---|
| Out of sight or past 16 blocks | Walks in |
| 9 to 16 blocks | Circles at casting range |
| Under 9 blocks | Gives ground as she circles |
| Under 5 blocks | Breaks away with Barrow-Step (or on foot while it cools down) |

She swings her sword at anyone who still reaches her.

**Rimewalk:** still water freezes under her feet in a 4-block radius, like Frost Walker II, and holds while she stands on it. She walks straight across lakes; if she's in water, she freezes the surface round her and climbs out. It's vanilla frosted ice, so it melts back once she's gone (in the light).

### Her attacks

| Attack | How often | What it does |
|---|---|---|
| **Withering Rime-Bolt** | Every 4 s, 3 to 32 blocks | 8 damage + 15% of the target's max health, and 4.5 s of Withering Rime |
| **Rimefire Crown** | Every 15 s, within 20 blocks | A ring of ice spikes and dread fire (radius 3) round her target for 5 s: roots everyone inside and drinks fire. Her next bolt shatters it for shrapnel: 4 + 6% of max health to everyone within 3 blocks |
| **Barrow-Step** | 12 s cooldown, when cornered | Steps 10 to 15 blocks away, leaving a husk in her shape that draws attackers; struck, or after 5 s, it shatters: 2 s root and Withering Rime within 3 blocks |
| **Court of the Dread** | Every 20 s | Raises up to 3 of Ice and Fire's dread mobs (thralls, ghouls, knights, scuttlers) round her, 6 standing at most. They take her target |
| **Beater's Decree** | Every 15 s | Burns the court member pressing an enemy closest in a rime burst: 2 + 12% of max health and Withering Rime within 3 blocks; the rest of her court gets Speed and Strength for 6 s |
| **Dread Lich** | First 30 s in, then every 90 s | Raises Ice and Fire's Dread Lich beside her, one at a time. It raises its own minions and drops its own loot, including a Gravemaster's Key now and then |
| **Whiteout Dominion** | First 15 s in, then every 2 min | A blizzard 32 blocks round where she stands, for 45 s ([below](#whiteout-dominion)) |

No share of max health from any of her attacks, spells or sword is ever more than 20, so a warden or a dragon is a long fight for her too.

### Withering Rime

`dreadpatch:withering_rime`, the mark of all her magic.

- Withers 1 health a second through armor.
- Freezes, faster than the body thaws.
- Stops all healing while it lasts.
- The Dread (her, her court, Ice and Fire's dread mobs, Syndrigossa) can't take it.
- **Her siphon:** each second it withers someone she afflicted, she heals 1 on Easy, 2 on Normal, 3 on Hard per victim, while she's within 32 blocks. A player's Withering Rime feeds no one.

### Whiteout Dominion

A storm that stands where it was raised.

- Her enemies inside are slowed by 20%, see no further than about 10 blocks through the fog (mobs lose half their follow range), and take 1 freezing damage every 3 s.
- Snow drifts over the ground and melts as it clears. It's only drawn, never placed, so it never stays behind.
- Still water under it freezes over in patches as the snow comes in, and thaws as the storm clears.
- From outside it's a wall of driven snow.
- **The dead rise:** whatever dies in her storm rises where it fell as one of her court, up to 8 a storm: players and anything that stands upright as thralls in their own gear, zombies as ghouls, spiders as scuttlers, horses as dread horses, animals and everything else as dread beasts their size. Golems, bosses, dragons and the Dread stay dead.
- It clears when she dies.

### Her loot

- The **Blade of the Dreaded Queen** and the **Staff of the Dreaded Queen**, always ([below](#her-sword-and-staff)).
- Two inks, each epic or legendary.
- One or two scrolls of her spells, found nowhere else.

## Syndrigossa

`dreadpatch:syndrigossa`, the queen's undead ice dragon: an Ice and Fire ice dragon rotted to the bone, with soot-black scales, bone showing through and torn wings. Allied with the queen and her court.

- **Growth:** an ice dragon's. From her spawn egg or `/summon` she's Ice and Fire's random 1 to 80 days.
- **Ancient** (the boss): 125 days, full strength. Use the **Ancient Syndrigossa Spawn Egg** or `/summon dreadpatch:syndrigossa ~ ~ ~ {Ancient:1b}`.
- **Health by difficulty** at full strength: 500 Easy, 750 Normal, 1,000 Hard. A younger one has an ice dragon's share of it for her age; she keeps her share when the difficulty changes.
- **Undead:** Smite hurts her more, Healing hurts and Harming heals. Poison, Regeneration and Wither don't take. She can't freeze or drown.
- **Hunts players** even with Ice and Fire's `neutralToPlayer` on (never in creative or spectator).
- **Tamed**, she can be ridden like any Ice and Fire dragon.

### Her attacks

| Attack | What it does |
|---|---|
| **Withering Rime breath** | A cone 20 degrees either side, up to 30 blocks: an ice dragon's breath damage for her size, Withering Rime, and half a heart more past armor that heals her. Drinks fire instead of freezing the ground |
| **Shatter-Spine** | A core of bone and dread-glass that bursts 9 blocks over her target into 5 to 7 shards; each cuts everyone within 1.5 blocks and leaves Withering Rime. Every 10 s in the air |
| **Sovereign's Eclipse** | From stage 3, every 30 s: a ring 16 blocks across round her target for 10 s. Slowness IV and Withering Rime inside, and it drinks fire |

She takes to the air far more than other dragons and favours strafing fly-bys.

### Her corpse

Harvested empty-handed like any dragon's, but it's Dread: her white scales or dread shards (no flesh, no blood), dread shards and witherbones in place of a heart, no ice dragon egg, then dragon bones and an ice dragon skull. An ancient Syndrigossa also leaves her **Syndrigossa Egg** with the heart's haul. It doesn't hatch yet.

## The queen and her dragon

As the boss (through a Dread Gate with room in front of it, the **Dread Queen and Syndrigossa Spawn Egg**, or `/summon dreadpatch:dread_queen ~ ~ ~ {WithSyndrigossa:1b}`), the queen rides an ancient Syndrigossa.

- In the saddle she casts as on foot; her court rises on the ground below; the two hunt the same prey.
- **Soul Tether:** within 28 blocks of each other, everything her Withering Rime and the dragon's breath heal is split half and half.
- **Bringing her down:** she's hard to reach in the saddle, so hit the dragon. At half its health Syndrigossa is forced to land, the queen Barrow-Steps off and fights on foot, and the dragon can't take off for 10 s. She also steps down at half her own health, or if her dragon dies under her. She never remounts.

## The Dread Gate

A summoning gate for the queen.

1. **The key:** a Gravemaster's Key (Ice and Fire's Mausoleum key), now craftable: three Dread Shards and a Bone across the top, a Dread Shard in the middle, two Gold Ingots at the bottom right. The recipe unlocks when you pick up a dread shard. It still opens a Mausoleum's sealed door.
2. **The frame:** Ice and Fire's dreadstone (stone, bricks, chiseled, cracked, mossy, tile or the dread stone face; not stairs, slabs or dreadwood) round an open rectangle 2 to 5 wide and 3 to 7 tall, like a Nether portal. Corners can be left out.
3. **Opening it:** right-click the frame with the key. The key is used up. The opening fills with a dark, cold-fire gate.
4. **5 seconds later** she comes through and the gate shatters, leaving the frame for another key.
   - With room in front for her dragon, she comes as the boss, riding an ancient Syndrigossa.
   - Without, she rises out of the gate on foot.

Breaking the frame during those 5 seconds collapses the gate and the key is lost. Nothing comes through on Peaceful, and the key isn't used.

**Every Mausoleum already has four:** the arches round the ice dragon forge in its far hall are dreadstone frames 3 wide and 6 tall. The hall is too low for her dragon, so there she comes on foot.

## Her spells

All four are Ice spells (Ice spell power counts), levels 1 to 5, found only on her scrolls.

| Spell | Cooldown | Cast by a player |
|---|---|---|
| **Withering Rime-Bolt** | 6 s | Spell power (6 + 2 per level) damage + 8% of max health (at most 20), and 4.5 s of Withering Rime |
| **Rimefire Crown** | 15 s | A crown where you look (24 blocks), standing 3.5 to 5.5 s by level |
| **Barrow-Step** | 12 s | Teleports 11 to 15 blocks where you look, leaving a husk in your own shape and skin |
| **Whiteout Dominion** | 120 s | A storm 16 to 32 blocks round for 25 to 45 s by level. It fogs, slows and freezes whoever you'd harm, and freezes water, but doesn't raise the dead. You and your allies get no fog |

## Her sword and staff

### Blade of the Dreaded Queen

| Property | Value |
|---|---|
| Attack damage | 11.5 |
| Attack speed | 1.6 |
| Reach | +0.5 blocks |
| Durability | Unbreakable |

- **Thermodynamic Theft:** every strike leaves 4.5 s of Withering Rime.
- **Glass-Rime Shatter:** every third full strike on the same chilled or frozen foe (each within 5 s of the last; hitting others in between doesn't reset it) deals 10% of its max health past armor (at most 20) and leaves it **Brittle** (10% less armor) for 4 s.
- **Fire-Drinking Guard:** hold right-click to raise it. It stops fire and magic from the front (Iron's spells and Ice and Fire's dragon fire included; arrows and blades still get through) and puts out fire on and around you. A stopped hit charges the blade, which glints until its next full strike shatters at once. If your off hand holds something with its own right-click (a shield, food, a bow, blocks, a staff), that wins.

The queen swings the same sword, so her strikes leave Withering Rime and shatter too.

### Staff of the Dreaded Queen

- An Iron's staff carrying Withering Rime-Bolt and Rimefire Crown at level 3, on your spell wheel while you hold it; right-click casts. The spells can't be taken out.
- In the main hand: +15% Ice Spell Power, +10% Cooldown Reduction.
- **Permafrost Conduit:** Ice spells cost 20% less mana while you stand on snow or ice.

## Status effects

| Effect | What it does |
|---|---|
| **Withering Rime** | Withers through armor, freezes, stops healing ([above](#withering-rime)) |
| **Rimebound** | Rooted: no movement or jumping |
| **Brittle** | 10% less armor |
| **Whiteout** | 20% slower, half follow range, fog for players |

## Dread scuttlers

Every Ice and Fire dread scuttler, hers or not, spits a weaker Withering Rime-Bolt from 3 to 12 blocks every 3 to 4 s: 3 + 4% of max health and 2 s of Withering Rime.

## Still to come

- Natural spawning for the queen and Syndrigossa.
- Hatching Syndrigossa's egg.
- The Dread Lands.
