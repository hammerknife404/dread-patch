"""Writes the Dreadlands' terrain (0.66.0): its noise settings' router, its noises, its three biomes' climate
points, and its underground features. Python 3, stdlib.

    python3 tools/datagen/make_dreadlands_terrain.py

Density is in vanilla's units: the base gradient falls 1 for every 24 blocks up, so adding 1 raises the ground 24
blocks. Deterministic; tune the numbers here and re-run rather than editing the JSON.

- Plains (dreadpatch:dreadlands): the ground at about 72, rolling, faint hummocks, now and then a field of drifts.
- Sastrugi Wastes: where the basins noise (continents) passes 0.12, the ground falls about 22 blocks to a sunken
  seabed of tall smooth dunes. How fast it falls is the shores noise (erosion): a gradual shelf, or a sharp bluff.
- Rime Chasma (0.68.0, the owner's two pictures; "No canyon walls. The floor is flat as shown with the hexegonal
  ridges and shattered deep cracks and ravines"): in the basins where the chasma noise (ridges) passes 0.35, the
  ground eases down gentle slopes to a flat floor at FLOOR_Y, an ice sheet (packed ice and blue ice in bands, its top
  clear blue ice or snow plates). Hexagonal plates (dreadpatch:cell_edges, hex, since 0.69.0) with ridges a block or two high along their
  edges; where the fissures noise says so an edge is a crack instead, and on wider cells trenches (pinching shut into
  snow bridges). Cracks reach CRACK_MIN below the floor at least ("almost always deep enough to cause fall damage"),
  mostly 10-30, and now and then to bedrock ("Rarely").
- Caves everywhere to bedrock: vanilla's spaghetti, noodle and entrance caves and its cheese caverns (closed near the
  surface). Sea level at the bottom, so nothing floods: the basins stay dry.
"""
import json
import pathlib

REPO = pathlib.Path(__file__).resolve().parents[2]
DATA = REPO / "src/main/resources/data/dreadpatch"

SURFACE_Y = 80       # the plains' ground (72 before 0.77.0)
SHELF = -1.5         # the basins' fall: 36 blocks (22 before 0.77.0)
# 0.77.0, the owner: "expand the three dreadlands biomes in size and verticality"; asked, "About 2x", "2.3 x+ is ok", and
# all four kinds of height. The large-scale noises (the biomes', the shores', the dune fields') are sampled this much
# slower, so each biome is about 2.3 times as wide; the chasma's plates and cracks keep their size.
SPREAD = 1.0 / 2.3
HILLS = 2.2          # the plains' hills and ridgelines at their most: about 50 blocks over the plains (to about y 120-130)
DUNES = 1.3          # the wastes' dune ridges (0.7 before 0.77.0): about ±17 blocks
CRAG = 1.4           # a crag's spike at its most: about 34 blocks
FLOOR_Y = 31          # the chasma's floor: its top block at 30 (the owner: "Mostly #1", about y 30)
ICE_SHEET = 16        # how deep its ice goes
CELL = 80.0           # a trench cell's size (blocks)
FISSURE_CELL = 18.0   # a plate's size
RIDGE_WIDTH = 1.2     # a ridge is the blocks this near a plate's edge
HEX_JITTER = 0.3      # how far a plate's point strays from its hexagonal lattice (of a plate); 0.9 before 0.69.0
CRACK_MIN = 6         # every crack at least this deep
BEDROCK_TOP = -59     # a crack to bedrock stops here (bedrock fades in from -64 to -60)


def noise(name, y_scale=0.0, xz_scale=1.0):
    return {"type": "minecraft:noise", "noise": name, "xz_scale": xz_scale, "y_scale": y_scale}


def wide(name):
    """A large-scale noise, spread out (0.77.0)."""
    return noise(name, 0.0, SPREAD)


def absolute(x):
    return {"type": "minecraft:abs", "argument": x}


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


def block(name):
    return {"type": "minecraft:block", "result_state": {"Name": name}}


def when(condition, then):
    return {"type": "minecraft:condition", "if_true": condition, "then_run": then}


def y_above(at):
    return {"type": "minecraft:y_above", "anchor": {"absolute": at}, "surface_depth_multiplier": 0, "add_stone_depth": False}


def depth(offset):
    return {"type": "minecraft:stone_depth", "offset": offset, "surface_type": "floor", "add_surface_depth": False, "secondary_depth_range": 0}


