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
| External Reader | 1 | External Storage |
| Mechanical Grid | 5 | Grid |
| Mechanical Crafting Grid | 10 | Crafting Grid |

Plus the Arcanetic Shaft and Arcanetic Cog for wiring, and the Arcanetic Gearbox for
turning a line of shafts a corner — Create parts in this mod's materials, with no storage
role of their own.

The controller itself is free. You pay for what you hang off it — which is why a bare
controller spins up on any amount of rotational force, and a network full of crafting
grids will stall a waterwheel.

## What is in this build

Four of the five parts exist. **The Primitive Cable does not, so nothing forms a network
yet** — every block below is a Create machine with the right cost and the right look, and
none of them store or move a single item. That work starts with the cable.

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

### Arcanetic Shaft (`primitive_refined:soulstained_shaft`)

Create's shaft, in soulstained steel. Same axis placement and alignment, same kinetic
relay behaviour, same shape; it extends `AbstractSimpleShaftBlock`, the same class Create's
own `ShaftBlock` extends, rather than reimplementing any of it.

It exists as a separate block so primitive machines can be wired with a visually distinct
line, and so a later version can restrict which shafts carry a network.

### Mechanical Grid / Mechanical Crafting Grid (`p_grid`, `p_crafting_grid`)

Refined Storage screen on the front, a recessed brass gearbox face on the back, mechanical
crafter body. Rotation enters along the facing axis, through the shaft standing in the
gearbox well, and the screen lights once the block is turning and the network is not
overstressed - an overstressed network means the stress units are not actually being
supplied, which is the unpowered case. They demand 5 and 10 stress.

The body follows Create's mechanical crafter element for element: two slabs with a gap at
z 6-10 and four thin rims. Every `crafter_side` face carries **Create's own face
`rotation`** — without it a 16x6 uv region lands transposed on a 6x16 face and every side
of the block stretches.

The back is the gearbox face at **three depths**: the two-pixel perimeter at the block
face, the plate one pixel in, and an 8x8 well three in with the shaft standing at the
bottom of it. Each step is a picture frame of four bars — full-width top and bottom, inset
sides — because that is the arrangement in which no two faces are ever coplanar, and
coplanar is what z-fights.

That deep well is a **deliberate choice, not a reading of the reference.** The gearbox
texture is two depths: perimeter and shaft's 4x4 at the block face, everything between one
pixel in. Both were built and placed side by side in game, and berlord picked the sunken
one — it reads better than it measures. Do not "correct" it back to the texture.

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

### External Reader (`primitive_refined:external_reader`)

Create's threshold switch, driven by rotation rather than by a comparator. It is the
network's **External Storage**: one per attached inventory, one stress. Horizontal facing
like the grids — indicator towards you, shaft on the face away — a deliberate narrowing,
since Create's own switch can also sit on floors and ceilings but the shaft has to arrive
somewhere and a horizontal line is where this mod puts it.

Its front is Create's pixels moved, not repainted: the 4x4 hue-rotated red to purple with
saturation untouched and value lifted, the ring around it scaled down in value alone.
Create's wood surround, top and bottom are untouched. The back is the **two-depth**
scheme — 2px perimeter and the middle 4x4 at the block face, everything between one pixel
in — with that 4x4 being the turning shaft rather than texture. Note this is not the grids'
back: they use three depths with a sunken well. Both were asked for deliberately.

The indicator down each side is a 4x10 panel, lit throughout while the block turns. It is
carved into short vertical runs, each carrying a gradient that sweeps purple to magenta to
a bright peak and advances one step per frame, so the gradient travels along its segment.
Colours, ramp and cadence — twelve frames at frametime 2 — are measured off
`refinedstorage:block/controller/cutouts/purple`, because that is how the controller draws
its traces.

**It is worth knowing what this is not.** An earlier attempt lit and unlit individual
pixels on a checkerboard, with a flare passing over each. At 4x10 that reads as a static
checkerboard with white dots skittering across it. Per-pixel twinkle is the wrong model
for this; flowing gradients along runs is the right one.

Opposite sides run **different textures**, because Minecraft drives every animation off
the same game time — one texture on both faces is frame-locked and no amount of authoring
will unstick it.

### Arcanetic Gearbox (`primitive_refined:arcanetic_gearbox`)

