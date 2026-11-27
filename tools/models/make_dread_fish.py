"""The Dread Fish's model, after the owner's picture (0.71.0): its geometry as Java (client/DreadFishGeometry.java),
its textures (skin, eyes, lure; 128x64) and, with --preview DIR, renders of it to compare with the picture.
Python 3 with Pillow. Deterministic: re-running gives identical files. Edit this, not the outputs.

The picture: a big, boxy, bone-pale anglerfish head, its jaw hanging open on rows of fangs, a dark eye each
side, a jagged crest; a lure on a tall stalk arching forward over the snout, a lantern hanging from its end;
fanned, spined fins behind the head; a thick, ribbed serpent's body curling round behind it into the water.

Model space as vanilla's: pixels, y down, the head toward -z, the ground at y 24. Each body segment hangs off
the one before, turned a little, so at rest the body curls round (the animation adds its wave to that).
"""
import math
import pathlib
import random
import sys

from PIL import Image, ImageDraw

REPO = pathlib.Path(__file__).resolve().parents[2]
TEXTURES = REPO / "src/main/resources/assets/dreadpatch/textures/entity"
JAVA = REPO / "src/main/java/io/github/hammerknife404/dreadpatch/client/DreadFishGeometry.java"
TEX_W, TEX_H = 128, 128

# Bone-pale with a cold cyan cast, frost-shadowed, from the picture.
LIGHT = (0xE6, 0xF5, 0xF3)
PALE = (0xCB, 0xE4, 0xE2)
MID = (0xAC, 0xCB, 0xCB)
DIM = (0x86, 0xA6, 0xAA)
DARK = (0x50, 0x68, 0x70)
SOOT = (0x10, 0x12, 0x18)
MOUTH = (0x16, 0x14, 0x1C)
GUM = (0x3A, 0x30, 0x3E)
FANG = (0xF6, 0xF8, 0xFA)
IRON = (0x22, 0x26, 0x30)
GLASS = (0x2E, 0x5E, 0x6E)
CYAN = (0x00, 0xCB, 0xFF)
BRIGHT = (0x6C, 0xE8, 0xFF)
CORE = (0xE6, 0xFD, 0xFF)
GLINT = (0xC8, 0xE6, 0xEA)


class Box:
    def __init__(self, x, y, z, w, h, d, mat):
        self.x, self.y, self.z, self.w, self.h, self.d, self.mat = x, y, z, w, h, d, mat
        self.u = self.v = None

    def size(self):
        w, h, d = (int(math.ceil(n)) for n in (self.w, self.h, self.d))
        return 2 * (d + w), d + h


class Part:
    def __init__(self, name, parent, pivot, rot, boxes):
        self.name, self.parent, self.pivot, self.rot, self.boxes = name, parent, pivot, rot, boxes


def B(*args):
    return Box(*args)


# The body's segments: (width/height, length, its turn from the one before), front to tail. At rest they lie
# flat on the water and curl round in a ring, nearly closing on the head, as the picture's does.
SEGMENTS = [(8, 6, 0.15), (8, 6, 0.45), (7, 6, 0.7), (7, 6, 0.62), (6, 6, 0.75), (5, 6, 0.75), (4, 5, 0.75), (3, 5, 0.7)]


def body_parts():
    parts = []
    parent = "head"
    pivot = (0.0, -3.0, -0.5)
    for i, (size, length, turn) in enumerate(SEGMENTS):
        name = f"body{i + 1}"
        half = size / 2
        boxes = [B(-half, -half, 0, size, size, length, "segment"), B(-half - 0.5, -half - 0.5, 0.5, size + 1, size + 1, 1, "ring")]
        if i == 0:
            # The dorsal fin's jagged spikes, just behind the head.
            boxes.append(B(0, -half - 6, 0, 0, 6, 7, "dorsal"))
        elif size >= 5:
            boxes.append(B(0, -half - 2, 1, 0, 2, length - 1, "spines"))
        # The first dips a little from the raised head to the water; the second levels out again.
        rot = (0.2 if i == 0 else -0.2 if i == 1 else 0.0, turn, 0.0)
        parts.append(Part(name, parent, pivot, rot, boxes))
        parent, pivot = name, (0.0, 0.0, float(length))
    parts.append(Part("tail_fin", parent, (0.0, 0.0, pivot[2] - 0.5), (0.0, 0.0, 0.0), [B(0, -4, 0, 0, 8, 6, "tail")]))
    return parts


