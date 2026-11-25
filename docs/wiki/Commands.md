# Commands

For operators and the server console, under `/dreadpatch`:

| Command | What it does |
|---|---|
| `/dreadpatch weather` | Tells you the Dreadlands' weather: clear, snow, a heavy snowstorm or a whiteout. |
| `/dreadpatch weather clear\|snow\|storm\|whiteout [seconds]` | Sets it, for 10 minutes unless you say. The Dreadlands share the overworld's weather, so the overworld's changes too. `whiteout` makes the storm a Whiteout Dominion whatever its odds; `snow` and `storm` are plain. |
| `/dreadpatch encounters reset [players]` | Forgets who has met which spire's Queen, so her encounter comes again (everyone's, unless you name players). |
| `/dreadpatch dreadlands enter [players]` | Sends players into the Dreadlands, where a gate would set them down, remembering where they came from. |
| `/dreadpatch dreadlands home [players]` | Sends players in the Dreadlands home again. |
| `/dreadpatch dreadlands arrival [x z]` | Where a gate there (or where you stand) would set someone down in the Dreadlands, and how far that is from the nearest spire. Arrivals are random, so each try may differ. |
| `/dreadpatch escalation [storm [seconds] \| reset]` | Whether a Dread Queen has been defeated in this world (so the Dread stir round the Mausoleums) and the defeat storm: on, waiting, or due; `storm` brings one now (30-60 s, or the seconds given), whatever the wait; `reset` forgets every defeat and the storm. |
| `/dreadpatch config [setting [value]]` | Lists every Dread Patch setting, shows one, or sets one (saved to `config/dreadpatch-server.toml`, applied at once). |
| `/dreadpatch patrol [pos]` | Raises a Dread patrol there (a dread knight leading 2 to 4 more). |
| `/dreadpatch rise [count] [pos]` | Raises 1 to 8 of the Dread there (3 by default). |

---
*Kept in step with the mod's [README](https://github.com/hammerknife404/dread-patch#readme); edits go in `docs/wiki/` in the repository.*
