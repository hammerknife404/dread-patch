# Installation

The mod is needed on both the client and the server (in single player, just the client).

1. Install [NeoForge](https://neoforged.net/) 21.1.265 or newer for Minecraft 1.21.1, with Java 21.
2. Add [Ice and Fire: Community Edition](https://modrinth.com/mod/iceandfire-ce) 2.1-beta.1 or newer, with what it requires: Uranus, Jupiter and GeckoLib.
3. Add [Iron's Spells 'n Spellbooks](https://modrinth.com/mod/irons-spells-n-spellbooks) for 1.21.1 (3.0.0 or newer), with what it requires: Iron's Lib, playerAnimator and Curios.
4. Download the latest `dreadpatch-<version>.jar` from [Releases](https://github.com/hammerknife404/dread-patch/releases) and put it in `mods/`.

The mod isn't on Modrinth or CurseForge; GitHub Releases is the only download.

## Checking it loaded

- The Spawn Eggs tab has a **Dread Queen Spawn Egg**, a **Syndrigossa Spawn Egg**, an **Ancient Syndrigossa Spawn Egg** and a **Dread Queen and Syndrigossa Spawn Egg**.
- `/summon dreadpatch:dread_queen` works.

## Updating

Replace the jar. NeoForge warns once that the world's `dreadpatch` version changed; that's expected. Scrolls of the Rimefire Crown made before 0.8.0 (when it was the Barrow-Glass Tomb) come out blank.

## Building from source

```
./gradlew build
```

The jar lands in `build/libs/`. `./gradlew runClient` and `./gradlew runServer` start dev runs with every dependency loaded.