PARTS = [
    # The head, the root (set off to one side and back, so the curled fish's middle is the entity's): a skull 9 wide,
    # 6 tall, 10 long, long fangs hanging under its lip, a black eye each side with a pale glint, a dark mouth behind.
    Part("head", None, (-9.0, 16.0, -2.0), (0.0, 0.0, 0.0), [
        B(-4.5, -6, -10, 9, 6, 10, "skull"),
        B(-4, 0, -9.5, 8, 3, 9, "fangs_up"),
        B(-4, 0, -4, 8, 3, 4, "mouth"),
        B(-4.7, -4.5, -8, 1, 2, 2, "eye"),
        B(3.7, -4.5, -8, 1, 2, 2, "eye"),
    ]),
    # Its jaw, hanging open (a positive x turn drops its front), fangs up from it.
    Part("jaw", "head", (0.0, 1.0, -1.0), (0.42, 0.0, 0.0), [
        B(-4, 0, -9.5, 8, 2, 9.5, "jaw"),
        B(-3.5, -2, -9, 7, 2, 8.5, "fangs_down"),
    ]),
    # The lure: a stalk up from the top of its head, leaning back; an arm reaching forward and a little down over the
    # snout; a short hook; the lantern (a vanilla lantern's shape: a nub, a cap, the glass, a base).
    Part("stalk", "head", (0.0, -6.0, -4.5), (-0.3, 0.0, 0.0), [B(-1, -12, -1, 2, 12, 2, "stalk")]),
    Part("stalk_arm", "stalk", (0.0, -12.0, 0.0), (0.75, 0.0, 0.0), [B(-1, -1, -15, 2, 2, 15, "stalk")]),
    Part("stalk_hook", "stalk_arm", (0.0, 0.0, -14.5), (0.0, 0.0, 0.0), [B(-0.5, 0, -0.5, 1, 2, 1, "stalk")]),
    # Hanging plumb at rest: its x turn undoes its parents' (-0.3 + 0.75).
    Part("lantern", "stalk_hook", (0.0, 2.0, 0.0), (-0.45, 0.0, 0.0), [
        B(-1, 0, -1, 2, 1, 2, "iron"),
        B(-2.5, 1, -2.5, 5, 1, 5, "iron"),
        B(-2, 2, -2, 4, 4, 4, "lantern"),
        B(-2.5, 6, -2.5, 5, 1, 5, "iron"),
    ]),
    # Fanned pectoral fins behind the jaw: long bony rays, swept back, out and down, torn between.
    Part("right_fin", "head", (-4.5, -1.0, -2.0), (0.35, -0.95, 0.45), [B(0, -4, 0, 0, 9, 12, "fin")]),
    Part("left_fin", "head", (4.5, -1.0, -2.0), (0.35, 0.95, -0.45), [B(0, -4, 0, 0, 9, 12, "fin")]),
] + body_parts()


# Its lure drawn in, idle (DreadFishModel's RETRACT_*: added to the stalk's and arm's x turns, all of it when idle; the
# geometry above is the lure reaching out, searching). The lantern hangs plumb either way (the model undoes its parents'
# x turns), so it's drawn so here too.
RETRACT = {"stalk": (-0.6, 0.0, 0.0), "stalk_arm": (-0.1, 0.0, 0.0)}
# The glass's middle, in the lantern part's own space.
LANTERN_MIDDLE = (0.0, 4.0, 0.0)


def plumb(extra=None):
    """The extra x turn on the lantern that undoes its own and its parents' (head, stalk, arm, hook) past plumb."""
    extra = dict(extra or {})
    by_name = {p.name: p for p in PARTS}
    if "lantern" not in by_name:
        return extra
    total, name = 0.0, "lantern"
    while name:
        p = by_name[name]
        total += p.rot[0] + extra.get(name, (0, 0, 0))[0]
        name = p.parent
    extra["lantern"] = (-total, 0.0, 0.0)
    return extra