Create's gearbox in this mod's materials. `ArcaneticGearboxBlock` extends Create's
`GearboxBlock` outright, so the awkward part — redirecting rotation around a corner and
reversing it across the block — stays Create's code and stays correct. The block entity
type is ours but holds Create's own `GearboxBlockEntity`, the same trick the Arcanetic
Shaft plays with `BracketedKineticBlockEntity`. **There is no speed or stress stat**: it
relays at input speed, exactly as Create's does.

The **Vertical Arcanetic Gearbox** is the same block placed on a horizontal axis, via its
own item — one block, two items, which is how Create ships its own. Create's
`VerticalGearboxItem` could not be reused: it takes only item properties and resolves
Create's gearbox internally.

The panel is a per-pixel substitution into Create's own: the light casing ring takes brass
casing's pixel at the same coordinate, and the wood and shaft socket take
`create:block/brass_gearbox` — which is the face Create's **sequenced gearshift** puts on
the two ends its axis runs through. That matters: a gearbox panel has a shaft coming out
of it, so the face substituted onto it must be one that also has a shaft.
`create:block/sequenced_gearshift` is the sequencer display on the four sides the axis
does *not* pass through, and putting that on a face with a shaft in it drops a red display
strip across the panel. Brightness is what separates the perimeter ring from the socket —
both are grey, but the socket is nearly black, and treating all grey alike fills the
socket with casing and erases the hole.

The four shafts needed a visual of their own. Create's `GearboxVisual` names
`AllPartialModels.SHAFT_HALF` inside its constructor and builds its instance map there, so
it cannot be subclassed and swapped. `ArcaneticGearboxVisual` is that class's shape
rewritten against our partial; the only logic in it decides each shaft's **direction**,
never its speed.

## Assets

Derived textures are **shipped outright, with permission obtained from the respective
authors** — see [NOTICE](NOTICE). That permission is personal to this project: a fork does
not inherit it.

Recolours are a luminance remap rather than a hue shift, so Create's shading survives and
only the palette changes. That matters mechanically as well as visually — the axis
texture's lengthwise grooves are what make a spinning shaft read as spinning.

## Deployment status

