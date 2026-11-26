"""Her regalia redrawn after the owner's three pictures (0.80.0): her sword and staff as voxel item models, her armour,
and the winged bodice's ice-bone wings; and with --preview DIR, renders of each to set against the pictures without a
client. Python 3 with Pillow. Deterministic: re-running gives identical files. Edit this, not the outputs.

The pictures: dark steel with a pale frosted edge and cyan gems; a sword with a broad blade, a diamond tip, an ornate
spiked guard and a gemmed pommel; a dark twisted staff, ice along it, crowned with ice claws round a glowing cyan
crystal; the armour (see ARMOUR below) and bone-white dragon wings with tattered, icicled membranes.

Weapons are voxels: a grid of materials, merged into boxes, each box's faces one colour from a small palette texture,
glowing ones lit full (NeoForge's per-element block and sky light). One unit is a model pixel.
"""
import json
import math
import pathlib
import sys

from PIL import Image, ImageDraw

import regalia_armour as armour

REPO = pathlib.Path(__file__).resolve().parents[2]
ASSETS = REPO / "src/main/resources/assets/dreadpatch"

# The palette: name -> (colour, glows). The pictures' dark steel is a cold blue-black.
PALETTE = {
    "steel_dark": ((0x16, 0x1C, 0x26), False),
    "steel": ((0x24, 0x2D, 0x3B), False),
    "steel_light": ((0x3A, 0x47, 0x5A), False),
    "edge": ((0x9C, 0xAE, 0xC2), False),
    "edge_light": ((0xC6, 0xD4, 0xE2), False),
    "frost": ((0xB8, 0xDC, 0xEC), False),
    "ice": ((0x8C, 0xCC, 0xE4), False),
    "ice_deep": ((0x5A, 0x9E, 0xC4), False),
    "gem": ((0x2C, 0xD8, 0xFF), True),
    "gem_core": ((0xC8, 0xF8, 0xFF), True),
    "crystal": ((0x7A, 0xF0, 0xF4), True),
    "crystal_light": ((0xD6, 0xFE, 0xFF), True),
    "wrap": ((0x1E, 0x24, 0x30), False),
    "wrap_band": ((0x4A, 0x5C, 0x74), False),
}
PALETTE_NAMES = list(PALETTE)


def palette_uv(name):
    """A material's pixel on the 16 x 16 palette, as an item model's UV (in sixteenths of the texture)."""
    i = PALETTE_NAMES.index(name)
    u, v = i % 16, i // 16
    return [u + 0.25, v + 0.25, u + 0.75, v + 0.75]


# --- The sword: pixel art, top (its tip) to bottom (its pommel), extruded by material. ---
SWORD_ART = """
.......K.......
......KEK......
.....KEFEK.....
....KEBFBEK....
...KEEBGBEEK...
...KEBBGBBEK...
....KEBFBEK....
....EKBFBKE....
....KEBFBEK....
....EKBFBEK....
....KEBFBEK....
....EKBFBEK....
....KEBFBEK....
....EKBFBEK....
....KEBFBEK....
....EKBFBEK....
....KEBFBEK....
....EKBFBEK....
....KEBFBEK....
....EKBFBEK....
....KEBFBEK....
....EKBFBEK....
...KEEBFBEEK...
...KEBBGBBEK...
....KEBGBEK....
.....DDGDD.....
.S..DDDGDDD..S.
SDDSDDGGGDDSDDS
.SDDDDDGDDDDDS.
..S.S.DDD.S.S..
......WLW......
......WWW......
......WLW......
......WWW......
......WLW......
.....DDDDD.....
....DDDGDDD....
.....DDDDD.....
"""
# Each material's half-thickness (its depth runs 8 - t .. 8 + t) and palette colour.
SWORD_MATERIALS = {
    "E": (0.6, "edge"), "K": (0.4, "edge_light"), "B": (1.0, "steel"), "F": (0.75, "steel_dark"),
    "G": (1.5, "gem"), "D": (1.5, "steel_light"), "S": (1.0, "steel"), "W": (1.0, "wrap"), "L": (1.25, "wrap_band"),
}


