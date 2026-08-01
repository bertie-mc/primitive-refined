# Primitive Refined

> [!IMPORTANT]
> Development has moved to the [`bertie` monorepo](https://github.com/bertie-mc/bertie/tree/main/mods/primitive-refined).
> This repository is retained read-only for historical tags, releases, and issues.

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

The Mechanical Grid and Mechanical Crafting Grid use Refined Storage’s network
components, menus and screens. `PrNetworkNodeContainer` adapts RS node behavior to Create
block entities: activity comes from rotation, stress and controller count, while network
connections follow the faces that are kinetically meshed. Upstream code reused or adapted
by this project is identified in [NOTICE](NOTICE).

The External Reader delegates adjacent storage to RS’s registered external-storage
providers. The first provider that moves a resource wins, idle readers reduce their scan
rate, neighbour changes wake them promptly, and last-modified ordering survives reloads.
The reader does not expose RS’s filter, priority, access-mode or void-excess configuration.

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
`src/test`.

CI keeps building and testing as separate jobs. Release workflows consume the artifact
from the build job and do not maintain another build recipe.

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
