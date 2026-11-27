"""Writes dread dragonsteel's block and dragon armour data and assets (0.84.4): the block (blockstate, models, loot, the
recipes to and from nine ingots) and the four dragon armour pieces (item models, recipes from blocks, as Ice and Fire's
ice dragonsteel's). Python 3, stdlib. Deterministic; edit and re-run.

    python3 tools/datagen/make_dread_dragonsteel.py

Their sprites are recoloured from Ice and Fire's as they load (`assets/minecraft/atlases/blocks.json`, by hand).
"""
import json
import pathlib

REPO = pathlib.Path(__file__).resolve().parents[2]
ASSETS = REPO / "src/main/resources/assets/dreadpatch"
DATA = REPO / "src/main/resources/data/dreadpatch"
BLOCK = "dread_dragonsteel_block"
INGOT = "dreadpatch:dread_dragonsteel_ingot"
# Ice and Fire's ice dragonsteel dragon armour's patterns (its recipe/dragonarmor_dragon_steel_ice_*.json).
PATTERNS = {
    "head": ["   ", " ##", "###"],
    "neck": ["   ", "###", " ##"],
    "body": ["###", "###", " ##"],
    "tail": ["   ", "  #", " ##"],
}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def main():
    write(ASSETS / f"blockstates/{BLOCK}.json", {"variants": {"": {"model": f"dreadpatch:block/{BLOCK}"}}})
    write(ASSETS / f"models/block/{BLOCK}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"dreadpatch:block/{BLOCK}"}})
    write(ASSETS / f"models/item/{BLOCK}.json", {"parent": f"dreadpatch:block/{BLOCK}"})
    write(DATA / f"loot_table/blocks/{BLOCK}.json", {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
        "entries": [{"type": "minecraft:item", "name": f"dreadpatch:{BLOCK}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(DATA / f"recipe/{BLOCK}.json", {"type": "minecraft:crafting_shaped", "category": "building",
                                          "key": {"#": {"item": INGOT}}, "pattern": ["###", "###", "###"],
                                          "result": {"id": f"dreadpatch:{BLOCK}"}})
    write(DATA / "recipe/dread_dragonsteel_ingot_from_block.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                                    "ingredients": [{"item": f"dreadpatch:{BLOCK}"}],
                                                                    "result": {"id": INGOT, "count": 9}})
    for part, pattern in PATTERNS.items():
        name = f"dragonarmor_dread_dragonsteel_{part}"
        write(ASSETS / f"models/item/{name}.json", {"parent": "item/generated", "textures": {"layer0": f"dreadpatch:item/{name}"}})
        write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
                                             "key": {"#": {"item": f"dreadpatch:{BLOCK}"}}, "pattern": pattern,
                                             "result": {"id": f"dreadpatch:{name}"}, "show_notification": True})


if __name__ == "__main__":
    main()
