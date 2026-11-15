"""Draws Dread Patch's spell and effect icons into src/main/resources. Python 3 with Pillow.

Run from anywhere: python3 tools/sprites/make_icons.py. Deterministic (fixed seeds), so re-running
gives identical files. Her palette, from the owner's notes: charcoal #14161D, bone-ash #D8DEE9,
abyssal indigo #1E2A4A. Spell icons are 16x16 and full-bleed like Iron's; effect icons are 18x18 on
transparency like vanilla's.
"""
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


def spire(im, x, base, height, width):
    """A black dread-glass spire rising from row `base`: soot body, indigo glint on its left edge, bone tip."""
    top = base - height + 1
    for y in range(top, base + 1):
        w = max(1, round(width * (y - top + 1) / height))  # narrows to a point
        x0 = x - w // 2
        for xi in range(x0, x0 + w):
            im.putpixel((xi, y), rgba(DEEP if xi == x0 and w > 1 else SOOT))
    im.putpixel((x, top), rgba(BONE_LIGHT))
    if height > 4:
        im.putpixel((x, top + 1), rgba(BONE))


def barrow_glass_tomb():
    im = dread_background(16, 11)
    # Frost-crusted ground.
    for x in range(16):
        im.putpixel((x, 15), rgba(BONE_DIM if x % 3 else BONE))
        if x % 2 == 0:
            im.putpixel((x, 14), rgba(BONE_DIM))
    # The ring of spires, back row shorter, front row tall.
    for x, h, w in [(2, 7, 2), (13, 8, 2), (5, 10, 3), (10, 11, 3), (8, 13, 3)]:
        spire(im, x, 14, h, w)
    return im


def barrow_glass_effect():
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for x, h, w in [(4, 9, 3), (13, 10, 3), (9, 14, 4)]:
        spire(im, x, 16, h, w)
    for x in range(2, 16):
        im.putpixel((x, 17), rgba(BONE_DIM))
    # Charcoal outline so it reads on any background.
    pts = [(x, y) for y in range(18) for x in range(18) if im.getpixel((x, y))[3]]
    for x, y in pts:
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + ox, y + oy)
            if 0 <= q[0] < 18 and 0 <= q[1] < 18 and im.getpixel(q)[3] == 0:
                im.putpixel(q, rgba(CHARCOAL))
    return im


def main():
    out = {
        "gui/spell_icons/withering_rime_bolt.png": withering_rime_bolt(),
        "mob_effect/withering_rime.png": withering_rime_effect(),
        "gui/spell_icons/barrow_glass_tomb.png": barrow_glass_tomb(),
        "mob_effect/barrow_glass.png": barrow_glass_effect(),
    }
    for rel, im in out.items():
        path = ROOT / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        im.save(path)
        print("wrote", path.relative_to(ROOT.parents[5]))


if __name__ == "__main__":
    main()
