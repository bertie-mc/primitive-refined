"""
Textures for the External Reader, derived from Create's threshold switch and Refined
Storage's controller.

Every transform is a remap of somebody else's pixels, never a repaint, so the shading
survives. Derived art is shipped outright with permission - see NOTICE.

Regions, read off Create's texture rather than assumed:
  front  threshold_switch_front        greys at cols/rows 3-12, dark square at 6-9
  side   threshold_switch/level_N      indicator is a 4x10 grid at cols 6-9, rows 3-12

Create shades that indicator's top and bottom rows darker than the middle. That is a
level meter reading bottom to top and it means nothing here, so the whole 4x10 is laid
out as one uniform two-colour checkerboard instead.
"""
import colorsys, json, os, random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
EXTRACTED = os.path.join(HERE, "extracted", "assets")
CREATE = os.path.join(EXTRACTED, "create", "textures", "block")
OUT = os.path.join(HERE, os.pardir, "src", "main", "resources", "assets",
                   "primitive_refined", "textures", "block")

# Refined Storage's controller cutout, lifted from
# refinedstorage:block/controller/cutouts/purple. Its traces are runs of pixels with a
# gradient flowing along them - not pixels twinkling on their own, which is what an
# earlier version of this file did and why the display read as a static checkerboard
# with white dots skittering over it.
PURPLE = (132, 43, 206)
MAGENTA = (181, 50, 207)
PEAK = (188, 106, 215)

# The controller's own cadence, unchanged: twelve frames at frametime 2.
FRAMES = 12
FRAMETIME = 2

# Unlit: the dark neutral screen the grids already use when unpowered. Flat, not a
# checkerboard - the lit display is one 4x10 panel, so the dark one should be too.
OFF = (35, 38, 43)

PATTERN_COLS = range(6, 10)
PATTERN_ROWS = range(3, 13)
BOX = range(3, 13)
CENTRE = range(6, 10)
CELLS = [(x, y) for y in PATTERN_ROWS for x in PATTERN_COLS]


def load(root, name):
    return Image.open(os.path.join(root, name + ".png")).convert("RGBA")


def save(im, name):
    im.save(os.path.join(OUT, name + ".png"))
    print("wrote", name + ".png", im.size)


def is_grey(p):
    r, g, b, _ = p
    return abs(r - g) < 12 and abs(g - b) < 12


def is_red(p):
    r, g, b, _ = p
    return r > g + 25 and r > b + 25


def hue_to(p, hue, value_scale=1.0):
    """Move a pixel onto a hue, keeping saturation and (optionally scaled) value."""
    r, g, b, a = p
    _, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    r, g, b = colorsys.hsv_to_rgb(hue, s, min(1.0, v * value_scale))
    return (round(r * 255), round(g * 255), round(b * 255), a)


def lerp(c0, c1, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c0, c1)) + (255,)


PURPLE_H = colorsys.rgb_to_hsv(*[c / 255 for c in PURPLE])[0]


def front():
    """Indicator square -> purple by hue and lifted in value; ring -> black by value."""
    im = load(CREATE, "threshold_switch_front")
    px = im.load()
    for y in BOX:
        for x in BOX:
            p = px[x, y]
            if x in CENTRE and y in CENTRE and is_red(p):
                # Create's square is nearly black, and a purple that dark reads as a hole
                # rather than as an indicator, so its value is scaled up.
                px[x, y] = hue_to(p, PURPLE_H, 1.9)
            elif is_grey(p):
                px[x, y] = tuple(round(c * 0.35) for c in p[:3]) + (p[3],)
    save(im, "external_reader_front")


def side_base():
    """The unlit side: one flat dark panel."""
    im = load(CREATE, "threshold_switch/level_0")
    px = im.load()
    for y in PATTERN_ROWS:
        for x in PATTERN_COLS:
            px[x, y] = OFF + (255,)
    save(im, "external_reader_side")


def ramp():
    """The colour a segment runs through, end to end and back.

    Purple into magenta into the bright peak and back down, which is the sweep the
    controller's traces make. Twelve entries, one per frame, so a segment advances
    exactly one step per frame.
    """
    stops = [PURPLE, MAGENTA, PEAK, MAGENTA]
    out = []
    for i in range(FRAMES):
        pos = i / FRAMES * len(stops)
        a = stops[int(pos) % len(stops)]
        b = stops[(int(pos) + 1) % len(stops)]
        out.append(lerp(a, b, pos - int(pos))[:3])
    return out


RAMP = ramp()


def segments(seed):
    """Carve the 4x10 panel into runs, and give each one a starting point on the ramp.

    Vertical runs of two to four, packed down each column, so the panel is completely
    filled and reads as segments rather than as loose pixels.
    """
    rng = random.Random(seed)
    out = []
    for x in PATTERN_COLS:
        y = PATTERN_ROWS.start
        while y < PATTERN_ROWS.stop:
            length = min(rng.randint(2, 4), PATTERN_ROWS.stop - y)
            out.append(([(x, y + i) for i in range(length)], rng.randrange(FRAMES)))
            y += length
    return out


def glow_frame(segs, t):
    """One frame: every cell lit, coloured by where it sits along its segment's gradient.

    Written in colour rather than alpha, because these models are cutout and alpha there
    is all or nothing.
    """
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for cells, offset in segs:
        for i, (x, y) in enumerate(cells):
            px[x, y] = RAMP[(i + offset + t) % FRAMES] + (255,)
    return im


def glow(seed, name):
    """The gradient advances one step per frame, so it travels along each segment.

    Two of these exist with different seeds - different segment layouts and different
    starting points - and opposite sides of the block take different ones. That is the
    only way to get them out of step: Minecraft drives every animation off the same game
    time, so one texture on both faces is frame-locked.
    """
    segs = segments(seed)
    im = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
    for t in range(FRAMES):
        im.paste(glow_frame(segs, t), (0, 16 * t))
    save(im, name)
    with open(os.path.join(OUT, name + ".png.mcmeta"), "w", newline="\n") as fh:
        json.dump({"animation": {"frametime": FRAMETIME, "interpolate": False}}, fh,
                  indent=2)
        fh.write("\n")


def glow_full(name):
    """A still frame of the lit panel, for the item - so its icon does not animate."""
    save(glow_frame(segments(20260731), 0), name)


if __name__ == "__main__":
    front()
    side_base()
    glow(20260731, "external_reader_side_glow_a")
    glow(19700101, "external_reader_side_glow_b")
    glow_full("external_reader_side_glow_full")
