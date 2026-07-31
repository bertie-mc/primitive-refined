# Primitive Refined

An early-game, Create-powered precursor to Refined Storage for **NeoForge 1.21.1**.

The machines are one-to-one in behaviour with their Refined Storage counterparts, but they
run on rotational force: the controller reports the network's total cost as a Create
**stress impact** instead of drawing FE. Build a waterwheel, not a generator.

Create is a **required** dependency.

## Planned parts

| Part | Stress | Refined Storage equivalent |
| --- | --- | --- |
| Primitive Controller | 0 | Controller |
| Primitive Cable | 0 | Cable |
| Primitive External Storage | 1 | External Storage |
| Primitive Grid | 5 | Grid |
| Primitive Crafting Grid | 10 | Crafting Grid |

The controller itself is free. You pay for what you hang off it — which is why a bare
controller spins up on any amount of rotational force, and a network full of crafting
grids will stall a waterwheel.

## What is in this build

Four of the five parts exist. The Primitive Cable does not, so nothing forms a
network yet.

### Primitive Controller (`primitive_refined:p_controller`)

Topologically Create's rotational speed controller. Rotation runs through it along its
horizontal axis, and a **large cogwheel goes on top** — without one it stays dark no
matter how fast the shaft line under it is turning. The cogwheel must be horizontal and
perpendicular to the controller's own axis, the same three conditions Create checks for
its own speed controller.

When it is running, the Refined Storage trace pattern on its sides animates and glows. The
side texture is Create's own with 50 pixels replaced; a separate four-quad layer carries
just the trace pixels as emissive, because a model that inherits Create's geometry has no
elements of its own to hang per-face lighting on.

The stress it demands is the sum of the network attached to it, so in this build — with no
cables or grids to attach — it is zero, and it lights on any rotational force at all.

### Soulstained Shaft (`primitive_refined:soulstained_shaft`)

Create's shaft, in soulstained steel. Same axis placement and alignment, same kinetic
relay behaviour, same shape; it extends `AbstractSimpleShaftBlock`, the same class Create's
own `ShaftBlock` extends, rather than reimplementing any of it.

It exists as a separate block so primitive machines can be wired with a visually distinct
line, and so a later version can restrict which shafts carry a network.

### Primitive Grid / Primitive Crafting Grid (`p_grid`, `p_crafting_grid`)

Refined Storage screen on the front, a recessed brass gearbox face on the back, mechanical
crafter body. Rotation enters along the facing axis, through the shaft standing in the
gearbox well, and the screen lights once the block is turning and the network is not
overstressed - an overstressed network means the stress units are not actually being
supplied, which is the unpowered case. They demand 5 and 10 stress.

The body follows Create's mechanical crafter element for element: two slabs with a gap at
z 6-10 and four thin rims. Every `crafter_side` face carries **Create's own face
`rotation`** — without it a 16x6 uv region lands transposed on a 6x16 face and every side
of the block stretches.

The back is the gearbox face at **two depths only**: the two-pixel perimeter and the
shaft's 4x4 sit at the block face, everything between them is one pixel in. The perimeter
is a picture frame of four bars — full-width top and bottom, inset sides — because that is
the arrangement in which no two faces are ever coplanar, and coplanar is what z-fights.
The plate behind runs across the shaft's square as well, so with the shaft's visual absent
that square reads as the dark hole the texture already draws there, not as a hole through
the block.

### Two backs, pending a decision

`p_grid_welled` and `p_crafting_grid_welled` are the same blocks with the shaft sunk in a
three-pixel well instead — a first attempt at reading the gearbox face, kept only so both
can be placed side by side in game. **One of the two sets is to be deleted** once berlord
picks: its models, blockstates, loot tables, lang entries and registry lines. They share
the plain grids' block entity types on purpose, so nothing else in the mod knows they
exist.

The shaft and the cogwheel turn, so they are **not in the block model at all**. They live
in `block/p_grid_kinetics` and are drawn by the block entity's Flywheel visual. A visual
does not suppress the block model, so anything drawn by both renders twice — once
spinning, once standing still. The item model has to bake them back in, which is exactly
what Create's own crafter item model does.

The cogwheel is Create's `cogwheel_shaftless` stood on the Z axis, at full size: its teeth
reach past the block and out through the window in the rims, which is how it meshes with a
cogwheel laid alongside. The block is an `ICogWheel` for the same reason Create's crafter
is one.

**Working:** the screen lights and unlights correctly.

**Their Refined Storage behaviour is not implemented.** They hold no items, open no GUI and
join no storage network. RS's own grids need cables too, so this waits on the Primitive
Cable; the intent is to mirror RS's design rather than invent one.

## Assets

Derived textures are **shipped outright, with permission obtained from the respective
authors** — see [NOTICE](NOTICE). That permission is personal to this project: a fork does
not inherit it.

