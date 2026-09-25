# Groundworks plans mass placement and Dismantle, not only draws placements

Beltworks' ADR 0011 moved its Dismantle and Stretch, and the Pack's family Dismantle, into this library. Beltworks and the Pack had drifted apart on one screen with one tool, which is the case its ADR 0010 made for extracting the preview. So the library is renamed from placementpreview to **Groundworks** and widened: it plans what a click would lay or take up, shows the plan before the click, and carries it out. As before, it keeps nothing about any particular block. The seams are narrow on purpose, so no Consumer's vocabulary leaks in:

- **A Dismantle family owns its span.** Given a start and an end, a family answers with what it draws, what it takes, or its own refusal. It also runs a hook over everything taken before any of it is removed, and says whether a stored start is still the same start. Groundworks stores a start as its position and its blockstate.
- **Groundworks ships a shortest-path helper** for families that are blocks joined to their neighbours, with ties refused, as the Pack's pipes need. A family that isn't a graph, such as a belt's transport line, doesn't use it.
- **An item joins one anchor to the next.** Groundworks stores the anchors and owns the route seen from above. It never changes height unless the player makes the height gesture, and it takes an obstacle on a leg with a flat detour around it.
- **Groundworks owns both gestures,** the tool tag `groundworks:dismantles`, and the generic refusals. Those refusals are "not the same kind", "queue full" and "stale start", and they are open like any other (ADR 0001). It also owns charging, removal, the hand-over to the player's inventory, and the preview of both.

A pass may take up spans of several families, but one span never crosses between families: no family's rule can join a belt to a pipe.

## Considered Options

- **Stay a renderer, and put the gestures in a library of their own.** Rejected. It would be a third repository ported with Beltworks and the Pack at every Minecraft version, and a second nested jar in Beltworks, for gestures that plan, draw and execute exactly as a Placement Plan does.
- **A family is only a graph, with a flag for direction,** as the Pack's ADR 0086 foresaw. Rejected. Every quirk of a later family would become another flag here.
- **Keep the name placementpreview.** Rejected. It would no longer say what the library is for, and the name had never been published.

## Consequences

- The mod id is `groundworks`, the packages are `io.github._5thlayer.groundworks`, and the repository is `5thlayer/groundworks`. The version restarts at 0.1.0. Beltworks and the Pack change their builds and imports, and nothing else resolves the old id.
- ADR 0001 holds: open refusals, predicate opt-in, and hooks as events. The dismantle previews move inside the library, so the no-order rule between Takeovers no longer has two Consumers claiming the same screen.
- The API stays 0.x until the Pack places pipes by stretch. The Stretch's seam has then been used by a second Consumer, and its shape is no longer only a belt's.
- The height gesture and the detour rules are decided in their own question before the Stretch moves in. They are decided in ADR 0004.
