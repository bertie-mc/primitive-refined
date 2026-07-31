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

This is the **demo**: two of the five parts.

### Primitive Controller (`primitive_refined:p_controller`)

Topologically Create's rotational speed controller. Rotation runs through it along its
horizontal axis, and a **large cogwheel goes on top** — without one it stays dark no
matter how fast the shaft line under it is turning. The cogwheel must be horizontal and
perpendicular to the controller's own axis, the same three conditions Create checks for
its own speed controller.

When it is running, the Refined Storage trace pattern on its sides lights up and animates.

The stress it demands is the sum of the network attached to it, so in this build — with no
cables or grids to attach — it is zero, and it lights on any rotational force at all.

### Soulstained Shaft (`primitive_refined:soulstained_shaft`)

Create's shaft, in soulstained steel. Same axis placement and alignment, same kinetic
relay behaviour, same shape; it extends `AbstractSimpleShaftBlock`, the same class Create's
own `ShaftBlock` extends, rather than reimplementing any of it.

It exists as a separate block so primitive machines can be wired with a visually distinct
line, and so a later version can restrict which shafts carry a network.

## Assets

**This mod ships no Create art.** Create's licence makes its `assets/` All Rights Reserved
even though its code is MIT, so the models reference Create's texture *paths* and let
Minecraft resolve them at runtime — a reference, not a redistribution. The shaft's colour
is a runtime tint over Create's grey axis texture, not a recoloured copy of it.

The only textures in the jar are the controller's two overlay layers, derived from Refined
Storage's MIT purple controller cutout. See [NOTICE](NOTICE).

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

## Temporary: the shaft borrows a Malum texture

The Soulstained Shaft and the controller's shaft stubs currently reference

    malum:block/storage_blocks/block_of_soul_stained_steel

as a placeholder until proper art exists. Malum is **All Rights Reserved**, so it is
referenced by path and resolved at runtime like Create's - no Malum pixel is in this jar.

Two consequences worth knowing:

- **Malum must be installed** or the shaft renders as a missing texture. It is not
  declared as a hard dependency, because this is placeholder art and the mod does not
  otherwise touch Malum. The s1 pack ships Malum, so this holds there.
- The shaft deliberately carries **no `tintindex`**. It cannot: Create's
  `KineticBlockEntityRenderer` bakes models into a `SuperByteBuffer` and applies no colour
  at all, so a tinted shaft is drawn grey the moment it starts spinning. The colour has to
  live in the texture. Whatever replaces this placeholder must be a real texture too.

## Known gaps

- **The controller can drive the cogwheel above it, but not be driven by it.** Create's
  `RotationPropagator` hardcodes its own block for the large-cog-to-speed-controller case,
  so the reverse direction would need a hook on Create's cogwheel entity that an addon
  cannot reach. Power the controller through its shaft line.
- **No recipes.** Both blocks are creative-tab only so far.
- Nothing here has been verified in a running client yet.
