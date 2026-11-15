"""Draws Dread Patch's spell and effect icons and its dread fire texture into src/main/resources.
Python 3 with Pillow.

Run from anywhere: python3 tools/sprites/make_icons.py. Deterministic (fixed seeds), so re-running
gives identical files. Her palette, from the owner's notes: charcoal #14161D, bone-ash #D8DEE9,
abyssal indigo #1E2A4A; her dread fire is the cyan of Ice and Fire's dread torch and her eye glow,
#00CBFF. Spell icons are 16x16 and full-bleed like Iron's; effect icons are 18x18 on transparency
like vanilla's. The dread fire is an animated block-atlas texture (16x16 frames, with its .mcmeta),
drawn by the Rimefire Crown's renderer.
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


def main():
    out = {
        "gui/spell_icons/withering_rime_bolt.png": withering_rime_bolt(),
        "mob_effect/withering_rime.png": withering_rime_effect(),
        "gui/spell_icons/rimefire_crown.png": rimefire_crown(),
        "mob_effect/rimebound.png": rimebound_effect(),
        "block/dread_fire_0.png": dread_fire(31),
        "block/dread_fire_1.png": dread_fire(8),
    }
    for rel, im in out.items():
        path = ROOT / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        im.save(path)
        print("wrote", path.relative_to(ROOT.parents[5]))
        if rel.startswith("block/dread_fire"):
            meta = path.with_name(path.name + ".mcmeta")
            meta.write_text(json.dumps({"animation": {"frametime": FIRE_FRAME_TIME}}, indent=2) + "\n")
            print("wrote", meta.relative_to(ROOT.parents[5]))


if __name__ == "__main__":
    main()
