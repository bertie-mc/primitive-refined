# Primitive Refined

Primitive Refined is an early-game, Create-powered storage network for NeoForge 1.21.1.
It uses Refined Storage’s network and grid implementations, but replaces FE power and RS
cables with a dedicated family of Create kinetics: the shafts and cogs that carry rotation
also define the storage-network topology.

Create and Refined Storage are required dependencies.

## Machines

| Part | Stress impact | Role |
| --- | ---: | --- |
| Primitive Controller | 0 | Admits Create power and activates one primitive network |
| Arcanetic Shaft, Cog and Gearbox | 0 | Carry rotation and network connectivity |
| External Reader | 1 | Exposes an adjacent inventory as external storage |
| Mechanical Grid | 5 | Refined Storage grid interface |
| Mechanical Crafting Grid | 10 | Grid interface with a 3×3 crafting matrix |

A node is active while its kinetic block is turning, its Create network is not
overstressed, and its primitive network has exactly one controller. Each machine charges
stress at its own position; the controller’s displayed total is informational and is not
charged a second time.

## Kinetic topology

`KineticConnectionStrategy` delegates adjacency to Create’s
`RotationPropagator.isConnected`. Primitive Refined therefore has one topology rather than
separate kinetic and storage graphs: breaking a rotational connection also splits the
storage network.

Arcanetic kinetics form a closed family and do not mesh with ordinary Create shafts or
cogs. `RotationPropagatorMixin` enforces the separation at Create’s common rotation-speed
modifier. `PrFamilyGuard` drops a newly placed block when it would form a cross-family
connection, giving the player immediate feedback instead of leaving a dead-looking join.

The controller’s top face is the intentional exception. A horizontal Create large
cogwheel drives that face; the controller’s other faces remain arcanetic and carry the
primitive network.

## Storage behavior

The storage half of this mod is Refined Storage’s implementation, not a reimplementation
of it. Where RS’s class is public it is called; where it is package-private it is copied
with its licence, and where it is a base class this project cannot extend — every block
entity here already extends Create’s `KineticBlockEntity`, and Java has one superclass to
give — it is ported method for method under the same names, so the two can be read side by
side. `NOTICE` lists each case.

`PrNetworkNodeContainer` is RS’s `AbstractNetworkNodeContainerBlockEntity` plus the
network-facing half of `AbstractBaseNetworkNodeContainerBlockEntity`, held as a field
instead of inherited. RS finds a node through a NeoForge capability rather than an
`instanceof` on its own base class, so nothing is lost by composing it. Two things differ,
and both follow from the power being rotation rather than FE:

- `calculateActive()` asks whether the block is turning, the kinetic network is within its
  stress budget, and the primitive network has exactly one controller — in place of RS’s
  redstone mode and stored energy.
- Connections are re-read from a six-bit mask of meshed faces rather than from this block’s
  own state changing. Kinetic adjacency is a property of the *pair* of blocks, so RS’s
  signal does not fire for the changes that matter here.

RS rate-limits activeness changes to one every twenty ticks, to damp a network sitting on
its energy threshold. That oscillation does not exist here — activeness is pushed from the
lazy tick, already once a second, and from `onSpeedChanged`, which is an edge — so the
limit is deliberately not carried over.

The Mechanical Grid and Mechanical Crafting Grid use Refined Storage’s own menus and
screens. Sorting, search, view modes, item insertion and extraction, and the crafting
matrix remain RS behavior rather than local copies. Both menu constructors lay their slots
out the way RS’s own grid menus do, so the server and the client agree about what a slot
click means. Autocrafting is answered by RS’s own network component rather than stubbed:
no pattern provider can join a primitive network, so RS’s code returns nothing on its own
account.

The External Reader is RS’s External Storage. It composes every registered
external-storage provider for the adjacent block, using RS’s own rule that the first
provider to move anything wins; scans on RS’s adaptive work rate, which backs off to once
every two seconds when idle and closes to once every quarter second while a chest is being
worked, and is wound back up by a neighbour change; and remembers who last touched a
resource across a save. Its configuration menu — filters, fuzzy mode, access mode,
priority, void excess — is the part of RS's external storage that is intentionally absent.

Primitive Refined intentionally has no disks or storage blocks. A network contains only
what its External Readers can access.

## Rendering

Kinetic parts are rendered by Flywheel visuals so shafts and cogs turn with their Create
network. Static block models omit those parts to avoid rendering stationary geometry over
the instances; item models include them because inventory rendering has no Flywheel
visual.

The controller, grids and reader synchronize their server-computed operating state for
goggle tooltips and lit models. Client tooltips render synchronized state instead of
trying to inspect server-only network or inventory data.

## Current limitations

