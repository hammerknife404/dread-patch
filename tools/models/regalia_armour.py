"""Her regalia's armour and the winged bodice's wings, after the owner's pictures (0.80.0); used by make_regalia.py.

Model space as vanilla's humanoid: pixels, y down, the wearer facing -z, its parts' pivots vanilla's (the head and body at
0, the right arm at -5 2 0, the left at 5 2 0, the right leg at -1.9 12 0, the left at 1.9 12 0). Boxes carry a `grow`
(vanilla's CubeDeformation: the box drawn that much bigger every way, its texture the box's own size), as vanilla's
armour layers grow theirs (1 for the outer pieces, 0.5 for leggings).

The pictures' armour: a dark helm open at the face under a tall crown of ice spikes; a dark breastplate with a cyan
collar, a belt with a cyan buckle; pauldrons crusted with frost and icicle spikes; dark vambraces and greaves banded
cyan; a dark blue tabard over each leg, jagged with ice at its edges; bone-white dragon wings with tattered dark
membranes, frosted at their edges and hung with icicles.
"""
import math
import random

# One sheet for the armour and the wings (the armour layer draws a piece in one texture, the wings with the bodice).
TEX_W, TEX_H = 256, 128

STEEL_DARK = (0x14, 0x1A, 0x24)
STEEL = (0x22, 0x2B, 0x38)
STEEL_MID = (0x30, 0x3C, 0x4E)
STEEL_LIGHT = (0x46, 0x56, 0x6C)
CYAN = (0x2C, 0xC8, 0xF0)
CYAN_LIGHT = (0x9C, 0xEC, 0xFF)
FROST = (0xC4, 0xE0, 0xEE)
FROST_MID = (0x92, 0xC2, 0xDC)
ICE_DEEP = (0x4E, 0x8C, 0xB8)
NAVY = (0x1C, 0x26, 0x5A)
NAVY_MID = (0x28, 0x36, 0x7A)
NAVY_LIGHT = (0x3A, 0x4C, 0x9C)
BONE = (0xD8, 0xE6, 0xEE)
BONE_SHADE = (0xA6, 0xBC, 0xCC)
MEMBRANE = (0x2A, 0x28, 0x44)
MEMBRANE_MID = (0x40, 0x3E, 0x5E)
MEMBRANE_FROST = (0x7C, 0x96, 0xB0)


class Box:
    def __init__(self, x, y, z, w, h, d, mat, grow=0.0):
        self.x, self.y, self.z, self.w, self.h, self.d, self.mat, self.grow = x, y, z, w, h, d, mat, grow
        self.u = self.v = None

    def dims(self):
        return tuple(int(math.ceil(n)) for n in (self.w, self.h, self.d))

    def size(self):
        w, h, d = self.dims()
        return 2 * (d + w), d + h


class Part:
    def __init__(self, name, parent, pivot, rot, boxes, slot=None):
        self.name, self.parent, self.pivot, self.rot, self.boxes, self.slot = name, parent, pivot, rot, boxes, slot


VANILLA = {"head": (0, 0, 0), "body": (0, 0, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0),
           "right_leg": (-1.9, 12, 0), "left_leg": (1.9, 12, 0)}


def crown_spikes():
    """Ice spikes standing round the helm's top, taller at the front's middle and the corners; each two wide."""
    spikes = []
    heights_front = [4, 6, 3, 7, 3, 6, 4]
    for i, h in enumerate(heights_front):
        x = -5 + i * 10 / 7
        spikes.append(Box(x, -9.2 - h, -5.2, 1.4, h, 1.2, "crown"))
        spikes.append(Box(x, -9.2 - (h - 1), 4.0, 1.4, h - 1, 1.2, "crown"))
    for i, h in enumerate([5, 3, 5, 3, 5]):
        z = -3.8 + i * 1.55
        spikes.append(Box(-5.2, -9.2 - h, z, 1.2, h, 1.4, "crown"))
        spikes.append(Box(4.0, -9.2 - h, z, 1.2, h, 1.4, "crown"))
    spikes.append(Box(-5.2, -9.4, -5.2, 10.4, 1.2, 10.4, "crown_band"))
    return spikes


def pauldron(side):
    """Its pauldron (over the arm's top, outward), and frost spikes along it."""
    x = -4.5 if side < 0 else -0.5
    boxes = [Box(x, -3.5, -3.0, 5.0, 4.0, 6.0, "pauldron")]
    out = -4.5 if side < 0 else 4.5
    for i, (dz, h) in enumerate(((-2.2, 3), (-0.5, 4), (1.3, 3))):
        sx = out - 1.0 if side < 0 else out - 0.0
        boxes.append(Box(sx, -3.5 - h, dz, 1.0, h, 1.0, "frost_spike"))
    return boxes