def chasma_surface():
    """The Rime Chasma's ice sheet: its ridges snow over packed ice; its floor's top clear blue ice (the owner's
    first picture) or snow plates (the second), "Mix of both"; under it to ICE_SHEET bands of packed ice and blue
    ice, which the cracks' walls show. A crack's bottom falls through to the plains' rules."""
    top = FLOOR_Y - 1
    bands = [when(y_above(top - 3), block("minecraft:packed_ice")), when(y_above(top - 5), block("minecraft:blue_ice")),
             when(y_above(top - 9), block("minecraft:packed_ice")), when(y_above(top - 10), block("minecraft:blue_ice")),
             block("minecraft:packed_ice")]
    return when({"type": "minecraft:biome", "biome_is": ["dreadpatch:rime_chasma"]}, {"type": "minecraft:sequence", "sequence": [
        when(y_above(FLOOR_Y), when(depth(0), block("minecraft:snow_block"))),
        when(y_above(FLOOR_Y), when(depth(ICE_SHEET), block("minecraft:packed_ice"))),
        when(y_above(top - 3), when(depth(0), {"type": "minecraft:sequence", "sequence": [
            when({"type": "minecraft:noise_threshold", "noise": "dreadpatch:dreadlands_clear_ice", "min_threshold": 0.0, "max_threshold": 10.0},
                 block("minecraft:blue_ice")),
            when({"type": "minecraft:noise_threshold", "noise": "minecraft:packed_ice", "min_threshold": -0.08, "max_threshold": 0.08},
                 block("minecraft:packed_ice")),
            block("minecraft:snow_block")]})),
        when(y_above(top - ICE_SHEET), when(depth(ICE_SHEET), {"type": "minecraft:sequence", "sequence": bands})),
    ]})


