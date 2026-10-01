# Changelog

From 0.5.0 each version has two sections: Players, what a player or pack developer sees, and Consumers, what a mod building against Groundworks can use or will see change (ADR 0007).

## Unreleased

### Players

- A **Stretch** is paid from the held stack first, then from the rest of the main inventory, and hands what it replaces back into the slot its charge freed: when it takes the held stack's last item, what it replaces lands in the hand, not in the first empty slot. Otherwise it joins a stack of its own before an empty slot, as before. (#36)
- **Fast Replace.** A plain click with a block on a placed block of the same Replace group puts the held block in its place, and the preview draws it blue before you click. It costs one of the held block and hands the replaced one back, into your hand when that was the last. A sneak-click places beside instead, and a held Raise or Lower height leaves a replace where you aim. When there is no room for what comes back, the click is refused and you are told why. No vanilla block is in a group yet. (#31)
- A **Fast Replace** that can't go through changes nothing and tells you why: it is refused where you may not build, in adventure mode, outside the world, in spawn protection or in a claim, and where an entity stands in the new block's way, and it never places beside instead. A creative player pays nothing for a replace, is handed nothing back, and is never refused for room. (#32)
- **Fast Replace** keeps the replaced block's orientation: the new block faces the way the old one did, and the preview shows it, unless you turned the held block with **Rotate**, which re-orients the replace and stays on the stack. (#33)
- A pack developer groups vanilla blocks for **Fast Replace** with block tags under `groundworks:replace_group/`: each tag is one **Replace group**, and a plain click with one of its blocks on another replaces it. A datapack adds or changes a tag, `remove` included, and a reload takes effect. A block in two tags belongs to the first by id, and the log warns once. Groundworks ships no tag, so a vanilla install sees no replace until a pack adds one; a group a mod states in code claims its blocks first. (#34)

### Consumers

- `FastReplace.group(Identifier, Predicate<? super Block>)` states a **Replace group**: its blocks replace one another on a plain click, through the held item's own plan, asked with a place context whose clicked block counts as replaceable so that the plan lands on the aimed block (ADR 0008). `Placements.isReplacing(BlockPlaceContext)` tells an item's plan it is planning a replace, and its context has no Raise height. Only the main hand's `BlockItem` replaces, and a replace is drawn whether or not its block is opted in. The library carries out the click, not the item's own `place`: it charges one held item, swaps the block, calls `setPlacedBy` and the placed-block trigger, and hands the old block's item back. An item whose plan puts no block in the aimed block's place is refused as `Refusal.FastReplace.PLANS_ELSEWHERE`, and no room for the refund as `Refusal.FastReplace.NO_ROOM_TO_RETURN`. (#31)
- `Refusal.FastReplace.MAY_NOT_BUILD` refuses a replace where the player may not build at a position its plan puts a block, or where a protection mod cancels the place event, which the library fires on the click for each placed position of a replace that would otherwise go through, and never in the plan the preview asks. Where the player may not build, it wins over every other refusal of a replace, the item's own included. (#32)
- A replace's plan carries the old block's orientation: the block at the aimed position takes each facing, axis or rotation it shares with the block it replaces, the properties **Rotate** turns, as the same `Property` object and never by name, and any other property stays what the item's plan made of it. No other property is copied, since one may count what a block holds, as a double slab's type does, or name a part of a block that spans two; a Consumer whose blocks keep more sets it in its own plan or builder. A **Rotate** turn held on the stack skips the copy, so the item's plan, asked with the turned look, is the new state. Only the aimed position is copied; a plan with other positions keeps the item's states there. (#33)

## 0.5.0

### Players

First release on Modrinth and CurseForge. Groundworks now works on its own in a vanilla game, with no other mod.

- **Placement preview.** Holding stairs, slabs, logs, doors, rails and other blocks that depend on how you place them shows a translucent preview of what a click would put down: blue where it replaces a block, red where it would be refused.
- **Rotate.** `R` turns the next placement a quarter turn, `Shift`+`R` turns it back. With nothing in hand that places, `R` turns the block you aim at in place.
- **Raise and Lower.** `G` and `B` move the next placement one block up or down, so you can place in mid-air.
- **Stretch.** Sneak-click stairs, slabs, fences, walls, panes, logs or rails to start a line, sneak-click to add a corner, click to lay it. Stairs climb as a staircase, rails slope, fences connect, and the preview shows each block joined as it will stand.
- **Columns.** Ladders and chains stretch straight up or down: sneak-click to start, `G` or `B` to set the height, click to lay. Your reach doesn't limit the height.
- **Dismantle.** Sneak-click a fence, wall, iron bar or rail with the tool that breaks it, then click another of the same kind: everything joined between them comes up into your inventory.
- **Pack developers:** every feature's reach is a tag you can change with a datapack: `groundworks:plan_opt_in`, `groundworks:rotates_in_place`, `groundworks:stretches`, `groundworks:stretches_vertically` and `groundworks:dismantle_family/*`.

### Consumers

- Groundworks opts vanilla blocks in for a **Vanilla Plan** by itself, through data, as the first part of its built-in vanilla Consumer (ADR 0005). The block tag `groundworks:plan_opt_in` ships holding every vanilla block whose placed state depends on how it is placed: stairs, slabs, fences, walls, panes, logs, doors, rails, furnaces, chests and the like. A pack changes it with a datapack, `remove` included. The server config's `planOptInNamespaces` opts in every block of the namespaces it lists. `VanillaConsumer.PLAN_OPT_IN` names the tag, and `GroundworksConfig.PLAN_OPT_IN_NAMESPACES` the list. Both add up with `Placements.optIn`, so a Consumer's own Opt-in in code works as before. A Consumer will notice that vanilla's oriented blocks are now drawn, and turned by **Rotate the Plan**, with no Opt-in of its own. (#23)

- Groundworks gives vanilla items a **Stretch** by itself, through data, as the second part of its built-in vanilla Consumer (ADR 0005). The item tag `groundworks:stretches` ships holding stairs, slabs, fences, walls, glass panes, logs and wood, and rails; `VanillaConsumer.STRETCHES` names it, and a pack changes it with a datapack, `remove` included. Each **Leg** lays the held item at each position through vanilla placement, looking along the leg: a rise climbs one block per column right after the leg's first **Anchor**, so stairs lay a staircase facing up it, and rails on a rise slope. Groundworks sets no block state itself; stairs join and rails slope as vanilla lays them, and the preview draws each block as it will stand once the whole stretch is laid, joined to its planned neighbours and to the world beside it, corners included. (#28) A rise that turns or doesn't fit before the leg's last anchor refuses the stretch. The vanilla builder is registered once every mod is constructed, so a Consumer's own `LegBuilder` for an item in the tag builds that item. A Consumer will notice that a sneak-click with a tagged item now starts a Stretch instead of placing it. (#25)

- Groundworks turns vanilla blocks by **Rotate in Place** by itself, through data, as the third part of its built-in vanilla Consumer (ADR 0005). The block tag `groundworks:rotates_in_place` ships holding what `groundworks:plan_opt_in` holds, plus the wall forms that place as blocks of their own: wall torches, wall heads and wall coral fans. `VanillaConsumer.ROTATES_IN_PLACE` names it, and a pack changes it with a datapack, `remove` included. A tagged block takes vanilla's turn unless it implements `TurnsInPlace`. The tag adds up with `Rotate.turnsInPlace`. A Consumer will notice that vanilla's oriented blocks now turn in place with no statement of its own. (#24)

- Groundworks gives vanilla blocks a **Dismantle** by itself, through data, as the last part of its built-in vanilla Consumer (ADR 0005). Every block tag under `groundworks:dismantle_family/` is one **Dismantle family**; `fences`, `walls`, `bars` and `rails` ship, and a pack adds a family with a tag file or changes one, `remove` included. A span takes the shortest joined path through one tag's blocks: a block whose state says which sides it connects on is joined only where it connects, and any other block, as a rail, to every member it touches. It is taken up with the tool vanilla breaks every one of its blocks with, so an axe takes up oak fences and a pickaxe rails. The vanilla families are registered once every mod is constructed, so a Consumer's own family claims its blocks first.

- `LegBuilder.reshapesAgainstNeighbours()` says whether the preview draws a builder's blocks reshaped against their neighbours, as the game reshapes a block when the next goes down beside it, once over the whole **Stretch**. It is false by default, so a Consumer's builder is drawn as it builds, as before. Only the preview changes: what is laid is `build`'s. (#28)

- A **Stretch** can go straight up or down as a **Column** (ADR 0006). The vanilla Consumer lays items in the new item tag `groundworks:stretches_vertically`, shipped holding ladders and chains, as a Column: store the start, set the length with **Raise** or **Lower**, which only the world's build height caps, not the player's reach, and click. Each block is placed through vanilla with the start's look, clicked on the block before it. A sneak-click while a Column is drawn is refused as `Refusal.Stretch.NO_ANCHOR_IN_A_COLUMN`. A Consumer's `LegBuilder` claims its own items as a Column through `claimsColumn(Item)` and builds them with `buildColumn(Level, Item, Column)`; both default to claiming none, so a Consumer's builder works as before. `VanillaConsumer.STRETCHES_VERTICALLY` names the tag. A Consumer switching exhaustively over `Refusal.Stretch` must handle the new value. (#27)

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