# A wing is three segments hinged end to end (0.81.2, after Cataclysm: Spellbooks' winged Cursium set, at the owner's
# word): its root, its middle and its tip, each a third of its span, so it can fold like an accordion and flex.
WING_SEGMENT = 10
WING_SEGMENTS = ("", "_mid", "_tip")


def wing_boxes(side, segment):
    """One segment of a wing: its piece of the membrane (a flat sheet, painted, cut out) and its leading bone along the
    top, reaching out from its own hinge at x 0: along -x for the right wing (side -1), +x for the left."""
    n = WING_SEGMENT
    x = -n if side < 0 else 0
    bone = Box(x, -14.5, -0.75, n, 1.5, 1.5, "wing_bone")
    sheet = Box(x, -14, 0, n, 28, 0, "membrane")
    sheet.side = side
    sheet.segment = segment
    return [sheet, bone]


def wing_parts(side):
    name = "right_wing" if side < 0 else "left_wing"
    parts = []
    parent, pivot = "body", (2.0 * side, 2.5, 3.2)
    for i, suffix in enumerate(WING_SEGMENTS):
        parts.append(Part(name + suffix, parent, pivot, (0, 0, 0), wing_boxes(side, i), "wings"))
        parent, pivot = name + suffix, (WING_SEGMENT * side, 0.0, 0.0)
    return parts


def parts(winged=True):
    """Every part, each box marked with the armour slot that draws it."""
    p = []
    p.append(Part("head", None, VANILLA["head"], (0, 0, 0), [Box(-4, -8, -4, 8, 8, 8, "helm", 1.0)] + crown_spikes(), "head"))
    p.append(Part("body", None, VANILLA["body"], (0, 0, 0), [
        Box(-4, 0, -2, 8, 12, 4, "plate", 1.01),
        Box(-4, 9.5, -2, 8, 2, 4, "belt", 1.3),
        Box(-1.5, 9.0, -3.7, 3, 3, 0.6, "buckle"),
    ], "chest"))
    p.append(Part("right_arm", None, VANILLA["right_arm"], (0, 0, 0),
                  [Box(-3, -2, -2, 4, 12, 4, "vambrace", 1.0)] + pauldron(-1), "chest"))
    p.append(Part("left_arm", None, VANILLA["left_arm"], (0, 0, 0),
                  [Box(-1, -2, -2, 4, 12, 4, "vambrace", 1.0)] + pauldron(1), "chest"))
    for name, side in (("right_leg", -1), ("left_leg", 1)):
        p.append(Part(name + "_legs", None, VANILLA[name], (0, 0, 0), [
            Box(-2, 0, -2, 4, 12, 4, "legging", 0.5),
            # Its half of the tabard, before and behind, hanging from the belt to the knee and swinging with the leg.
            Box(-2.4, -0.5, -3.1, 4.8, 9, 0.6, "tabard"),
            Box(-2.4, -0.5, 2.5, 4.8, 9, 0.6, "tabard"),
            Box(-3.0 if side < 0 else 2.4, -0.5, -2.5, 0.6, 7, 5.0, "tabard_side"),
        ], "legs"))
        p.append(Part(name + "_feet", None, VANILLA[name], (0, 0, 0), [
            Box(-2, 6, -2, 4, 6, 4, "greave", 1.0),
            Box(-2.6, 10.2, -3.8, 5.2, 1.8, 1.4, "toe"),
        ], "feet"))
    if winged:
        p += wing_parts(-1) + wing_parts(1)
    return p


def pack(boxes, width, height):
    order = sorted(range(len(boxes)), key=lambda i: (-boxes[i].size()[1], i))
    x = y = shelf = 0
    for i in order:
        w, h = boxes[i].size()
        if x + w > width:
            x, y, shelf = 0, y + shelf, 0
        if y + h > height:
            raise SystemExit(f"the texture's full ({width} x {height})")
        boxes[i].u, boxes[i].v = x, y
        x += w
        shelf = max(shelf, h)


