"""The winged bodice's wings (0.82.0), after the owner's spec and two example videos: a scaled-down Syndrigossa's tattered
ice wings. Used by make_regalia.py, which writes client/RegaliaWingsGeometry and the wings' textures from it.

Each wing is a bone chain hung off the wearer's back: its root (the shoulder), its arm bone rising out and up to a spiked
wrist just over the shoulder line, and its hand at the wrist, carrying four fanned finger spars (the lowest a hooked
claw), icicles hanging from the leading spar, and the membrane: a flat sheet in the hand's plane, ragged, scalloped
between the fingers, frayed and torn, fringed with icicles, slate near the bones frosting to pale cyan at its edges,
half see-through. Built for the right wing (reaching out along -x); the left is its mirror.

Model space as vanilla's: pixels, y down, the wearer facing -z; the wings' parts hang off the body's (0, 0, 0).
"""
import math
import random

from regalia_armour import Box, Part

TEX_W, TEX_H = 128, 128

BONE = (0xDC, 0xEA, 0xF0)
BONE_SHADE = (0xB4, 0xC8, 0xD6)
BONE_DARK = (0x8C, 0xA4, 0xB6)
ICE = (0xA8, 0xDA, 0xEE)
ICE_DEEP = (0x6C, 0xAC, 0xCC)
SLATE = (0x2C, 0x2A, 0x42)
SLATE_MID = (0x44, 0x46, 0x66)
FROST = (0x9C, 0xCC, 0xDE)
FROST_LIGHT = (0xCC, 0xEA, 0xF4)
MEMBRANE_GREY = (0x7A, 0x86, 0xA0)
GLOW = (0x6C, 0xF0, 0xFF)

# Where the wing hangs on the back, and its chain's rest lengths and angles (the right wing's).
ROOT = (-2.5, 1.5, 2.6)
ARM_LENGTH = 10.0
ARM_RISE = 1.0       # the arm bone's turn up from level: its wrist out beside the head
# The fingers' tips, in the hand's frame (its origin the wrist, level when the hand undoes the arm's rise).
# 0.82.1 (the owner's screenshots: a starburst, its leading finger rising past the wrist): every finger fans outward
# and down from the wrist, the arm bone the wing's top edge, as in the videos.
FINGERS = [(-18.0, 1.5), (-16.5, 9.0), (-10.5, 16.0), (-2.5, 24.0)]
# Where the membrane meets each finger: its tip, but for the claw, which hooks on past the membrane.
WEB = [(-18.0, 1.5), (-16.5, 9.0), (-10.5, 16.0), (-2.2, 20.0)]
# The membrane sheet's extent in the hand's frame: out past the tips, in to the body, from over the wrist to the claw.
SHEET = (-19, -2, 29, 26)   # x, y, width, height


def finger_parts(side):
    """Each finger a thin bone along -x, turned toward its tip, as a child of the hand."""
    parts = []
    for i, (tx, ty) in enumerate(FINGERS):
        length = math.hypot(tx, ty)
        angle = math.atan2(ty, -tx)  # a z turn putting -x on the tip (y down)
        boxes = [Box(-length, -0.6, -0.6, length, 1.2, 1.2, "spar")]
        if i == 0:
            # Icicles hanging off the leading spar.
            for k, (at, h) in enumerate(((3.0, 2), (6.0, 3), (9.0, 2), (12.0, 4), (15.0, 2))):
                boxes.append(Box(-at - 0.5, 0.5, -0.4, 0.8, h, 0.8, "icicle"))
        parts.append((f"finger{i}", (0.0, 0.0, 0.0), (0.0, 0.0, -angle if side < 0 else angle), boxes))
    return parts