def part_point(name, point, extra_rot=None):
    """A point in a part's own space, in model space at rest (with extra_rot added to some parts' turns)."""
    by_name = {p.name: p for p in PARTS}

    def mat(p):
        base = mat(by_name[p.parent]) if p.parent else [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
        r = p.rot if not extra_rot or p.name not in extra_rot else tuple(a + b for a, b in zip(p.rot, extra_rot[p.name]))
        return matmul(base, matmul(translate(*p.pivot), rot_zyx(*r)))
    return apply(mat(by_name[name]), point)


def pack():
    """Shelf-packs every box's texture onto the sheet, tallest first; fixed order, so stable."""
    boxes = [b for p in PARTS for b in p.boxes]
    order = sorted(range(len(boxes)), key=lambda i: (-boxes[i].size()[1], i))
    x = y = shelf = 0
    for i in order:
        w, h = boxes[i].size()
        if x + w > TEX_W:
            x, y, shelf = 0, y + shelf, 0
        if y + h > TEX_H:
            raise SystemExit("the texture's full")
        boxes[i].u, boxes[i].v = x, y
        x += w
        shelf = max(shelf, h)


def faces(b):
    """Each face's rect on the sheet (x, y, w, h), as vanilla's ModelPart.Cube lays them (its DOWN face is the
    top in the world; on the top the front is the bottom row; on each side the front is the column nearest
    the front face)."""
    u, v = b.u, b.v
    w, h, d = (int(math.ceil(n)) for n in (b.w, b.h, b.d))
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint(skin, eyes, lure):
    rnd = random.Random(71)

    def px(im, x, y, c):
        im.putpixel((x, y), c if len(c) == 4 else (*c, 255))

    def fill(im, rect, at):
        x0, y0, w, h = rect
        for j in range(h):
            for i in range(w):
                c = at(i, j, w, h)
                if c is not None:
                    px(im, x0 + i, y0 + j, c)

    def mottle(i, j, w, h, top=False):
        r = rnd.random()
        if top:
            return DIM if r < 0.25 else MID if r < 0.6 else PALE
        down = j / max(1, h - 1)  # paler toward the belly
        return DARK if r < 0.06 else DIM if r < 0.22 - 0.12 * down else MID if r < 0.5 - 0.2 * down else PALE if r < 0.9 else LIGHT

    for part in PARTS:
        for b in part.boxes:
            f = faces(b)
            m = b.mat
            if m in ("skull", "jaw", "segment"):
                fill(skin, f["top"], lambda i, j, w, h: mottle(i, j, w, h, True))
                fill(skin, f["bottom"], lambda i, j, w, h: LIGHT if rnd.random() < 0.5 else PALE)
                for s in ("right", "left", "front", "back"):
                    fill(skin, f[s], mottle)
            if m == "skull":
                # The roof of its mouth; a heavy brow and lip on its snout.
                fill(skin, f["bottom"], lambda i, j, w, h: GUM if i in (0, w - 1) or j == h - 1 else MOUTH)
                fill(skin, f["front"], lambda i, j, w, h: DIM if j == 0 else DARK if j == h - 1 else mottle(i, j, w, h))
                for s in ("right", "left"):
                    # A dark seam along the lip, and pits along the cheek.
                    fill(skin, f[s], lambda i, j, w, h: DARK if j == h - 1 else (DIM if (i + j) % 4 == 0 else mottle(i, j, w, h)))
            if m == "jaw":
                fill(skin, f["top"], lambda i, j, w, h: GUM if i in (0, w - 1) or j == h - 1 else MOUTH)
                fill(skin, f["front"], lambda i, j, w, h: DARK if j == 0 else mottle(i, j, w, h))
                for s in ("right", "left"):
                    fill(skin, f[s], lambda i, j, w, h: DARK if j == 0 else mottle(i, j, w, h))
            if m == "segment":
                # Ribbed: a groove every other column along its length, darker along its back.
                for s in ("right", "left"):
                    fill(skin, f[s], lambda i, j, w, h: DIM if i % 2 == 1 else (MID if j == 0 else mottle(i, j, w, h)))
                fill(skin, f["top"], lambda i, j, w, h: DARK if j % 2 == 1 else DIM if rnd.random() < 0.5 else MID)
            if m == "ring":
                for s in faces(b):
                    fill(skin, f[s], lambda i, j, w, h: LIGHT if rnd.random() < 0.6 else PALE)
                for s in ("right", "left", "top", "bottom"):
                    fill(skin, f[s], lambda i, j, w, h: DIM if i in (0, w - 1) and s in ("top", "bottom") else (LIGHT if rnd.random() < 0.6 else PALE))
            if m in ("fangs_up", "fangs_down"):
                gum_row = 0 if m == "fangs_up" else None
                for s in ("right", "left", "front", "back"):
                    def fang(i, j, w, h, up=(m == "fangs_up")):
                        root = j == (0 if up else h - 1)
                        if root:
                            return FANG
                        return FANG if i % 2 == 0 else None
                    fill(skin, f[s], fang)
            if m == "eye":
                for s in faces(b):
                    fill(skin, f[s], lambda i, j, w, h: SOOT)
                # The outward face (right on the right eye, left on the left): a cyan pupil.
                side = "right" if b.x < 0 else "left"
                x0, y0, w, h = f[side]
                front_col = x0 + w - 1 if side == "right" else x0
                # The Dread's cyan: a pupil burning in the black, a pale glint over it.
                px(skin, front_col, y0, GLINT)
                px(eyes, front_col, y0, (*GLINT, 160))
                px(skin, front_col, y0 + 1, CYAN)
                px(eyes, front_col, y0 + 1, BRIGHT)
                back_col = front_col - 1 if side == "right" else front_col + 1
                px(skin, back_col, y0 + 1, CYAN)
                px(eyes, back_col, y0 + 1, (*CYAN, 200))
            if m == "mouth":
                for s in faces(b):
                    fill(skin, f[s], lambda i, j, w, h: GUM if j == 0 else MOUTH)
            if m == "stalk":
                for s in faces(b):
                    fill(skin, f[s], lambda i, j, w, h: DIM if (i + j) % 5 == 0 else PALE)
            if m == "iron":
                for s in faces(b):
                    fill(skin, f[s], lambda i, j, w, h: IRON if (i + j) % 3 else DARK)
            if m == "lantern":
                for s in faces(b):
                    def glass(i, j, w, h, lit=False, s=s):
                        if s in ("right", "left", "front", "back") and i in (0, w - 1):
                            return None if lit else IRON  # its frame's corner posts
                        if lit:
                            return CORE if s not in ("top", "bottom") and j in (1, 2) else BRIGHT
                        return GLASS
                    fill(skin, f[s], glass)
                    fill(lure, f[s], lambda i, j, w, h, s=s: glass(i, j, w, h, True, s))
            if m in ("crest", "spines", "dorsal"):
                # A flat box: its two sides side by side, d wide each, h tall; sawtooth spines up from the base row.
                x0, y0 = b.u, b.v + int(b.d)
                d, h = int(b.d), int(b.h)
                for side in range(2):
                    for i in range(d):
                        k = i if side else d - 1 - i  # mirrored on the far side, so the teeth line up
                        tooth = h - (k % 3) * h // 3  # tall, mid, short, repeating back
                        for j in range(h):
                            if j >= h - tooth:
                                px(skin, x0 + side * d + i, y0 + j, LIGHT if j >= h - 1 else PALE if (k % 3) == 0 else MID)
            if m in ("fin", "tail"):
                # Spines fanning from its root (the front, mid-height) to the trailing edge, webbed between, ragged.
                x0, y0 = b.u, b.v + int(b.d)
                d, h = int(b.d), int(b.h)
                rays = 5
                for side in range(2):
                    for i in range(d):
                        k = i if side else d - 1 - i  # k: 0 at the root
                        t = (k + 0.5) / d
                        for j in range(h):
                            # Which ray this pixel's nearest, by its angle out from the root.
                            centre = (h - 1) / 2
                            a = (j - centre) / max(0.5, centre * (0.35 + 0.65 * t))
                            if abs(a) > 1.0:
                                continue
                            ray = (a + 1) / 2 * (rays - 1)
                            on_ray = abs(ray - round(ray)) < 0.22 + 0.2 * (1 - t)
                            if on_ray:
                                c = LIGHT if rnd.random() < 0.7 else PALE
                            elif t > 0.3 and (m == "fin" or rnd.random() < (t - 0.2) * 1.3):
                                continue  # bare bony rays past the root (the picture's), the tail's torn toward its edge
                            else:
                                c = (*(MID if rnd.random() < 0.5 else DIM), 255)
                            px(skin, x0 + side * d + i, y0 + j, c)


# --- The preview: the boxes posed as vanilla would, each texel drawn as a quad, painter-sorted. ---

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


def world_quads(textures, extra_rot=None):
    """Every texel of every face as (corners in model space, colour), the model posed at rest."""
    by_name = {p.name: p for p in PARTS}
    mats = {}

    def mat(p):
        if p.name not in mats:
            base = mat(by_name[p.parent]) if p.parent else [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
            r = p.rot if not extra_rot or p.name not in extra_rot else tuple(a + b for a, b in zip(p.rot, extra_rot[p.name]))
            mats[p.name] = matmul(base, matmul(translate(*p.pivot), rot_zyx(*r)))
        return mats[p.name]

    quads = []
    for p in PARTS:
        m = mat(p)
        for b in p.boxes:
            x0, y0, z0, x1, y1, z1 = b.x, b.y, b.z, b.x + b.w, b.y + b.h, b.z + b.d
            V = {"7": (x0, y0, z0), "0": (x1, y0, z0), "1": (x1, y1, z0), "2": (x0, y1, z0),
                 "3": (x0, y0, z1), "4": (x1, y0, z1), "5": (x1, y1, z1), "6": (x0, y1, z1)}
            u, v = b.u, b.v
            w, h, d = (int(math.ceil(n)) for n in (b.w, b.h, b.d))
            f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + 2 * w, u + 2 * d + w, u + 2 * d + 2 * w
            f10, f11, f12 = v, v + d, v + d + h
            polys = [("4370", f5, f10, f6, f11), ("1265", f6, f11, f7, f10), ("7362", f4, f11, f5, f12),
                     ("0721", f5, f11, f6, f12), ("4015", f6, f11, f8, f12), ("3456", f8, f11, f9, f12)]
            for verts, u1, v1, u2, v2 in polys:
                c = [apply(m, V[k]) for k in verts]
                # Polygon.remap: vertex 0 at (u2, v1), 1 at (u1, v1), 2 at (u1, v2), 3 at (u2, v2).
                cols, rows = abs(u2 - u1), abs(v2 - v1)
                for i in range(cols):
                    for j in range(rows):
                        tu = (u1 + i) if u2 > u1 else (u1 - 1 - i)
                        tv = (v1 + j) if v2 > v1 else (v1 - 1 - j)
                        colour = None
                        for tex in textures:
                            pxl = tex.getpixel((tu, tv))
                            if pxl[3] > 0:
                                colour = pxl
                        if colour is None:
                            continue

                        def at(s, t):  # s along u1->u2, t along v1->v2
                            top = [c[1][k] + (c[0][k] - c[1][k]) * s for k in range(3)]
                            bot = [c[2][k] + (c[3][k] - c[2][k]) * s for k in range(3)]
                            return [top[k] + (bot[k] - top[k]) * t for k in range(3)]
                        s0, s1, t0, t1 = i / cols, (i + 1) / cols, j / rows, (j + 1) / rows
                        quads.append(([at(s0, t0), at(s1, t0), at(s1, t1), at(s0, t1)], colour))
    return quads


def render(path, yaw, pitch, scale=12, size=(720, 560), extra_rot=None):
    skin = Image.open(TEXTURES / "dread_fish.png").convert("RGBA")
    lure = Image.open(TEXTURES / "dread_fish_lure.png").convert("RGBA")
    eyes = Image.open(TEXTURES / "dread_fish_eyes.png").convert("RGBA")
    quads = world_quads([skin, lure, eyes], plumb(extra_rot))
    im = Image.new("RGBA", size, (0x1C, 0x24, 0x34, 255))
    draw = ImageDraw.Draw(im)
    cy_, sy_, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)
    light = (0.4, -0.8, -0.45)

    def view(p):
        # Vanilla draws the model flipped (x and y): to the world, then turn by yaw and tip by pitch.
        x, y, z = -p[0], 24 - p[1], p[2]
        x, z = x * cy_ - z * sy_, x * sy_ + z * cy_
        y, z = y * cp - z * sp, y * sp + z * cp
        return x, y, z

    drawn = []
    for corners, colour in quads:
        vs = [view(c) for c in corners]
        depth = sum(v[2] for v in vs) / 4
        a, b, c = vs[0], vs[1], vs[3]
        n = [(b[1] - a[1]) * (c[2] - a[2]) - (b[2] - a[2]) * (c[1] - a[1]),
             (b[2] - a[2]) * (c[0] - a[0]) - (b[0] - a[0]) * (c[2] - a[2]),
             (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])]
        ln = math.sqrt(sum(k * k for k in n)) or 1
        shade = 0.6 + 0.4 * abs(sum(n[k] / ln * light[k] for k in range(3)))
        glow = colour[:3] in (BRIGHT, CORE, CYAN)
        rgb = colour[:3] if glow else tuple(int(k * shade) for k in colour[:3])
        drawn.append((depth, [(size[0] / 2 + v[0] * scale, size[1] * 0.72 - v[1] * scale) for v in vs], rgb))
    for depth, pts, rgb in sorted(drawn, key=lambda q: -q[0]):
        draw.polygon(pts, fill=rgb)
    im.save(path)


