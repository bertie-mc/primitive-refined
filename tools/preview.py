"""
Isometric previewer for a Minecraft block model.

Not a Minecraft renderer, but it implements the parts of the format these models use, and
implements them the way FaceBakery does:

  * per-face `uv` with `rotation` (90/180/270)  - the old previewer ignored rotation, which
    is exactly the thing Create's crafter uses to fit a 16x6 uv region onto a 6x16 face
  * `texture_size` - the cogwheel atlas is 32x32
  * element `rotation` {angle, axis, origin, rescale}
  * per-face `tintindex` is ignored; `cullface` is ignored (nothing neighbours the block)

Renders two views so both the front and the back of a block can be checked at once.

Other mods' assets come from tools/extracted, which tools/extract_assets.py fills; set
PR_ASSET_ROOT to override. Requires pillow and numpy.

    python tools/extract_assets.py path/to/create.jar
    python tools/preview.py src/main/resources/assets/primitive_refined/models/block/p_grid.json out.png

It reproduced every fault in the first grid body without a game launch, and it renders
Create's own mechanical_crafter correctly - which is the check to re-run if you ever
doubt it, because that model exercises uv rotation on all six faces.
"""
import json, math, os, sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OURS = os.path.join(HERE, os.pardir, "src", "main", "resources", "assets")
# Every mod's assets, extracted whole rather than file-by-file. Extracting piecemeal is
# what left the previewer unable to render and forced two rounds to ship blind.
ALL = os.environ.get("PR_ASSET_ROOT", os.path.join(HERE, "extracted", "assets"))


def _tex(ns, root):
    return os.path.join(root, ns, "textures")


TEX_ROOTS = {ns: _tex(ns, ALL) for ns in
             ("create", "refinedstorage", "malum", "forbidden_arcanus", "slag")}
TEX_ROOTS["primitive_refined"] = _tex("primitive_refined", OURS)
# Vanilla textures a Create model may name; they ship inside Create's own jar too.
TEX_ROOTS["minecraft"] = _tex("create", ALL)

MODEL_ROOTS = {
    "primitive_refined": os.path.join(OURS, "primitive_refined", "models"),
    "create": os.path.join(ALL, "create", "models"),
}

_tex_cache = {}


