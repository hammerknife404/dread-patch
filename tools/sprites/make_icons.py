"""Draws Dread Patch's spell and effect icons and its dread fire texture into src/main/resources.
Python 3 with Pillow.

Run from anywhere: python3 tools/sprites/make_icons.py. Deterministic (fixed seeds), so re-running
gives identical files. Her palette, from the owner's notes: charcoal #14161D, bone-ash #D8DEE9,
abyssal indigo #1E2A4A; her dread fire is the cyan of Ice and Fire's dread torch and her eye glow,
#00CBFF. Spell icons are 16x16 and full-bleed like Iron's; effect icons are 18x18 on transparency
like vanilla's. The dread fire and the Dread Gate's face are animated block-atlas textures (16x16
frames, each with its .mcmeta): the fire drawn by the Rimefire Crown's renderer, the gate's face by its
block. Syndrigossa's egg and the Dread Queen's regalia are 16x16 item textures on transparency, the
regalia in her outfit's colours from Ice and Fire's texture of her.
"""
import json
import math
import pathlib
import random

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2] / "src/main/resources/assets/dreadpatch/textures"

CHARCOAL = (0x14, 0x16, 0x1D)
SOOT = (0x0B, 0x0C, 0x10)
INDIGO = (0x1E, 0x2A, 0x4A)
DEEP = (0x2A, 0x38, 0x5E)
BONE = (0xD8, 0xDE, 0xE9)
BONE_LIGHT = (0xF4, 0xF6, 0xFA)
BONE_DIM = (0x8C, 0x96, 0xAB)
# Ice spikes, after vanilla's packed ice.
ICE_LIGHT = (0xE4, 0xF4, 0xFF)
ICE = (0xA9, 0xCD, 0xF5)
ICE_DIM = (0x74, 0xA0, 0xDA)
ICE_DARK = (0x45, 0x6E, 0xAE)
# Dread fire, white-hot core out to soot-black tips.
FIRE_CORE = (0xE6, 0xFD, 0xFF)
FIRE_BRIGHT = (0x6C, 0xE8, 0xFF)
DREAD_CYAN = (0x00, 0xCB, 0xFF)
FIRE_DEEP = (0x00, 0x8C, 0xD2)
FIRE_DARK = (0x16, 0x4A, 0x96)


def rgba(c, a=255):
    return (*c, a)


def dread_background(size, seed):
    """Iron's icons sit on a dithered dark field; hers is charcoal shading into indigo."""
    rnd = random.Random(seed)
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            t = (x + (size - 1 - y)) / (2 * (size - 1))  # lighter toward the top-right
            pick = rnd.random()
            if pick < 0.55 - 0.35 * t:
                c = CHARCOAL
            elif pick < 0.92 - 0.2 * t:
                c = INDIGO
            else:
                c = DEEP
            im.putpixel((x, y), rgba(c))
    return im


def withering_rime_bolt():
    im = dread_background(16, 7)
    # A thick bolt, bottom-left to top-right: bright core, bone body, dim edge; soot trails behind.
    for i in range(16):
        for j in range(16):
            along = (i + (15 - j)) / 2.0          # position along the diagonal
            across = (i - (15 - j)) / 2.0         # distance from it
            if along < 2.5 or along > 14:
                continue
            width = 0.6 + 1.2 * min(1.0, (along - 2.5) / 6.0)   # tapers toward the tail
            d = abs(across)
            if along < 5.5 and d <= width + 0.8:
                im.putpixel((i, j), rgba(SOOT))
            elif d <= width * 0.35:
                im.putpixel((i, j), rgba(BONE_LIGHT))
            elif d <= width * 0.8:
                im.putpixel((i, j), rgba(BONE))
            elif d <= width + 0.6:
                im.putpixel((i, j), rgba(BONE_DIM))
    # Rime flecks thrown off the tip.
    for x, y in [(15, 0), (13, 0), (15, 3), (11, 1), (14, 2)]:
        im.putpixel((x, y), rgba(BONE))
    return im


