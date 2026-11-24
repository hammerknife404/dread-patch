"""Writes the Dreadlands' terrain (0.66.0): its noise settings' router, its noises, its three biomes' climate
points, and its underground features. Python 3, stdlib.

    python3 tools/datagen/make_dreadlands_terrain.py

Density is in vanilla's units: the base gradient falls 1 for every 24 blocks up, so adding 1 raises the ground 24
blocks. Deterministic; tune the numbers here and re-run rather than editing the JSON.

- Plains (dreadpatch:dreadlands): the ground at about 72, rolling, faint hummocks, now and then a field of drifts.
- Sastrugi Wastes: where the basins noise (continents) passes 0.12, the ground falls about 22 blocks to a sunken
  seabed of tall smooth dunes. How fast it falls is the shores noise (erosion): a gradual shelf, or a sharp bluff.
- Rime Chasma: in the basins where the chasma noise (ridges) passes 0.3, a lakebed lower still, cut by polygonal
  trenches (dreadpatch:cell_edges) with sheer walls, pinching shut into snow bridges, down into the caves; and
  narrower sheer fissures through it. The owner: "Caves should be much more sheered in the cracked biomes."
- Caves everywhere to bedrock: vanilla's spaghetti, noodle and entrance caves and its cheese caverns (closed near the
  surface). Sea level at the bottom, so nothing floods: the basins stay dry.
"""
import json
import pathlib

REPO = pathlib.Path(__file__).resolve().parents[2]
DATA = REPO / "src/main/resources/data/dreadpatch"

SURFACE_Y = 72
SHELF = -0.92        # the basins' fall: 22 blocks
CHASMA_FLOOR = -0.45  # the chasma's lakebed, 11 blocks lower again
TRENCH_FLOOR = -24    # the trenches cut down to here
CELL = 80.0           # a trench cell's size (blocks)
FISSURE_CELL = 20.0


def noise(name, y_scale=0.0):
    return {"type": "minecraft:noise", "noise": name, "xz_scale": 1.0, "y_scale": y_scale}


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def clamp(x, lo, hi):
    return {"type": "minecraft:clamp", "input": x, "min": lo, "max": hi}


def grad(from_y, to_y, from_v, to_v):
    return {"type": "minecraft:y_clamped_gradient", "from_y": from_y, "to_y": to_y, "from_value": from_v, "to_value": to_v}


def total(*parts):
    out = parts[0]
    for part in parts[1:]:
        out = add(out, part)
    return out


def minimum(a, b):
    return {"type": "minecraft:min", "argument1": a, "argument2": b}


def spline(coordinate, points):
    return {"coordinate": coordinate, "points": [{"location": loc, "derivative": 0.0, "value": val} for loc, val in points]}