def sword_boxes():
    """The sword's boxes: each row of the art merged into runs of one material, standing on its pommel at y 0."""
    rows = [r for r in SWORD_ART.strip("\n").split("\n")]
    height = len(rows)
    boxes = []
    for j, row in enumerate(rows):
        y = height - 1 - j
        i = 0
        while i < len(row):
            c = row[i]
            if c == ".":
                i += 1
                continue
            k = i
            while k < len(row) and row[k] == c:
                k += 1
            t, mat = SWORD_MATERIALS[c]
            boxes.append(((0.5 + i, y, 8 - t), (0.5 + k, y + 1, 8 + t), mat))
            i = k
    # The gems' cores, a shade lighter, standing a little proud of them.
    for (fx, fy, fz), (tx, ty, tz), mat in list(boxes):
        if mat == "gem" and tx - fx == 1:
            boxes.append(((fx + 0.2, fy + 0.2, fz - 0.15), (tx - 0.2, ty - 0.2, tz + 0.15), "gem_core"))
    return boxes


def staff_boxes():
    """The staff: a dark shaft twisting as it rises, ice along it, claws of ice round a glowing crystal at its head,
    an ice tip at its foot. Built from 0 (its foot) up, about 46 tall."""
    boxes = []
    shaft_top = 32  # 34 put the staff past the item model's bounds (-16 to 32), and the game refused it (0.81.1)
    for y in range(0, shaft_top):
        # It twists: its two-wide core drifts a pixel either way, slowly.
        dx = round(0.8 * math.sin(y * 0.45))
        dz = round(0.8 * math.cos(y * 0.33))
        mat = "steel_dark" if (y // 4) % 2 == 0 else "steel"
        # Slender: a pixel and a half, a twist of a lighter strand round it.
        boxes.append(((7.25 + dx * 0.5, y, 7.25 + dz * 0.5), (8.75 + dx * 0.5, y + 1, 8.75 + dz * 0.5), mat))
        a = y * 0.7
        boxes.append(((7.75 + 0.9 * math.cos(a), y, 7.75 + 0.9 * math.sin(a)), (8.25 + 0.9 * math.cos(a), y + 1, 8.25 + 0.9 * math.sin(a)), "steel_light"))
        if y in (9, 10):  # the grip's band of frost
            boxes.append(((6.9 + dx * 0.5, y, 6.9 + dz * 0.5), (9.1 + dx * 0.5, y + 1, 9.1 + dz * 0.5), "frost"))
    # Shards of ice growing off it, angled up.
    for y, side in ((24, 1), (28, -1), (31, 1)):
        dx = round(0.8 * math.sin(y * 0.45)) * 0.5
        x0 = 8.75 + dx if side > 0 else 6.25 + dx
        boxes.append(((x0, y, 7.5), (x0 + 1, y + 2, 8.5), "ice"))
        boxes.append(((x0 + side, y + 1, 7.6), (x0 + side + 1, y + 3, 8.4), "frost"))
    # The head: a collar, four claws curling up, the crystal.
    boxes.append(((6, shaft_top, 6), (10, shaft_top + 2, 10), "steel_light"))
    boxes.append(((5.5, shaft_top + 2, 5.5), (10.5, shaft_top + 3, 10.5), "steel"))
    for cx, cz in ((5, 5), (10, 5), (5, 10), (10, 10)):
        ox = 1 if cx > 8 else -1
        oz = 1 if cz > 8 else -1
        boxes.append(((cx, shaft_top + 3, cz), (cx + 1, shaft_top + 6, cz + 1), "steel_dark"))
        boxes.append(((cx + 0.5 * ox, shaft_top + 6, cz + 0.5 * oz), (cx + 1 + 0.5 * ox, shaft_top + 9, cz + 1 + 0.5 * oz), "ice"))
        boxes.append(((cx + ox * 0.2, shaft_top + 9, cz + oz * 0.2), (cx + 1 + ox * 0.2, shaft_top + 11, cz + 1 + oz * 0.2), "frost"))
    boxes.append(((5.75, shaft_top + 3, 5.75), (10.25, shaft_top + 10, 10.25), "crystal"))
    boxes.append(((6.5, shaft_top + 10, 6.5), (9.5, shaft_top + 12, 9.5), "crystal"))
    boxes.append(((7.25, shaft_top + 12, 7.25), (8.75, shaft_top + 13, 8.75), "crystal_light"))
    boxes.append(((6.75, shaft_top + 5, 5.5), (9.25, shaft_top + 9, 5.75), "crystal_light"))
    # Its foot: an ice point.
    boxes.append(((7.25, -2, 7.25), (8.75, 0, 8.75), "ice_deep"))
    boxes.append(((7.6, -3, 7.6), (8.4, -2, 8.4), "ice"))
    return boxes


def shift(boxes, dy):
    return [((f[0], f[1] + dy, f[2]), (t[0], t[1] + dy, t[2]), m) for f, t, m in boxes]


def item_model(boxes, display):
    # Vanilla refuses a whole model with an element past -16..32 on any axis (it bit the staff in 0.81.0).
    for f, t, mat in boxes:
        for v in (*f, *t):
            if not -16 <= v <= 32:
                raise SystemExit(f"an element past the item model's bounds (-16 to 32): {f} to {t}")
    elements = []
    for f, t, mat in boxes:
        uv = palette_uv(mat)
        element = {"from": [round(v, 3) for v in f], "to": [round(v, 3) for v in t],
                   "faces": {side: {"uv": uv, "texture": "#palette"} for side in ("north", "east", "south", "west", "up", "down")}}
        if PALETTE[mat][1]:
            element["neoforge_data"] = {"block_light": 15, "sky_light": 15}
        elements.append(element)
    return {"textures": {"palette": "dreadpatch:item/regalia_palette", "particle": "dreadpatch:item/regalia_palette"},
            "gui_light": "front", "elements": elements, "display": display}


# Held: upright models, grip low. Vanilla's handheld turns a diagonal sprite 55 degrees; ours stand straight up, so
# 10 (55 less the sprite's 45). The numbers want a look in a client.
SWORD_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 10], "translation": [0, 3.5, 0.5], "scale": [0.65, 0.65, 0.65]},
    "thirdperson_lefthand": {"rotation": [0, 90, -10], "translation": [0, 3.5, 0.5], "scale": [0.65, 0.65, 0.65]},
    "firstperson_righthand": {"rotation": [0, -90, 20], "translation": [1.13, 2.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -20], "translation": [1.13, 2.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "gui": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.46, 0.46, 0.46]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
    "fixed": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
}
STAFF_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 0], "translation": [0, 2.5, 0.5], "scale": [0.65, 0.65, 0.65]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 2.5, 0.5], "scale": [0.65, 0.65, 0.65]},
    "firstperson_righthand": {"rotation": [0, -90, 15], "translation": [1.13, 1.0, 1.13], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 90, -15], "translation": [1.13, 1.0, 1.13], "scale": [0.6, 0.6, 0.6]},
    "gui": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.36, 0.36, 0.36]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.35, 0.35, 0.35]},
    "fixed": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.42, 0.42, 0.42]},
}