def wing(side):
    """The wing's parts, root to fingers: (name, parent, pivot, rot, boxes). The right wing is side -1."""
    s = side
    name = "right_wing" if side < 0 else "left_wing"
    root = Part(name, None, (ROOT[0] * -s, ROOT[1], ROOT[2]), (0, 0, 0),
                [Box(-1.0, -1.0, -1.0, 2.0, 2.0, 2.0, "joint")], "wings")
    arm_box = Box(-ARM_LENGTH, -1.1, -1.1, ARM_LENGTH, 2.2, 2.2, "bone") if s < 0 else Box(0, -1.1, -1.1, ARM_LENGTH, 2.2, 2.2, "bone")
    arm = Part(name + "_arm", name, (0, 0, 0), (0, 0, ARM_RISE if s < 0 else -ARM_RISE), [arm_box], "wings")
    hx = -ARM_LENGTH if s < 0 else ARM_LENGTH
    # The wrist: a knuckle and its spikes, rising just over the shoulder line.
    wrist = [Box(-1.2, -1.2, -1.2, 2.4, 2.4, 2.4, "joint"),
             Box(-0.5 + (-0.6 if s < 0 else 0.6), -4.5, -0.5, 1.0, 3.5, 1.0, "spike"),
             Box(-0.5 + (0.8 if s < 0 else -0.8), -3.2, -0.4, 0.8, 2.2, 0.8, "spike"),
             Box(-0.5 + (-1.8 if s < 0 else 1.8), -2.6, -0.4, 0.8, 1.6, 0.8, "spike")]
    sx, sy, sw, sh = SHEET
    sheet = Box(sx if s < 0 else -(sx + sw), sy, 0.0, sw, sh, 0.0, "membrane")
    sheet.side = side
    hand = Part(name + "_hand", name + "_arm", (hx, 0, 0), (0, 0, -ARM_RISE if s < 0 else ARM_RISE), wrist + [sheet], "wings")
    parts = [root, arm, hand]
    for fname, pivot, rot, boxes in finger_parts(side):
        if s > 0:
            boxes = [Box(-(b.x + b.w), b.y, b.z, b.w, b.h, b.d, b.mat) for b in boxes]
        parts.append(Part(f"{name}_{fname}", name + "_hand", pivot, rot, boxes, "wings"))
    return parts


def parts():
    return wing(-1) + wing(1)


def membrane_polygon():
    """The membrane's outline in the hand's frame (right wing): the wrist, out along the leading spar to its tip, then
    scalloped in between each finger's tip and the next, along the claw, and back in to the body under the arm."""
    root = (ARM_LENGTH * math.cos(ARM_RISE), ARM_LENGTH * math.sin(ARM_RISE))  # where the arm leaves the body
    pts = [(0.0, -0.6), (WEB[0][0], WEB[0][1])]
    corners = WEB + [(7.0, 17.0)]
    for (x0, y0), (x1, y1) in zip(corners, corners[1:]):
        # A scallop: the edge bows in toward the wrist between two tips.
        for k in range(1, 9):
            t = k / 8
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            bow = 0.14 * math.sin(math.pi * t)
            pts.append((x * (1 - bow), y * (1 - bow)))
    pts += [root, (0.0, -0.6)]
    return pts


def inside(poly, x, y):
    hit = False
    for (x0, y0), (x1, y1) in zip(poly, poly[1:] + poly[:1]):
        if (y0 > y) != (y1 > y) and x < x0 + (y - y0) * (x1 - x0) / (y1 - y0):
            hit = not hit
    return hit


def edge_distance(poly, x, y):
    best = 1e9
    for (x0, y0), (x1, y1) in zip(poly, poly[1:] + poly[:1]):
        dx, dy = x1 - x0, y1 - y0
        l2 = dx * dx + dy * dy or 1e-9
        k = max(0.0, min(1.0, ((x - x0) * dx + (y - y0) * dy) / l2))
        best = min(best, math.hypot(x - (x0 + k * dx), y - (y0 + k * dy)))
    return best


def hash01(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h ^ (h >> 16)) / 0xFFFFFFFF


