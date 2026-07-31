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

# Refined Storage's own controller cutout colours and cadence, lifted from
# refinedstorage:block/controller/cutouts/purple, so the reader pulses like an RS
# controller rather than like something invented here. Two colours, which is exactly
# what the checkerboard needs.
PURPLE = (132, 43, 206)
MAGENTA = (181, 50, 207)
FRAMETIME = 1        # RS uses 2; this is the "twice as fast" that was asked for

# Unlit: the dark neutral screen the grids already use when unpowered, as a matching
# two-tone checkerboard so the pattern still reads when the machine is stopped.
OFF_A = (35, 38, 43)
OFF_B = (46, 49, 56)

# The bright end of RS's shimmer. Its pixels sit at their base colour and briefly ramp
# up to about here before settling back - measured off the animation, not guessed.
PEAK = (188, 106, 215)

FRAMES = 12          # RS's own frame count, at half its frametime: twice the speed
PULSE = 0.30         # share of a cell's cycle spent ramping up and back down

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


def parity(x, y):
    return (x + y) % 2 == 0


def cell_colours(x, y):
    return (OFF_A, PURPLE) if parity(x, y) else (OFF_B, MAGENTA)


def side_base():
    """The unlit side: one uniform two-colour checkerboard, no level-meter shading."""
    im = load(CREATE, "threshold_switch/level_0")
    px = im.load()
    for y in PATTERN_ROWS:
        for x in PATTERN_COLS:
            px[x, y] = cell_colours(x, y)[0] + (255,)
    save(im, "external_reader_side")


def shimmer(phase):
    """How far a cell is toward the bright peak.

    Every cell is lit the whole time the machine is powered; what moves is a flare
    passing over it. That is what Refined Storage's controller pixels do - sit at their
    colour, briefly flare, settle back - rather than switching on and off.
    """
    p = phase % 1.0
    if p >= PULSE:
        return 0.0
    return 1.0 - abs(p / PULSE * 2.0 - 1.0)


def glow_frame(phases, t):
    """Written in colour rather than alpha: these models are cutout, where alpha is all
    or nothing, so a mid-flare pixel has to be a colour between base and peak."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for (x, y), ph in phases.items():
        base = cell_colours(x, y)[1]
        px[x, y] = lerp(base, PEAK, shimmer(ph + t / FRAMES))
    return im


def glow(seed, name):
    """Each cell flares at its own random phase, so they read as independent.

    Two of these exist with different seeds, and opposite sides of the block take
    different ones. That is the only way to get them out of step: Minecraft drives every
    animation off the same game time, so one texture on both faces is frame-locked.
    """
    rng = random.Random(seed)
    phases = {c: rng.random() for c in CELLS}
    im = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
    for t in range(FRAMES):
        im.paste(glow_frame(phases, t), (0, 16 * t))
    save(im, name)
    with open(os.path.join(OUT, name + ".png.mcmeta"), "w", newline="\n") as fh:
        json.dump({"animation": {"frametime": FRAMETIME, "interpolate": False}}, fh,
                  indent=2)
        fh.write("\n")


def glow_full(name):
    """Every cell at its base colour, none flaring - a still frame, so the item's icon
    does not animate in the inventory."""
    save(glow_frame({c: PULSE for c in CELLS}, 0), name)


if __name__ == "__main__":
    front()
    side_base()
    glow(20260731, "external_reader_side_glow_a")
    glow(19700101, "external_reader_side_glow_b")
    glow_full("external_reader_side_glow_full")
