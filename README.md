# Dread Patch

Patching in the Dread Queen for my own world.

[Ice and Fire: Community Edition](https://modrinth.com/mod/iceandfire-ce) ships a Dread Queen (her class, textures, name, sword and staff) but never puts her in the game. This NeoForge mod finishes her.

**Status:** in development. The Dread Queen exists as `dreadpatch:dread_queen`: spawn her with her spawn egg (Spawn Eggs tab) or `/summon dreadpatch:dread_queen`. She has Ice and Fire's stats for her (750 health, set by Ice and Fire's `dreadQueenMaxHealth` config; 30 armor), a boss bar, her sword and staff, and her original model. She doesn't spawn naturally yet, and her loot is a placeholder.

**Requires:** Minecraft 1.21.1, NeoForge 21.1.265+, Ice and Fire CE 2.1-beta.1+ (with Uranus, Jupiter and GeckoLib, which it requires anyway).

## Building

`./gradlew build` puts the jar in `build/libs/`. `./gradlew runClient` starts a dev client with Ice and Fire loaded.

## Credits

Her model is the geometry of Ice and Fire's unused `ModelDreadQueen` (Alexthe666 and contributors, LGPL-3.0). Her textures, name, sword, staff and behavior come from Ice and Fire CE at runtime.