def membrane_texel(x, y, poly):
    """A texel of the sheet at (x, y) in the hand's frame (centre of the texel): (colour, alpha, glows) or None."""
    if not inside(poly, x, y):
        # Icicles hanging under the lower edge: below a point of the outline, a drip of 1-3.
        if y > 2 and inside(poly, x, y - 1.0) is False:
            for drop in range(1, 6):
                if inside(poly, x, y - drop) and hash01(int(math.floor(x)), 7, 3) < 0.6 and drop <= 1 + int(hash01(int(math.floor(x)), 9, 5) * 5):
                    return (FROST_LIGHT if drop < 3 else ICE), 225, drop >= 4
        return None
    edge = edge_distance(poly, x, y)
    ix, iy = int(math.floor(x)), int(math.floor(y))
    # Small tears near the edge, and the edge frayed.
    if edge < 2.5 and hash01(ix, iy, 11) < 0.10:
        return None
    reach = min(1.0, math.hypot(x, y) / 21.0)  # 0 at the wrist, 1 out at the tips
    low = max(0.0, min(1.0, (y + 4.0) / 20.0))   # 0 up by the leading spar, 1 at the foot
    # Slate up under the spars, frosting to pale grey-cyan down toward the ragged foot (the videos'), streaked
    # downward in places, the very edge frost.
    shade = 0.15 + 0.75 * low + 0.2 * reach + (0.18 if hash01(ix, 0, 41) < 0.35 else 0.0)
    if edge < 1.0:
        shade += 0.4
    if shade > 1.05:
        colour = FROST_LIGHT
    elif shade > 0.8:
        colour = FROST
    elif shade > 0.55:
        colour = MEMBRANE_GREY
    elif shade > 0.35:
        colour = SLATE_MID
    else:
        colour = SLATE
    frost = shade
    alpha = 228 if edge > 1.5 else 200
    glows = frost > 0.7 and hash01(ix, iy, 31) < 0.05
    return colour, alpha, glows


def paint(skin, glow, boxes, faces):
    """Paints the wings' boxes on `skin` (and their glints on `glow`), `faces(box)` giving each face's rect."""
    rnd = random.Random(82)
    poly = membrane_polygon()

    def px(im, x, y, c, a=255):
        im.putpixel((x, y), (*c, a))

    for b in boxes:
        f = faces(b)
        m = b.mat
        if m == "membrane":
            right = getattr(b, "side", -1) < 0
            for face, flip in (("front", False), ("back", True)):
                x0, y0, w, h = f[face]
                for j in range(h):
                    for i in range(w):
                        # Column i is model x0+i on the front, mirrored on the back; the left wing's sheet is the
                        # right's mirrored, so in the right wing's hand frame its x is negated.
                        mx = (b.x + i) if not flip else (b.x + b.w - 1 - i)
                        hx = mx + 0.5 if right else -(mx + 0.5)
                        hy = b.y + j + 0.5
                        texel = membrane_texel(hx, hy, poly)
                        if texel is None:
                            continue
                        colour, alpha, glows = texel
                        px(skin, x0 + i, y0 + j, colour, alpha)
                        if glows:
                            px(glow, x0 + i, y0 + j, GLOW, 200)
            continue
        for face, (x0, y0, w, h) in f.items():
            for j in range(h):
                for i in range(w):
                    r = rnd.random()
                    if m in ("bone", "spar", "joint"):
                        c = BONE if r < 0.55 else BONE_SHADE if r < 0.9 else BONE_DARK
                        if m == "joint" and r > 0.93:
                            px(glow, x0 + i, y0 + j, GLOW, 180)
                    elif m == "spike":
                        c = BONE if j < h / 2 else BONE_SHADE
                        if j == 0:
                            px(glow, x0 + i, y0 + j, GLOW, 200)
                    else:  # icicle
                        c = ICE if j < h - 1 else FROST_LIGHT
                        if j == h - 1:
                            px(glow, x0 + i, y0 + j, GLOW, 160)
                    px(skin, x0 + i, y0 + j, c, 235 if m == "icicle" else 255)
