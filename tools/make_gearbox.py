"""
Texture and models for the Arcanetic Gearbox, derived from Create's gearbox.

The panel is a pixel-for-pixel substitution rather than a repaint: wherever Create's
gearbox panel is grey metal it takes brass casing's pixel at the same coordinate, and
wherever it is anything else - wood, or the shaft socket - it takes the sequenced gearshift's *shaft* face at that
coordinate. Shape and shading are Create's throughout; only which of Create's materials
fills them changes.

The shaft face is the point. A gearbox panel has a shaft coming out of it, so the face
substituted onto it must be one that also has a shaft - which for the sequenced gearshift
is create:block/brass_gearbox, the texture on the two ends its axis runs through. Its
create:block/sequenced_gearshift is the sequencer display on the four sides the axis does
NOT pass through, and putting that on a face with a shaft in it was wrong.

The block's end caps need no new texture at all - they are andesite casing in Create's
model and simply point at brass casing instead, which is the 1-to-1 swap asked for.
"""
import json, os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
CREATE_TEX = os.path.join(HERE, "extracted", "assets", "create", "textures", "block")
CREATE_MODEL = os.path.join(HERE, "extracted", "assets", "create", "models", "block")
RES = os.path.join(HERE, os.pardir, "src", "main", "resources")
TEX_OUT = os.path.join(RES, "assets", "primitive_refined", "textures", "block")
MODEL_OUT = os.path.join(RES, "assets", "primitive_refined", "models")

BRASS = "create:block/brass_casing"
PANEL = "primitive_refined:block/arcanetic_gearbox"
AXIS = "primitive_refined:block/soulstained_axis"
AXIS_TOP = "primitive_refined:block/soulstained_axis_top"


def is_perimeter_metal(p):
    """The casing ring around the panel - and only that.

    Brightness is what separates it from the shaft socket in the middle, which is grey
    too but nearly black. Testing for grey alone swallowed the socket and filled it with
    brass casing, which erased the hole the shaft comes out of.
    """
    r, g, b, _ = p
    grey = abs(r - g) < 14 and abs(g - b) < 14
    return grey and (0.3 * r + 0.6 * g + 0.1 * b) > 70


def panel():
    src = Image.open(os.path.join(CREATE_TEX, "gearbox.png")).convert("RGBA")
    brass = Image.open(os.path.join(CREATE_TEX, "brass_casing.png")).convert("RGBA")
    wood = Image.open(os.path.join(CREATE_TEX, "brass_gearbox.png")).convert("RGBA")
    out = Image.new("RGBA", src.size)
    sp, bp, wp, op = src.load(), brass.load(), wood.load(), out.load()
    grey = 0
    for y in range(src.height):
        for x in range(src.width):
            p = sp[x, y]
            if p[3] == 0:
                op[x, y] = p
            elif is_perimeter_metal(p):
                op[x, y] = bp[x % brass.width, y % brass.height]
                grey += 1
            else:
                op[x, y] = wp[x % wood.width, y % wood.height]
    out.save(os.path.join(TEX_OUT, "arcanetic_gearbox.png"))
    print("wrote arcanetic_gearbox.png", out.size, f"({grey} metal pixels from brass casing, rest from brass_gearbox)")


def retexture(model, mapping, name, folder="block", comment=None):
    """Take one of Create's gearbox models and only change which textures it names."""
    d = json.load(open(os.path.join(CREATE_MODEL, "gearbox", model + ".json")))
    d.pop("credit", None)
    d["textures"] = {k: mapping.get(v, v) for k, v in d["textures"].items()}
    if comment:
        d = {"__comment": comment, **d}
    path = os.path.join(MODEL_OUT, folder, name + ".json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as fh:
        json.dump(d, fh, indent=2)
        fh.write("\n")
    print("wrote", folder + "/" + name + ".json")


MAP = {
    "create:block/andesite_casing": BRASS,
    "create:block/gearbox": PANEL,
    "create:block/axis": AXIS,
    "create:block/axis_top": AXIS_TOP,
}

BLOCK_COMMENT = (
    "Create's gearbox, element for element, with only the texture names changed: the "
    "andesite casing caps become brass casing, and the wooden panel becomes ours. The "
    "four shafts are not here - they turn, at four different speeds, so they are drawn "
    "by the block entity's Flywheel visual from block/arcanetic_gearbox_shaft."
)
ITEM_COMMENT = (
    "Create's gearbox item model, retextured. It bakes the shafts in, because a Flywheel "
    "visual only exists for a placed block entity."
)


def shaft():
    """Create's shaft_half, in our steel. One end only, pointing south, which is what the
    visual rotates onto each connected face."""
    d = json.load(open(os.path.join(CREATE_MODEL, "shaft_half.json")))
    d.pop("credit", None)
    d["textures"] = {k: MAP.get(v, v) for k, v in d["textures"].items()}
    d["__comment"] = ("Create's shaft_half in soulstained steel. Authored pointing south "
                      "(+Z); the gearbox visual turns a copy onto each face that is not "
                      "on the block's own axis.")
    path = os.path.join(MODEL_OUT, "block", "arcanetic_gearbox_shaft.json")
    with open(path, "w", newline="\n") as fh:
        json.dump(d, fh, indent=2)
        fh.write("\n")
    print("wrote block/arcanetic_gearbox_shaft.json")


def blockstate_and_loot():
    bs = {"variants": {
        "axis=x": {"model": "primitive_refined:block/arcanetic_gearbox",
                   "uvlock": True, "x": 90, "y": 90},
        "axis=y": {"model": "primitive_refined:block/arcanetic_gearbox", "uvlock": True},
        "axis=z": {"model": "primitive_refined:block/arcanetic_gearbox",
                   "uvlock": True, "x": 90, "y": 180},
    }}
    p = os.path.join(RES, "assets", "primitive_refined", "blockstates",
                     "arcanetic_gearbox.json")
    with open(p, "w", newline="\n") as fh:
        json.dump(bs, fh, indent=2)
        fh.write("\n")
    print("wrote blockstates/arcanetic_gearbox.json")
    p = os.path.join(RES, "data", "primitive_refined", "loot_table", "blocks",
                     "arcanetic_gearbox.json")
    with open(p, "w", newline="\n") as fh:
        json.dump({"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item",
                         "name": "primitive_refined:arcanetic_gearbox"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]}, fh, indent=2)
        fh.write("\n")
    print("wrote loot_table/blocks/arcanetic_gearbox.json")


if __name__ == "__main__":
    panel()
    retexture("block", MAP, "arcanetic_gearbox", comment=BLOCK_COMMENT)
    retexture("item", MAP, "arcanetic_gearbox", folder="item", comment=ITEM_COMMENT)
    retexture("item_vertical", MAP, "arcanetic_gearbox_vertical", folder="item",
              comment=ITEM_COMMENT)
    shaft()
    blockstate_and_loot()
