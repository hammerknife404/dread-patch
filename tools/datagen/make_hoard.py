"""Writes the hoard's data and assets (0.84.0): the iron and blacksteel piles (blockstates, models, loot, recipes) and
the blacksteel dragon armour (item models, recipes), after Ice and Fire's own gold pile and diamond dragon armour.
Python 3, stdlib. Deterministic; edit and re-run.

    python3 tools/datagen/make_hoard.py

Blacksteel is Cataclysm's, only through our tags (dreadpatch:blacksteel_nuggets, dreadpatch:blacksteel_blocks): its
recipes carry a mod_loaded condition, and the blacksteel pile drops a nugget from the tag (none, if it's empty).
"""
import json
import pathlib

REPO = pathlib.Path(__file__).resolve().parents[2]
ASSETS = REPO / "src/main/resources/assets/dreadpatch"
DATA = REPO / "src/main/resources/data/dreadpatch"
CATACLYSM = [{"type": "neoforge:mod_loaded", "modid": "cataclysm"}]
PARTS = {  # Ice and Fire's diamond dragon armour's patterns
    "head": ["   ", " ##", "###"],
    "neck": ["   ", "###", " ##"],
    "body": ["###", "###", "# #"],
    "tail": ["   ", "  #", "## "],
}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def pile(name, nugget, conditions=None):
    """A pile, as Ice and Fire's: eight layers of its models, two nuggets a layer, two nuggets make one."""
    write(ASSETS / f"blockstates/{name}.json", {"variants": {
        f"layers={n}": {"model": f"dreadpatch:block/{name}_height{2 * n}"} for n in range(1, 9)}})
    for n in range(1, 9):
        write(ASSETS / f"models/block/{name}_height{2 * n}.json", {
            "parent": f"iceandfire:block/goldpile_height{2 * n}",
            "textures": {"particle": f"dreadpatch:block/{name}", "texture": f"dreadpatch:block/{name}"}})
    write(ASSETS / f"models/item/{name}.json", {"parent": f"dreadpatch:block/{name}_height2"})
    entry = {"type": "minecraft:tag", "name": nugget[1:], "expand": True} if nugget.startswith("#") else {"type": "minecraft:item", "name": nugget}
    write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1.0, "entries": [
        dict(entry, conditions=[{"condition": "minecraft:block_state_property", "block": f"dreadpatch:{name}", "properties": {"layers": str(n)}}],
             functions=[{"function": "minecraft:set_count", "count": 2 * n}]) for n in range(1, 9)]}]})
    ingredient = {"tag": nugget[1:]} if nugget.startswith("#") else {"item": nugget}
    recipe = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [ingredient, ingredient],
              "result": {"id": f"dreadpatch:{name}"}}
    if conditions:
        recipe = {"neoforge:conditions": conditions, **recipe}
    write(DATA / f"recipe/{name}.json", recipe)


def main():
    pile("iron_pile", "minecraft:iron_nugget")
    pile("blacksteel_pile", "#dreadpatch:blacksteel_nuggets", CATACLYSM)
    for part, pattern in PARTS.items():
        name = f"dragonarmor_blacksteel_{part}"
        write(ASSETS / f"models/item/{name}.json", {"parent": "item/generated", "textures": {"layer0": f"dreadpatch:item/{name}"}})
        write(DATA / f"recipe/{name}.json", {"neoforge:conditions": CATACLYSM, "type": "minecraft:crafting_shaped", "category": "equipment",
                                             "key": {"#": {"tag": "dreadpatch:blacksteel_blocks"}}, "pattern": pattern,
                                             "result": {"id": f"dreadpatch:{name}"}, "show_notification": True})
    write(DATA / "tags/item/blacksteel_nuggets.json", {"values": [{"id": "cataclysm:black_steel_nugget", "required": False},
                                                                   {"id": "#c:nuggets/black_steel", "required": False}]})
    write(DATA / "tags/item/blacksteel_blocks.json", {"values": [{"id": "cataclysm:black_steel_block", "required": False},
                                                                  {"id": "#c:storage_blocks/black_steel", "required": False}]})


if __name__ == "__main__":
    main()