def faces(b):
    u, v = b.u, b.v
    w, h, d = b.dims()
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint(skin, boxes, wings_image=None):
    rnd = random.Random(80)

    def px(im, x, y, c):
        im.putpixel((x, y), c if len(c) == 4 else (*c, 255))

    def fill(im, rect, at):
        x0, y0, w, h = rect
        for j in range(h):
            for i in range(w):
                c = at(i, j, w, h)
                if c is not None:
                    px(im, x0 + i, y0 + j, c)

    def steel(i, j, w, h):
        r = rnd.random()
        return STEEL_DARK if r < 0.3 else STEEL if r < 0.8 else STEEL_MID

    for b in boxes:
        f = faces(b)
        m = b.mat
        im = wings_image if m in ("membrane", "wing_bone") and wings_image is not None else skin
        if m == "helm":
            for s in f:
                fill(im, f[s], steel)
            # The front (0.82.0, the owner's videos): a closed helm, a T-shaped visor (an eye slit across, a slot down
            # its middle) open onto the face, cyan-edged; a cyan brow over it.
            # 0.82.1 (the owner: "the helmet covers the default player eye location"): the eye slit on row 4, where a
            # player's eyes are, the slot down from it to the mouth.
            def front(i, j, w, h):
                if j == 4 and 1 <= i <= 6 or 3 <= i <= 4 and 5 <= j <= 7:
                    return (0, 0, 0, 0)
                if j == 3 and 1 <= i <= 6:
                    return CYAN
                if (j == 5 and i in (1, 2, 5, 6)) or (i in (2, 5) and 6 <= j <= 7):
                    return STEEL_LIGHT
                return steel(i, j, w, h)
            fill(im, f["front"], front)
            for s in ("right", "left"):
                fill(im, f[s], lambda i, j, w, h: CYAN if j == 2 and 1 <= i <= 6 else (STEEL_LIGHT if j == 6 else steel(i, j, w, h)))
            fill(im, f["back"], lambda i, j, w, h: CYAN if j == 2 else steel(i, j, w, h))
        elif m == "crown":
            # Ice: deep at its foot, bright at its tip.
            for s in f:
                fill(im, f[s], lambda i, j, w, h: CYAN_LIGHT if j < h * 0.3 else CYAN if j < h * 0.65 else ICE_DEEP)
        elif m == "crown_band":
            for s in f:
                fill(im, f[s], lambda i, j, w, h: CYAN if (i + j) % 3 else ICE_DEEP)
        elif m == "plate":
            for s in f:
                fill(im, f[s], steel)
            # A cyan V at its collar, a cyan line down its middle, ridges of lighter steel.
            def chest(i, j, w, h):
                if j <= 3 and abs((i - 3.5)) >= 3.5 - j:
                    return CYAN if abs(abs(i - 3.5) - (3.5 - j)) < 1.0 else STEEL_MID
                if (i in (3, 4)) and 4 <= j <= 8:
                    return CYAN if j % 2 == 0 else STEEL_LIGHT
                if j in (5, 6) and i in (1, 6):
                    return STEEL_LIGHT
                return steel(i, j, w, h)
            fill(im, f["front"], chest)
            fill(im, f["back"], lambda i, j, w, h: STEEL_MID if i in (3, 4) and j < 9 else steel(i, j, w, h))
        elif m == "belt":
            for s in f:
                fill(im, f[s], lambda i, j, w, h: STEEL_DARK if j == 0 else STEEL)
        elif m == "buckle":
            for s in f:
                fill(im, f[s], lambda i, j, w, h: CYAN_LIGHT if (i, j) == (1, 1) else CYAN)
        elif m == "pauldron":
            # Layered steel (0.82.0, the videos'): plates stepping down, each rimmed cyan, a little frost on top.
            for s in f:
                fill(im, f[s], lambda i, j, w, h: CYAN if j in (1, h - 1) else STEEL_DARK if j in (0, 2) else (STEEL_LIGHT if rnd.random() < 0.3 else STEEL_MID))
            fill(im, f["top"], lambda i, j, w, h: FROST_MID if rnd.random() < 0.35 else STEEL_MID)
        elif m == "frost_spike":
            for s in f:
                fill(im, f[s], lambda i, j, w, h: FROST if j < h / 2 else FROST_MID)
        elif m == "vambrace":
            for s in f:
                fill(im, f[s], lambda i, j, w, h: CYAN if j in (3, 8) else (STEEL_DARK if j >= 10 else steel(i, j, w, h)))
        elif m == "legging":
            for s in f:
                fill(im, f[s], lambda i, j, w, h: NAVY_MID if rnd.random() < 0.3 else NAVY)
            for s in ("right", "left"):
                fill(im, f[s], lambda i, j, w, h: CYAN if i == 1 and j % 3 != 2 else (NAVY_MID if rnd.random() < 0.3 else NAVY))
        elif m in ("tabard", "tabard_side"):
            # Dark blue, its edges and hem jagged with ice.
            def tab(i, j, w, h):
                # Ice creeping up its edges in jagged tongues (as the pictures' lightning-like streaks), a ragged hem.
                tongue = (3, 5, 2, 6, 4)[i % 5]
                if j >= h - 1 and i % 2 == 1:
                    return None
                if j >= h - 1 - (1 if i % 3 == 0 else 0):
                    return FROST_MID
                if (i == 0 or i == w - 1) and j >= h - tongue:
                    return FROST if j % 2 == 0 else CYAN_LIGHT
                if j == 0:
                    return STEEL_DARK
                return NAVY_MID if rnd.random() < 0.3 else NAVY
            for s in f:
                fill(im, f[s], tab)
        elif m == "greave":
            for s in f:
                fill(im, f[s], lambda i, j, w, h: CYAN if j == 1 else (STEEL_LIGHT if j == 0 else steel(i, j, w, h)))
        elif m == "toe":
            for s in f:
                fill(im, f[s], lambda i, j, w, h: STEEL_LIGHT if j == 0 else STEEL_MID)
        elif m == "wing_bone":
            for s in f:
                fill(im, f[s], lambda i, j, w, h: BONE if rnd.random() < 0.7 else BONE_SHADE)
        elif m == "membrane":
            right = getattr(b, "side", -1) < 0
            seg = getattr(b, "segment", 0)
            paint_membrane(im, f["front"], mirrored=not right, rnd=rnd, segment=seg)
            paint_membrane(im, f["back"], mirrored=right, rnd=rnd, segment=seg)


