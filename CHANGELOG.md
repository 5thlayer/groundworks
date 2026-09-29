# Changelog

Written for Consumers: what a mod building against Groundworks can use, or will see change.

## Unreleased

- Groundworks opts vanilla blocks in for a **Vanilla Plan** by itself, through data, as the first part of its built-in vanilla Consumer (ADR 0005). The block tag `groundworks:plan_opt_in` ships holding every vanilla block whose placed state depends on how it is placed: stairs, slabs, fences, walls, panes, logs, doors, rails, furnaces, chests and the like. A pack changes it with a datapack, `remove` included. The server config's `planOptInNamespaces` opts in every block of the namespaces it lists. `VanillaConsumer.PLAN_OPT_IN` names the tag, and `GroundworksConfig.PLAN_OPT_IN_NAMESPACES` the list. Both add up with `Placements.optIn`, so a Consumer's own Opt-in in code works as before. A Consumer will notice that vanilla's oriented blocks are now drawn, and turned by **Rotate the Plan**, with no Opt-in of its own. (#23)

- Groundworks gives vanilla items a **Stretch** by itself, through data, as the second part of its built-in vanilla Consumer (ADR 0005). The item tag `groundworks:stretches` ships holding stairs, slabs, fences, walls, glass panes, logs and wood, and rails; `VanillaConsumer.STRETCHES` names it, and a pack changes it with a datapack, `remove` included. Each **Leg** lays the held item at each position through vanilla placement, looking along the leg: a rise climbs one block per column right after the leg's first **Anchor**, so stairs lay a staircase facing up it, and rails on a rise slope. Groundworks sets no block state itself; stairs join and rails slope as vanilla lays them, so the preview draws each block as it would be placed alone. A rise that turns or doesn't fit before the leg's last anchor refuses the stretch. The vanilla builder is registered once every mod is constructed, so a Consumer's own `LegBuilder` for an item in the tag builds that item. A Consumer will notice that a sneak-click with a tagged item now starts a Stretch instead of placing it. (#25)

- Groundworks turns vanilla blocks by **Rotate in Place** by itself, through data, as the third part of its built-in vanilla Consumer (ADR 0005). The block tag `groundworks:rotates_in_place` ships holding what `groundworks:plan_opt_in` holds, plus the wall forms that place as blocks of their own: wall torches, wall heads and wall coral fans. `VanillaConsumer.ROTATES_IN_PLACE` names it, and a pack changes it with a datapack, `remove` included. A tagged block takes vanilla's turn unless it implements `TurnsInPlace`. The tag adds up with `Rotate.turnsInPlace`. A Consumer will notice that vanilla's oriented blocks now turn in place with no statement of its own. (#24)

- Groundworks gives vanilla blocks a **Dismantle** by itself, through data, as the last part of its built-in vanilla Consumer (ADR 0005). Every block tag under `groundworks:dismantle_family/` is one **Dismantle family**; `fences`, `walls`, `bars` and `rails` ship, and a pack adds a family with a tag file or changes one, `remove` included. A span takes the shortest joined path through one tag's blocks: a block whose state says which sides it connects on is joined only where it connects, and any other block, as a rail, to every member it touches. It is taken up with the tool vanilla breaks every one of its blocks with, so an axe takes up oak fences and a pickaxe rails. The vanilla families are registered once every mod is constructed, so a Consumer's own family claims its blocks first.

- `DismantleFamily.acceptsTool(ItemStack, BlockState)` says whether a held tool dismantles a member. Its default is membership of `groundworks:dismantles`, so a Consumer's family works as before. A span that takes a block its family doesn't accept the tool for is refused as `Refusal.Dismantle.WRONG_TOOL`, told by the library. A click with a tool no family accepts at the clicked block, and nothing stored, passes on to the tool's own use. `Dismantles.isTool` is gone: ask the family. (#26)

## 0.4.6

- The jar carries its licence, `LICENSE` and `LICENSES/MIT.txt`, at its root, so a Consumer that nests it hands the licence on with it. The build fails if either is missing. (5thlayer/beltworks#61)

## 0.4.5

- A **Stretch** is refused whole, as `Refusal.Stretch.MAY_NOT_BUILD`, when the player may not build at some position it lays: in adventure mode, outside the world's bounds, or where `Level.mayInteract` says no, as under spawn protection. The preview draws it refused, and a click lays and charges nothing. (#16)
- `groundworks:dismantles` ships one optional entry, `groundworks:gametest_dismantles`, a tool that exists only when game tests are enabled, as in a dev client. With it, and the cyan terracotta row family registered alongside, Groundworks' own dev client and game tests run the **Dismantle** with no Consumer. A production game has neither.

## 0.4.4

- A **Dismantle** takes up two spans in one pass. A sneak-click with a start stored queues the span to the clicked block, in `Groundworks.DISMANTLE_QUEUE`, instead of moving the start. A click confirms every queued span and the one it ends as one `DismantlePass`, whole or nothing, taking a shared position once and running each family's `beforeTaking` once over all its positions. `Dismantles.MAX_SPANS` is the limit, and `Dismantles.passTo` answers the pass a click would confirm. A queued span whose start is gone refuses as `Refusal.Dismantle.START_GONE`. (#6)

## 0.4.3

- A **Leg** says where it sits in its **Stretch**. `Leg.arrives()` is the way the stretch arrives at its first anchor, the travel of the leg before it through its last column, or `null` at the stretch's start, which `Leg.startsStretch()` answers. A builder can build the block at an intermediate **Anchor** as the corner it becomes. `Leg.endsStretch()` says whether its last anchor is the stretch's end. A **Detour** keeps both. The three-argument constructor stays, for a leg that is its stretch's only one. (#20)

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
