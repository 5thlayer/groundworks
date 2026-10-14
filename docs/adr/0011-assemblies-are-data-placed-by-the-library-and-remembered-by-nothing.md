# Assemblies are data, placed by the library and remembered by nothing

Voidworks' Void Well became a 5×5×5 structure of real blocks a player assembles by hand: 56 casing, 16 rim and a core. Vanilla has the same kind: a beacon's pyramid, a conduit's frame, a nether portal's frame, a golem's body. A **Footprint** (ADR 0009) is the wrong tool for them: it is one block on several positions, broken whole, while these are several blocks, each its own item, loot and texture, that read as masonry and break one at a time (5thlayer/groundworks#51).

**Decision.** Groundworks places an **Assembly** in one step from its **Key**, and nothing more.

- **It is declared as data**, not code: a JSON file under `data/<namespace>/groundworks/assembly/`, the shape as layers of strings with a legend, in the Key's frame. A letter of the legend names a block, or a tag with the default a creative player gets, or a position that must stay empty; one marks the Key. Vanilla's beacon pyramids, conduit frames, nether portal frame and iron and snow golems ship as data, and a pack adds its own the same way.
- **Which blocks are Keys is a statement**, `Assemblies.keys`, which a Consumer makes as it makes its Opt-in, several adding up. The vanilla Consumer makes it from one block tag, `groundworks:assembly_keys`, shipped with vanilla's Keys, as ADR 0005 sets every reach of the vanilla Consumer, so a pack or a Consumer such as Voidworks still makes a Key with a tag entry and no code (5thlayer/groundworks#64).
- **Nothing remembers an Assembly once placed.** Its blocks stand block by block: breaking one breaks that block, **Rotate in Place** turns one block, and Groundworks never checks that it is whole. Whether it works is its Consumer's own rule, vanilla's beacon level or Voidworks' `WellStructure`, as it is for one the player built by hand.
- **The Key goes down last**, after every other block, so vanilla's rules that watch for a finished shape, a golem's pumpkin, see one. Its placement, through its item's own rule, is asked beforehand of a read-only level that holds the shape's blocks where they will stand and the world's everywhere else, so a Key the shape makes placeable, a torch on a wall it builds, is planned, and one it makes unplaceable, a door under one of its blocks, refuses the whole before anything is laid. Where vanilla still refuses the Key once the rest is laid, for what that level doesn't plan, the laying is taken back wherever something of it still stands, and only what is gone is charged, so no Assembly stands without its Key (5thlayer/groundworks#65).
- **It is paid whole from the inventory**, free in creative, and refused whole when a position is taken or the inventory falls short. A block the shape accepts already standing at its position stays, uncharged.
- **A Key may have several Assemblies, and the Key alone is always one of the choices**, which **Next Assembly** steps through. A declaration may start a fresh stack on the Key alone, as the portal's obsidian does, so obsidian stays a building block.

## Considered Options

- **Extend the Footprint.** Rejected: a Footprint's parts are invisible stand-ins that break whole, and an Assembly's blocks must stay real blocks that drop as themselves.
- **A code API, as Voidworks' `WellStructure.layout` is.** Rejected: vanilla's Consumer states everything as data, a pack could not add or change a shape, and Voidworks would need a dependency on Groundworks. As data, Voidworks ships its Well's file and tag entry and needs none: it is placed in one step whenever Groundworks is present.
- **Remember a placed Assembly**, to check it whole, break it whole or turn it whole. Rejected: the record goes stale the moment a player changes a block by hand, and every Consumer already checks its own structure.
- **A keyless portal frame placed from any obsidian, or lit by flint and steel as its Key.** Rejected: an Assembly always has a Key, and lighting a portal is the player's act, not part of the frame.
- **Lay the shape into a scratch copy of the world to ask the Key's placement, then undo it.** Rejected: the preview asks the plan continuously, on the client's own level, and laying there fires block updates; the planned level reads the shape without writing anything.
- **Allow only Keys whose placement reads no neighbour, refused at load.** Rejected: no code can tell which those are, and a torch on a wall is a Key a pack should have.
- **Charge nothing when a laying is taken back.** Rejected: what the laying spawned, a golem, would be free; the player pays for what didn't come back.
- **The largest Assembly the inventory affords.** Rejected: the placement would change under the player as the inventory changes.

## Consequences

- An API addition under ADR 0001's semver: the data format, `Assemblies.keys`, `groundworks:assembly_keys`, and the **Next Assembly** key.
- **Rotate the Plan** counts a held Key's drawn Assembly as rotatable, even when the Key's own block has no facing, such as a beacon.
- The planned level is a `Level` of Groundworks' own, tied to vanilla's abstract surface, which a Minecraft update may change; light, heightmaps and entities are read from the world as it stands, so a Key whose survival reads light under a roof the shape builds is judged by the light before it, and is taken back, not left alone, should vanilla judge otherwise.
- A Consumer whose structure is checked in code, as the Well is, states its shape twice, in its code and in its file, until it reads its own file.