def membrane_at(s, t):
    """What the wing's sheet is at s (0 at its root, 1 at its tip) and t (0 at its top, 1 at its foot): 'bone', 'frost',
    'membrane', 'icicle' or None (cut out). Three finger bones fan from the wrist (s 0.42, t 0.06) to the foot of the sheet;
    between them it scallops upward, frayed at its edge, and icicles hang off the bones' tips."""
    wrist = (0.42, 0.06)
    if t <= 0.07:
        return "bone" if s < 0.98 else None
    tips = [(0.30, 0.92), (0.62, 1.0), (0.97, 0.78)]
    # The arm bone to the wrist runs along the top; fingers from the wrist.
    for ts, tt in tips:
        # Distance from the line wrist -> tip.
        ax, ay = wrist
        bx, by = ts, tt
        l2 = (bx - ax) ** 2 + (by - ay) ** 2
        k = max(0.0, min(1.0, ((s - ax) * (bx - ax) + (t - ay) * (by - ay)) / l2))
        dx, dy = s - (ax + k * (bx - ax)), t - (ay + k * (by - ay))
        if dx * dx + dy * dy < 0.028 ** 2 * (1.6 - k):
            return "bone"
    # The scalloped foot: between neighbouring tips, the edge rises in an arc.
    edges = [(0.0, 0.7)] + tips + [(1.02, 0.1)]
    foot = 1.0
    for (s0, t0), (s1, t1) in zip(edges, edges[1:]):
        if s0 <= s <= s1:
            k = (s - s0) / (s1 - s0)
            foot = t0 + (t1 - t0) * k - 0.16 * math.sin(math.pi * k)
    if t > foot:
        # Icicles hanging below the edge now and then.
        if (int(s * 30) % 4 == 0) and t < foot + 0.09 * (1 + (int(s * 30) % 3)):
            return "icicle"
        return None
    if t > foot - 0.06:
        return "frost"
    return "membrane"


def paint_membrane(im, rect, mirrored, rnd, segment=0):
    x0, y0, w, h = rect
    span = w * len(WING_SEGMENTS)
    for j in range(h):
        for i in range(w):
            # The root is at the face's right end on the front (the wing reaches out along -x), its left on the back;
            # s runs over the whole wing, this segment its third of it.
            k = (w - 1 - i) if not mirrored else i
            s = (segment * w + k) / (span - 1)
            t = j / (h - 1)
            kind = membrane_at(s, t)
            if kind is None:
                continue
            if kind == "bone":
                c = BONE if rnd.random() < 0.75 else BONE_SHADE
            elif kind == "frost":
                c = FROST_MID if rnd.random() < 0.6 else FROST
            elif kind == "icicle":
                c = FROST if rnd.random() < 0.5 else ICE_DEEP
            else:
                # Darker toward the root, frosting toward the tip and the edge: streaks along the fingers, not speckle.
                streak = math.sin(s * 22.0 + t * 6.0) > 0.55
                c = MEMBRANE_FROST if s > 0.75 and streak else MEMBRANE_MID if streak or t > 0.6 else MEMBRANE
            im.putpixel((x0 + i, y0 + j), (*c, 255))
