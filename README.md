# Primitive Refined

An early-game, Create-powered precursor to Refined Storage for **NeoForge 1.21.1**.

The machines behave one-to-one with their Refined Storage counterparts because they **are**
Refined Storage: every block here is an RS network node and the grids drive RS's own
container menus and screens. What is this mod's own is the power and the wiring — the
network runs on rotational force rather than FE, and **the shafts and cogs that carry that
force are the cables that carry the network**.

**Create and Refined Storage are both required dependencies.**

## The parts

| Part | Stress | Refined Storage equivalent |
| --- | --- | --- |
| Primitive Controller | 0 | Controller |
| Arcanetic Shaft / Cog / Gearbox | 0 | Cable |
| External Reader | 1 | External Storage |
| Mechanical Grid | 5 | Grid |
| Mechanical Crafting Grid | 10 | Crafting Grid |

The controller itself is free. You pay for what you hang off it — which is why a bare
controller spins up on any amount of rotational force, and a network full of crafting
grids will stall a waterwheel.

## How a network works

**The kinetic network is the storage network.** There is one topology, not two that have to
be kept in agreement: `KineticConnectionStrategy` answers RS's "which neighbours do you
connect to" by asking Create's `RotationPropagator.isConnected` which neighbours it is
already turning. A shaft line that carries rotation carries the network; break the line and
the network splits in the same tick, because it is the same break.

Consequences worth knowing:

- **A node is active when its block is turning and the kinetic network is not
  overstressed** — plus exactly one controller on the network. There is no RS energy
  anywhere in the mod. Overstressed is the brownout: the stress units are not in fact being
  supplied, so the grids go dark and the storage leaves the network.
- **Each part charges its own stress where it stands**, on Create's network. The controller
  charges nothing and reports the sum through the goggles as information only — billing it
  there as well would charge every grid twice.
- **Six faces only.** Create also meshes large cogwheels diagonally; the arcanetic family
  has no large cogwheel, so that case cannot arise inside a network.
- **A closed family.** A real Refined Storage cable or controller will not join a primitive
  network and a primitive part will not join an RS one. `canAcceptIncomingConnection`
  refuses anything that is not ours.

Topology changes that move no speed — a shaft placed against an unpowered line — are
noticed on the **lazy tick**, so up to half a second late. Anything that moves speed
arrives through `onSpeedChanged` immediately.

## Arcanetic parts do not mesh with Create's

Two kinetic families, and they refuse each other. `RotationPropagatorMixin` returns a
rotation speed modifier of **zero** for any cross-family pair, which is both "carries no
speed" and "not connected" at the one point every caller in the propagator routes through.

A block placed where the two would have met **breaks and drops** — `destroyBlock(pos, true)`,
the same call Create makes when a cogwheel is asked to turn two ways at once, so the two
failures look and sound alike. The block that pops is always the one just placed, which is
why `PrFamilyGuard` hangs off the place event rather than off the propagator: only there is
it known which of the two is new.

Whether Create *would* have meshed them is asked by suppressing the veto for the length of
one question and putting it to Create, rather than by reimplementing its meshing rules here.

**The one crossing is the controller's roof.** Family membership is per *face*, not per
block: the controller's top face belongs to Create, everything else about it is arcanetic.
That is how rotational force gets into a network at all, and it is the only way it can.

**Not covered:** blocks that appear without a place event — contraption disassembly,
`/setblock`, worldgen. Those leave a dead join rather than a pop. Nothing drives across it
either way, so the failure mode is inert rather than wrong.

## The blocks

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

Its own stress impact is **zero** and stays zero: every part on the network charges where it
stands, so a bare controller still lights on any rotational force at all. The goggle readout
adds the network's total demand and its controller count to the conditions it already
reported.

**Only one controller to a system**, which is Refined Storage's rule and berlord's. Zero
means nothing is feeding it; more than one and every node on the network goes inactive
rather than one of them being picked as the real one, because which one that would be is not
a question with an answer. The controller says so through the goggles.

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

**Extraction works in game; insertion does not.** See Known gaps.

**They open Refined Storage's own grid screens.** Sorting, the search box and its query
syntax, the view modes, the synchronizers, insert, extract, scroll, shift-click and — on the
crafting grid — the 3x3 matrix, recipe transfer from EMI or JEI, and crafting straight out
of the network are all RS's classes, bound to menu types this mod registers.
`PGridContainerMenu` and `PCraftingGridContainerMenu` are two-constructor shims and nothing
more; nothing about the grids' front end is reimplemented, so nothing about it can drift
from RS's.