def withering_rime_effect():
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    cx = cy = 8
    arms = [(0, -1), (0, 1), (-1, 0), (1, 0), (-1, -1), (1, 1), (1, -1), (-1, 1)]
    pts = {}
    for dx, dy in arms:
        reach = 6 if 0 in (dx, dy) else 4
        for r in range(1, reach + 1):
            pts[(cx + dx * r, cy + dy * r)] = BONE if r < reach else BONE_LIGHT
        if 0 in (dx, dy):  # side barbs on the long arms, like frost
            bx, by = cx + dx * 4, cy + dy * 4
            for sx, sy in ((dy, dx), (-dy, -dx)):
                pts[(bx + sx, by + sy)] = BONE_DIM
    # Outline everything in charcoal so it reads on any background.
    for (x, y) in list(pts):
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + ox, y + oy)
            if q not in pts and 0 <= q[0] < 18 and 0 <= q[1] < 18:
                im.putpixel(q, rgba(CHARCOAL))
    for (x, y), c in pts.items():
        im.putpixel((x, y), rgba(c))
    # The withered heart: indigo core, soot centre.
    for x, y in ((cx, cy), (cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
        im.putpixel((x, y), rgba(INDIGO))
    im.putpixel((cx, cy), rgba(SOOT))
    return im


def outline(im):
    """Charcoal outline round everything drawn, so it reads on any background."""
    w, h = im.size
    pts = [(x, y) for y in range(h) for x in range(w) if im.getpixel((x, y))[3]]
    for x, y in pts:
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + ox, y + oy)
            if 0 <= q[0] < w and 0 <= q[1] < h and im.getpixel(q)[3] == 0:
                im.putpixel(q, rgba(CHARCOAL))


def ice_spike(im, x, base, height, width):
    """An ice spike rising from row `base` to a point: lit left face, shaded right, a bright edge."""
    top = base - height + 1
    for y in range(top, base + 1):
        w = max(1, round(width * (y - top + 1) / height))
        x0 = x - w // 2
        for xi in range(x0, x0 + w):
            if w == 1 or xi == x0:
                c = ICE_LIGHT
            elif xi < x0 + (w + 1) // 2:
                c = ICE
            elif xi < x0 + w - 1:
                c = ICE_DIM
            else:
                c = ICE_DARK
            im.putpixel((xi, y), rgba(c))


def flame(im, x, base, height):
    """A small tongue of dread fire on row `base`: white heart, cyan body, deep blue edge."""
    body = [FIRE_CORE, FIRE_BRIGHT, DREAD_CYAN, FIRE_DEEP, FIRE_DARK]
    for i in range(height):
        im.putpixel((x, base - i), rgba(body[min(len(body) - 1, i * len(body) // height)]))
    for dx in (-1, 1):
        for i in range(max(1, height - 2)):
            if 0 <= x + dx < im.width:
                im.putpixel((x + dx, base - i), rgba(DREAD_CYAN if i == 0 else FIRE_DEEP))


def rimefire_crown():
    im = dread_background(16, 11)
    # Smoke pooled on the ground, soot and cyan.
    rnd = random.Random(23)
    for x in range(16):
        for y in (14, 15):
            if y == 15 or rnd.random() < 0.6:
                im.putpixel((x, y), rgba(DREAD_CYAN if rnd.random() < 0.4 else SOOT))
    # The crown of spikes, back row shorter, front row tall, with dread fire between them.
    for x, h, w in [(2, 7, 2), (13, 8, 2), (5, 10, 3), (10, 11, 3), (8, 13, 4)]:
        ice_spike(im, x, 14, h, w)
    for x, h in [(0, 3), (3, 4), (7, 3), (12, 4), (15, 3)]:
        flame(im, x, 14, h)
    return im


def rimebound_effect():
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for x, h, w in [(4, 9, 3), (13, 10, 3), (9, 14, 4)]:
        ice_spike(im, x, 16, h, w)
    for x, h in [(2, 4), (6, 3), (11, 4), (15, 3)]:
        flame(im, x, 16, h)
    for x in range(1, 17):
        im.putpixel((x, 17), rgba(DREAD_CYAN if x % 3 == 0 else SOOT))
    outline(im)
    return im


def robed_figure(im, x, light, mid, dark):
    """A small robed figure, 5 wide, head at row 2 and hem at row 14, lit from the left."""
    rows = [(1, 3), (1, 3), (1, 3), (0, 5), (0, 5), (0, 5), (0, 5), (0, 5), (1, 3), (1, 3), (0, 5), (0, 5)]
    for i, (start, width) in enumerate(rows):
        y = 2 + i + (1 if i >= 3 else 0)
        for k in range(width):
            c = light if k == 0 else (dark if k == width - 1 else mid)
            im.putpixel((x + start + k, y), rgba(c))


def barrow_step():
    im = dread_background(16, 19)
    # Her husk left behind in rime-glass, cracked; her, bone-white, stepped away; the cold between.
    robed_figure(im, 1, ICE_LIGHT, ICE, ICE_DIM)
    for x, y in [(3, 7), (4, 8), (3, 9), (4, 11)]:
        im.putpixel((x, y), rgba(ICE_DARK))
    robed_figure(im, 10, BONE_LIGHT, BONE, BONE_DIM)
    for x in (11, 13):
        im.putpixel((x, 3), rgba(DREAD_CYAN))
    for y, x0, x1 in [(7, 6, 9), (9, 7, 9), (11, 6, 9), (13, 7, 8)]:
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), rgba(DREAD_CYAN if x > (x0 + x1) // 2 else FIRE_DEEP))
    return im


def summon_court():
    """Her court rising: three dread figures climbing out of cracked ground lit cyan from below, the
    middle one bone-white and risen to its waist, the two beside it slate, risen to their shoulders,
    all with her cyan in their eyes; soul sparks above."""
    im = dread_background(16, 53)
    for y in range(12, 16):
        for x in range(16):
            im.putpixel((x, y), rgba(SOOT if (x + y) % 3 else CHARCOAL))
    for x, y in [(1, 13), (2, 14), (3, 14), (6, 13), (7, 14), (8, 15), (11, 13), (12, 14), (14, 15)]:
        im.putpixel((x, y), rgba(FIRE_DEEP))

    def figure(x, top, light, mid, dark, eyes):
        # A head 3 wide, then shoulders 5 wide down to the ground at row 12.
        for y in range(top, 12):
            xs = list(range(x + 1, x + 4)) if y < top + 3 else list(range(x, x + 5))
            for k, px in enumerate(xs):
                im.putpixel((px, y), rgba(light if k == 0 else dark if k == len(xs) - 1 else mid))
        for px in (x + 1, x + 3):
            im.putpixel((px, top + 1), rgba(eyes))
        # Where it breaks the ground, the earth glows.
        for px in range(x, x + 5):
            im.putpixel((px, 12), rgba(DREAD_CYAN if px in (x + 1, x + 3) else FIRE_DEEP))

    slate = (0x5C, 0x66, 0x7E)
    figure(0, 7, BONE_DIM, slate, INDIGO, DREAD_CYAN)
    figure(10, 7, BONE_DIM, slate, INDIGO, DREAD_CYAN)
    figure(5, 3, BONE_LIGHT, BONE, BONE_DIM, DREAD_CYAN)
    for x, y in [(2, 4), (13, 3), (8, 1), (11, 5)]:
        im.putpixel((x, y), rgba(DREAD_CYAN if (x + y) % 2 else FIRE_BRIGHT))
    return im


def create_rimefrost_dragon():
    """A dragon's skull in profile rising from cracked ground lit cyan: bone-white, horned, its eye
    socket burning with her cyan, soul sparks above."""
    im = dread_background(16, 61)
    for y in range(13, 16):
        for x in range(16):
            im.putpixel((x, y), rgba(SOOT if (x + 2 * y) % 3 else CHARCOAL))
    for x, y in [(2, 14), (3, 15), (6, 14), (9, 15), (12, 14), (13, 15)]:
        im.putpixel((x, y), rgba(FIRE_DEEP))
    # Row by row: (y, first x, last x) of bone, facing right, horns swept back.
    rows = [(2, 2, 3), (3, 3, 4), (4, 3, 6), (5, 3, 8), (6, 4, 11), (7, 4, 13), (8, 5, 14), (9, 5, 13),
            (10, 6, 13), (11, 6, 12), (12, 5, 11)]
    for y, x0, x1 in rows:
        for x in range(x0, x1 + 1):
            shade = BONE_LIGHT if y <= 5 or x == x0 else BONE if y <= 9 else BONE_DIM
            im.putpixel((x, y), rgba(shade))
    # Eye socket, nostril, the gap of the jaw with teeth.
    im.putpixel((7, 7), rgba(DREAD_CYAN))
    im.putpixel((8, 7), rgba(FIRE_BRIGHT))
    im.putpixel((13, 7), rgba(CHARCOAL))
    for x in range(8, 14):
        im.putpixel((x, 10), rgba(SOOT if x % 2 else BONE_LIGHT))
    for x in range(5, 12):
        im.putpixel((x, 12), rgba(DREAD_CYAN if x in (6, 9) else FIRE_DEEP))
    for x, y in [(1, 6), (14, 3), (11, 1), (2, 10)]:
        im.putpixel((x, y), rgba(DREAD_CYAN if (x + y) % 2 else FIRE_BRIGHT))
    return im


def line(im, a, b, c):
    """A 1-pixel line from a to b (Bresenham)."""
    (x0, y0), (x1, y1) = a, b
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        im.putpixel((x0, y0), rgba(c))
        if (x0, y0) == (x1, y1):
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def brittle_effect():
    """A chunk of ice split by a soot crack, chips flying off it: armor gone brittle after a shatter."""
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    cx, top, bottom = 8, 2, 15
    for y in range(top, bottom + 1):
        half = min(y - top, bottom - y, 5)
        for x in range(cx - half, cx + half + 1):
            if x < cx - half + 2:
                c = ICE_LIGHT
            elif x <= cx:
                c = ICE
            elif x < cx + half:
                c = ICE_DIM
            else:
                c = ICE_DARK
            im.putpixel((x, y), rgba(c))
    for a, b in [((8, 2), (7, 5)), ((7, 5), (9, 8)), ((9, 8), (7, 11)), ((7, 11), (8, 15)), ((9, 8), (12, 9))]:
        line(im, a, b, SOOT)
    for x, y in [(15, 4), (16, 3), (14, 2), (2, 13), (1, 15)]:
        im.putpixel((x, y), rgba(ICE_LIGHT if (x + y) % 2 else DREAD_CYAN))
    outline(im)
    return im


def syndrigossa_egg():
    """Her egg (an item, 16x16 on transparency): a soot-black shell lit from the top left, bone-ash
    scale ridges across it, and a crack down it glowing her dread cyan."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    top, bottom, cx = 1, 14, 7.5
    for y in range(top, bottom + 1):
        v = (y - top) / (bottom - top)
        half = 5.6 * math.sqrt(max(0.0, 1.0 - (2.0 * v - 1.0) ** 2)) * (0.78 + 0.3 * v)
        for x in range(16):
            dx = x - cx
            if abs(dx) > half:
                continue
            light = -(dx / 6.0) - (y - 6.5) / 8.0  # toward the top left
            if light > 0.55:
                c = DEEP
            elif light > 0.1:
                c = INDIGO
            elif light > -0.45:
                c = CHARCOAL
            else:
                c = SOOT
            # Scale ridges: short bone-ash arcs on alternate rows.
            if y % 3 == 1 and (x + y // 3) % 3 == 0 and abs(dx) < half - 1:
                c = BONE_DIM if light < 0.3 else BONE
            im.putpixel((x, y), rgba(c))
    for a, b in [((8, 2), (7, 5)), ((7, 5), (9, 8)), ((9, 8), (8, 11))]:
        line(im, a, b, DREAD_CYAN)
    for x, y in [(8, 2), (9, 8)]:
        im.putpixel((x, y), rgba(FIRE_CORE))
    im.putpixel((5, 4), rgba(BONE_LIGHT))
    outline(im)
    return im


def whiteout_dominion():
    """The storm: a charcoal sky closing into bone-grey whiteout toward the ground, her hooded shadow
    in it with her two cyan eyes, and snow driven down and to the left across everything."""
    rnd = random.Random(41)
    im = Image.new("RGBA", (16, 16))
    bands = [CHARCOAL, CHARCOAL, CHARCOAL, INDIGO, INDIGO, DEEP, DEEP, BONE_DIM, BONE_DIM, BONE]
    for y in range(16):
        for x in range(16):
            band = (y + (x % 3 == 0)) * (len(bands) - 1) / 16.0
            i = int(band)
            # Dithered between neighbouring bands, like Iron's fields.
            if rnd.random() < band - i and i + 1 < len(bands):
                i += 1
            im.putpixel((x, y), rgba(bands[i]))
    # Her hood and shoulders, a soot shadow in the storm.
    for y, x0, x1 in [(4, 6, 9), (5, 5, 10), (6, 5, 10), (7, 5, 10), (8, 5, 10), (9, 5, 10),
                      (10, 4, 11), (11, 3, 12), (12, 3, 12)]:
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), rgba(SOOT))
    for x in (6, 9):
        im.putpixel((x, 7), rgba(DREAD_CYAN))
    # Streaks of snow on the wind: short diagonals, brightest at their heads.
    for x0, y0, n in [(15, 0, 3), (10, 0, 3), (5, 1, 3), (13, 4, 3), (3, 6, 3),
                      (12, 9, 3), (7, 12, 3), (15, 12, 3), (2, 13, 3)]:
        for k in range(n):
            x, y = x0 - k, y0 + k
            if 0 <= x < 16 and 0 <= y < 16:
                im.putpixel((x, y), rgba(BONE_LIGHT if k == n - 1 else BONE))
    return im


def whiteout_effect():
    """Three gusts of wind, each curling over at its head, with flakes caught between them."""
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y, x0, x1 in [(5, 1, 11), (10, 4, 14), (15, 2, 10)]:
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), rgba(BONE if x < x1 - 3 else BONE_LIGHT))
        for x, dy in [(x1 + 1, -1), (x1 + 1, -2), (x1, -3), (x1 - 1, -2)]:
            im.putpixel((x, y + dy), rgba(BONE_LIGHT))
    for x, y in [(15, 3), (2, 8), (13, 14)]:
        im.putpixel((x, y), rgba(ICE_LIGHT))
    outline(im)
    return im


def summon_dread_phantoms():
    """Two dread phantoms diving out of the dark, wings swept up: bone leading edges, torn slate
    membrane, her cyan in their eyes; a big one across the top and a small one below; frost flecks."""
    im = dread_background(16, 71)
    palette = {
        "B": BONE_LIGHT, "b": BONE, "d": BONE_DIM,
        "M": (0x6E, 0x7A, 0x96), "m": (0x4C, 0x58, 0x74),
        "h": (0x2B, 0x32, 0x47), "c": DREAD_CYAN, "f": ICE_LIGHT,
    }
    art = [
        "B..............B",
        "Mb............bM",
        ".MbB........BbM.",
        ".mMMbB....BbMMm.",
        "..m.MMbcdcbMM.m.",
        "...m.mMhhhMm.m..",
        "......mhhhm.....",
        ".......dhd......",
        "........d.......",
        "..b...b....f....",
        "..Mbcb..........",
        "...Mhd.......f..",
        "....d...........",
        "..............f.",
        "........f.......",
        "................",
    ]
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch in palette:
                im.putpixel((x, y), rgba(palette[ch]))
    return im


def rimefire_blast():
    """A cone of cold dread fire pouring up and right from a bone-white hand at the lower left: white-hot
    at the hand, her cyan through the middle, deep blue and soot at its ragged edge."""
    im = dread_background(16, 83)
    rnd = random.Random(41)
    ox, oy = 2.5, 13.5
    ax, ay = 1 / math.sqrt(2), -1 / math.sqrt(2)
    for y in range(16):
        for x in range(16):
            dx, dy = x - ox, y - oy
            along = dx * ax + dy * ay
            across = abs(-dx * ay + dy * ax)
            if along < 1.0 or along > 15.5:
                continue
            width = 0.6 + along * 0.42
            if across > width + rnd.random() * 0.8:
                continue
            t = along / 15.5
            edge = across / max(width, 0.01)
            if t < 0.2 and edge < 0.6:
                c = FIRE_CORE
            elif edge < 0.45:
                c = FIRE_BRIGHT if t < 0.55 else DREAD_CYAN
            elif edge < 0.85:
                c = DREAD_CYAN if t < 0.5 else FIRE_DEEP
            else:
                c = SOOT if rnd.random() < 0.4 else FIRE_DARK
            im.putpixel((x, y), rgba(c))
    for x, y in [(1, 14), (2, 14), (1, 15), (2, 13), (3, 14)]:
        im.putpixel((x, y), rgba(BONE if (x + y) % 2 else BONE_LIGHT))
    return im


def rimefrost_effect():
    """A six-armed flake of rime in ice blues, barbed, round her cyan: the cold without the withering."""
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    cx = cy = 8
    pts = {}
    for i in range(6):
        a = math.pi / 2 + i * math.pi / 3
        for r in range(1, 7):
            x, y = round(cx + math.cos(a) * r), round(cy - math.sin(a) * r)
            pts[(x, y)] = ICE_LIGHT if r >= 6 else ICE if r >= 3 else ICE_DIM
        for side in (-1, 1):
            b = a + side * math.pi / 3
            bx, by = cx + math.cos(a) * 4, cy - math.sin(a) * 4
            q = (round(bx + math.cos(b) * 1.5), round(by - math.sin(b) * 1.5))
            pts.setdefault(q, ICE)
    for (x, y), c in pts.items():
        im.putpixel((x, y), rgba(c))
    for x, y in ((cx, cy), (cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
        im.putpixel((x, y), rgba(FIRE_DEEP))
    im.putpixel((cx, cy), rgba(DREAD_CYAN))
    outline(im)
    return im


def periodic_noise(width, height, cells_x, cells_y, rnd):
    """Smooth value noise that wraps in both directions, so an animation scrolled through it loops."""
    grid = [[rnd.random() for _ in range(cells_x)] for _ in range(cells_y)]

    def sample(x, y):
        gx, gy = x / width * cells_x, y / height * cells_y
        ix, iy = math.floor(gx), math.floor(gy)
        fx, fy = gx - ix, gy - iy
        sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        x0, x1 = ix % cells_x, (ix + 1) % cells_x
        y0, y1 = iy % cells_y, (iy + 1) % cells_y
        top = grid[y0][x0] * (1 - sx) + grid[y0][x1] * sx
        bottom = grid[y1][x0] * (1 - sx) + grid[y1][x1] * sx
        return top * (1 - sy) + bottom * sy

    return sample


FIRE_FRAMES = 16
FIRE_FRAME_TIME = 2


def dread_fire(seed):
    """16x16 frames of cold fire: tongues licking upward, white-hot low down, soot-black at the tips.
    Each frame scrolls 2 rows through noise that wraps every 32 (tall cells, so it makes tongues
    rather than blobs), so the 16 frames loop seamlessly."""
    rnd = random.Random(seed)
    coarse = periodic_noise(16, 32, 5, 2, rnd)
    fine = periodic_noise(16, 32, 8, 8, rnd)
    bands = [(1.15, FIRE_CORE), (0.9, FIRE_BRIGHT), (0.66, DREAD_CYAN), (0.48, FIRE_DEEP), (0.34, FIRE_DARK)]
    im = Image.new("RGBA", (16, 16 * FIRE_FRAMES), (0, 0, 0, 0))
    for t in range(FIRE_FRAMES):
        frame = {}
        for y in range(16):
            rise = (15 - y) / 15.0
            for x in range(16):
                sy = (y + 2 * t) % 32 + 0.5
                n = 0.5 + (0.8 * coarse(x + 0.5, sy) + 0.2 * fine(x + 0.5, sy) - 0.5) * 1.6
                heat = 0.4 + 1.3 * n - 1.05 * rise - 0.3 * ((x - 7.5) / 7.5) ** 4
                for floor, c in bands:
                    if heat > floor:
                        frame[(x, y)] = c
                        break
        # Drop specks: anything with fewer than two lit neighbours.
        for (x, y) in list(frame):
            if sum((x + dx, y + dy) in frame for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))) < 2:
                del frame[(x, y)]
        # Soot on the tips of the tongues, in the upper part of the frame.
        for (x, y) in list(frame):
            if y < 9 and (x, y - 1) not in frame and frame[(x, y)] in (FIRE_DARK, FIRE_DEEP):
                frame[(x, y)] = CHARCOAL
        for (x, y), c in frame.items():
            im.putpixel((x, 16 * t + y), rgba(c))
    return im


GATE_FRAMES = 32
GATE_FRAME_TIME = 2


def dread_gate(seed):
    """32 frames of a Dread Gate's open face (16x16, translucent): a dark swirl of soot and indigo
    crossed by thin cold-cyan wisps (where one noise passes its middle: its contour lines, so they're
    spread evenly over every frame) with a few ice-white glints. The wisps' noise scrolls up a row a
    frame through 32 rows, the swirl's sideways half a column a frame through 16, so both have gone
    round once in 32 frames and it loops seamlessly. Seed and scale picked for an even amount of
    wisp on every frame (about a fifth of it, give or take 3%)."""
    rnd = random.Random(seed)
    wisps = periodic_noise(16, 32, 5, 8, rnd)
    swirl = periodic_noise(16, 16, 3, 3, rnd)
    glint = periodic_noise(16, 32, 8, 12, rnd)
    im = Image.new("RGBA", (16, 16 * GATE_FRAMES), (0, 0, 0, 0))
    for t in range(GATE_FRAMES):
        for y in range(16):
            for x in range(16):
                w = wisps(x + 0.5, (y + t) % 32 + 0.5)
                sw = swirl((x + 0.5 * t) % 16 + 0.5, y + 0.5)
                # Bend the wisps' contours with the swirl, then take how close they are to the middle.
                ridge = 1.0 - abs(2.0 * (0.7 * w + 0.3 * sw) - 1.0) * 3.6
                if ridge > 0.82:
                    colour, alpha = FIRE_BRIGHT, 245
                elif ridge > 0.55:
                    colour, alpha = DREAD_CYAN, 235
                elif ridge > 0.25:
                    colour, alpha = FIRE_DARK, 220
                elif sw > 0.55:
                    colour, alpha = DEEP, 210
                elif sw > 0.4:
                    colour, alpha = INDIGO, 205
                else:
                    colour, alpha = SOOT, 200
                if ridge > 0.55 and glint(x + 0.5, (y + 2 * t) % 32 + 0.5) > 0.8:
                    colour, alpha = ICE_LIGHT, 250
                im.putpixel((x, 16 * t + y), rgba(colour, alpha))
    return im


# The Dread Queen's regalia, in her outfit's colours as Ice and Fire's texture of her has them.
REGALIA = {
    ".": None,
    "O": (0x0B, 0x0C, 0x10),  # outline
    "S": (0xDF, 0xE6, 0xF2), "s": (0xA0, 0xAB, 0xBF), "d": (0x6B, 0x73, 0x80),  # her silver
    "I": (0x7F, 0xB2, 0xEE), "i": (0xC9, 0xE3, 0xFF),  # her crown's ice
    "C": (0x00, 0xF4, 0xFF),  # her pendant's gem
    "K": (0x15, 0x17, 0x1B), "k": (0x29, 0x2C, 0x33), "m": (0x3A, 0x3E, 0x47), "g": (0x5A, 0x60, 0x6C),  # her black, banded
    "B": (0x3F, 0x71, 0x93), "b": (0x2D, 0x51, 0x6C), "c": (0x2C, 0x58, 0x75), "n": (0x1D, 0x37, 0x4C),  # her blues
}


def mirrored_icon(left_half):
    """A 16x16 item icon from its left half (8 columns a row, 16 rows), mirrored, in REGALIA's colours."""
    assert len(left_half) == 16 and all(len(row) == 8 for row in left_half), left_half
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(left_half):
        for x, ch in enumerate(row + row[::-1]):
            if REGALIA[ch] is not None:
                im.putpixel((x, y), rgba(REGALIA[ch]))
    return im


def queen_crown():
    """Her crown: a silver band under spikes of ice, the tallest at its middle, her gem hung beneath."""
    return mirrored_icon([
        "........",
        "........",
        "........",
        ".......i",
        "...i...I",
        "...I..iI",
        ".i.I..II",
        ".IiIi.II",
        ".IIIIiII",
        "OSSSSSSS",
        "OsSSSsSC",
        "OddssssC",
        ".OOdddds",
        "......Od",
        ".......O",
        "........",
    ])


def queen_bodice():
    """Her bodice: black, her silver pauldrons at the shoulders, blue straps, a silver V of necklace down to her gem, a blue sash."""
    return mirrored_icon([
        "........",
        ".OOOO...",
        "OSSSSO..",
        "OSsssdOO",
        "OsdddBsk",
        ".OOOOBks",
        "...OkBkC",
        "...OKBkk",
        "...OKBkk",
        "...OKBkk",
        "...ObBBB",
        "...OKmkk",
        "...OKmkk",
        "...OKkmk",
        "....OOOO",
        "........",
    ])


def queen_robe():
    """Her robes: a silver girdle, then deep blue, flaring to a ragged hem."""
    return mirrored_icon([
        "........",
        "..OOOOOO",
        "..OsSSSS",
        "..ObBBBB",
        "..ObBBcc",
        "..ObBccO",
        "..ObBcnO",
        "..ObBcnO",
        ".ObBccnO",
        ".ObBccnO",
        ".ObBcnnO",
        "ObBBcnnO",
        "ObBccnnO",
        "OdnndnnO",
        ".O.OO.O.",
        "........",
    ])


def queen_greaves():
    """Her greaves: her legs' black wraps, banded in grey."""
    return mirrored_icon([
        "........",
        "........",
        "........",
        "........",
        "........",
        "........",
        "..OOOOO.",
        "..OkgmO.",
        "..OkkkO.",
        "..OgggO.",
        "..OkkmO.",
        "OOOkkkO.",
        "OgmkgmO.",
        "OkkkkkO.",
        "OOOOOOO.",
        "........",
    ])


def main():
    out = {
        "gui/spell_icons/withering_rime_bolt.png": withering_rime_bolt(),
        "mob_effect/withering_rime.png": withering_rime_effect(),
        "gui/spell_icons/rimefire_crown.png": rimefire_crown(),
        "mob_effect/rimebound.png": rimebound_effect(),
        "mob_effect/brittle.png": brittle_effect(),
        "gui/spell_icons/barrow_step.png": barrow_step(),
        "gui/spell_icons/whiteout_dominion.png": whiteout_dominion(),
        "gui/spell_icons/summon_court.png": summon_court(),
        "gui/spell_icons/create_rimefrost_dragon.png": create_rimefrost_dragon(),
        "mob_effect/whiteout.png": whiteout_effect(),
        "gui/spell_icons/summon_dread_phantoms.png": summon_dread_phantoms(),
        "mob_effect/rimefrost.png": rimefrost_effect(),
        "gui/spell_icons/rimefire_blast.png": rimefire_blast(),
        "item/syndrigossa_egg.png": syndrigossa_egg(),
        "item/dread_queen_crown.png": queen_crown(),
        "item/dread_queen_bodice.png": queen_bodice(),
        "item/dread_queen_robe.png": queen_robe(),
        "item/dread_queen_greaves.png": queen_greaves(),
        "block/dread_fire_0.png": dread_fire(31),
        "block/dread_fire_1.png": dread_fire(8),
        "block/dread_gate.png": dread_gate(17),
    }
    for rel, im in out.items():
        path = ROOT / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        im.save(path)
        print("wrote", path.relative_to(ROOT.parents[5]))
        frame_time = FIRE_FRAME_TIME if rel.startswith("block/dread_fire") else GATE_FRAME_TIME if rel == "block/dread_gate.png" else None
        if frame_time is not None:
            meta = path.with_name(path.name + ".mcmeta")
            meta.write_text(json.dumps({"animation": {"frametime": frame_time}}, indent=2) + "\n")
            print("wrote", meta.relative_to(ROOT.parents[5]))


if __name__ == "__main__":
    main()
