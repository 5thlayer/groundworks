# A Dismantle claims a click only on a member

A stored **Dismantle** start made every click the Dismantle's, whatever it aimed at, so that the preview, which draws the Dismantle whatever the aim, and the click agreed. A dismantling tool is often another mod's, such as a tech mod's wrench, and that click ate the wrench's own sneak-click on its own machines: a span was planned, refused, and the event cancelled (#48). Beltworks' ADR 0008 already said its Dismantle claims only belt tiles, even while a start is stored; since its ADR 0011 the code that eats the click is Groundworks' own.

**Decision.** Beltworks ADR 0008's rule holds for every **Dismantle family**. A click, sneaking or not, on a block no family claims is never the Dismantle's: it passes on to the tool's and the block's own use, with no message, and leaves the stored start and the queued spans as they are. A click on a member behaves as before, refusals included, whichever family it belongs to and whether or not that family accepts the held tool. The preview reads an aim outside every family as no aim and draws the start alone, since a click there would confirm nothing.

## Considered Options

- **Every click with something stored is the Dismantle's,** as before. Rejected: another mod's tool loses its own gesture on its own blocks for as long as a start is stored.
- **Claim a click only on the start's own family.** Rejected: a click on a member of another family is a Dismantle click the player meant, and the refusal, not the same kind, tells them why it did nothing.
- **Claim a click only on a member whose family accepts the held tool.** Rejected: a span refused for the tool should say so, not let the click fall through to a use the player didn't aim for.

## Consequences

- A plain click with only queued spans confirms them on any member, not on any block. Confirming by clicking the open floor no longer works.
- A Consumer's tool keeps its own use on every block outside the Dismantle's families, start stored or not.
