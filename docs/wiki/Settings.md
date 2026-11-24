# Settings

Dread Patch's server settings (how a summoned court behaves, how long until a spire's queen comes back for you, how often the Dread rise, patrol and fly in the Dreadlands, how often its storms are whiteouts) are in the mods list: select Dread Patch and press **Config**. In singleplayer you can change them there as you play, and they apply at once. On a server they're in `config/dreadpatch-server.toml` (a copy in the world's `serverconfig` folder overrides it).

## `[summoned_court]`

How a court raised by the Summon Court spell behaves. It always fights hostile mobs that fight its caster, and never the Dread or its caster's side.

| Option | Default | What it does |
|---|---|---|
| `attack_friendly_mobs` | `false` | Joins its caster's fights with mobs that aren't hostile (animals, villagers, golems, other players' pets). Off, friendly mobs leave it be too. |
| `retaliate` | `false` | Fights back against friendly mobs and players that hurt it. |
| `attack_players` | `false` | Joins its caster's fights with other players. |

## `[dreadlands]`

The "one in" values are chances: 0 turns that thing off.

| Option | Default | What it does |
|---|---|---|
| `encounter_reset_hours` | `6.0` | Real hours before a spire's Queen and Syndrigossa encounter comes again to a player who's met it (each player, each spire). 0: every time, once she's gone. |
| `dread_spawns_clear` | `12` | One in this chance, per player every 10 s, of 1-3 Dread rising 24-48 blocks off in clear weather, day or night. |
| `dread_spawns_snow` | `6` | The same while it snows. |
| `dread_spawns_storm` | `5` | The same in a heavy snowstorm (thunder). |
| `dread_cap` | `8` | No more rise while this many of the Dread are within 64 blocks of the player. |
| `patrols` | `3` | One in this chance, per player every 2 minutes, of a Dread patrol (a dread knight leading 2-4 more). |
| `undead_spawns` | `16` | One in this chance, per player every 10 seconds, of 1-2 of vanilla's undead (strays, zombies, skeletons, zombie villagers) rising 24-48 blocks off in the Dreadlands, day or night. 0 for never. |
| `undead_cap` | `4` | No more of them rise while this many of vanilla's undead are within 64 blocks of the player. |
| `phantoms` | `8` | One in this chance, every 30 s, of dread phantoms coming down on a player under the open sky. |
| `phantoms_near_spires` | `3` | The same within 96 blocks of a spire. |
| `flame_fade_min_seconds` / `flame_fade_max_seconds` | `30` / `45` | How long a placed lantern or torch waits for each step toward the Dread's. |
| `whiteout_storms` | `6` | About one storm in this many is a Whiteout Dominion. Never two in a row (so 2 or less is every other storm), never starting while a Dread Queen is about, breaking into a thunderstorm when her fight starts. |


## `[overworld]`

What the Dread Queen's defeat stirs.

| Option | Default | What it does |
|---|---|---|
| `mausoleum_dread` | `12` | Once a Dread Queen has been defeated in the world, one in this chance per player every 10 seconds of 1-3 Dread rising round a Mausoleum within 128 blocks (cold biomes, on snow, ice or dreadstone, never in a village); patrols cross there too. 0 for never, patrols included. |
| `defeat_storm_hours` | `6.0` | Real hours after a defeat storm before her defeat brings another. 0 for one every defeat. |
| `defeat_storm_cold_only` | `true` | Whether the defeat storm reaches only players in cold and snowy biomes (off: everyone in the overworld under the sky). |

## `[risen]`

Bats, parrots and water creatures risen Dread, stirring on their own.

| Option | Default | What it does |
|---|---|---|
| `cave_dread_phantoms` | `40` | In the overworld and the Dreadlands, one in this chance per player every 10 seconds of a miniature dread phantom stirring in dark cave air near them. 0 for never. |
| `cave_dread_water` | `40` | The same for a Dread water creature (glow squid mostly, squid, cod, salmon) in underground water near them. 0 for never. |

## `[regalia]`

| Option | Default | What it does |
|---|---|---|
| `crown_storm_sight` | `true` | Her crown keeps the Dreadlands' storm fog off its wearer. |
| `crown_whiteout_sight` | `0.75` | How much further her crown lets its wearer see in any whiteout (0.75 is 75%). |
| `regalia_eases_weather` | `true` | Her full set eases the Dreadlands' weather slow a step. |

Any of these can also be read or set with `/dreadpatch config`.

---
*Kept in step with the mod's [README](https://github.com/hammerknife404/dread-patch#readme); edits go in `docs/wiki/` in the repository.*