def write_palette():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, name in enumerate(PALETTE_NAMES):
        image.putpixel((i % 16, i // 16), (*PALETTE[name][0], 255))
    path = ASSETS / "textures/item/regalia_palette.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


# --- The preview: boxes drawn face by face, flat-shaded, painter-sorted. ---

def rot_y(p, a):
    c, s = math.cos(a), math.sin(a)
    return (p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c)


def rot_x(p, a):
    c, s = math.cos(a), math.sin(a)
    return (p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c)


def box_faces(f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    v = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    return [[v[i] for i in q] for q in ((0, 1, 2, 3), (5, 4, 7, 6), (4, 0, 3, 7), (1, 5, 6, 2), (3, 2, 6, 7), (4, 5, 1, 0))]


def render_boxes(path, boxes, yaw, pitch, scale, size=(560, 720), centre=(8, 20, 8), background=(0x12, 0x16, 0x1E)):
    """`boxes`: (from, to, colour, glows). Up is up."""
    im = Image.new("RGB", size, background)
    draw = ImageDraw.Draw(im)
    light = (0.35, 0.8, -0.5)
    polys = []
    for f, t, colour, glow in boxes:
        for face in box_faces(f, t):
            pts = [rot_x(rot_y((p[0] - centre[0], p[1] - centre[1], p[2] - centre[2]), yaw), pitch) for p in face]
            a, b, c = pts[0], pts[1], pts[3]
            n = ((b[1] - a[1]) * (c[2] - a[2]) - (b[2] - a[2]) * (c[1] - a[1]),
                 (b[2] - a[2]) * (c[0] - a[0]) - (b[0] - a[0]) * (c[2] - a[2]),
                 (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]))
            ln = math.sqrt(sum(k * k for k in n)) or 1.0
            shade = 0.55 + 0.45 * abs(sum(n[k] / ln * light[k] for k in range(3)))
            rgb = colour if glow else tuple(int(k * shade) for k in colour)
            depth = sum(p[2] for p in pts) / 4
            polys.append((depth, [(size[0] / 2 + p[0] * scale, size[1] / 2 - p[1] * scale) for p in pts], rgb, glow))
    for depth, pts, rgb, glow in sorted(polys, key=lambda q: -q[0]):
        draw.polygon(pts, fill=rgb)
    im.save(path)


# --- The figure: a body wearing it all, the weapons in hand, posed as the pictures pose it. ---

def matmul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translate(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def rot_zyx(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = [[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]]
    Ry = [[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]]
    Rz = [[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
    return matmul(Rz, matmul(Ry, Rx))


def apply(m, p):
    return tuple(m[i][0] * p[0] + m[i][1] * p[1] + m[i][2] * p[2] + m[i][3] for i in range(3))


IDENTITY = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


def part_matrices(parts, pose):
    by = {p.name: p for p in parts}
    mats = {}

    def mat(p):
        if p.name not in mats:
            base = mat(by[p.parent]) if p.parent else IDENTITY
            r = pose.get(p.name.replace("_legs", "").replace("_feet", ""), p.rot)
            mats[p.name] = matmul(base, matmul(translate(*p.pivot), rot_zyx(*r)))
        return mats[p.name]
    for p in parts:
        mat(p)
    return mats


def textured_quads(parts, mats, textures):
    """Every texel of every face, as vanilla's ModelPart.Cube lays its UVs, grown by each box's grow."""
    quads = []
    for p in parts:
        m = mats[p.name]
        for b in p.boxes:
            g = b.grow
            x0, y0, z0, x1, y1, z1 = b.x - g, b.y - g, b.z - g, b.x + b.w + g, b.y + b.h + g, b.z + b.d + g
            V = {"7": (x0, y0, z0), "0": (x1, y0, z0), "1": (x1, y1, z0), "2": (x0, y1, z0),
                 "3": (x0, y0, z1), "4": (x1, y0, z1), "5": (x1, y1, z1), "6": (x0, y1, z1)}
            u, v = b.u, b.v
            w, h, d = b.dims()
            f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + 2 * w, u + 2 * d + w, u + 2 * d + 2 * w
            f10, f11, f12 = v, v + d, v + d + h
            tex = textures[p.slot]
            for verts, u1, v1, u2, v2 in (("4370", f5, f10, f6, f11), ("1265", f6, f11, f7, f10), ("7362", f4, f11, f5, f12),
                                          ("0721", f5, f11, f6, f12), ("4015", f6, f11, f8, f12), ("3456", f8, f11, f9, f12)):
                c = [apply(m, V[k]) for k in verts]
                cols, rows = abs(u2 - u1), abs(v2 - v1)
                for i in range(cols):
                    for j in range(rows):
                        tu = (u1 + i) if u2 > u1 else (u1 - 1 - i)
                        tv = (v1 + j) if v2 > v1 else (v1 - 1 - j)
                        colour = tex.getpixel((tu, tv))
                        if colour[3] == 0:
                            continue

                        def at(s, t):
                            top = [c[1][k] + (c[0][k] - c[1][k]) * s for k in range(3)]
                            bot = [c[2][k] + (c[3][k] - c[2][k]) * s for k in range(3)]
                            return [top[k] + (bot[k] - top[k]) * t for k in range(3)]
                        s0, s1, t0, t1 = i / cols, (i + 1) / cols, j / rows, (j + 1) / rows
                        glow = colour[:3] in (armour.CYAN, armour.CYAN_LIGHT)
                        quads.append(([at(s0, t0), at(s1, t0), at(s1, t1), at(s0, t1)], colour[:3], glow))
    return quads


def body_quads(mats):
    """A plain figure under it all (vanilla's sizes): a face with eyes, a cyan shirt, blue legs."""
    skin, shirt, pants, hair, eye = (0xB0, 0x7C, 0x5C), (0x2A, 0x9C, 0xA8), (0x3A, 0x34, 0x8C), (0x3A, 0x28, 0x1C), (0x50, 0x48, 0xA8)
    parts = {"head": ((-4, -8, -4, 8, 8, 8), skin), "body": ((-4, 0, -2, 8, 12, 4), shirt), "right_arm": ((-3, -2, -2, 4, 12, 4), skin),
             "left_arm": ((-1, -2, -2, 4, 12, 4), skin), "right_leg_legs": ((-2, 0, -2, 4, 12, 4), pants), "left_leg_legs": ((-2, 0, -2, 4, 12, 4), pants)}
    quads = []
    for name, ((x, y, z, w, h, d), colour) in parts.items():
        for face in box_faces((x, y, z), (x + w, y + h, z + d)):
            quads.append(([apply(mats[name], q) for q in face], colour, False))
    # The face: eyes and a beard, on the head's front (z -4), in front of the skin.
    m = mats["head"]
    for (x, y, w, h, c) in ((-3, -4, 2, 1, (0xF0, 0xF0, 0xF0)), (1, -4, 2, 1, (0xF0, 0xF0, 0xF0)), (-2, -4, 1, 1, eye), (2, -4, 1, 1, eye),
                            (-3, -2, 6, 2, hair), (-1, -2, 2, 1, (0x6A, 0x40, 0x34))):
        quads.append(([apply(m, q) for q in ((x, y, -4.01), (x + w, y, -4.01), (x + w, y + h, -4.01), (x, y + h, -4.01))], c, False))
    return quads


# How big the weapons are held: about the wearer's height for the staff, a little less for the sword (the pictures').
HELD_SCALE = 0.65


def weapon_quads(boxes, grip, at, tilt):
    """A weapon's boxes (item space: y up, 16 a block), its grip put at `at` (model space), standing up, leaning by `tilt`."""
    quads = []
    k = HELD_SCALE
    m = matmul(translate(*at), matmul(rot_zyx(0, 0, tilt), [[k, 0, 0, 0], [0, k, 0, 0], [0, 0, k, 0], [0, 0, 0, 1]]))
    for f, t, mat in boxes:
        colour, glow = PALETTE[mat]
        for face in box_faces(f, t):
            # Item space to model space: y flipped (model y is down), centred on its grip.
            pts = [apply(m, (q[0] - grip[0], -(q[1] - grip[1]), q[2] - grip[2])) for q in face]
            quads.append((pts, colour, glow))
    return quads


def render_quads(path, quads, yaw, pitch, scale, size=(720, 720), centre=(0, 8, 0), background=(0x12, 0x16, 0x1E), label=None):
    """Model space (y down) seen from the front at yaw 0: the figure's right on the left."""
    im = Image.new("RGB", size, background)
    draw = ImageDraw.Draw(im)
    light = (0.35, 0.8, -0.5)
    polys = []
    for corners, colour, glow in quads:
        pts = [rot_x(rot_y((q[0] - centre[0], -(q[1] - centre[1]), q[2] - centre[2]), yaw), pitch) for q in corners]
        a, b, c = pts[0], pts[1], pts[3]
        n = ((b[1] - a[1]) * (c[2] - a[2]) - (b[2] - a[2]) * (c[1] - a[1]),
             (b[2] - a[2]) * (c[0] - a[0]) - (b[0] - a[0]) * (c[2] - a[2]),
             (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]))
        ln = math.sqrt(sum(k * k for k in n)) or 1.0
        shade = 0.55 + 0.45 * abs(sum(n[k] / ln * light[k] for k in range(3)))
        rgb = colour if glow else tuple(int(k * shade) for k in colour)
        polys.append((sum(p[2] for p in pts) / 4, [(size[0] / 2 + p[0] * scale, size[1] / 2 - p[1] * scale) for p in pts], rgb))
    for depth, pts, rgb in sorted(polys, key=lambda q: -q[0]):
        draw.polygon(pts, fill=rgb)
    if label:
        draw.text((12, 10), label, fill=(0xC8, 0xD8, 0xE0))
    im.save(path)


# Posed as the pictures: arms raised out holding the weapons, wings spread.
POSE = {"right_arm": (-0.35, 0, 0.55), "left_arm": (-0.35, 0, -0.55), "right_wing": (0, -0.35, 0.3), "left_wing": (0, 0.35, -0.3)}


def sheet():
    """Every part, packed onto one sheet and painted: (parts, image)."""
    parts = armour.parts(winged=True)
    boxes = [b for p in parts for b in p.boxes]
    armour.pack(boxes, armour.TEX_W, armour.TEX_H)
    image = Image.new("RGBA", (armour.TEX_W, armour.TEX_H), (0, 0, 0, 0))
    armour.paint(image, boxes)
    return parts, image


def figure(out, sword, staff, parts, image):
    textures = {"head": image, "chest": image, "legs": image, "feet": image, "wings": image}
    mats = part_matrices(parts, POSE)
    quads = textured_quads(parts, mats, textures) + body_quads(mats)
    # The hands: each arm's foot, a little in.
    right_hand = apply(mats["right_arm"], (-1, 10, -1))
    left_hand = apply(mats["left_arm"], (1, 10, -1))
    quads += weapon_quads(sword, (8, -1.5, 8), right_hand, -0.3)
    quads += weapon_quads(staff, (8, -5.5, 8), left_hand, 0.25)
    for name, yaw, pitch in (("front", 0.0, -0.08), ("back", math.pi, -0.08), ("left_side", -math.pi / 2, -0.05),
                             ("right_side", math.pi / 2, -0.05), ("top", 0.0, -1.25), ("under", 0.0, 0.9)):
        render_quads(out / f"figure_{name}.png", quads, yaw, pitch, 9.0, label=name.replace("_", " ").upper())


# The armour layer's base parts (vanilla's humanoid needs all of them), and which of ours hang on which.
BASE = (("head", "PartPose.ZERO"), ("hat", "PartPose.ZERO"), ("body", "PartPose.ZERO"),
        ("right_arm", "PartPose.offset(-5.0F, 2.0F, 0.0F)"), ("left_arm", "PartPose.offset(5.0F, 2.0F, 0.0F)"),
        ("right_leg", "PartPose.offset(-1.9F, 12.0F, 0.0F)"), ("left_leg", "PartPose.offset(1.9F, 12.0F, 0.0F)"))
LAYERS = {"head": ("head",), "chest": ("chest",), "chest_winged": ("chest", "wings"), "legs": ("legs",), "feet": ("feet",)}


def java(parts):
    def f(n):
        return f"{float(n)}F"

    def cubes(boxes):
        out = "CubeListBuilder.create()"
        for b in boxes:
            g = f", new CubeDeformation({f(b.grow)})" if b.grow else ""
            out += f".texOffs({b.u}, {b.v}).addBox({f(b.x)}, {f(b.y)}, {f(b.z)}, {f(b.w)}, {f(b.h)}, {f(b.d)}{g})"
        return out

    methods = []
    for layer, slots in LAYERS.items():
        lines = ["        MeshDefinition mesh = new MeshDefinition();", "        PartDefinition root = mesh.getRoot();"]
        for name, pose in BASE:
            lines.append(f"        PartDefinition {name.replace('_', '')} = root.addOrReplaceChild(\"{name}\", CubeListBuilder.create(), {pose});")
        for p in parts:
            if p.slot not in slots:
                continue
            if p.slot == "wings":
                lines.append(f"        body.addOrReplaceChild(\"{p.name}\", {cubes(p.boxes)},")
                lines.append(f"                PartPose.offsetAndRotation({', '.join(f(n) for n in p.pivot)}, {', '.join(f(n) for n in p.rot)}));")
                continue
            base = p.name.replace("_legs", "").replace("_feet", "")
            lines.append(f"        {base.replace('_', '')}.addOrReplaceChild(\"{base}_regalia\", {cubes(p.boxes)}, PartPose.ZERO);")
        lines.append("        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);")
        methods.append(f"    static LayerDefinition {layer.replace('_w', 'W')}() {{\n" + "\n".join(lines) + "\n    }")
    body = "\n\n".join(methods)
    return f'''package io.github.hammerknife404.dreadpatch.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Her regalia's geometry as worn (0.81.0), generated by {{@code tools/models/make_regalia.py}} (which also paints its
 * texture and renders previews): edit and re-run that, not this. A humanoid a piece, every base part there (vanilla's
 * model needs them), carrying only that piece; the winged bodice's wings hang off the body.
 */
@SuppressWarnings("unused")
final class RegaliaGeometry {{
    static final int TEXTURE_WIDTH = {armour.TEX_W};
    static final int TEXTURE_HEIGHT = {armour.TEX_H};

    private RegaliaGeometry() {{
    }}

{body}
}}
'''


def coloured(boxes):
    return [(f, t, PALETTE[m][0], PALETTE[m][1]) for f, t, m in boxes]


def main():
    write_palette()
    sword = shift(sword_boxes(), -7)  # its grip about the model's middle height
    staff = shift(staff_boxes(), -13)
    models = ASSETS / "models/item"
    models.mkdir(parents=True, exist_ok=True)
    (models / "dread_queen_sword_regalia.json").write_text(json.dumps(item_model(sword, SWORD_DISPLAY), indent=1) + "\n")
    (models / "dread_queen_staff_regalia.json").write_text(json.dumps(item_model(staff, STAFF_DISPLAY), indent=1) + "\n")
    parts, image = sheet()
    (ASSETS / "textures/entity").mkdir(parents=True, exist_ok=True)
    image.save(ASSETS / "textures/entity/regalia.png")
    (REPO / "src/main/java/io/github/hammerknife404/dreadpatch/client/RegaliaGeometry.java").write_text(java(parts))
    for name, boxes in (("sword", sword), ("staff", staff)):
        ys = [b[0][1] for b in boxes] + [b[1][1] for b in boxes]
        print(f"{name}: {len(boxes)} elements, y {min(ys)} to {max(ys)}")
    if "--preview" in sys.argv:
        out = pathlib.Path(sys.argv[sys.argv.index("--preview") + 1])
        out.mkdir(parents=True, exist_ok=True)
        render_boxes(out / "sword.png", coloured(sword), math.radians(25), math.radians(-12), 14, centre=(8, 12, 8))
        render_boxes(out / "staff.png", coloured(staff), math.radians(25), math.radians(-12), 11, centre=(8, 9, 8))
        figure(out, sword, staff, parts, image)
        print("previews in", out)


if __name__ == "__main__":
    main()
