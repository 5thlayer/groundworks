# Footprints live in the library, declared by each Consumer

The FactoryWorks Pack built the **Footprint** for its machines: an origin block holding the block entity, invisible part blocks on every other position, one item that places the whole and a break of any block that takes the whole down. FactoryWorks is now a suite of mods (its ADR-0115), and Craftworks', Wireworks' and the Pack's machines all stand on one, so it moves into Groundworks, the base they share, as **Rotate** (ADR 0003) and **Fast Replace** (ADR 0008) did. Placing and breaking a multi-block whole is placement, and Groundworks already plans and draws a plan of several blocks (5thlayer/groundworks#40).

- **A Consumer declares each footprint; Groundworks has none.** A declaration names the shape, the **Origin** block, the **Part** block and the item, at mod construction. Each footprint has its own part block, which its Consumer registers from the library's part class, so the part's numbering and facing name the origin's position and nothing is stored that a reload could lose.
- **The shape is in the origin's own frame:** forward, up and to the side, turned with the origin's facing by the turn the Pack's machines were built on, so their shapes and the models drawn over them carry over unchanged. The library's rotation is its own and names no other mod's type.
- **The origin is the Consumer's block,** with any superclass: it has a horizontal facing, calls the footprint's teardown from its own removal, one line, as four of the Pack's machines already do, and is one a piston doesn't move. The library's part class makes its parts immovable and lets them hide no neighbour's face, whatever properties the Consumer passes, since a part moved alone would strand the rest and an invisible full cube would show through the world. The library doesn't patch every block's removal to find origins itself.
- **Breaking any block breaks the origin as the player would.** A part's break drops what the origin's loot table gives with the player's tool, and the origin's block entity's own removal decides what else drops; parts drop nothing. So exactly one item comes back whichever block was hit, without the library knowing any inventory type.
- **A part forwards energy, fluid and item lookups to its origin,** for every part block the library's class registers, with the side unchanged, and a part, which has no block entity to do it, tells a cache of its capabilities when it is laid, turned or gone. An origin with no such capability answers none. A Consumer forwards any other capability itself.
- **Rotate in Place turns a footprint whole** (factoryworks #406's plan), on any of its blocks and with no separate statement: the origin stays where it stands with its block entity and takes the new facing, the parts are laid again for it, and the turn is refused whole, with its reason, when the turned shape doesn't fit. There is no preview of it, as of any Rotate in Place.
- **Jade reads a part as its origin**, through a plugin Groundworks carries with Jade as an optional compile-time dependency, so the library still runs alone.

## Considered Options

- **The Pack keeps the footprint.** Rejected. Craftworks and Wireworks may not depend on the Pack.
- **An origin base class.** Rejected. The Pack's origins already extend Oritech's machine or vanilla's directional blocks, and a Consumer's own machine class may extend anything.
- **A frame of the library's own, unmirrored.** Rejected. Every shape the Pack and Craftworks have drawn would need rewriting, and the models would no longer sit over their blocks.
- **A part pops the footprint's item itself,** as the Pack's did. Rejected. It drops nothing the origin holds unless the library knows its inventory, and it ignores the tool.
- **Footprints refuse Rotate in Place,** as the Pack's did. Rejected: a machine placed facing the wrong way would have to be broken and placed again.

## Consequences

- An API addition under ADR 0001's semver: the footprint, its declaration, the part block, the item and `Refusal.Footprint`.
- The Pack's machines switch to it and delete their copy, keeping what is theirs: the tooltip in their units, Oritech's assembled flag, Wireworks' energy owner, and dropping an Oritech machine's inventory from its block entity's removal.
- Groundworks has its first optional dependency, Jade, at compile time only.
