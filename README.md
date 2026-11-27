# Dread Patch

Patching in the Dread Queen for my own world.

[Ice and Fire: Community Edition](https://modrinth.com/mod/iceandfire-ce) ships a Dread Queen (her class, textures, name, sword and staff) but never puts her in the game. This NeoForge mod finishes her, and gives her the Rimefrost, undead frost dragons of the Dread, among them her own: Syndrigossa.

**Requires:** Minecraft 1.21.1, NeoForge 21.1.230+, Ice and Fire CE 2.1-beta.1+ (with Uranus, Jupiter and GeckoLib, which it requires anyway), and Iron's Spells 'n Spellbooks 3.x for 1.21.1.

**Status:** in development. In the wild she's met only atop the Dreadlands' spires.

**The full guide is the [wiki](https://github.com/hammerknife404/dread-patch/wiki).** This page is the short version.

## What's in it

- **[The Dread Queen](https://github.com/hammerknife404/dread-patch/wiki/The-Dread-Queen):** a necromancer boss with 300 health, her own theme, and Withering Rime, which withers through armour and heals her. She casts her bolt, her crown of ice and Barrow-Step, raises her court, a lich and Rimefrost dragons, and calls a storm that raises the dead. She drops her scrolls and her regalia, a four-piece armour set with a winged bodice that glides. Her defeat stirs the Dread round Ice and Fire's Mausoleums. Optionally, the Dread's weather reaches the overworld.
- **[The Dread Gate](https://github.com/hammerknife404/dread-patch/wiki/The-Dread-Gate):** a dreadstone frame keyed with a Gravemaster's Key opens a way to the Dreadlands.
- **[The Dreadlands](https://github.com/hammerknife404/dread-patch/wiki/The-Dreadlands):** a frozen dimension of four lands: plains, the Sastrugi Wastes, the Rime Chasma and the Glacial Plains. It has its own storms, abandoned villages, wild Rimefrost dragons' nests, the Dread rising and patrolling, and the queen's spires with her encounter at the top.
- **[Rimefrost dragons and Syndrigossa](https://github.com/hammerknife404/dread-patch/wiki/Rimefrost-Dragons):** undead frost dragons, wild or the queen's, and Syndrigossa, her own named mount.
- **[Dread phantoms](https://github.com/hammerknife404/dread-patch/wiki/Dread-Phantoms):** phantoms of the Dread that never burn by day, called by the queen, liches and Syndrigossa. Also the risen Dread.
- **[Dread Fish](https://github.com/hammerknife404/dread-patch/wiki/Dread-Fish):** dread anglerfish of the Dreadlands' dark water. They swim and breach like dolphins and leap at prey, even from land.
- **[Gear](https://github.com/hammerknife404/dread-patch/wiki/Gear):** Rimefrost dragonscale and dread dragonsteel gear and dragon armour, the Rimefrost dragonforge, and her staff and sword.
- **[Her spells](https://github.com/hammerknife404/dread-patch/wiki/Spells):** the Rimefrost school, a kind of Ice magic, learnt from her scrolls.
- **[Fixes to Ice and Fire](https://github.com/hammerknife404/dread-patch/wiki/Fixes-to-Ice-and-Fire):** riders sit upright in the saddle, nothing goes on fighting a dragon's corpse, and dreadwood leaves drop saplings.
- **[Other mods](https://github.com/hammerknife404/dread-patch/wiki/Other-Mods):** infection mods' effects never take on the Dread.

## Trying her out

- **Spawn eggs**, in the Spawn Eggs tab: the Dread Queen; the Dread Queen and Syndrigossa (her riding it, as the boss); Syndrigossa; an Ancient Syndrigossa; a Rimefrost Dragon; and a Dread Phantom.
- **Commands:** `/summon dreadpatch:dread_queen`, `/summon dreadpatch:dread_queen ~ ~ ~ {WithSyndrigossa:1b}` (riding her dragon), `/summon dreadpatch:syndrigossa`, and `/summon dreadpatch:syndrigossa ~ ~ ~ {Ancient:1b}` (full strength). Operators also have [`/dreadpatch`](https://github.com/hammerknife404/dread-patch/wiki/Commands).

## Settings

Dread Patch's server settings are in the mods list: select Dread Patch and press **Config**. In singleplayer you can change them there as you play, and they apply at once. On a server they're in `config/dreadpatch-server.toml` (a copy in the world's `serverconfig` folder overrides it). Every value is listed on the wiki's [Settings](https://github.com/hammerknife404/dread-patch/wiki/Settings) page.

## Building

`./gradlew build` puts the jar in `build/libs/`. `./gradlew runClient` starts a dev client with Ice and Fire loaded.

## Credits

Her model is the geometry of Ice and Fire's unused `ModelDreadQueen` (Alexthe666 and contributors, LGPL-3.0). Her textures, name, sword, staff and behavior come from Ice and Fire CE at runtime. The Dreadlands' music, "Entombed In Rimefrost", and her theme, "Beneath the Permafrost", are hammerknife404's own.
