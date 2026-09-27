# A pack developer switches Groundworks on through data

Groundworks is meant to work in a vanilla game with no other mod, and vanilla is a **Consumer** like any other. ADR 0001 made every feature wait for a statement a mod makes in code: an **Opt-in** predicate, the blocks a Consumer turns in place, a **Stretch**'s builder, and a **Dismantle** family. So with no Consumer mod installed, Groundworks does nothing.

**Decision.** Groundworks' features ship switched off, and a pack developer switches them on through data, with no code: which plain blocks get a **Vanilla Plan**, which turn in place, which items stretch, and which families dismantle. Groundworks still keeps no block of its own in code. The choice of blocks is the pack's.

This replaces ADR 0001's rejection of opt-in by data ("Opt-in by namespace string or block tag"). A code predicate stays for a Consumer mod that wants one, and the two add up. The rest of ADR 0001 holds: refusals stay open, and the hooks stay events.

## Considered Options

- **Groundworks switches vanilla on by default, with sensible choices of its own.** Rejected. The choice of blocks belongs to the pack, and Groundworks would start keeping lists of vanilla blocks.
- **Code only, as ADR 0001 has it.** Rejected. A pack developer would have to write a mod to use Groundworks at all.

## Consequences

- Still open: the data's form (block and item tags, a datapack registry, or a config file), and how a namespace-wide opt-in such as the Pack's "my namespace" is written in it.
- Each feature needs a vanilla answer that data can select: a Stretch builder that lays the held block item, and a Dismantle family made of blocks of one kind.
- Groundworks can then have a standalone page on CurseForge and Modrinth, as a mod and not only as a library.