def java():
    by_child = {}
    for p in PARTS:
        by_child.setdefault(p.parent, []).append(p)
    lines = []

    def f(n):
        return f"{float(n)}F"

    def emit(p, parent_var):
        var = p.name.replace("_", "")
        cubes = "CubeListBuilder.create()" + "".join(
            f".texOffs({b.u}, {b.v}).addBox({f(b.x)}, {f(b.y)}, {f(b.z)}, {f(b.w)}, {f(b.h)}, {f(b.d)})" for b in p.boxes)
        lines.append(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{p.name}\", {cubes},")
        lines.append(f"                PartPose.offsetAndRotation({', '.join(f(n) for n in p.pivot)}, {', '.join(f(n) for n in p.rot)}));")
        for c in by_child.get(p.name, []):
            emit(c, var)

    for p in by_child[None]:
        emit(p, "root")
    body = "\n".join(lines)
    return f'''package io.github.hammerknife404.dreadpatch.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The Dread Fish's geometry, generated by {{@code tools/models/make_dread_fish.py}} (which also draws its textures and
 * renders previews): edit and re-run that, not this. Its parts by name for {{@link DreadFishModel}}.
 */
@SuppressWarnings("unused")
final class DreadFishGeometry {{
    static final int TEXTURE_WIDTH = {TEX_W};
    static final int TEXTURE_HEIGHT = {TEX_H};

    private DreadFishGeometry() {{
    }}

    static LayerDefinition create() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }}
}}
'''