def main():
    # Scaled so the basins (past 0.14) cover about a third of the land, the chasma a smaller share within them.
    basins = mul(1.6, noise("dreadpatch:dreadlands_basins"))
    shores = noise("dreadpatch:dreadlands_shores")
    chasma = noise("dreadpatch:dreadlands_chasma")
    # Ground at SURFACE_Y: vanilla's gradient over the world's height, shifted.
    base = grad(-64, 320, 5.666666666666667 + (SURFACE_Y - 72) / 24.0, -10.333333333333334 + (SURFACE_Y - 72) / 24.0)
    rolling = mul(0.42, noise("dreadpatch:dreadlands_rolling"))
    hummocks = mul(0.06, noise("dreadpatch:dreadlands_hummocks"))
    ridge = add(0.55, mul(-1.1, {"type": "minecraft:abs", "argument": noise("dreadpatch:dreadlands_drifts")}))
    fields = clamp(mul(4.0, add(-0.15, noise("dreadpatch:dreadlands_dune_fields"))), 0.0, 1.0)
    # The shore: a sharp bluff where the shores noise is low, a long shelf where it's high.
    shelf = {"type": "minecraft:spline", "spline": spline(shores, [
        (-1.0, spline(basins, [(0.10, 0.0), (0.14, SHELF)])),
        (0.0, spline(basins, [(0.08, 0.0), (0.20, SHELF)])),
        (1.0, spline(basins, [(0.04, 0.0), (0.28, SHELF)])),
    ])}
    sunk = clamp(mul(shelf, 1.0 / SHELF), 0.0, 1.0)
    chasma_mask = mul(sunk, clamp(mul(4.0, add(-0.4, chasma)), 0.0, 1.0))
    terrain = total(
        base, rolling, hummocks, shelf,
        mul(add(1.0, mul(-1.0, sunk)), mul(0.32, mul(fields, ridge))),  # the plains' occasional drifts
        mul(sunk, add(mul(0.7, ridge), mul(0.25, noise("dreadpatch:dreadlands_rolling")))),  # sastrugi: tall dunes
        mul(CHASMA_FLOOR, chasma_mask),
    )
    # Vanilla's noodle caves carry their own interpolation, so they go outside ours, as in vanilla's own overworld
    # (nested inside, they left the first six columns of every chunk air, bedrock and all).
    caves = minimum(
        add("minecraft:overworld/caves/spaghetti_2d", "minecraft:overworld/caves/spaghetti_roughness_function"),
        minimum("minecraft:overworld/caves/entrances",
                add(add(0.27, noise("minecraft:cave_cheese", 0.6666666666666666)), grad(16, 60, 0.0, 1.5))))
    cells = {"type": "dreadpatch:cell_edges", "noise": "dreadpatch:dreadlands_cells", "size": CELL, "jitter": 0.9}
    width = add(3.5, mul(5.0, noise("dreadpatch:dreadlands_bridges")))
    trench = {"type": "minecraft:cache_2d", "argument": mul(chasma_mask, clamp(add(width, mul(-1.0, cells)), 0.0, 1.0))}
    trench_carve = add(64.0, mul(-128.0, mul(trench, grad(TRENCH_FLOOR - 6, TRENCH_FLOOR, 0.0, 1.0))))
    fissure_cells = {"type": "dreadpatch:cell_edges", "noise": "dreadpatch:dreadlands_cells", "size": FISSURE_CELL, "jitter": 0.9}
    fissure = {"type": "minecraft:cache_2d", "argument": mul(chasma_mask, mul(
        clamp(add(1.0, mul(-1.0, fissure_cells)), 0.0, 1.0),
        clamp(mul(3.0, noise("dreadpatch:dreadlands_fissures")), 0.0, 1.0)))}
    fissure_carve = add(64.0, mul(-128.0, mul(fissure, mul(grad(-48, -40, 0.0, 1.0), grad(52, 60, 1.0, 0.0)))))
    final = minimum({"type": "minecraft:interpolated", "argument": minimum(terrain, caves)},
                    minimum("minecraft:overworld/caves/noodle", minimum(trench_carve, fissure_carve)))

    path = DATA / "worldgen/noise_settings/dreadlands.json"
    settings = json.loads(path.read_text())
    settings["sea_level"] = -63
    router = settings["noise_router"]
    router.update({"continents": basins, "erosion": shores, "ridges": chasma, "temperature": 0.0, "vegetation": 0.0, "depth": 0.0,
                   "initial_density_without_jaggedness": terrain, "final_density": final})
    path.write_text(json.dumps(settings, indent=2) + "\n")

    noises = {
        "dreadlands_basins": (-10, [1.0, 1.0, 0.5]),
        "dreadlands_shores": (-8, [1.0, 0.5]),
        "dreadlands_chasma": (-9, [1.0, 0.5]),
        "dreadlands_cells": (0, [1.0]),
        "dreadlands_bridges": (-6, [1.0, 0.5]),
        "dreadlands_fissures": (-5, [1.0]),
    }
    for name, (octave, amplitudes) in noises.items():
        (DATA / f"worldgen/noise/{name}.json").write_text(json.dumps({"firstOctave": octave, "amplitudes": amplitudes}, indent=2) + "\n")

    point = lambda c, w: {"temperature": 0.0, "humidity": 0.0, "continentalness": c, "erosion": [-1.0, 1.0], "weirdness": w, "depth": 0.0, "offset": 0.0}
    dimension = DATA / "dimension/dreadlands.json"
    dim = json.loads(dimension.read_text())
    dim["generator"]["biome_source"] = {"type": "minecraft:multi_noise", "biomes": [
        {"biome": "dreadpatch:dreadlands", "parameters": point([-1.0, 0.14], [-1.0, 1.0])},
        {"biome": "dreadpatch:sastrugi_wastes", "parameters": point([0.14, 1.0], [-1.0, 0.45])},
        {"biome": "dreadpatch:rime_chasma", "parameters": point([0.2, 1.0], [0.45, 1.0])},
    ]}
    dimension.write_text(json.dumps(dim, indent=2) + "\n")

    # Underground: blobs of dreadstone and packed ice in the stone, and now and then a crusted lava pocket.
    for name, block, size, count in (("dreadstone_blobs", "iceandfire:dread_stone", 33, 8), ("packed_ice_blobs", "minecraft:packed_ice", 33, 6)):
        (DATA / f"worldgen/configured_feature/{name}.json").parent.mkdir(parents=True, exist_ok=True)
        (DATA / f"worldgen/configured_feature/{name}.json").write_text(json.dumps({"type": "minecraft:ore", "config": {
            "size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
                {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:stone"}, "state": {"Name": block}}]}}, indent=2) + "\n")
        (DATA / f"worldgen/placed_feature/{name}.json").parent.mkdir(parents=True, exist_ok=True)
        (DATA / f"worldgen/placed_feature/{name}.json").write_text(json.dumps({"feature": f"dreadpatch:{name}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"above_bottom": 0}, "max_inclusive": {"absolute": 64}}},
            {"type": "minecraft:biome"}]}, indent=2) + "\n")
    (DATA / "worldgen/configured_feature/crusted_lava_pocket.json").write_text(json.dumps({"type": "dreadpatch:crusted_lava_pocket", "config": {}}, indent=2) + "\n")
    (DATA / "worldgen/placed_feature/crusted_lava_pocket.json").write_text(json.dumps({"feature": "dreadpatch:crusted_lava_pocket", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 24}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -50}, "max_inclusive": {"absolute": 20}}},
        {"type": "minecraft:biome"}]}, indent=2) + "\n")

    plains = json.loads((DATA / "worldgen/biome/dreadlands.json").read_text())
    underground = ["dreadpatch:dreadstone_blobs", "dreadpatch:packed_ice_blobs", "dreadpatch:crusted_lava_pocket"]
    for name in ("dreadlands", "sastrugi_wastes", "rime_chasma"):
        biome = json.loads(json.dumps(plains))
        steps = biome.get("features", [])
        while len(steps) < 7:
            steps.append([])
        # Step 6: underground ores.
        steps[6] = [f for f in steps[6] if f not in underground] + underground
        biome["features"] = steps
        (DATA / f"worldgen/biome/{name}.json").write_text(json.dumps(biome, indent=2) + "\n")


if __name__ == "__main__":
    main()
