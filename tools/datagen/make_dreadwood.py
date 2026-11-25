"""Writes the dreadwood blocks' JSON (0.65.0: stairs, slab, fence, fence gate, door, trapdoor, button, pressure
plate) from vanilla's oak ones, read out of the dev run's Minecraft jars. Python 3, stdlib.

    python3 tools/datagen/make_dreadwood.py

Blockstates, block and item models, recipes and their unlocking advancements, and loot tables: vanilla's oak
files with oak's names and textures swapped for ours. The planks texture is Ice and Fire's dreadwood planks'; the
door and trapdoor are vanilla's dark oak ones recoloured as they load (assets/minecraft/atlases/blocks.json), so
nothing of vanilla's ships but the files' shapes. Deterministic; re-run it rather than editing its output.
"""
import json
import pathlib
import zipfile

REPO = pathlib.Path(__file__).resolve().parents[2]
NEO = next(line.split("=", 1)[1].strip() for line in (REPO / "gradle.properties").read_text().splitlines() if line.startswith("neo_version="))
# Vanilla's assets and data both, in the dev run's resources jar (run a build first).
ASSETS_JAR = DATA_JAR = REPO / f"build/moddev/artifacts/neoforge-{NEO}-client-extra-aka-minecraft-resources.jar"
RES = REPO / "src/main/resources"
KINDS = ["stairs", "slab", "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate"]
SWAPS = [
    ("minecraft:block/oak_planks", "iceandfire:block/dreadwood_planks"),
    ("minecraft:oak_planks", "iceandfire:dreadwood_planks"),
    ("minecraft:block/oak_door_top", "dreadpatch:block/dreadwood_door_top"),
    ("minecraft:block/oak_door_bottom", "dreadpatch:block/dreadwood_door_bottom"),
    ("minecraft:block/oak_trapdoor", "dreadpatch:block/dreadwood_trapdoor"),
    ("minecraft:item/oak_door", "dreadpatch:item/dreadwood_door"),
    ("minecraft:block/oak_", "dreadpatch:block/dreadwood_"),
    ("minecraft:item/oak_", "dreadpatch:item/dreadwood_"),
    ("minecraft:oak_", "dreadpatch:dreadwood_"),
    ("minecraft:blocks/oak_", "dreadpatch:blocks/dreadwood_"),
]


def swap(text):
    for old, new in SWAPS:
        text = text.replace(old, new)
    return text


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(json.loads(text), indent=2) + "\n", encoding="utf-8")


def main():
    with zipfile.ZipFile(ASSETS_JAR) as assets, zipfile.ZipFile(DATA_JAR) as data:
        names = set(assets.namelist()) | set(data.namelist())
        for kind in KINDS:
            oak = f"oak_{kind}"
            ours = f"dreadwood_{kind}"
            state = assets.read(f"assets/minecraft/blockstates/{oak}.json").decode()
            write(RES / f"assets/dreadpatch/blockstates/{ours}.json", swap(state))
            for model in sorted(n for n in names if n.startswith(f"assets/minecraft/models/block/{oak}")):
                tail = model.rsplit("/", 1)[1].replace("oak_", "dreadwood_", 1)
                text = swap(assets.read(model).decode())
                if kind in ("door", "trapdoor"):
                    # Vanilla sets its doors' cutout in code; ours, in the model, or their holes draw black.
                    text = json.dumps({**json.loads(text), "render_type": "minecraft:cutout"})
                write(RES / f"assets/dreadpatch/models/block/{tail}", text)
            write(RES / f"assets/dreadpatch/models/item/{ours}.json", swap(assets.read(f"assets/minecraft/models/item/{oak}.json").decode()))
            write(RES / f"data/dreadpatch/loot_table/blocks/{ours}.json", swap(data.read(f"data/minecraft/loot_table/blocks/{oak}.json").decode()))
            write(RES / f"data/dreadpatch/recipe/{ours}.json", swap(data.read(f"data/minecraft/recipe/{oak}.json").decode()))
            unlock = next(n for n in names if n.startswith("data/minecraft/advancement/recipes/") and n.endswith(f"/{oak}.json"))
            folder = unlock.split("/")[-2]
            write(RES / f"data/dreadpatch/advancement/recipes/{folder}/{ours}.json", swap(data.read(unlock).decode()))


if __name__ == "__main__":
    main()