A grid opens whether or not the network is running, the way RS's own does — a dark, empty
grid is a readable answer to "is this thing on", and closing the screen in the player's face
is not. The screen lights on the same condition the node is active on.

**Autocrafting is not implemented and there is nothing to implement it with:** no pattern
provider, no autocrafter. The four `PreviewProvider` methods answer "nothing" rather than
throwing, and no resource is ever shown as autocraftable.

### External Reader (`primitive_refined:external_reader`)

Create's threshold switch, driven by rotation rather than by a comparator. It is the
network's **External Storage**: one per attached inventory, one stress. Horizontal facing
like the grids — indicator towards you, shaft on the face away — a deliberate narrowing,
since Create's own switch can also sit on floors and ceilings but the shaft has to arrive
somewhere and a horizontal line is where this mod puts it.

**It reads the inventory in front of it**, so the block sits in a straight line between the
two: shaft, reader, chest. Every registered `ExternalStorageProviderFactory` is **composed**,
not merely tried in turn, so a reader picks up anything an RS External Storage would — this
matters because `ItemHandlerPlatformExternalStorageProviderFactory.create` never returns
null, so "take the first factory that answers" silently meant "take whichever is first in
the collection". RS's own composite for this is package-private, hence
`CompositeStorageProvider`.

**Its goggle readout states what it can see**: the block in front, whether that block hands
out an item handler on the face the reader touches, how many of its slots are empty, and
whether the node is on a live network. Between them those separate "not powered", "not
connected", "not an inventory" and "the chest is full".
Change detection runs every tick — RS rate-limits its own against an adaptive work rate, but
a primitive network is small enough to afford it and it is what makes the grid feel live.

**This is the only storage medium in the mod.** No disks, no storage blocks. What a network
holds is whatever its readers can see, which is the early-game shape of the thing: you are
wiring up the chests you already have.

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

**Its drops are overridden, and had to be.** Create's `GearboxBlock#getDrops` bypasses the
loot table whenever the axis is horizontal and hands back `AllItems.VERTICAL_GEARBOX`
directly — one block, two items, and the block decides which one comes back. Inherited
unchanged, that made an Arcanetic Gearbox placed on its side drop a **Create** Vertical
Gearbox; the loot table was never consulted and was never wrong. `getDrops` now applies the
same rule with our items.


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

`build.yml` composes the independent `bertie-ci` build, client world-join, and
dedicated-server readiness jobs. Both runtime jobs install shared, hash-pinned fixtures
before loading this mod's built artifact. There are no unit-test or GameTest jobs because
the repository does not yet contain either kind of test.

Refined Storage is a hard dependency, so both runtime jobs select the shared
`create,refined-storage` fixture. `bertie-ci` resolves both names directly from the
hash-pinned canonical pack; no one-to-one profile declaration is needed. The client
world-join and dedicated-server probes both pass with that dependency set.

`release.yml` composes the same build job with the artifact-only GitHub publisher, so a
release never maintains or runs a second build recipe.

## The mixin

`RotationPropagatorMixin` does two things, in one injection point, in declaration order.

The **second** is the cross-family veto described above. The **first**, and the original
reason the mixin exists, adds the missing large-cogwheel-drives-controller case. Create
supports it for its own speed controller via `isLargeCogToSpeedController`, hardcoded to
`AllBlocks.ROTATION_SPEED_CONTROLLER`, and exposes no hook on the receiving side - the
propagator only asks the *upstream* block for a custom connection, and upstream here is
Create's own cogwheel entity.

Both inject at the head of the private `getRotationSpeedModifier`, which is the single
point every caller routes through: `getConveyedSpeed` multiplies the source speed by it,
and `isConnected` tests it for non-zero.

Mixin runs callbacks at a shared injection point in declaration order and stops at the first
that cancels, so the sanctioned bridge is decided before the veto ever sees it. It would
survive either order — the controller's top face is Create's, so that pair is not
cross-family — but the ordering makes the intent explicit rather than incidental.

**Being private, it is not API.** Re-check this mixin on every Create bump. It is set to
`defaultRequire: 1`, so if the target moves the mod fails loudly at load rather than
quietly losing the connection.

## Known gaps

### The whole storage layer is unverified in game

Everything in "How a network works", "Arcanetic parts do not mesh with Create's" and the
grid and reader sections above **compiles and has never been run.** No client has been
launched against it. It is written against Refined Storage 2.0.9's API as read out of the
jar with `javap`, which gives signatures and bytecode but not intent, so the places to look
first are the ones where intent mattered:

