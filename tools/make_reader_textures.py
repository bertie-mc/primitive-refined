"""
Textures for the External Reader, derived from Create's threshold switch.

Every transform here is a remap of Create's own pixels, never a repaint: the front keeps
Create's shading and only moves hue or value, so the block still reads as part of Create's
set. Derived art is shipped outright with permission - see NOTICE.

Regions, read straight off Create's texture rather than assumed:
  front  threshold_switch_front  greys at cols/rows 3-12, dark red square at 6-9
  side   threshold_switch/level_N  the indicator is a 4x10 checkerboard at cols 6-9,
                                   rows 3-12
"""
import colorsys, json, os, random

from PIL import Image

CREATE = ("C:/Users/berlord/Documents/claude_base/bertie/mods/primitive-refined"
          "/tools/extracted/assets/create/textures/block")
OUT = ("C:/Users/berlord/Documents/claude_base/bertie/mods/primitive-refined"
       "/src/main/resources/assets/primitive_refined/textures/block")

# The mod's own two screen colours, taken from p_grid_screen_glow and
# p_crafting_grid_screen_glow so the reader matches the grids rather than inventing a
# third purple.
PURPLE = (150, 42, 195)
MAGENTA = (205, 50, 225)
PURPLE_H = colorsys.rgb_to_hsv(*[c / 255 for c in PURPLE])[0]
MAGENTA_H = colorsys.rgb_to_hsv(*[c / 255 for c in MAGENTA])[0]

# The dark, unlit screen colour the grids already use for their "off" front.
OFF_DARK = (35, 38, 43)

PATTERN_COLS = range(6, 10)
PATTERN_ROWS = range(3, 13)
GREY_BOX = range(3, 13)
RED_BOX = range(6, 10)


def load(name):
    return Image.open(os.path.join(CREATE, name + ".png")).convert("RGBA")


def save(im, name):
    im.save(os.path.join(OUT, name + ".png"))
    print("wrote", name + ".png", im.size)


def is_grey(p):
    r, g, b, _ = p
    return abs(r - g) < 12 and abs(g - b) < 12


def is_red(p):
    r, g, b, _ = p
    return r > g + 25 and r > b + 25


def hue_to(p, hue):
    """Move a pixel onto a hue, keeping its saturation and value - so the shading that
    Create painted survives and only the colour changes."""
    r, g, b, a = p
    _, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    r, g, b = colorsys.hsv_to_rgb(hue, s, v)
    return (round(r * 255), round(g * 255), round(b * 255), a)


def darken(p, factor):
    r, g, b, a = p
    return (round(r * factor), round(g * factor), round(b * factor), a)


def front():
    """Red 4x4 -> purple by hue alone; grey 10x10 -> black by value alone."""
    im = load("threshold_switch_front")
    px = im.load()
    for y in GREY_BOX:
        for x in GREY_BOX:
            p = px[x, y]
            if x in RED_BOX and y in RED_BOX and is_red(p):
                px[x, y] = hue_to(p, PURPLE_H)
            elif is_grey(p):
                px[x, y] = darken(p, 0.35)
    save(im, "external_reader_front")


def parity_hue(x, y):
    """The checkerboard decides which of the two colours a lit pixel takes."""
    return PURPLE_H if (x + y) % 2 == 0 else MAGENTA_H


def side_base():
    """The unlit side. The indicator goes dark and neutral, the way the grids' unpowered
    screens do; everything outside it is Create's wood, untouched."""
    im = load("threshold_switch/level_0")
    px = im.load()
    for y in PATTERN_ROWS:
        for x in PATTERN_COLS:
            r, g, b, a = px[x, y]
            # keep the cell's own relative brightness, but on the off-screen colour
            rel = max(r, g, b) / 88.0
            px[x, y] = tuple(round(c * (0.75 + 0.35 * rel)) for c in OFF_DARK) + (a,)
    save(im, "external_reader_side")


def glow_frame(lit):
    """One animation frame: only the lit pixels, everything else transparent, so this can
    be laid over the base as a separate emissive quad - the same trick the controller's
    trace uses."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    src = load("threshold_switch/level_5").load()
    for y in PATTERN_ROWS:
        for x in PATTERN_COLS:
            if (x, y) not in lit:
                continue
            px[x, y] = hue_to(src[x, y], parity_hue(x, y))
    return im


def strip(frames, name):
    im = Image.new("RGBA", (16, 16 * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        im.paste(f, (0, 16 * i))
    save(im, name)


ALL_CELLS = [(x, y) for y in PATTERN_ROWS for x in PATTERN_COLS]


def glow(seed, name, frame_count=24):
    """40-60% of the forty cells lit per frame, redrawn every frame.

    Two of these exist with different seeds, and opposite sides of the block use
    different ones, which is what makes them flicker out of step. One texture on both
    sides could not: Minecraft drives every animation off the same game time.
    """
    rng = random.Random(seed)
    frames = []
    for _ in range(frame_count):
        n = rng.randint(round(len(ALL_CELLS) * 0.4), round(len(ALL_CELLS) * 0.6))
        frames.append(glow_frame(set(rng.sample(ALL_CELLS, n))))
    strip(frames, name)
    with open(os.path.join(OUT, name + ".png.mcmeta"), "w", newline="\n") as fh:
        json.dump({"animation": {"frametime": 3, "interpolate": False}}, fh, indent=2)
        fh.write("\n")
    print("wrote", name + ".png.mcmeta")


def glow_full(name):
    """Every cell lit at once. Only the item uses this - a running block never shows it."""
    save(glow_frame(set(ALL_CELLS)), name)


if __name__ == "__main__":
    front()
    side_base()
    glow(20260731, "external_reader_side_glow_a")
    glow(19700101, "external_reader_side_glow_b")
    glow_full("external_reader_side_glow_full")
