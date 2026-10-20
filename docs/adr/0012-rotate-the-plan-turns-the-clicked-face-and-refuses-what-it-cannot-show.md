# Rotate the Plan turns the clicked face, and refuses what it cannot show

ADR 0003 turned only the look: a placement's horizontal direction, its rotation and its nearest looking directions, but never the face it is made against, which stays first among those directions and unturned. A block that faces the face it is placed on, such as Beltworks' feeder or belt end, Pipeworks' outlet, Voidworks' well hatch, or vanilla's trapdoor, hopper or log, so ignored Rotate whenever it was placed against a side. The press still stored the turn on the stack. The player saw the held item bob, with no change drawn and no reason given, and the turn then came back on the next placement against a top face (5thlayer/groundworks#72). That broke ADR 0003's rule that R never turns a placement the player cannot see.

So Rotate the Plan now turns the clicked face as well as the look, when that face is a side and the stack has a turn. The position placed at still comes from the hit, so it does not move. Every block whose placement reads the face turns with no code of its own, as every block reading the look already did. The nearest looking directions still put the face as hit first, as ADR 0003 left them: a block that hangs on the face it is clicked on, as a ladder or wall torch does, picks its side from that list and needs that face to stand on, so it keeps hanging there whatever the turn, and a press has nothing to turn it to.

To keep ADR 0003's rule, the server decides a press of Rotate the Plan by comparing plans. The client sends the whole aim, meaning the block, the face and the hit point. The press takes the next quarter turn, in its direction, whose Placement Plan for that aim differs from the plan drawn now and, unless the plan drawn now is already refused, is not refused itself. If no turn qualifies, or nothing is aimed so no preview is drawn, the press is refused with its reason, and the stack keeps its turn. While a Stretch's start is stored, or a sneak-click at the aim would store one, the check is skipped and the stack is turned as before, since the turn is for that start, and a sneak-click that stores a start draws no plan to compare.

## Considered Options

- **Each Consumer turns its own face.** Each block reads the stack's turn and turns the face itself. Rejected. Five blocks across three Consumers already needed it, and vanilla's blocks would never get it.
- **A Consumer states which blocks face their face.** Rejected. That is a statement every Consumer could forget, and it can drift from what the preview shows. Comparing plans follows the preview itself.
- **Allow a turn into a refused plan.** Rejected. A block that stands on the face it is placed against, such as an amethyst cluster, would cycle through red states that it cannot be placed in.

## Consequences

- Consumers' blocks that read the clicked face turn on walls in the next Groundworks they load, with no release of their own. Any block that relies on the face being unturned gets its own issue.
- An Assembly's Key is placed with the hit's face, so a Key that reads the clicked face must be turned once, not twice.
- The rotate payload changes shape, so its version is bumped, and `Rotate.press` takes the aimed hit rather than a block position. A Consumer's game tests that press Rotate change with it, so the change is breaking under ADR 0001.
- The server takes a hit only where its point lands on the block it names, as vanilla takes a click's, since the hit is now what a plan is worked out from.
