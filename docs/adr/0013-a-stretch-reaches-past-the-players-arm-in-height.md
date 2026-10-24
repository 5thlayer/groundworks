# A stretch reaches past the player's arm in height

ADR 0006 freed a **Column**'s length from the player's reach but kept the reach cap on a **Leg**'s rise, so a line could climb no higher than the player could reach from where they stood (#73). That stops a belt or a pipe from climbing a cliff in one stretch, though the Column's reason, that a stretch is meant to reach past the player's arm, holds for it too.

**Decision.** Once a stretch's start is stored, every height **Raise** and **Lower** set is capped only by the world's build height: the rise of each Leg being drawn, and a Column's length, whatever stretch tags the item carries. A stretch that would leave the world is refused whole, as any stretch is. Heights set with no start stored keep the reach cap: a single placement's, and the start's own, which is set as a single placement's before the start is stored. This supersedes ADR 0006's "Legs' rises and single placements keep the reach cap" for Legs' rises.

## Considered Options

- **The start's own height uncapped too.** Rejected: until the start is stored it is a single placement's height, placed at the player's hand.
- **No reach cap anywhere.** Rejected for the same reason: a single placement is placed where the player can reach.