def main():
    pack()
    skin = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    eyes = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    lure = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    paint(skin, eyes, lure)
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for name, im in (("dread_fish", skin), ("dread_fish_eyes", eyes), ("dread_fish_lure", lure)):
        im.save(TEXTURES / f"{name}.png")
    JAVA.write_text(java())
    print("wrote", JAVA.relative_to(REPO), "and the textures")
    # Its extent at rest, for the hitbox and DreadFishRules.MODEL_LENGTH.
    pts = [c for q, _ in world_quads([skin]) for c in q]
    for axis, name in ((0, "x"), (1, "y"), (2, "z")):
        lo, hi = min(p[axis] for p in pts), max(p[axis] for p in pts)
        print(f"{name}: {lo:.1f} to {hi:.1f} ({(hi - lo) / 16:.2f} blocks)")
    # Where its lantern's glass hangs, reaching out and drawn in (DreadFishRules.LANTERN_OUT / LANTERN_IN; model pixels).
    for name, extra in (("out", None), ("in", RETRACT)):
        x, y, z = part_point("lantern", LANTERN_MIDDLE, plumb(extra))
        print(f"lantern {name}: {x:.1f}, {y:.1f}, {z:.1f}")
    if "--preview" in sys.argv:
        out = pathlib.Path(sys.argv[sys.argv.index("--preview") + 1])
        out.mkdir(parents=True, exist_ok=True)
        for name, yaw, pitch in (("front_quarter", math.radians(215), math.radians(25)), ("side", math.radians(270), math.radians(5)),
                                 ("top", math.radians(180), math.radians(80)), ("back_quarter", math.radians(40), math.radians(30))):
            render(out / f"dread_fish_{name}.png", yaw, pitch)
        render(out / "dread_fish_lure_in.png", math.radians(40), math.radians(28), extra_rot=RETRACT)
        print("previews in", out)


if __name__ == "__main__":
    main()