Recolours are a luminance remap rather than a hue shift, so Create's shading survives and
only the palette changes. That matters mechanically as well as visually — the axis
texture's lengthwise grooves are what make a spinning shaft read as spinning.

## Deployment status

Released. [`bertie-mc/primitive-refined`](https://github.com/bertie-mc/primitive-refined),
tag `v0.1.0`, jar attached to the GitHub Release by `release.yml`.

`packs/s1-pack` consumes it — `mods/primitive-refined.pw.toml`, added with
`packwiz github add bertie-mc/primitive-refined`, pinned to `v0.1.0`. The **s1 demo**
instance has been synced against that pack, which replaced the hand-copied jar that had
been sitting there since before the grid rebuild. Nothing is hand-copied any more; a
re-sync now brings the released build.

It is **not** in `packs/bertie-pack`, `packs/full-test-pack` or `packs/worldgen-pack`.
Adding it is one `packwiz github add` in each, whenever those packs want it.

### CI

`build.yml` and `release.yml`, both on `bertie-ci` v3.1.0, the same as every other bertie
mod — **minus the client and server runtime jobs.** Those boot the jar in a real game, and
Create is a required dependency here, so without Create present NeoForge stops at a
missing-dependency screen and the job fails having tested nothing. `bertie-ci`'s fixture
catalogue has no Create entry. Adding one there is the fix, and it belongs in that
repository, not this one — until then this mod's CI proves that it *compiles* and nothing
more.

## The one mixin

`RotationPropagatorMixin` adds the missing large-cogwheel-drives-controller case. Create
supports it for its own speed controller via `isLargeCogToSpeedController`, hardcoded to
`AllBlocks.ROTATION_SPEED_CONTROLLER`, and exposes no hook on the receiving side - the
propagator only asks the *upstream* block for a custom connection, and upstream here is
Create's own cogwheel entity.

It injects at the head of the private `getRotationSpeedModifier`, which is the single
point every caller routes through: `getConveyedSpeed` multiplies the source speed by it,
and `isConnected` tests it for non-zero.

**Being private, it is not API.** Re-check this mixin on every Create bump. It is set to
`defaultRequire: 1`, so if the target moves the mod fails loudly at load rather than
quietly losing the connection.

## Known gaps

### The rebuilt grid body has not been seen in game

The six faults the previous round left in the grid body are all addressed, but the rebuild
was done against the previewer, not the game. berlord's in-game session predates it. So
everything in the grid section above — the recessed back, the shaft standing in its well,
the cogwheel turning in the gap, cogwheels meshing against the sides — is **unverified**.

What *has* been checked, short of launching: the mod builds; no two faces in the block
model are coplanar and overlapping (the z-fighting case, checked by script); and the
previewer that produced it renders Create's own `mechanical_crafter` correctly, which is
the model that exercises uv rotation on all six faces.

There is also an open decision: the two backs above ship side by side, and one set has to
go once it has been looked at.

The one to look at hardest is the visual. `SingleAxisRotatingVisual` — what the controller
and the shaft use — turns the model onto the rotation *axis*, and an axis has no sign, so
a model with a shaft at one end only lands on the wrong end for two of the four facings.
The controller's stubs get away with it by being symmetric. The grids use
`OrientedRotatingVisual.backHorizontal` instead, which turns SOUTH onto
`HORIZONTAL_FACING.getOpposite()` — a direction, not an axis — and is what Create drives
its own mechanical crafter with. That reasoning is from the bytecode, not from play.

### Elsewhere

- **With Flywheel's backend off, nothing that turns renders.** That was already true of the
  controller's shaft stubs; it now covers the grids' shaft and cogwheel too, so a grid with
  the backend off has an empty well and an empty gap. A fallback would have to place and
  orient the parts by hand through `CachedBuffers.partial`, the way Create's
  `MechanicalCrafterRenderer` does — a real path, deliberately not taken here because it
  cannot be tested from the previewer and would have shipped unverified either way.
- **No recipes.** Everything is creative-tab only.

### Settled: the shaft's resting angle is deliberate — leave it

**Shafts at rest sit at an angle inside an axis-aligned outline.** Create applies a
per-position rotation offset (`getRotationAngleOffset`) so that neighbouring shafts look
continuous, `SingleAxisRotatingVisual` honours it, and Create's own shafts do the same.
The block's hitbox and selection outline do not follow that rotation, because a hitbox is
a `VoxelShape` — axis-aligned by definition — and the angle is a render-time transform the
game never sees.

berlord looked at this in game and **decided to keep it as it is**, hitbox included. It is
not a fault and it is not an open question. Do not "align" it.

## Previewing models without launching the game

`tools/preview.py` is a small isometric renderer for these block models. It is in the repo
now, because it had been written from scratch twice by the time it was worth keeping.

```bash
python tools/extract_assets.py path/to/create.jar
python tools/preview.py src/main/resources/assets/primitive_refined/models/block/p_grid.json out.png
```

Needs `pillow` and `numpy`. It draws two views, from opposite corners, so the front and
back of a block can be checked at once, and it implements the parts of the model format
these models actually use — per-face `uv` with `rotation`, `texture_size`, element
`rotation`, and alpha — the way `FaceBakery` does.

**Face `rotation` is the one that matters.** The previous previewer ignored it and so was
blind to the exact fault it was being used to look for: a 16x6 uv region sitting
transposed on a 6x16 face. If you doubt the renderer, render Create's own
`mechanical_crafter/block.json` with it — that model rotates uvs on all six faces, so it
either comes out looking like a crafter or the renderer is wrong.

**Extract each mod's assets whole, once**, from the jars pinned in this pack. Extracting
file-by-file is what left the previewer unable to resolve Create's parents and cost two
rounds of grid work shipped blind. `unzip` with a wildcard is unreliable here; on MSYS it
extracted directory entries and no files even with `MSYS2_ARG_CONV_EXCL` set — which is
why `extract_assets.py` uses Python's `zipfile`. It writes into `tools/extracted/`, which
is gitignored.

What it will not tell you: anything drawn by a block entity renderer or a Flywheel visual,
which for the grids is the shaft and the cogwheel. To look at those, render
`block/p_grid_kinetics.json` on its own, or merge its elements into the body by hand.

## The texture_size trap — uvs are always /16

**`texture_size` does nothing.** The string does not appear anywhere in NeoForge or in
Minecraft — grep the jars. It is a Blockbench authoring note, written into the file and
then ignored by the game. Every uv is normalised by **16**, whatever the texture's real
resolution: a 32x32 sprite is still addressed `0..16`.

So Create's cogwheel uvs, which look like they are in a 32-wide space because
`cogwheel.json` declares `"texture_size": [32, 32]` next to them, are not. They are
ordinary uvs and must be copied **verbatim**. Rescaling them to "match" the atlas breaks
them.

This cost a round. The grid's cogwheel kept Create's uvs and was right; the shaft's were
doubled to suit the declared 32, which ran them to 1.25 of the sprite — off the edge, into
the atlas — and the shaft rendered **black**. Two parts of one model, one correct and one
not, which is what made it look like a lighting fault a second time.

`tools/preview.py` deliberately does not honour `texture_size` either, for the same
reason. An earlier version did, and so disagreed with the game on exactly the models that
declare one.

## The occlusion trap — why instanced parts render black

A Flywheel instance is lit from the light value at the **block's own position**. A block
that occludes blocks light, so that value is zero, so anything drawn as an instance comes
out pitch black. The block's body is unaffected, because chunk-mesh faces take their light
from the neighbouring position instead — which is what makes this present as a texture or
material fault when it is neither.

The grids shipped in `v0.1.0` with exactly this: `gridProperties()` was the one set of
block properties in the mod without `.noOcclusion()`, and the grids were the only blocks
whose shaft and cogwheel were black. Every other block here that draws an instance — the
controller, the Soulstained Shaft, the whole cogwheel family — had always set it.

So: **any block with a Flywheel visual needs `.noOcclusion()`.** For these it is right
anyway; the rims have a see-through window and the back is recessed, so it was never a
solid cube.

## The crafter_side trap

`create:block/crafter_side` has a **48-pixel transparent window** at cols 2-13, rows 6-9.
Create never samples it: its side uvs take rows 0-6 on the front slab and rows 10-16 on the
back one, and the gap between them - z 6 to 10 - is a real hole in the geometry where the
mechanism shows. Stretch the whole texture over one 0-16 cube and the block is
see-through. This cost a round; it is why the body is built as two slabs and four rims.

Read the other way round, the window is not a trap but the *point*: it is the slot a
cogwheel's teeth stick out through. Create's crafter is an `ICogWheel` and its cogwheel is
full size, teeth from -1 to 17, so they emerge through this window and mesh. Ours does the
same.

## Verified in game

Confirmed by berlord in the s1 demo instance: the controller renders and lights, its
goggle readout reports every lit condition, it takes power both through its horizontal
shaft line and from a large cogwheel above (the mixin), it drives that cogwheel, and the
Soulstained Shaft relays force and spins.

Also confirmed: the grids light when powered, and our cogwheels place correctly onto a
large cogwheel (they did not before - the item must be Create's `CogwheelBlockItem`, whose
`onItemUseFirst` does the meshing; a plain `BlockItem` shows the ghost but places flat).

Not verified: the controller's shaft stubs spinning, the restored controller glow, and
anything with Flywheel's backend switched off.

**Nothing from the grid-body rebuild is in that list.** It all postdates the last session
in the s1 demo instance. The whole of the grid section above is previewer work; see Known
gaps for what to check first. The instance now carries the `v0.1.0` release, so it is
finally possible to look.
