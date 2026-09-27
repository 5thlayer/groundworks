# Vanilla is a built-in Consumer, and tags set what it reaches

Groundworks is meant to work in a vanilla game with no other mod. ADR 0001 made every feature wait for a statement a mod makes in code: an **Opt-in** predicate, the blocks a **Consumer** turns in place, a **Stretch**'s builder, and a **Dismantle family**. So with no Consumer mod installed, Groundworks did nothing.

**Decision.** Groundworks carries a Consumer of its own for vanilla, wired the way Beltworks is: it makes the same four statements through the same hooks. What each vanilla block does is vanilla's: a Stretch lays the held item at each position through vanilla placement, a rise included, so stairs climb as a staircase and join their neighbours, and a rail slopes; Rotate in Place is the block's own rotation; a Dismantle span takes the shortest joined path through blocks of one family. Groundworks restates none of it.

Which blocks and items the vanilla Consumer reaches is data: one tag per feature, whose entries are vanilla tags or single ids. Groundworks ships them filled with defaults: every block whose placed state depends on how it is placed gets Opt-in and Rotate in Place; stairs, slabs, fences, walls, panes, logs and wood, and rails stretch. A pack developer adds to a tag or takes from it, with no code. Namespaces listed in the server config count as opted in as well, so a pack can opt in "all my blocks" in one line.

This replaces ADR 0001's rejection of opt-in by data ("Opt-in by namespace string or block tag"). A code statement stays for a Consumer mod, and the two add up: a block is reached when either says so. The rest of ADR 0001 holds: refusals stay open, and the hooks stay events.

## Considered Options

- **Features ship switched off, and the pack developer switches each on.** Rejected. Groundworks alone would do nothing until someone writes data, and every pack would write the same data.
- **A rule in code for which blocks are "dynamic".** Rejected. Nearly every block overrides its placement, so no rule tells them apart, and the list would be hidden from the pack.
- **Files pairing a block with the block it rises as.** Rejected. The player holds the stairs, and vanilla already knows how stairs join and climb.
- **Code only, as ADR 0001 has it.** Rejected. A pack developer would have to write a mod to change what Groundworks reaches.

## Consequences

- The default lists name vanilla's own tags, and single ids only where vanilla has no tag. They are data, not code, so Groundworks still keeps no block in code.
- A Leg has no vertical form, so a ladder does not stretch until one exists (5thlayer/groundworks#27).
- Groundworks can have a standalone page on CurseForge and Modrinth, as a mod and not only as a library.
