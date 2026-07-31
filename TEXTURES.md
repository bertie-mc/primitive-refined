# Grid / Crafting Grid front-face textures

Art handoff for the two remaining display parts. The textures exist; the block, model,
blockstate and registration do not. This file is the contract for whoever wires them up.

The look is a Refined Storage screen set into a Create railway casing: the casing's
brass frame and black recess, with the RS grid screen in the middle and the casing's
top-corner gold overhangs enlarged so they lap over the recess.

---

## 1. These are overlays, not full faces

Read [`NOTICE`](NOTICE) first. Create's assets are All Rights Reserved and this repo
ships **no copy of a Create texture, modified or not**. A merged front face would be
one — outside the screen and the wedges it is byte-identical to Create's.

So the shipped files carry *only* the pixels that differ from
`create:block/railway_casing_side`. Everything else is transparent, and Create's
texture is referenced by path underneath, exactly as `p_controller_overlay*.json`
already does. Composited in game the result is pixel-identical to the merged face.

| File | Pixels | Emissive | Origin |
| --- | --- | --- | --- |
| `p_grid_front_glow.png` | 88 | yes | Refined Storage, MIT |
| `p_grid_front_base.png` | 18 | no | authored here |
| `p_crafting_grid_front_glow.png` | 88 | yes | Refined Storage, MIT |
| `p_crafting_grid_front_base.png` | 18 | no | authored here |

Do not flatten these onto Create's texture and ship the result. That is the one thing
`NOTICE` rules out.

---

## 2. Pixel contract

16x16, origin top-left, `y` increasing downward — this matches model `uv`, where `v=0`
is the top edge of the texture.

```
        0123456789012345
   0    ................     . = transparent (Create's casing shows through)
   1    ................     B = base overlay, the gold wedge
   2    ..BBBB....BBBB..     G = glow overlay, the RS screen
   3    ..BBB......BBB..
   4    ..BBGGGGGGGGBB..
   5    ...GGGGGGGGGG...
   6    ...GGGGGGGGGG...
   7    ...GGGGGGGGGG...
   8    ...GGGGGGGGGG...
   9    ...GGGGGGGGGG...
  10    ...GGGGGGGGGG...
  11    ...GGGGGGGGGG...
  12    ...GGGGGGGGGG...
  13    ................
  14    ................
  15    ................
```

**Screen** — `x3..x12`, `y4..y12`, 10 wide by 9 tall. Lifted unchanged from
`refinedstorage:block/grid/cutouts/purple.png` and `.../crafting_grid/cutouts/purple.png`,
which are 10x9 at `y3..y11` in the source and sit one pixel lower here. All 88 pixels are
the RS originals; the wedges clip the two top corners and cost no lit cell in either
variant. It is not 10x10 — the row below it is casing recess, not screen.

**Wedge** — the two top corners only. The bottom corners are square, because
`railway_casing_side` has no bottom overhang (its top/bottom face,
`railway_casing.png`, does — don't take the shape from that one). Create's overhang is
3 px per corner; this is 9, ramped outward-in through
`#FFEB8C -> #F7CB6C -> #CEA05A -> #724731` and then straight into the black, so it stays
gold across the wedge and does all its falling in the last pixel.

Those four values are sampled from Create's brass palette. Four colour values in a
from-scratch 9-pixel shape is the same latitude `NOTICE` already takes for the
Soulstained Shaft's runtime tint — but it is a judgement call, so it is written down
here rather than left implicit.

---

## 3. Wiring it up

Follow `models/block/p_controller_overlay.json` / `_lit.json` — same structure, simpler,
because this is one face instead of six.

- `"render_type": "cutout"` — the overlays are mostly transparent.
- Base and glow must be **separate elements**. Only the glow face carries
  `"neoforge_data": { "block_light": 15, "sky_light": 15 }`; the gold wedge is lit
  normally or it stops reading as metal.
- Draw the overlay quads a hair proud of the face (the controller uses `0.99` / `15.01`)
  to avoid z-fighting.
- Underneath, reference Create by path — nothing copied:
  - front face: `create:block/railway_casing_side`
  - other sides: `create:block/railway_casing_side`
  - top / bottom: `create:block/railway_casing`
- Give the blockstate a `facing` property so the screen faces the player, like RS's grid.

## 4. Not included

- **No unpowered variant.** The controller has `_off` textures; these have none. RS ships
  `cutouts/inactive.png` for both grids, which drops into the same generator — ask for it.
- No item model, particle texture, GUI, block class, or registration.
- Nothing has been seen in game. The composite is verified only as pixel maths:
  `railway_casing_side + base + glow == the intended face`, exact, for both variants.

## 5. Provenance

Sources were read from the jars pinned in this pack — `refinedstorage-neoforge-2.0.9`
and `create-1.21.1-6.0.10`. Colours are exact, not eyedropped from a screenshot.

The generator is not in this repo; it lives in the authoring session's scratchpad. If the
wedge size or ramp needs changing, regenerating is cheaper than hand-editing — the knobs
are wedge size (6 / 7 / 9 px), the ramp, and the screen's vertical offset.
