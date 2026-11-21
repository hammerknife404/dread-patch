# Dread Patch

Patching in the Dread Queen for my own world.

[Ice and Fire: Community Edition](https://modrinth.com/mod/iceandfire-ce) ships a Dread Queen (her class, textures, name, sword and staff) but never puts her in the game. This NeoForge mod finishes her: a necromancer boss with her own spells, court, storm and undead dragon, a way to summon her, and the loot to make her worth it.

Full details are in the [wiki](https://github.com/hammerknife404/dread-patch/wiki).

**Status:** in development. She doesn't spawn naturally yet; summon her through a Dread Gate, a spawn egg or `/summon`.

## Requirements

Required on both the client and the server.

| Requirement | Version | Notes |
|---|---|---|
| Minecraft | 1.21.1 | Exactly 1.21.1 |
| NeoForge | 21.1.265 or newer | Not Forge, not Fabric |
| Java | 21 | |
| [Ice and Fire: Community Edition](https://modrinth.com/mod/iceandfire-ce) | 2.1-beta.1 or newer | Built and tested against 2.1-beta.1 |
| [Iron's Spells 'n Spellbooks](https://modrinth.com/mod/irons-spells-n-spellbooks) | 1.21.1-3.0.0 or newer | Built and tested against 1.21.1-3.16.3 |

Ice and Fire also needs Uranus, Jupiter and GeckoLib; Iron's Spells needs Iron's Lib, playerAnimator and Curios. Install whatever those two ask for. The versions this mod is built against are pinned in [`gradle.properties`](gradle.properties).

## Compatibility

- **Tested:** the owner's pack, The Living World 1.3.6c (NeoForge 21.1.265, about 200 mods), and Better Combat 2.4.0 with Better Mob Combat Reimagined (the sword's three-strike shatter counts per target, so sweeping swings work).
- **Untested:** Iris shader packs and Fabulous graphics with Whiteout Dominion's fog and snow; Ice and Fire CE versions after 2.1-beta.1.
- **Not supported:** Forge, Fabric, any Minecraft version but 1.21.1.

More in the wiki's [Requirements and Compatibility](https://github.com/hammerknife404/dread-patch/wiki/Requirements-and-Compatibility) page.

## Installation

1. Install NeoForge 21.1.265 or newer for Minecraft 1.21.1.
2. Add Ice and Fire CE and Iron's Spells 'n Spellbooks, with their own requirements.
3. Download the latest `dreadpatch` jar from [Releases](https://github.com/hammerknife404/dread-patch/releases) and put it in `mods/` on the client and the server.

## Features

- **The Dread Queen** (`dreadpatch:dread_queen`): 300 health, 20 armor, 12 toughness, a boss bar, and her original Ice and Fire model. She rises out of the ground, keeps her distance like a necromancer, and walks over water (**Rimewalk**).
- **Her spells:** Withering Rime-Bolt, Rimefire Crown, Barrow-Step and Whiteout Dominion (a blizzard in which the dead rise in her court). Players can learn them from her scrolls.
- **Her court:** Ice and Fire's thralls, ghouls, knights and scuttlers raised round her, burnt for **Beater's Decree**, and a Dread Lich every 90 seconds.
- **Withering Rime:** the mark of her magic. It withers through armor, freezes, stops all healing and heals her.
- **Syndrigossa:** her undead ice dragon, with a Withering Rime breath, Shatter-Spine and Sovereign's Eclipse. As the boss, the queen rides her.
- **The Dread Gate:** a dreadstone frame opened with a Gravemaster's Key (now craftable) summons her. Every Mausoleum has four.
- **Her loot:** her sword and staff (both reworked into real weapons), epic or legendary inks, and scrolls of her spells.

See [Features](https://github.com/hammerknife404/dread-patch/wiki/Features) for numbers and mechanics.

## Building

`./gradlew build` puts the jar in `build/libs/`. `./gradlew runClient` starts a dev client with Ice and Fire and Iron's Spells loaded.

## Credits

Her model is the geometry of Ice and Fire's unused `ModelDreadQueen` (Alexthe666 and contributors, LGPL-3.0). Her textures, name, sword, staff and behavior come from Ice and Fire CE at runtime.