- There are no recipes yet; blocks are available from the creative tab.
- There is no RS security-manager equivalent or autocrafting machinery.
- Cross-family blocks created without a placement event, such as by world generation or
  `/setblock`, are left as inert joins instead of being dropped.
- With the Flywheel backend disabled, rotating partials have no fallback renderer.
- Several original registry IDs (`p_grid`, `p_crafting_grid`, `soulstained_shaft`, and
  `obsidiansteel_cogwheel_soulstained`) predate the current display names. They remain
  stable to preserve existing worlds.

## Building and testing

The project uses Java 21, Gradle 8.14.4 from the shared Nix environment, NeoForge
21.1.233, ModDevGradle 2.0.134, Minecraft 1.21.1, Create 6.0.10 and Refined Storage 2.0.9.
There is intentionally no Gradle wrapper.

From a shell containing the shared toolchain:

```bash
bertie-ci build --project . --output-dir .bertie-ci/artifact
bertie-ci unit-test --project .
```

The unit suite covers the External Reader’s multi-provider storage semantics and its scan
pacing, the grid menus’ slot layout, and verifies that every registered block ships its
blockstate, block model, item model, loot table and English name. Test code lives under
`src/test`; diagnostic commands are not included in the release mod.

CI keeps building and testing as separate jobs. Release workflows consume the artifact
from the build job and do not maintain another build recipe.

## Verification status

**The Refined Storage half was rebuilt on RS's own implementation after the run below, and
has not been re-exercised in a client since.** It compiles and the unit suite passes.
Everything in the list below was true of the code the run tested and the intent is that it
stays true, but the behaviour that changed needs eyes in game before it can be claimed
again:

- The External Reader now resolves what it reads when its node first goes active, rather
  than by polling the block in front of it every second.
- It now scans on RS's adaptive rate rather than every tick, so a chest's contents reach
  the grid with up to two seconds' delay while nothing else is happening.
- Both grid menus now lay out slots in the client constructor as well as the server one.
- A grid screen now closes when the player walks more than eight blocks away, which it did
  not before.

Exercised in a running client on 2026-08-01, against a rig of controller, two gearboxes,
a shaft, a cog, both grids, a reader and a chest, driven by a Create large cogwheel:

- Network forms across shafts, cogs and gearboxes, and reports one controller.
- Insertion and extraction both work, on the plain grid and the crafting grid.
- The crafting matrix is nine slots and resolves recipes; one oak log gave four planks.
- The reader exposes the chest it faces and notices contents added behind its back.
- Breaking a shaft splits the network; the offcut goes inactive and loses its storage.
- A second controller takes the whole network inactive and darkens the grids.
- Losing rotation does the same, and the network recovers when it returns.
- A Create shaft placed against an arcanetic one pops and drops; the arcanetic one
  survives. The gearbox drops our own item on both axes.

**Not verified.** Overstress specifically: a creative motor's capacity cannot realistically
be exceeded, so that branch of `PrNodes.isPowered` was only reached through its twin,
speed-zero. Both set the same flag and nothing downstream distinguishes them.

**Not verified, and not verifiable from `tools/preview.py`** — all four are drawn by a
Flywheel visual rather than a block model, so they need eyes in game:

- The direction each of the gearbox's four shafts turns. `ArcaneticGearboxVisual`'s rule
  is transcribed from Create's bytecode, not played; a wrong one is a flipped sign in
  `speedOf`, which decides direction only.
- The vertical gearbox's item model in hand.
- Whether the External Reader's two side displays visibly differ. They run different
  textures with different segment layouts, so they should.
- The grid's rotating parts on all four facings.

## Assets and authoring tools

Derived textures are distributed with permission from their respective authors. See
[NOTICE](NOTICE); those permissions are project-specific and are not granted to forks by
the Unlicense.

The repository includes four optional Python tools:

| Script | Purpose |
| --- | --- |
| `tools/extract_assets.py` | Extract dependency assets into the ignored `tools/extracted/` tree |
| `tools/preview.py` | Render the subset of Minecraft block models used by this project |
| `tools/make_reader_textures.py` | Regenerate the External Reader textures |
| `tools/make_gearbox.py` | Regenerate the gearbox texture, models, blockstate and loot table |

The generated reader and gearbox assets are checked in, but their generator scripts are
the source of truth. Do not hand-edit generated output.

`tools/preview.py` requires Pillow and NumPy. It does not render block-entity or Flywheel
visuals. Minecraft normalizes model UV coordinates by 16 regardless of Blockbench’s
`texture_size` authoring hint; the previewer follows that behavior.

## License

Original code is released under [The Unlicense](UNLICENSE). Third-party and derived assets
are excluded as described in [NOTICE](NOTICE).