- **Does a network form at all?** The one load-bearing assumption is that RS resolves a
  node through the `NetworkNodeContainerProvider` **block capability**
  (`RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability()`) rather
  than through an `instanceof` on its own base class. That is what lets a Create
  `KineticBlockEntity` be an RS node at all — Java has one superclass to give and Create has
  it. If nothing connects, this is why.
- **Do the grid screens open, and do sorting, search and shift-click work?** They are RS's
  screens under menu types this mod registers, opened with NeoForge's extended screen data
  and `GridData.STREAM_CODEC`.
- **Does the crafting grid craft?** `startExtractTransaction`'s boolean was read off RS's
  bytecode and is **the opposite way round to the obvious guess** — true is direct commit,
  false is snapshot. If crafting duplicates or eats ingredients, start there.
- **Does the External Reader see a chest?** And on the face intended — see below.
- **Does a cross-family placement actually pop?** And, more importantly, does a
  *same*-family one not: the veto runs inside `getRotationSpeedModifier`, on every
  propagation in the world, so a false positive would break Create itself.
- **Two controllers on one network** should darken everything and say so through the
  goggles.

### Inserting into the grid does not work, and the cause is not yet known

**Extraction works; insertion does not.** Reported from the first in-game test of 0.2.0.

The whole insert path was traced through Refined Storage's bytecode and **no defect was
found in it.** `AbstractGridContainerMenu.onInsert` and `onExtract` are symmetric;
`GridOperationsImpl.insert` is `TransferHelper.transfer(cursor -> rootStorage)`;
`CompositeStorageImpl.addSource` populates the insert and extract source lists together.
The only guard `compositeInsert` has that `compositeExtract` does not is
`StorageConfiguration.isAllowed`, and the default filter is `FilterMode.BLOCK` over an
empty set, which allows everything. `AccessMode` defaults to `INSERT_EXTRACT`. Nothing was
logged.

Also checked and identical to RS's own: both packet handlers dispatch on the **interface**
(`GridInsertionStrategy` / `GridExtractionStrategy`), not on RS's concrete menu class; the
server-side menu is built by the same protected constructor and gets its strategies from
the same `initStrategies`; and `MenuOpenerImpl` opens an `ExtendedMenuProvider` with exactly
the `serverPlayer.openMenu(provider, buf -> codec.encode(...))` this mod uses. **There is no
structural difference left between our grid and RS's.** The difference is in state.

So 0.2.2 asks the block instead of reading more bytecode. The External Reader's goggle
readout now performs a **simulated insert of one stone at the root storage** — the same call
`TransferHelper` makes on a player's behalf — and reports the answer, alongside the target's
slot and empty-slot counts. That splits the problem in half:

| Readout | Where the fault is |
| --- | --- |
| `empty: 0` | The chest is full. Not a bug. |
| would accept **yes** | The storage is willing; the fault is between the grid and it. |
| would accept **no**, with empty slots | The storage refuses; the fault is in the reader or the network. |

### The 0.2.1 readout answered about the wrong world

Worth recording because it is a trap this mod will meet again: **Create's goggle tooltip is
a client HUD.** `addToGoggleTooltip` runs on the client, where a primitive network does not
exist — it is only ever built server-side — and where a chest's contents are not present
either. The readout added in 0.2.1 asked its questions there, so it reported an absent
network and an empty inventory with total confidence. That is worse than no readout.

0.2.2 computes the diagnosis on the server in the lazy tick and ships it through
`write`/`read` with `sendData()`, which is how Create syncs block entity state for goggles.
The controller's network demand had the same fault — it was a server-only field being read
on the client, showing `0.0 su` regardless — and is now synced the same way, along with its
controller count.

### The External Reader's inventory face is a coin-flip that was flipped one way

The reader reads the block **in front** of it — the face with the display — because the
shaft has to be on the back and that puts the three in a line. The consequence is that
placing one against a chest means placing it while looking *away* from the chest, since
`getStateForPlacement` puts the front towards the player, as the grids do.

The alternative is to face it away from the player on placement, the way RS's own External
Storage does, at the cost of the reader no longer being placed like the grids. Not asked
about, and a one-line change either way.

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

- **The ids in `PrCogwheels.NAMES` are one entry long.** Its javadoc still describes six
  variants, three gear materials by two shaft materials. That was true once.
- **No security, and no place to put it.** RS wraps its grid operations in
  `SecuredGridOperations`; this does not, because there is no security card, no security
  manager and no network owner anywhere in the mod for it to consult. The fuzzy wrapper
  *is* applied, so shift-clicking a damaged tool still finds the other damaged ones.
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

**Nothing in the storage layer has been verified at all.** It has never been launched. See
"The whole storage layer is unverified in game" above for the specific things to look at,
in the order worth looking at them.
