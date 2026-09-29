# A Column is a Stretch straight up or down, sized by Raise and Lower

A **Leg** rises after its first anchor and then runs level, so a **Stretch** has no vertical form, and ladders stayed out of `groundworks:stretches` (#25, #27).

**Decision.** A **Column** is a Stretch of its own kind that goes only straight up or down from its start, with no Leg:

- **Which items.** Items in a new tag, `groundworks:stretches_vertically`, stretch as a Column, whichever block the end is clicked on. It ships with ladders and chains; a pack adds modded vertical connecting blocks. An item in both stretch tags draws a Column when the end is clicked on the start's own column seen from above, and Legs anywhere else.
- **Height only from Raise and Lower.** The player stores the start, sets the length with Raise or Lower, and clicks the start's column. The aim sets nothing else, so ADR 0004's rule that height comes only from Raise and Lower holds. The length is not capped by the player's reach, as other heights are, but only by the world's build height: a stretch is meant to reach past the player's arm. Legs' rises and single placements keep the reach cap.
- **Placed with the start's look.** Every block takes the look stored with the start, turned by Rotate (ADR 0003), clicked on the block before it, the face the column grows toward. The item's vanilla placement does the rest, so a ladder faces off the wall its look finds and a chain stands upright. The start's own aimed face is not kept: a chain started on a wall's side would lie across its column.
- **Alone and whole.** A Column has no anchors between its ends, so a sneak-click is refused, and it never joins level Legs. One block that cannot stand refuses the whole Column, as any Stretch is refused whole.

## Considered Options

- **A Leg with no run,** its next anchor on its first anchor's column. Rejected: the player wanted a Column as its own concept, not a corner case of the Leg.
- **Height from the aimed block,** so a ladder ends at the cliff top the player looks at. Rejected: it breaks ADR 0004's one rule for height, and clicking the start again is the same gesture as a single placement's Raise.
- **Aim the wall face,** pitch picking up or down. Rejected for the same reason.
- **Mix Columns with level Legs** in one Stretch. Deferred until a modded pipe needs it; a ladder cannot run level anyway.
- **Scaffolding and vines by default.** Rejected: vanilla already builds them up or down by itself. A pack may add them.

## Consequences

- A tall Column is one press sequence, however far past the player's reach it climbs; each block is still checked against where the player may build.
- Ladders join `groundworks:stretches_vertically`, not `groundworks:stretches`.
- Groundworks gains a second stretch shape beside the Leg, and the Stretch's plan must say which it is.
