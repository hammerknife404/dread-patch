# Dread Patch

Patching in the Dread Queen for my own world.

[Ice and Fire: Community Edition](https://modrinth.com/mod/iceandfire-ce) ships a Dread Queen (her class, textures, name, sword and staff) but never puts her in the game. This NeoForge mod finishes her.

**Status:** in development. The Dread Queen exists as `dreadpatch:dread_queen`: spawn her with her spawn egg (Spawn Eggs tab) or `/summon dreadpatch:dread_queen`. She has Ice and Fire's stats for her (750 health, set by Ice and Fire's `dreadQueenMaxHealth` config; 30 armor), a boss bar, her sword and staff, and her original model, and she rises out of the ground when spawned. She fights like a necromancer: she keeps her distance, circling at casting range, giving ground when pressed and breaking away when cornered, and saves her sword for whoever still reaches her. From range she shoots her **Withering Rime-Bolt**: soot-black, bone-white frost that takes a share of the target's own life on top of its damage and leaves **Withering Rime**, which withers through armor, freezes and stops all healing. She raises a **Rimefire Crown** round her target, a ring of ice spikes wreathed in cold cyan dread fire over cyan and black smoke, that roots everyone inside and drinks every other fire within it, then shatters it with her bolt for a burst of shrapnel. She raises her **Court of the Dread**, Ice and Fire's own thralls, ghouls, knights and scuttlers rising from the ground around her, and with **Beater's Decree** burns one of them from behind as it presses an enemy: a burst of rime that sends the rest of her court into a frenzy. She drops her sword and staff, two epic or legendary inks, and one or two scrolls of her spells, Ice spells (they count Ice spell power) found nowhere else. She doesn't spawn naturally yet.

**Requires:** Minecraft 1.21.1, NeoForge 21.1.265+, Ice and Fire CE 2.1-beta.1+ (with Uranus, Jupiter and GeckoLib, which it requires anyway), and Iron's Spells 'n Spellbooks 3.x for 1.21.1.

## Building

`./gradlew build` puts the jar in `build/libs/`. `./gradlew runClient` starts a dev client with Ice and Fire loaded.

## Credits

Her model is the geometry of Ice and Fire's unused `ModelDreadQueen` (Alexthe666 and contributors, LGPL-3.0). Her textures, name, sword, staff and behavior come from Ice and Fire CE at runtime.
