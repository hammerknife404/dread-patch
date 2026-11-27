"""Writes the hoard's data and assets (0.84.0): the iron pile (blockstates, models, loot, recipe), after Ice and Fire's
own gold pile. Python 3, stdlib. Deterministic; edit and re-run.

    python3 tools/datagen/make_hoard.py

(0.84.0's blacksteel pile and blacksteel dragon armour, made from L_Ender's Cataclysm's blacksteel, were taken out in
0.84.2, at the owner's word.)
"""
import json
import pathlib

REPO = pathlib.Path(__file__).resolve().parents[2]
ASSETS = REPO / "src/main/resources/assets/dreadpatch"
DATA = REPO / "src/main/resources/data/dreadpatch"


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def pile(name, nugget):
    """A pile, as Ice and Fire's: eight layers of its models, two nuggets a layer, two nuggets make one."""
    write(ASSETS / f"blockstates/{name}.json", {"variants": {
        f"layers={n}": {"model": f"dreadpatch:block/{name}_height{2 * n}"} for n in range(1, 9)}})
    for n in range(1, 9):
        write(ASSETS / f"models/block/{name}_height{2 * n}.json", {
            "parent": f"iceandfire:block/goldpile_height{2 * n}",
            "textures": {"particle": f"dreadpatch:block/{name}", "texture": f"dreadpatch:block/{name}"}})
    write(ASSETS / f"models/item/{name}.json", {"parent": f"dreadpatch:block/{name}_height2"})
    write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1.0, "entries": [
        {"type": "minecraft:item", "name": nugget,
         "conditions": [{"condition": "minecraft:block_state_property", "block": f"dreadpatch:{name}", "properties": {"layers": str(n)}}],
         "functions": [{"function": "minecraft:set_count", "count": 2 * n}]} for n in range(1, 9)]}]})
    write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                         "ingredients": [{"item": nugget}, {"item": nugget}], "result": {"id": f"dreadpatch:{name}"}})


def main():
    pile("iron_pile", "minecraft:iron_nugget")


if __name__ == "__main__":
    main()