Released. [`bertie-mc/primitive-refined`](https://github.com/bertie-mc/primitive-refined),
jar attached to each GitHub Release by `release.yml`. The current version is whatever
`mod_version` in `gradle.properties` says — this file deliberately does not repeat it.

**`packs/s1-pack` does include it** — `mods/primitive-refined.pw.toml`, added with
`packwiz github add bertie-mc/primitive-refined` and moved forward with `packwiz update`.
The **s1 demo** instance is synced against that pack. It is *not* in `packs/bertie-pack`,
`packs/full-test-pack` or `packs/worldgen-pack`; adding it is one `packwiz github add`
each.

### A hand-copied jar cannot survive a sync — do not try

The obvious shortcut for a quick look in game is to build locally and drop the jar into
the instance, keeping the filename identical so nothing is duplicated. **It does not
work.** packwiz records a **hash**, not a filename: the next sync sees the file does not
match, and re-downloads the released jar over it. This cost a round — a sync ran two
minutes before the game launched, and three brand-new blocks were simply absent, with the
log reporting them as unknown registry keys.

So there is no in-game-before-release route for a packwiz-managed instance. Bump, tag,
let the release build, `packwiz update`, sync. It is about six minutes and it stays put.

### CI

`build.yml` composes the independent `bertie-ci` v3.2.1 build, client world-join, and
dedicated-server readiness jobs. Both runtime jobs install the shared, hash-pinned
`create` fixture before loading this mod's built artifact. There are no unit-test or
GameTest jobs because the repository does not yet contain either kind of test.

`release.yml` composes the same build job with the artifact-only GitHub publisher, so a
release never maintains or runs a second build recipe.

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

### Display names and registry ids have diverged

The blocks were renamed; their ids were not. So:

| Shown in game | Registry id |
| --- | --- |
| Mechanical Grid | `p_grid` |
| Mechanical Crafting Grid | `p_crafting_grid` |
| Arcanetic Shaft | `soulstained_shaft` |
| Arcanetic Cog | `obsidiansteel_cogwheel_soulstained` |

The blocks added since — `external_reader`, `arcanetic_gearbox` — have ids that match
their names, so the mod now runs two conventions at once.

Deliberate, for now: renaming an id destroys every placed block of that type in every
existing world, and berlord is actively testing in one. **The window to fix this closes
the moment anything references these ids** — a recipe, a quest, a tag, another pack.
Nothing does yet. Do it before anything does, or accept the ids permanently.

The Primitive Controller and the creative tab still read "Primitive"; only the four blocks
above were renamed.

### What the grid body still has not been checked for

berlord has now seen the rebuilt body in game and taken it as it stands, so the six faults
the previous round handed off are closed: the sides no longer stretch, the back is
recessed, the shaft stands in its well, the cogwheel is in the gap, and both light
correctly. The sunken back was chosen there over the flat one.

### The External Reader and the gearboxes are barely tested

berlord has seen the reader's display and accepted it after three passes at the effect,
and has called the gearboxes good. Nothing else about either has been checked. In
particular:

- **The gearbox shafts' directions.** `ArcaneticGearboxVisual`'s rule is transcribed from
  Create's bytecode, not played. If one of the four turns the wrong way it is a flipped
  sign in `speedOf`, which decides direction only — magnitude is always the input speed.
- **The vertical gearbox's placement**, and whether its item model reads right in hand.
- **The reader's shaft** turning, and its lit/unlit switching under load and overstress.
- **Whether the two sides' displays visibly differ.** They run different textures with
  different segment layouts, so they should.

### What the grid body still has not been checked for

Two things about it were **never explicitly looked for**, only assumed from their code:

- **Cogwheels meshing against the sides.** `PGridBlock` is an `ICogWheel`, so a cogwheel
  laid alongside on a parallel axis should mesh and counter-rotate, the way Create's
  crafters do. Nobody has put one there.
- **The visual on all four facings.** `OrientedRotatingVisual.backHorizontal` turns SOUTH
  onto `HORIZONTAL_FACING.getOpposite()` — a direction, so the sign is right, unlike
  `SingleAxisRotatingVisual`, which turns onto the *axis* and would land a one-ended shaft
  on the wrong end for two facings out of four. That reasoning is from the bytecode. A
  grid facing each of north/east/south/west would confirm it in ten seconds.

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

## tools/

Four scripts, all runnable from the repo root, none of them part of the build.

| Script | What it does |
| --- | --- |
| `extract_assets.py` | Pulls another mod's `assets/` out of its jar, whole, into `tools/extracted/` (gitignored). Everything else here needs it run first. |
| `preview.py` | Isometric renderer for block models — see below. |
| `make_reader_textures.py` | Regenerates the External Reader's front, unlit side, and the three glow layers. |
| `make_gearbox.py` | Regenerates the Arcanetic Gearbox's panel texture, its three models, its shaft partial, blockstate and loot table. |

The two `make_*` scripts are **generators of record**: the textures and models they emit
are checked in, but they are derived from Create's and Refined Storage's pixels by rule,
not by hand, and the rules are in the scripts. Change a colour or a substitution there and
re-run it — do not hand-edit the output, or the next run silently reverts you.

The grids' models are the exception: their generator was never checked in, so
`p_grid*.json` are hand-maintained, and the four of them must be kept identical below the
front texture. Their `__comment` blocks carry the structure.

### Previewing models without launching the game

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
controller, the Arcanetic Shaft, the whole cogwheel family — had always set it.

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

Confirmed by berlord in the development instance: the controller renders and lights, its
goggle readout reports every lit condition, it takes power both through its horizontal
shaft line and from a large cogwheel above (the mixin), it drives that cogwheel, and the
Arcanetic Shaft relays force and spins.

Also confirmed: the grids light when powered, and our cogwheels place correctly onto a
large cogwheel (they did not before - the item must be Create's `CogwheelBlockItem`, whose
`onItemUseFirst` does the meshing; a plain `BlockItem` shows the ghost but places flat).

Not verified: the controller's shaft stubs spinning, the restored controller glow, and
anything with Flywheel's backend switched off.

Since then, and confirmed the same way: the rebuilt grid body renders correctly and its
cogwheel and shaft are lit rather than black; the sunken back was chosen over the flat one
by looking at both; the Arcanetic Shaft's resting angle was looked at and kept; and the
gearboxes were called good.

Everything else on the External Reader and the gearboxes is previewer work — see
Known gaps for what to check first.