def load_tex(ref, tint=None):
    key = (ref, tint)
    if key in _tex_cache:
        return _tex_cache[key]
    ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
    im = Image.open(os.path.join(TEX_ROOTS[ns], path + ".png")).convert("RGBA")
    if im.height > im.width:                      # animated strip -> first frame
        im = im.crop((0, 0, im.width, im.width))
    arr = np.asarray(im).astype(np.int16)
    if tint:
        t = np.array([(tint >> 16) & 255, (tint >> 8) & 255, tint & 255, 255])
        arr = (arr * t // 255).astype(np.int16)
    _tex_cache[key] = arr
    return arr


def load_model(path_or_ref):
    """Resolve a model, following `parent` far enough to inherit elements and textures."""
    if os.path.exists(path_or_ref):
        model = json.load(open(path_or_ref))
    else:
        ns, path = path_or_ref.split(":") if ":" in path_or_ref else ("minecraft", path_or_ref)
        if ns not in MODEL_ROOTS:
            return {}
        fp = os.path.join(MODEL_ROOTS[ns], path + ".json")
        if not os.path.exists(fp):
            return {}
        model = json.load(open(fp))
    parent = model.get("parent")
    if parent:
        base = load_model(parent)
        textures = dict(base.get("textures", {}))
        textures.update(model.get("textures", {}))
        merged = dict(base)
        merged.update(model)
        merged["textures"] = textures
        if "elements" not in model and "elements" in base:
            merged["elements"] = base["elements"]
        model = merged
    return model


def resolve(textures, name):
    seen = 0
    while isinstance(name, str) and name.startswith("#") and seen < 8:
        name = textures.get(name[1:], name)
        seen += 1
    return name


# Corners in Minecraft's vertex order: 0 top-left, 1 bottom-left, 2 bottom-right,
# 3 top-right, "left/right" as seen by someone outside the block looking at the face.
def face_corners(f, x1, y1, z1, x2, y2, z2):
    return {
        "north": [(x2, y2, z1), (x2, y1, z1), (x1, y1, z1), (x1, y2, z1)],
        "south": [(x1, y2, z2), (x1, y1, z2), (x2, y1, z2), (x2, y2, z2)],
        "west":  [(x1, y2, z1), (x1, y1, z1), (x1, y1, z2), (x1, y2, z2)],
        "east":  [(x2, y2, z2), (x2, y1, z2), (x2, y1, z1), (x2, y2, z1)],
        "up":    [(x1, y2, z1), (x1, y2, z2), (x2, y2, z2), (x2, y2, z1)],
        "down":  [(x1, y1, z2), (x1, y1, z1), (x2, y1, z1), (x2, y1, z2)],
    }[f]


def face_uvs(uv, rotation):
    """BlockFaceUV.getVertexU/V - the shifted-index rule, verbatim."""
    u1, v1, u2, v2 = uv
    out = []
    for index in range(4):
        i = (index + rotation // 90) % 4
        u = u1 if i in (0, 1) else u2
        v = v1 if i in (0, 3) else v2
        out.append((u, v))
    return out


def rotate_point(p, rot):
    if not rot:
        return p
    ox, oy, oz = rot["origin"]
    ang = math.radians(rot["angle"])
    c, s = math.cos(ang), math.sin(ang)
    x, y, z = p[0] - ox, p[1] - oy, p[2] - oz
    axis = rot["axis"]
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    if rot.get("rescale"):
        f = 1 / math.cos(ang)
        if axis == "x":
            y, z = y * f, z * f
        elif axis == "y":
            x, z = x * f, z * f
        else:
            x, y = x * f, y * f
    return (x + ox, y + oy, z + oz)


SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}

# Two opposite corners, so one call shows both sides of the block. `mirror` swaps the
# camera to the (-x, +y, -z) octant; the visible faces and the depth ordering both follow
# from it, which is what the previous version got wrong.
VIEWS = {
    "front": (("up", "north", "west"), True),
    "back": (("up", "south", "east"), False),
}


def view_coords(p, mirror):
    x, y, z = p
    return (16 - x, y, 16 - z) if mirror else (x, y, z)


def project(p, scale, mirror):
    x, y, z = view_coords(p, mirror)
    return (x - z) * 0.866 * scale, ((x + z) * 0.5 - y) * scale


def raster(canvas, zbuf, tri_pts, tri_uv, tri_depth, tex, shade):
    """Fill one triangle, nearest-neighbour texture sampling, painter's z-buffer."""
    (x0, y0), (x1, y1), (x2, y2) = tri_pts
    minx, maxx = int(math.floor(min(x0, x1, x2))), int(math.ceil(max(x0, x1, x2)))
    miny, maxy = int(math.floor(min(y0, y1, y2))), int(math.ceil(max(y0, y1, y2)))
    H, W = canvas.shape[:2]
    minx, maxx = max(minx, 0), min(maxx, W - 1)
    miny, maxy = max(miny, 0), min(maxy, H - 1)
    if minx > maxx or miny > maxy:
        return
    det = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
    if abs(det) < 1e-9:
        return
    xs = np.arange(minx, maxx + 1)
    ys = np.arange(miny, maxy + 1)
    px, py = np.meshgrid(xs + 0.5, ys + 0.5)
    l0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / det
    l1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / det
    l2 = 1 - l0 - l1
    inside = (l0 >= -1e-6) & (l1 >= -1e-6) & (l2 >= -1e-6)
    if not inside.any():
        return
    th, tw = tex.shape[:2]
    u = l0 * tri_uv[0][0] + l1 * tri_uv[1][0] + l2 * tri_uv[2][0]
    v = l0 * tri_uv[0][1] + l1 * tri_uv[1][1] + l2 * tri_uv[2][1]
    tx = np.clip((u * tw / 16).astype(int), 0, tw - 1)
    ty = np.clip((v * th / 16).astype(int), 0, th - 1)
    texel = tex[ty, tx]
    depth = l0 * tri_depth[0] + l1 * tri_depth[1] + l2 * tri_depth[2]
    sub_z = zbuf[miny:maxy + 1, minx:maxx + 1]
    mask = inside & (texel[..., 3] > 0) & (depth > sub_z)
    if not mask.any():
        return
    sub_c = canvas[miny:maxy + 1, minx:maxx + 1]
    col = texel.copy()
    col[..., :3] = (col[..., :3] * shade).astype(np.int16)
    sub_c[mask] = col[mask]
    sub_z[mask] = depth[mask]


def render_view(model, view, tint, scale, supersample):
    textures = model.get("textures", {})
    elements = model.get("elements", [])
    visible, flip = VIEWS[view]

    s = scale * supersample
    # Fit the camera to the block's own bounds rather than to magic numbers - the old
    # previewer's offsets clipped the bottom of every render.
    corners = [project((x, y, z), s, flip)
               for x in (0, 16) for y in (0, 16) for z in (0, 16)]
    pad = int(1.5 * s)
    minx = min(c[0] for c in corners) - pad
    maxx = max(c[0] for c in corners) + pad
    miny = min(c[1] for c in corners) - pad
    maxy = max(c[1] for c in corners) + pad
    W, H = int(maxx - minx), int(maxy - miny)
    canvas = np.zeros((H, W, 4), dtype=np.int16)
    zbuf = np.full((H, W), -1e9)
    ox, oy = -minx, -miny

    for el in elements:
        x1, y1, z1 = el["from"]
        x2, y2, z2 = el["to"]
        rot = el.get("rotation")
        for fname, fdef in el.get("faces", {}).items():
            if fname not in visible:
                continue
            ref = resolve(textures, fdef["texture"])
            if not isinstance(ref, str) or ref.startswith("#"):
                continue
            tex = load_tex(ref, tint)
            tsz = model.get("texture_size", [16, 16])
            uv = fdef.get("uv", [0, 0, 16, 16])
            uv = [uv[0] * 16 / tsz[0], uv[1] * 16 / tsz[1],
                  uv[2] * 16 / tsz[0], uv[3] * 16 / tsz[1]]
            uvs = face_uvs(uv, fdef.get("rotation", 0))
            pts3 = [rotate_point(p, rot) for p in face_corners(fname, x1, y1, z1, x2, y2, z2)]
            pts2 = [(ox + a, oy + b) for a, b in (project(p, s, flip) for p in pts3)]
            # depth along the view direction, for the z-buffer
            depths = [sum(view_coords(p, flip)) for p in pts3]
            sh = SHADE[fname]
            for a, b, c in ((0, 1, 2), (0, 2, 3)):
                raster(canvas, zbuf,
                       (pts2[a], pts2[b], pts2[c]),
                       (uvs[a], uvs[b], uvs[c]),
                       (depths[a], depths[b], depths[c]), tex, sh)

    im = Image.fromarray(canvas.astype(np.uint8), "RGBA")
    im = im.resize((W // supersample, H // supersample), Image.LANCZOS)
    bg = Image.new("RGBA", im.size, (34, 34, 42, 255))
    bg.alpha_composite(im)
    return bg


def render(model_path, out_path, tint=None, scale=22, supersample=3):
    model = load_model(model_path)
    views = [render_view(model, v, tint, scale, supersample) for v in ("front", "back")]
    w = sum(v.width for v in views) + 8
    out = Image.new("RGBA", (w, views[0].height), (20, 20, 24, 255))
    x = 0
    for v in views:
        out.paste(v, (x, 0))
        x += v.width + 8
    out.save(out_path)
    print("wrote", out_path, out.size)


if __name__ == "__main__":
    render(sys.argv[1], sys.argv[2], int(sys.argv[3], 16) if len(sys.argv) > 3 else None)
