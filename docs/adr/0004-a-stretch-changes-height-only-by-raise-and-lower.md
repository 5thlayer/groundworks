# A stretch changes height only by Raise and Lower

ADR 0002 moved the **Stretch** into Groundworks and ruled that Groundworks never changes height unless the player asks. It left open how the player asks, and how a leg goes round an obstacle (#7). The answer is two actions, **Raise** and **Lower**, and nothing else:

- **The aim picks only where an anchor lies seen from above.** Its height is the stretch's height there, never the aimed block's. A stretch starts at its start's height and stays there until the player presses Raise or Lower.
- **A leg's rise sits right after its first anchor,** along the leg's first direction, by the leg's net height; after it the leg runs level to the next anchor. Storing an anchor freezes the height into the leg, and the next leg starts level.
- **Raise and Lower are Groundworks' actions** (`G` and `B` by default, each rebindable alone). They set one height on the held stack, as Rotate sets a turn (ADR 0003), capped by the player's reach in whole blocks. The height applies wherever a **Placement Preview** is drawn. So a single placement moves straight up or down in its column, and a stretch's start takes it as its own height. Laying or clearing a stretch resets it; otherwise it stays with the stack until the stack's last item is placed.
- **Groundworks owns a leg's shape; the item owns only what a rise is built of.** Given a leg's first anchor, its rise and its route seen from above, the item answers with its blocks or a refusal: slopes for a belt, a straight climb for a pipe. This narrows ADR 0002's "an item joins one anchor to the next".
- **An obstacle is where the item refuses the leg at a position.** An item's refusal says whether it stands at a position. Only those are detoured, flat, on the side of the leg the player stands on (right of travel when they stand on its line), within a narrow sideways band. A leg that no detour clears is refused with the item's refusal at the first obstacle. Any other refusal refuses the stretch whole.

## Considered Options

- **Height from the aim.** Aiming at a platform's top would make the line climb to it. Rejected. The player can't choose a height over open ground, and aiming at the wrong block's top silently tilts a line.
- **The end anchor must be aimed at the leg's height, or be refused.** Rejected. An elevated line could never end in mid-air.
- **The item chooses where a leg climbs,** given two anchors at any heights. Rejected. Each item would grow its own rules for the same gesture, and the player could not predict the climb from the keys pressed.
- **Keep following the ground,** as Beltworks' stretch does today. Already rejected by ADR 0002 and Beltworks' ADR 0011.
- **Sneak-scroll or modifier-scroll.** Rejected. Sneak already means "anchor" in the stretch, and modifier-scroll is commonly claimed by other mods. None of the three repositories handles the scroll wheel.
- **`F` and `V`.** Rejected, because `F` is vanilla's swap-to-offhand, and NeoForge can't make one in-game key fire only one of two bindings. `G` and `B` keep the same pairing and are unbound in vanilla.
- **Raise and Lower on stretches only.** Rejected. A player should be able to choose a single placement's height too, and one rule scoped by the preview is the one Rotate the Plan already follows.
- **Refuse detour ties,** as the Dismantle's shortest path does. Rejected. A block on a straight leg always ties, so detours would almost never happen.

## Consequences

- A crossing line is no longer climbed by itself (the Pack's #422, already accepted in ADR 0002). The player crosses it with Raise, an anchor past the line, and Lower. Beltworks treats a tile of another line crossing a leg as an obstacle, so a stretch never cuts a line. A tile running along the leg is merged, and a start or end anchor on a line joins it, as before.
- Where a Consumer has opted in plain blocks, those blocks can be placed in mid-air with Raise. That includes the Pack's oriented blocks under ADR 0003. A Consumer that doesn't want this keeps the block out of its Opt-in.
- Floating is not Groundworks' concern. Beltworks' supports are decoration and never refuse a placement (SimpleBelts#1).
- Refusals gain "at a position", and the Stretch's seam is the leg. Both are API additions under ADR 0001, and the API stays 0.x until the Pack stretches pipes.
- Raise and Lower on single placements need no Stretch, so they can land first.
- Beltworks' ADR 0005 paragraph on the two-click stretch is superseded, and its ADR 0011's "the item joins anchors" is narrowed as above.