def main():
    # Scaled so the basins (past 0.14) cover about a third of the land, the chasma a smaller share within them.
    basins = mul(1.6, wide("dreadpatch:dreadlands_basins"))
    shores = wide("dreadpatch:dreadlands_shores")
    chasma = mul(1.5, wide("dreadpatch:dreadlands_chasma"))  # widened (0.68.0), so a chasma is mostly its floor
    # Ground at SURFACE_Y: vanilla's gradient over the world's height, shifted.
    base = grad(-64, 320, 5.666666666666667 + (SURFACE_Y - 72) / 24.0, -10.333333333333334 + (SURFACE_Y - 72) / 24.0)
    rolling = mul(0.42, noise("dreadpatch:dreadlands_rolling"))
    hummocks = mul(0.06, noise("dreadpatch:dreadlands_hummocks"))
    ridge = add(0.55, mul(-1.1, {"type": "minecraft:abs", "argument": noise("dreadpatch:dreadlands_drifts")}))
    fields = clamp(mul(4.0, add(-0.15, wide("dreadpatch:dreadlands_dune_fields"))), 0.0, 1.0)
    # The shore: a sharp bluff where the shores noise is low, a long shelf where it's high.
    shelf = {"type": "minecraft:spline", "spline": spline(shores, [
        (-1.0, spline(basins, [(0.10, 0.0), (0.14, SHELF)])),
        (0.0, spline(basins, [(0.08, 0.0), (0.20, SHELF)])),
        (1.0, spline(basins, [(0.04, 0.0), (0.28, SHELF)])),
    ])}
    sunk = clamp(mul(shelf, 1.0 / SHELF), 0.0, 1.0)
    # The chasma: 0 outside, 1 on its flat floor; the slope between is the gentle way down.
    # Saturating soon, so most of a chasma is its level floor: by the chasma noise and by the basins (not the shelf,
    # which on a long shore is still part-way where the chasma starts).
    chasma_mask = {"type": "minecraft:cache_2d", "argument": mul(clamp(mul(10.0, add(-0.12, basins)), 0.0, 1.0),
                                                                 clamp(mul(6.0, add(-0.38, chasma)), 0.0, 1.0))}
    plains = add(1.0, mul(-1.0, sunk))
    # The plains' hills (0.77.0): broad swells and frozen ridgelines (sharp crests where the ridgelines noise crosses
    # 0), their height by the highlands noise, so some stretches are hill country and some stay low and open.
    highland = clamp(add(0.5, mul(1.5, wide("dreadpatch:dreadlands_highlands"))), 0.2, 1.0)
    swells = clamp(noise("dreadpatch:dreadlands_swells"), 0.0, 1.0)
    crests = clamp(add(1.0, mul(-2.5, absolute(noise("dreadpatch:dreadlands_ridgelines")))), 0.0, 1.0)
    hills = mul(HILLS, mul(highland, add(mul(0.55, swells), mul(0.6, crests))))
    # Crags (0.77.0): rare outcrops of jagged spikes, in any land but the chasma (masked off it below with the rest).
    crag_mask = clamp(mul(5.0, add(-0.3, noise("dreadpatch:dreadlands_crags"))), 0.0, 1.0)
    spikes = clamp(add(0.1, noise("dreadpatch:dreadlands_spikes")), 0.0, 1.0)
    crags = mul(crag_mask, mul(CRAG, spikes))
    land = total(
        base, rolling, hummocks, shelf, crags,
        mul(plains, add(hills, mul(0.32, mul(fields, ridge)))),  # the plains' hills, and their occasional drifts
        mul(sunk, add(mul(DUNES, ridge), mul(0.35, noise("dreadpatch:dreadlands_rolling")))),  # sastrugi: tall dunes
    )
    # Eased toward a level floor: on it, exactly FLOOR_Y (a straight gradient interpolates exactly). Twice the land's
    # steepness, so the way down stays gentle but the floor holds its height.
    # Zero half a block under FLOOR_Y: at FLOOR_Y itself, rounding made it solid.
    level_floor = grad(-64, 320, (FLOOR_Y - 0.5 + 64) / 12.0, (FLOOR_Y - 0.5 - 320) / 12.0)
    terrain = add(land, mul(chasma_mask, add(level_floor, mul(-1.0, land))))
    # The crags' faces broken up in three dimensions: ledges and overhangs, not smooth cones.
    terrain = add(terrain, mul(mul(crag_mask, add(1.0, mul(-1.0, chasma_mask))), mul(0.45, noise("dreadpatch:dreadlands_crag_faces", 1.0))))
    # Vanilla's noodle caves carry their own interpolation, so they go outside ours, as in vanilla's own overworld
    # (nested inside, they left the first six columns of every chunk air, bedrock and all).
    caves = minimum(
        add("minecraft:overworld/caves/spaghetti_2d", "minecraft:overworld/caves/spaghetti_roughness_function"),
        minimum("minecraft:overworld/caves/entrances",
                add(add(0.27, noise("minecraft:cave_cheese", 0.6666666666666666)), grad(16, 60, 0.0, 1.5))))
    # Kept out of the chasma's ice sheet (as vanilla keeps its caves off its surface): inside the interpolation, their
    # small values at the cell corner under the floor pulled the floor down 2-4 blocks. Its cracks still reach them.
    # The noodle caves (outside the interpolation) too.
    sheet = mul(chasma_mask, grad(FLOOR_Y - ICE_SHEET - 6, FLOOR_Y - ICE_SHEET + 2, 0.0, 10.0))
    caves = add(caves, sheet)
    noodle = add("minecraft:overworld/caves/noodle", sheet)  # likewise: they holed its floor 1-6 deep
    y = grad(-64, 320, -64.0, 320.0)  # the block's height itself
    on_floor = {"type": "minecraft:cache_2d", "argument": clamp(mul(25.0, add(-0.95, chasma_mask)), 0.0, 1.0)}
    # How deep the cracks go, by place: their bottom from FLOOR_Y - CRACK_MIN - 4 down to 10 under the floor's sheet;
    # where the depths noise is highest (now and then), to bedrock.
    depths = noise("dreadpatch:dreadlands_crack_depths")
    shallow_bottom = FLOOR_Y - CRACK_MIN - 4
    bottom = {"type": "minecraft:cache_2d", "argument": {"type": "minecraft:spline", "spline": spline(depths, [
        (-0.7, shallow_bottom), (-0.3, FLOOR_Y - 14), (0.0, FLOOR_Y - 24), (0.3, FLOOR_Y - 40), (0.45, FLOOR_Y - 52), (0.52, BEDROCK_TOP)])}}
    fissure_bottom = {"type": "minecraft:cache_2d", "argument": {"type": "minecraft:max", "argument1": bottom,
                                                                 "argument2": float(FLOOR_Y - CRACK_MIN - 16)}}

    def carve(columns, floor):
        """Air where the 2D mask passes a half, from just over the floor's height down to `floor` (a 2D function)."""
        band = mul(clamp(mul(0.25, add(y, mul(-1.0, floor))), 0.0, 1.0), clamp(mul(0.25, add(float(FLOOR_Y + 8), mul(-1.0, y))), 0.0, 1.0))
        return add(64.0, mul(-128.0, mul(columns, band)))

    cells = {"type": "dreadpatch:cell_edges", "noise": "dreadpatch:dreadlands_cells", "size": CELL, "jitter": 0.9}
    width = add(1.5, mul(2.5, noise("dreadpatch:dreadlands_bridges")))  # half-width: up to about 4
    trench = {"type": "minecraft:cache_2d", "argument": mul(on_floor, clamp(add(width, mul(-1.0, cells)), 0.0, 1.0))}
    trench_carve = carve(trench, bottom)
    # Hexagons (0.69.0, the owner: "not nearly as sharply defined as a hexagon"): a hexagonal lattice, jittered a little.
    plates = {"type": "dreadpatch:cell_edges", "noise": "dreadpatch:dreadlands_cells", "size": FISSURE_CELL, "jitter": HEX_JITTER, "hex": True}
    cracked = clamp(mul(3.0, add(-0.1, noise("dreadpatch:dreadlands_fissures"))), 0.0, 1.0)  # an edge is a crack here (about a third)
    crack_width = add(0.7, mul(0.6, clamp(noise("dreadpatch:dreadlands_bridges"), 0.0, 1.0)))  # 1-2 blocks across
    fissure = {"type": "minecraft:cache_2d", "argument": mul(on_floor, mul(clamp(add(crack_width, mul(-1.0, plates)), 0.0, 1.0), cracked))}
    fissure_carve = carve(fissure, fissure_bottom)
    # The plates' ridges: along every edge that isn't a crack, a block or two over the floor (two where the
    # ridges noise is up), only on the floor itself. Positive from FLOOR_Y to the ridge's top, else nothing.
    ridge_cols = {"type": "minecraft:cache_2d", "argument": mul(on_floor, mul(
        clamp(mul(4.0, add(RIDGE_WIDTH, mul(-1.0, plates))), 0.0, 1.0), add(1.0, mul(-1.0, cracked))))}
    ridge_top = add(float(FLOOR_Y) + 0.5, {"type": "minecraft:cache_2d", "argument": clamp(mul(4.0, noise("dreadpatch:dreadlands_ridges")), 0.0, 1.0)})
    ridges = mul(ridge_cols, minimum(add(ridge_top, mul(-1.0, y)), add(y, -(FLOOR_Y - 0.5))))
    ground = {"type": "minecraft:max", "argument1": minimum({"type": "minecraft:interpolated", "argument": minimum(terrain, caves)},
                                                             noodle), "argument2": ridges}
    final = minimum(ground, minimum(trench_carve, fissure_carve))
    # The world's floor solid whatever's carved (0.69.0, the owner: "Make sure Bedrock is at least 1 layer thick
    # throughout the entire dimension"): -64 and -63 never air, so the surface rule's bedrock is always there at -64.
    final = {"type": "minecraft:max", "argument1": final, "argument2": grad(-64, -61, 3.0, -3.0)}

    path = DATA / "worldgen/noise_settings/dreadlands.json"
    settings = json.loads(path.read_text())
    settings["sea_level"] = -63
    # The chasma's ice (0.68.0): its own rules, before the rest (which still makes its cracks' bottoms and the stone).
    rules = [r for r in settings["surface_rule"]["sequence"] if r.get("if_true", {}).get("type") != "minecraft:biome"]
    rules.insert(1, chasma_surface())
    # Steep ground (0.77.0: the crags, the bluffs, the hills' steeper sides) bare dreadstone and packed ice, not snow.
    rules = [r for r in rules if r.get("if_true", {}).get("type") != "minecraft:steep"]
    rules.insert(2, when({"type": "minecraft:steep"}, when(depth(0), {"type": "minecraft:sequence", "sequence": [
        when({"type": "minecraft:noise_threshold", "noise": "minecraft:packed_ice", "min_threshold": 0.0, "max_threshold": 10.0},
             block("minecraft:packed_ice")),
        block("iceandfire:dread_stone")]})))
    settings["surface_rule"]["sequence"] = rules
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
        "dreadlands_crack_depths": (-6, [1.0, 0.5]),
        "dreadlands_ridges": (-4, [1.0]),
        "dreadlands_clear_ice": (-5, [1.0, 0.5]),
        # 0.77.0: the plains' hills, and the crags.
        "dreadlands_highlands": (-9, [1.0, 0.5]),
        "dreadlands_swells": (-8, [1.0, 0.5, 0.25]),
        "dreadlands_ridgelines": (-8, [1.0, 0.35]),
        "dreadlands_crags": (-7, [1.0]),
        "dreadlands_spikes": (-4, [1.0, 0.6, 0.3]),
        "dreadlands_crag_faces": (-4, [1.0, 0.5]),
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
    for name, ore, size, count in (("dreadstone_blobs", "iceandfire:dread_stone", 33, 8), ("packed_ice_blobs", "minecraft:packed_ice", 33, 6)):
        (DATA / f"worldgen/configured_feature/{name}.json").parent.mkdir(parents=True, exist_ok=True)
        (DATA / f"worldgen/configured_feature/{name}.json").write_text(json.dumps({"type": "minecraft:ore", "config": {
            "size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
                {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:stone"}, "state": {"Name": ore}}]}}, indent=2) + "\n")
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

    # The chasma's water pockets and heaved slabs (0.68.0).
    for name, count in (("chasma_pocket", 5), ("heaved_slabs", 3)):
        (DATA / f"worldgen/configured_feature/{name}.json").write_text(json.dumps({"type": f"dreadpatch:{name}", "config": {}}, indent=2) + "\n")
        (DATA / f"worldgen/placed_feature/{name}.json").write_text(json.dumps({"feature": f"dreadpatch:{name}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]}, indent=2) + "\n")

    # The deeps flooded (0.77.0): once a chunk, the feature covers the chunk itself.
    (DATA / "worldgen/configured_feature/deep_water.json").write_text(json.dumps({"type": "dreadpatch:deep_water", "config": {}}, indent=2) + "\n")
    (DATA / "worldgen/placed_feature/deep_water.json").write_text(json.dumps({"feature": "dreadpatch:deep_water", "placement": []}, indent=2) + "\n")
    # The plains' grave patches (0.67.0): graveyard soil, wither roses, now and then a dead dreadwood tree.
    (DATA / "worldgen/configured_feature/grave_patch.json").write_text(json.dumps({"type": "dreadpatch:grave_patch", "config": {}}, indent=2) + "\n")
    (DATA / "worldgen/placed_feature/grave_patch.json").write_text(json.dumps({"feature": "dreadpatch:grave_patch", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 6}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]}, indent=2) + "\n")
    plains = json.loads((DATA / "worldgen/biome/dreadlands.json").read_text())
    underground = ["dreadpatch:dreadstone_blobs", "dreadpatch:packed_ice_blobs", "dreadpatch:crusted_lava_pocket"]
    for name in ("dreadlands", "sastrugi_wastes", "rime_chasma"):
        biome = json.loads(json.dumps(plains))
        steps = biome.get("features", [])
        while len(steps) < 7:
            steps.append([])
        # Step 6: underground ores.
        steps[6] = [f for f in steps[6] if f not in underground] + underground
        while len(steps) < 10:
            steps.append([])
        # Step 1, lakes: the deeps flooded (0.77.0), every biome.
        steps[1] = [f for f in steps[1] if f != "dreadpatch:deep_water"] + ["dreadpatch:deep_water"]
        # Step 4, surface structures: an ice dragon's bones now and then (0.76.0), every biome.
        steps[4] = [f for f in steps[4] if f != "dreadpatch:dragon_skeleton"] + ["dreadpatch:dragon_skeleton"]
        # Step 9: vegetal decoration; the plains only.
        steps[9] = [f for f in steps[9] if f != "dreadpatch:grave_patch"] + (["dreadpatch:grave_patch"] if name == "dreadlands" else [])
        if name == "rime_chasma":
            # Step 2, local modifications: water sealed in its ice; step 9: slabs heaved up at its cracks' lips. No
            # snow laid at step 10, which would bury its clear ice (blue ice takes snow; packed ice doesn't).
            steps[2] = steps[2] + ["dreadpatch:chasma_pocket"]
            steps[9] = steps[9] + ["dreadpatch:heaved_slabs"]
            steps[10] = [f for f in steps[10] if f != "minecraft:freeze_top_layer"]
        biome["features"] = steps
        (DATA / f"worldgen/biome/{name}.json").write_text(json.dumps(biome, indent=2) + "\n")


if __name__ == "__main__":
    main()
