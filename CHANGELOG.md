# Changelog

Written for Consumers: what a mod building against Groundworks can use, or will see change.

## Unreleased

## 0.4.2

- A **Stretch**'s start takes **Rotate the Plan**: a sneak-click stores the look turned by the held stack's turn, and the start marker draws it so before the click. Storing the start uses the turn up, as it uses up the height. A press with a start stored leaves the stretch alone and turns the next start or placement. `Stretches.startedAt` answers the stretch a sneak-click would store, start and look. (#19)

## 0.4.1

- `ShortestPath`: the shortest joined path between two members of a family whose blocks join their neighbours, start first, refused when the end is outside the family, not joined, or reached by two equally short paths. A family's `span` can answer from it, as the Pack's pipes do (ADR 0002). (#3)

## 0.4.0

- The **Stretch**: a held stretch-able item lays a line of blocks in one drag. A sneak-click stores the start, each further sneak-click adds an **Anchor**, and a click lays the stretch. Groundworks owns the gesture, the route seen from above, the **Legs**' shape, charging and the preview; the item builds each Leg and refuses what it can't. Height changes only by **Raise** and **Lower** (ADR 0004). (#14)
- A stretch's direction is drawn before its start is stored. (#14)
- **Detours**: a Leg that meets an obstacle at its own height goes round it flat, on the player's side and within a few blocks of its line, instead of refusing the stretch. An obstacle is only ever what the item refuses at a position. (#15)

## 0.3.0

- **Raise** (`G`) and **Lower** (`B`) move the held item's next single placement one block up or down, capped by the player's reach. The height stays on the held stack until its last item is placed, and the preview draws the block where it will go. (#13)

## 0.2.0

- **Rotate** (`R`) and **Reverse Rotate** (`Shift+R`), under Groundworks' own key category. With a rotatable item held they **Rotate the Plan**, turning its next placement a quarter from the way the player looks. (#10)
- Otherwise they **Rotate in Place** the block under the crosshair, only where a Consumer has stated it turns that block. A block that answers for itself turns its own way; any other takes vanilla's turn. A refusal shows its reason on the action bar. (#11)

## 0.1.0

- The library, extracted from the Pack and renamed from placementpreview to Groundworks: mod id `groundworks`, packages `io.github._5thlayer.groundworks`, artifact `io.github.5thlayer:groundworks`. (#4)
- The **Placement Preview**: a held item's **Placement Plan** drawn translucent before the click, blue where it replaces, red where refused. **Refusals** are open: each Consumer's own enum implements the interface. Plain blocks get a **Vanilla Plan** once a Consumer's **Opt-in** predicate covers them (ADR 0001).
- **Overlays**, **Takeovers** and **Markers** are NeoForge events a Consumer subscribes to, to draw its own additions (ADR 0001).
- The **Dismantle**, run by Groundworks for every registered **Dismantle family**: a sneak-click with a tool in `groundworks:dismantles` stores a start, and a click takes up the span to the aim. A Consumer supplies each family. An end outside the start's family is refused as not the same kind, naming the start's block. (#5)
