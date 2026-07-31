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

A mechanical crafter body, rebuilt from Create's own element layout: two slabs with a gap
at z 6-10 and four thin rims. That gap matters - `crafter_side` carries a 48-pixel
transparent window at rows 6-9 which Create never samples, because its side UVs take rows
0-6 and 10-16 only and the gap in the geometry is where the mechanism shows. Ours shows a
cogwheel there. Refined Storage screen on the front, the sequenced gearshift's shaft face
on the back. Rotation enters along the facing axis
through that rear shaft, and the screen lights once the block is turning and the network
is not overstressed - an overstressed network means the stress units are not actually
being supplied, which is the unpowered case.

They demand 5 and 10 stress respectively.

**Their Refined Storage behaviour is not implemented.** They are kinetic blocks with the
right cost, appearance and lit state; they hold no items, open no GUI and join no storage
network. That work needs the Primitive Cable to exist first.

## Assets

Derived textures are **shipped outright, with permission obtained from the respective
authors** — see [NOTICE](NOTICE). That permission is personal to this project: a fork does
not inherit it.

Recolours are a luminance remap rather than a hue shift, so Create's shading survives and
only the palette changes. That matters mechanically as well as visually — the axis
texture's lengthwise grooves are what make a spinning shaft read as spinning.

## Deployment status — the packs do not have this mod

`primitive_refined-0.1.0.jar` was copied by hand into the **s1 demo** Prism instance
(`instances/s1 demo/.minecraft/mods/`) for testing. That is the only place it exists.

It is **not** in `packs/s1-pack`, `packs/bertie-pack`, `packs/full-test-pack` or
`packs/worldgen-pack`, and it cannot be until there is a GitHub Release to point
`packwiz github add bertie-mc/primitive-refined` at. Loose jars must never go into
`packs/*/mods/`.

So: a fresh instance built from any pack will not have this mod, and the hand-placed jar
will be wiped by the next pack sync of that instance. Closing the gap means creating the
`bertie-mc` repo, tagging `v0.1.0`, and adding it to the pack from the release.

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

- **With Flywheel's backend off, the controller's shaft stubs do not render at all.** They
  are drawn only by a Flywheel visual; unlike the shaft, no block entity renderer fallback
  is registered for them, because a fallback would have to place and orient the stubs by
  hand through `CachedBuffers.partial` and that path is untested. The body and everything
  else is unaffected.
- **No recipes.** Both blocks are creative-tab only so far.

## Verified in game

Confirmed by berlord in the s1 demo instance: the controller renders and lights, its
goggle readout reports every lit condition, it takes power both through its horizontal
shaft line and from a large cogwheel above (the mixin), it drives that cogwheel, and the
Soulstained Shaft relays force and spins.

Not yet verified: the controller's shaft stubs spinning (added last, untested), and
anything with Flywheel's backend switched off.
