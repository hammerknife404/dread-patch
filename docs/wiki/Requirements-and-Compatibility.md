# Requirements and Compatibility

Taken from the mod's build files (`gradle.properties`, `build.gradle`, `neoforge.mods.toml`). Everything here is required on both sides.

## Requirements

| Requirement | Accepted versions | Built against | Required by |
|---|---|---|---|
| Minecraft | 1.21.1 only | 1.21.1 | |
| NeoForge | 21.1.265 or newer | 21.1.265 | |
| Java | 21 | 21 | |
| Ice and Fire: Community Edition | 2.1-beta.1 or newer | 2.1-beta.1 | Dread Patch |
| Iron's Spells 'n Spellbooks | 1.21.1-3.0.0 or newer | 1.21.1-3.16.3 | Dread Patch |
| Uranus | as Ice and Fire asks | 2.4.1 | Ice and Fire |
| Jupiter | as Ice and Fire asks | 2.3.7 | Ice and Fire |
| GeckoLib | as Ice and Fire asks | 4.9.3 | Ice and Fire |
| Iron's Lib | as Iron's Spells asks | 1.21.1-2.3.0 | Iron's Spells |
| playerAnimator | as Iron's Spells asks | 2.0.4 | Iron's Spells |
| Curios API | as Iron's Spells asks | 9.5.1 | Iron's Spells |

"Accepted versions" is what NeoForge will load the mod with. Only the "built against" versions have been played.

## Compatibility

### Tested

- **The Living World 1.3.6c**, the owner's pack (about 200 mods, NeoForge 21.1.265, with Ice and Fire CE 2.1-beta.1, `iceandfire_create`, Iron's Spells 1.21.1-3.16.3 and its addons, L_Ender's Cataclysm, AttributeFix, JEI, Jade).
- **Better Combat 2.4.0** with Better Mob Combat Reimagined and Cloth Config: loaded in the dev runs. The sword's Glass-Rime Shatter counts strikes per target, so a sweep through a crowd doesn't reset it, and its extra half block of reach carries into Better Combat's range.
- **Ice and Fire's config:** Syndrigossa reads the ice dragon breath damage from it, and hunts players even with `neutralToPlayer` on. Her health is set by difficulty instead of `dragon.maxHealth`. The queen's health ignores `misc.dreadQueenMaxHealth`.

### Untested

- **Iris shader packs:** Whiteout Dominion's fog and snow wall may not show, or may show differently. The pack runs Iris and Sodium, but the storm hasn't been checked under a shader pack.
- **Fabulous graphics:** the storm has a path for it, not yet checked in game.
- **Ice and Fire CE after 2.1-beta.1:** the mod reaches into Ice and Fire's dragons and dread mobs, so a later version could break it.
- **Other mods that change Ice and Fire's dragons or the Mausoleum.**

### Not supported

- Forge, Fabric or Quilt.
- Any Minecraft version but 1.21.1.

## Notes for pack makers

- Her spells (Withering Rime-Bolt, Rimefire Crown, Barrow-Step, Whiteout Dominion) are kept out of Iron's random loot and the scroll forge: only her loot drops them.
- She's tagged `#c:bosses`, so mods that respect it treat her as a boss (not capturable, for example).
- Whiteout Dominion's snow is only drawn, never placed, so it never stays behind; its ice is frosted ice that thaws.
- The Dread Gate's frame blocks are the tag `#dreadpatch:dread_gate_frame`; the Permafrost Conduit's ground is `#dreadpatch:permafrost`.
