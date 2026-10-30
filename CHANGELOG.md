# Changelog

From 0.5.0 each version has two sections: Players, what a player or pack developer sees, and Consumers, what a mod building against Groundworks can use or will see change (ADR 0007).

## Unreleased

### Players

- **Breaking**: Next Assembly and Reverse Next Assembly are now **Next Choice** and **Reverse Next Choice**, still `V` and `Shift+V`, which step the held item's choice; a held Key steps through its Assemblies and the Key alone as before. Their key mapping ids are now `key.groundworks.next_choice` and `key.groundworks.reverse_next_choice`, so a player who rebound either must bind it again. (#77)
- **Blueprints**: copy a box of blocks with a sheet of paper and paste it elsewhere. A sneak-click on a block stores the first Corner, drawn wherever you aim, with the box from it to your aim outlined; a click on a block captures the box, both Corners included, onto one sheet off the stack, which then stands alone in its stack. A sheet holding a Copy draws its paste at the aim, resting on the aimed face in the orientation it was captured in, air skipped, and a click lays it whole or not at all: refused where a position it places is taken, charged in survival as the item you would pick each block with, a block already standing as wanted kept uncharged, and free in creative. Anyone holding the sheet may paste it, in any dimension, again and again. A sneak-use in the air clears a stored Corner or the sheet's Copy. A capture is refused over the server's `maxSelection`, 32,768 positions by default, or reaching a chunk that isn't loaded. Pack developers choose the sheets through the `groundworks:blueprints` item tag, shipped with paper. (#78)
- **Blueprints** can paste air: Next Choice (`V`, or `Shift+V`) on a sheet holding a Blueprint switches between skipping its air, the default, and pasting it, and the sheet keeps the choice. Pasting air clears what stands where the Blueprint has air, as you would break it, drawn in amber; its drops, a chest's contents among them, go to your inventory and the rest drops at your feet, and creative hands nothing back. A block there you could not break, such as bedrock, refuses the whole paste. (#82)
- A Copy's paste stands as it was captured: torches, rails, carpets and whatever else hangs or stands on another block are laid after what they need, and a door or a bed is placed whole by its lower half or foot, charged once. A Copy carries no block's contents, so a chest pastes empty and a sign blank; in survival it lays waterlogged blocks dry and skips water, lava and fire and any other block with no item of its own, which creative's paste lays. A block that can't stand where it lands, such as a torch whose wall stayed outside the box, is left out, uncharged and outlined red, and the rest is laid. (#80)
- A Blueprint's capture leaves out what it can't take whole, and says so: a block standing partly outside the Selection, such as a door or bed with one half in it or a Footprint partly inside, is left out whole, and the capture's message gives how many were left out and why, naming one. While a Corner is stored, the blocks the capture would leave out are outlined red in the Selection. Blocks in the `groundworks:blueprint_deny` block tag are left out silently; it ships with bedrock, piston heads, end portal frames and `#minecraft:portals`, and pack developers change it through data. (#81)
- A held Blueprint's paste follows Rotate, Raise and Lower: **Rotate** and **Reverse Rotate** turn it a quarter at a time from the orientation it was captured in, each block's own facing turned with it, and **Raise** and **Lower** move it up and down within your reach. The turn and the height stay on the sheet. A blank sheet isn't turned, so Rotate turns the block under the crosshair, as with any item that isn't rotatable. (#79)
- **Undo Paste**, on `U` by default, takes back the latest paste of the Copy on the sheet in your main hand, one per press, up to its last ten pastes. It takes up only the blocks still standing as the paste laid them, leaving any placed or changed since, and hands back what they drop as a Dismantle does, into the inventory first and then at your feet; nothing in creative. What the paste cleared or replaced isn't put back. It is refused, with its reason, in another dimension than the paste's, over a chunk that isn't loaded, or with no paste left to undo; clearing a Copy clears its history, which is kept with the world's saved data. (#84)
- **Cut**: Next Choice (`V`, or `Shift+V`) on a blank sheet switches between capturing a Copy, the default, and a Cut, keeping a stored Corner. A Cut takes its blocks out of the world silently, with no drops and with what they hold, so a chest moves with its contents; what stood outside the box on them falls or breaks as vanilla says, and fluids outside flow in. It leaves out, outlined red and with a message, blocks you could not break, such as unbreakable blocks or blocks a claim guards. A Cut is pasted once, uncharged, with its contents, and the sheet is blank again; a block of it that can't stand where it lands drops at your feet with what it held. A Cut can't be cleared by a sneak-use or undone, and its sheet survives fire and lava and never despawns. (#83)

### Consumers

- The `next_assembly` payload is now `next_choice`: a client and server must both be on this version. (#77)
- `Blueprints.sheets` states which held stacks are Blueprint sheets, as the vanilla Consumer does for `groundworks:blueprints`, and the `Feature` order a click walks now asks a sheet's Blueprint first. A world gains Groundworks' first saved data, `data/groundworks/blueprints.dat`, and a client is sent the new `blueprint` payload while it holds a sheet holding a Blueprint. (#78)
- `PlacementPlan` gains `leftOut`, the positions a plan leaves out each alone while the rest is laid, which the preview outlines red; its former canonical constructor stays, leaving nothing out. (#80)
- `Blueprints.denies` states which blocks a capture leaves out silently, as the vanilla Consumer does for `groundworks:blueprint_deny`. (#81)
- A client sends the new `undo_paste` payload when Undo Paste is pressed, and `UndoPaste.press` takes back the held sheet's latest paste. (#84)
- The `blueprint` payload carries whether the Blueprint is a Cut, so a client and server must both be on this version. `BlueprintSelection.Capturing` gains `cut`, `BlueprintSelection.shown` takes it, and `Blueprints.cuts` tells whether a blank sheet is set to capture a Cut. A capture by a player now fires NeoForge's `BreakBlockEvent` for each block of a Cut, and leaves out each one whose event is cancelled. (#83)

## 0.6.4

### Players

- A Stretch of slabs over a single slab of its own kind no longer crashes the game when it is aimed or laid: each such slab becomes a double slab, as placing one there by hand does. A Stretch of snow layers, which a pack may add to `groundworks:stretches`, no longer crashes over snow either: each snow layer it meets, short of a full block, gains one. A Stretch's Leg of trapdoors a pack adds still hangs each on the block ahead of it, now in its bottom half rather than its top, as a player clicking that face would place it.

### Consumers

- A block placed through the vanilla Consumer's Stretch or Column is now told `replacingClickedOnBlock()` is false, the click being on a block beside it, unless the Stretch lies in a block of the held block's Replace group, where it is true. Before, it was told vanilla's `DirectionalPlaceContext` answer, which asks `canPlace()`.

## 0.6.3

### Players

- Once a Stretch's start is stored, Raise and Lower are no longer capped by your reach, only by the world's build height, so a Leg's rise climbs as high as a Column does. An item that stretches both in legs and as a column draws a Column past your reach too. A press past the build height says "That is as far as the world goes." The start's own height and a single placement's keep the reach cap.
- A door, a bed or a tall plant, such as a sunflower, is now previewed whole, its upper half or its head drawn with the part you place, where before only that part was drawn; so is each one a Fast Replace or a Stretch lays. A Stretch of doors, which a pack may add to `groundworks:stretches`, now draws its doors, where before it planned air in their place.
- A door, a tall plant or a bed aimed where its other half has no room, such as a door under a ceiling or a bed against a wall, is now previewed red, both halves, where before nothing was drawn.
- A bed, a chest, a sign, a banner, a shulker box, a head or skull, a decorated pot, a copper chest or a copper golem statue is now previewed, translucent in the preview's tint as other blocks are, where before nothing was drawn; a mob head or skull draws tinted but solid. A bell is now previewed with its bell, where before only its frame was drawn. A block a pack opts in that has nothing to draw it by is outlined where it would go.

### Consumers

- `PlacementPlan` gains `companions`: the blocks its placed blocks put down themselves, as a door's lower half puts down its upper half. The preview draws them with the plan's tint, never the replace tint, and no laying path places them or charges for them. Every Vanilla Plan names them, for a lower `DOUBLE_BLOCK_HALF` and a `BED_PART` foot, a Stretch's vanilla legs and a Fast Replace included; every existing constructor and factory stays, with none.
- The preview draws a block whose look comes from its block entity by the model NeoForge's `RegisterBlockModelsEvent` registers for its state, as vanilla does its beds and chests; a Consumer's block with none is outlined.

## 0.6.2

### Players

- An **Assembly** where you may not build, in adventure mode or on protected ground, now tells you so whatever else refuses it, as a Stretch and a Fast Replace do, where before a taken position, a Key that wouldn't stand or the blocks it lacks were told first.
- A torch, a sign, a hanging sign, a banner, a head or skull, or a coral fan aimed at the side of a block is now previewed as the wall one vanilla places there, where before the preview drew a standing or ceiling one. The preview now asks each item for the state it places, as vanilla's own placing does, so a block a pack opts in whose item chooses its own state, such as a command block for a player who may not use it, is drawn as vanilla places it too.

### Consumers

- A `ReplaceBuilder` or `LegBuilder` plan's `clears` are now emptied when a Fast Replace or a Stretch lays it, before its blocks go down, as the preview already drew them and an Assembly's always were; before, they were marked and guarded but left standing.
- A `LegBuilder` leg's `clears` now reach its Stretch's plan, a later leg's block or clear winning a position an earlier one takes, so the preview marks them and the Stretch empties them; before, a Stretch drawn in legs dropped them, and only a Column kept its own.
- `Refusal.Assembly.MAY_NOT_BUILD` now refuses an Assembly's plan ahead of `TAKEN`, `KEY_WOULD_NOT_STAND` and `NOT_ENOUGH_ITEMS`, as `Refusal.Stretch.MAY_NOT_BUILD` and `Refusal.FastReplace.MAY_NOT_BUILD` already do theirs.

## 0.6.1

### Players

- A **Stretch** is refused whole, laying and charging nothing, when a claim mod guarding the place event protects any position it lays, as a Fast Replace and a footprint already were. The preview asks no claim mod, so it draws such a stretch accepted and the click refuses it.
- Each block a **Stretch** lays, and each block an **Assembly** lays besides its Key, now counts as placed by you, for the placed-block advancement among others, as a block placed by hand or by a Fast Replace does.

### Consumers

- `Refusal.Stretch.MAY_NOT_BUILD` also refuses a stretch where a protection mod cancels the place event, which Groundworks fires on the click for each position of a stretch that would otherwise go through, and never in the plan the preview asks.
- A Consumer's block that a Stretch lays, or that an Assembly lays before its Key, now gets `Block.setPlacedBy` with the placing player and the item it was laid from, as one a Fast Replace lays already did: a Stretch's once all its blocks are down, an Assembly's once its Key stands, and only where the block still stands. The upper half of a door or a tall plant, and a bed's head, is not told, as vanilla tells only the half it placed; this holds for a Fast Replace too.

## 0.6.0

### Players

- **Rotate** and **Reverse Rotate** turn a block placed against the side of another as well as one placed on top: a block that faces the face it is placed on, such as a trapdoor, a hopper, a log or a Beltworks feeder, now turns a quarter per press there, where before the press did nothing you could see. A block that hangs on that face, as a ladder or wall torch does, keeps hanging on it, and a turn that would leave a block with nothing to stand on, as an amethyst cluster would, is skipped. (#72)
- A press that wouldn't change what the preview draws, or made with nothing aimed so no preview is drawn, is refused with its reason on the action bar, and the held stack keeps its turn, so a turn you couldn't see never shows up on a later placement. With a Stretch's start stored, a press still turns the held stack for the next start. (#72)

### Consumers

- A placement context's clicked face, when it is a side, is turned by the held stack's turn, as its look already was (ADR 0012), so a block whose `getStateForPlacement` reads `getClickedFace()` turns with Rotate with no code of its own. The nearest looking directions still put the face as hit first, so a block that hangs on it keeps it. Where the placement goes is unchanged. (#72)
- **Breaking:** `Rotate.press(Player, BlockPos, boolean)` is now `Rotate.press(Player, BlockHitResult, boolean)`: a game test that presses Rotate passes the hit the crosshair would have, block, face and point, since the press now works out the plan there. A hit whose point lies off the block it names is no hit. `RotatePayload` carries the hit too, and its version is bumped. `Rotate.NOTHING_DRAWN` and `Rotate.UNCHANGED` name the two refusals' translation keys. (#72)

## 0.5.13

### Consumers

- Groundworks' game-test code and structures no longer ship in its jar (libworks ADR 0011): a Consumer's game tests and dev client no longer register Groundworks' test blocks and items, such as `groundworks:gametest_dismantles`, its cyan terracotta Dismantle family or its tests' Footprint, nor carry its `gametest/platform` structures. Groundworks builds on libworks-plugin 0.4.0.

## 0.5.12

### Players

- **Assemblies**: holding an Assembly's **Key**, the preview draws the whole structure of real blocks, resting on the aimed face and facing you, and one click places all of it, the Key last, paid from the main inventory and free in creative. It is refused whole, placing nothing and taking nothing, when a position holds a block the shape doesn't take, telling you so, or when the inventory falls short, naming what is missing. It is refused whole too, and the preview draws it refused, when the Key wouldn't stand on the shape laid round it, as a cactus won't beside a block. Air, water, grass and other blocks a placement overwrites count free, and a block of the shape already standing at its position stays, uncharged. Sneak-placing a Key places it alone, as vanilla does. Once placed, its blocks are ordinary blocks, broken one at a time. (#55)
- A pack's Assembly naming a block tag the game doesn't know, such as a misspelt one, is skipped with a line in the log naming it and the tag, rather than loading and asking for its default block everywhere; fix the tag and `/reload` loads it. (#61)
- Scaffolding opted in by a pack is drawn and placed where vanilla places it, past the reach of the scaffolding holding it up, where it falls, instead of being drawn refused there. (#66)
- An Assembly's Key is drawn and placed as it will stand among the blocks laid round it: a pack's torch Key hangs on a wall its shape builds, and a door Key with a block of its shape over it is refused whole, drawn refused, placing and taking nothing. Should vanilla still refuse the Key once the rest is laid, as when the shape spawns a golem in its way, the click takes back what it laid and you pay only for what is gone, the golem's blocks, never the Key; you are told the Key couldn't be placed. No path leaves an Assembly built without its Key. (#65)
- You can stand inside the Assembly you place, as you must to aim at the middle of a beacon's 7×7 or 9×9 pyramid: your own body no longer refuses it, nor draws it refused in the preview. Once it is laid you are lifted straight up to the first place you fit, normally on top of it, and take no suffocation damage. A mob or another player in the way still refuses it whole. (#67)
- **Next Assembly** (`V`) steps the held Key through its Assemblies and the Key alone, and round, and Reverse Next Assembly (`Shift+V`) steps back; the preview redraws. The stack remembers its choice. A fresh stack starts on the Key's first Assembly, or on the Key alone where its declaration says so, and on the Key alone it places as vanilla does. (#58)
- An Assembly's position may take any block of a tag: it is filled as uniformly as the inventory allows, from the block you hold most of first, and the preview draws the block each position will get. In creative, where blocks never run out, every position of a tag takes the block of it you hold most of, uncharged, and only when you hold none of the tag its default. A block the tag takes already standing there stays, uncharged. A position an Assembly keeps empty is cleared of grass and other blocks a placement overwrites, keeping any water, and a block there that a placement can't overwrite refuses the Assembly whole. (#56, #68)
- **Rotate** and **Reverse Rotate** turn a drawn Assembly about its Key a quarter at a time, even when the Key's own block has no facing, as a beacon's has none; the preview redraws turned and the click places what is drawn. (#57)
- The preview outlines in amber each block an Assembly's click will clear from a position it keeps empty, the grass beside a golem's legs or a flower inside a portal frame, and both halves of tall grass, so its going is no surprise. Air, and water the clearing keeps, aren't outlined, nor is anything while the Assembly is drawn refused. (#62)
- Vanilla's Assemblies ship with Groundworks. A **beacon** places its pyramid, level 1 to 4 (3×3 to 9×9), of any beacon base blocks; in creative, of the base block you hold most of, or iron when you hold none. A **conduit** places its 16-block ring, standing upright across your view so it can be set on the sea floor, or its full 42-block frame, of prismarine, prismarine bricks, dark prismarine or sea lanterns, its inside kept for water. A **carved pumpkin** places an iron golem, the spaces beside its head and legs kept clear, or a snow golem, and the golem spawns. **Obsidian** places obsidian until Next Assembly switches it to a nether portal frame, ten obsidian without corners, placed unlit. (#59)
- A **carved pumpkin** also places a **copper golem**: one copper block of any oxidation stage, waxed or not, under the pumpkin, filled from the copper block you hold most of, in creative too, and plain copper when you hold none. The golem spawns and vanilla turns the block into the matching copper chest. It comes third in Next Assembly's order: the iron golem, the snow golem, the copper golem, then the carved pumpkin alone, so a fresh carved pumpkin still starts on the iron golem. (#63)

### Consumers

- An Assembly is declared as data, a JSON file under `data/<namespace>/groundworks/assembly/`: its shape as layers of strings, bottom first, a legend of block ids, the letter that marks its Key, and `starts_alone`; `AssemblyShape`'s javadoc sets the format and its frame, a Footprint's, and a block takes its default state turned with the shape, so stairs, facing north by default, face forward, away from the player. A Key places its Assemblies once a Consumer states it a Key (#64), as the vanilla Consumer does for the block tag `groundworks:assembly_keys`. A malformed declaration, or one naming a block with no item to pay for it with, such as fire or water, is logged with its id and skipped, `/reload` reads them again, and the server sends them to each client. `Placements.planFor` serves an Assembly's plan, refused with the new `Refusal.Assembly` reasons, `TAKEN` as a `Refusal.At` its position and `KEY_WOULD_NOT_STAND` as one at the Key's, asked of the Key's survival on the shape laid round it, where its item says it must survive. (#55)
- The held stack's choice is the data component `groundworks:assembly_choice`, an index into its Key's Assemblies by id with the Key alone last, absent on the starting choice; `starts_alone` on any of a Key's declarations starts a fresh stack on the Key alone. The `next_assembly` payload carries a press to the server, and the key mappings `key.groundworks.next_assembly` and `key.groundworks.reverse_next_assembly` join Groundworks' category. (#58)
- An Assembly's legend letter may name a block tag with the block a position gets where the inventory gives none, `{"tag": "<tag>", "default": "<block>"}`, or a position kept empty, `"empty"`; the Key's letter still names a block. `AssemblyShape.fill` is the uniform fill, and its free fill, creative's, gives every position of a tag the member held most of, or the default where none is held, and an empty position taken refuses as `TAKEN` at it. (#56, #68)
- A declaration whose legend names a block tag the game doesn't know, such as a misspelt one, is logged with its id and the tag and skipped, checked against the tags of the same reload, so `/reload` after fixing it loads it; a tag that exists with no blocks is no error. (#61)
- `Rotate.isRotatable(Player, ItemStack)` also counts a held Key with an Assembly drawn, unless the player sneaks, which places the Key alone; `Rotate.isRotatable(ItemStack)` still answers for the block alone. (#57)
- `PlacementPlan` gains `clears`, the positions a plan empties without placing there, as an Assembly's empty positions holding grass, which the preview outlines; the three-argument constructor still builds a plan clearing nothing, and a record pattern on it now takes four components. (#62)
- `groundworks:assembly_keys` ships with `minecraft:beacon`, `minecraft:conduit`, `minecraft:carved_pumpkin` and `minecraft:obsidian`, and vanilla's declarations under `data/groundworks/groundworks/assembly/`: `beacon/level_1` to `level_4`, `conduit/frame_16` and `frame_42`, `golem/iron` and `golem/snow`, and `nether_portal`, which starts on the Key alone. A pack overrides one by its id, or drops a Key from the tag. The new block tag `groundworks:conduit_frame_blocks` holds the blocks a conduit's frame takes. (#59)
- Vanilla's copper golem ships as `golem/z_copper`, its body any block of `minecraft:copper` with `minecraft:copper_block` as the default; its id sorts after `golem/iron` and `golem/snow` so it comes third. (#63)
- An Assembly's Key is planned through its item's own placement, `BlockItem#getPlacementState`, asked of a read-only level as the shape will stand, so a Key's state, room and survival read the blocks the shape lays; `TAKEN` at the Key's position now also means it has no room or no state for placement there once the rest is laid. Light, heightmaps and entities are still read from the world as it stands. A Key vanilla refuses once the rest is laid has the laying taken back, and only what didn't come back, as blocks a golem took into itself, is charged. (#65)
- `Assemblies.keys(Predicate<? super Block>)` states which blocks are Keys, as `Placements.optIn` states an Opt-in, and `Assemblies.isKey(Block)` asks it; a block is a Key when any statement says so. The vanilla Consumer states its Keys from `groundworks:assembly_keys`, so a pack or a Consumer still makes a Key with a tag entry and no code. (#64)

## 0.5.11

### Players

- A **Stretch** and a **Fast Replace** play the place sound a block makes where it stands, and the preview hides a face against a neighbour the way the world does, for a modded block whose sound or faces depend on its surroundings.

## 0.5.10

### Players

- A footprint is refused whole, placing nothing and charging nothing, when a claim mod guarding the place event protects any of its positions, not only the clicked one. A footprint reaching into spawn protection is refused the same way, and drawn refused. (#50)

### Consumers

- `Footprint.plan` refuses the footprint as `BLOCKED` where the player may not build at any position, and `Footprint.place` fires the place event at each part as well as NeoForge's own at the origin, refusing the click whole where any is cancelled. (#50)

## 0.5.9

### Players

- Cocoa hanging on a log a **Stretch** replaces now goes with it, dropping nothing, as a plant standing on a replaced block does. (#39)

## 0.5.8

### Players

- With a Dismantle start or a queued span stored on a tool, a click on a block no Dismantle family claims now passes on to the tool and the block, with no message, and keeps what is stored, so another mod's wrench works on its own machines again. Confirming queued spans takes a click on a member; a click on open floor no longer confirms them. (#48)
- A **Stretch** goes through blocks of the held block's own **Replace group** instead of round them: each one at the stretch's height is replaced in place, drawn in the replace tint, charged one item and handed back what breaking it drops, as a Fast Replace hands back. A start, anchor or end aimed at one lies in it, so a road of stone bricks over a pack's `groundworks:replace_group/` tag of dirt and gravel is laid into the ground; Raise once before storing the start to lay it on top. A block that already is the held block stays as it stands, uncharged. A plant standing on a replaced block, sugar cane, cactus and bamboo included, goes with it, dropping nothing. A block outside the group, or of a group whose replace spans several blocks, is still gone round or refuses the leg. Columns do the same. The whole stretch is refused when the inventory has no room for every hand-back. (#39)
- The game tests' datapacks, `gametest_packs/`, no longer ship in the jar. Nothing ever switched them on outside Groundworks' own game tests (libworks ADR 0004).

### Consumers

- `FastReplace.groupOf(block)` answers a block's Replace group, a Consumer's or a vanilla tag's, as a `FastReplace.Group` with its `id()`, its `builder()` and `spansBlocks()`, or `null`. `FastReplace.stretchesThrough(held, there)` says whether a Stretch of `held` goes through `there`: both of one group whose replace is one block. A `LegBuilder` asks it to replace the blocks in its leg's way, naming them in its plan's `replaces()`; the vanilla Consumer's does. Every position any Consumer's stretch replaces now hands back what breaking it drops, rolled on the server. (#39)
- `ReplaceBuilder.spansBlocks()` says whether one replace of the builder's group spans several blocks, true by default. A builder that replaces the aimed block alone, as a belt tile's tier swap does, returns false, and a Stretch of its group then goes through its blocks: `FastReplace.Group.spansBlocks()` and `stretchesThrough` read it, where before any group with a builder was an obstacle. The stretch lays its leg builder's blocks and hands back what breaking each old one drops; the builder's `plan` and `refund` stay a Fast Replace's. (#49)
- `Dismantles.aimsAtMember(level, clicked)` says whether a click on `clicked` names a member of any Dismantle family, and so may be the Dismantle's; a click anywhere else passes on, whatever the held stack stores (ADR 0010). (#48)

## 0.5.7

### Consumers

- `HandBack` makes public the rules Fast Replace and a Stretch hand back by, for an item that plans its own replace and lays it itself. `HandBack.fits(player, hand, returned)` says whether `returned` fits once one item of the held stack is charged, and `HandBack.charge(player, hand, returned)` charges it and hands `returned` back into the slot the charge freed, else onto a stack of its own, else the first empty slot; either hand may hold the item. A player with infinite materials is charged nothing and handed nothing. (#37)

## 0.5.6

### Consumers

- The jar now carries every licence under `LICENSES/` (Apache-2.0 and Mojang's as well as MIT). Its Maven coordinates, pom, module metadata and `v<version>` tags are unchanged. Groundworks now builds from the libworks convention plugin. (#46)

## 0.5.5

### Consumers

- A **Footprint** may have up to **124 parts**, a 5x5x5 block less its origin: `FootprintShape.MAX_PARTS` is 124 and `FootprintPartBlock.PART` runs 1 to 124. A part's number is unchanged, so a world saved with parts 1 to 26 loads as before. A part block's blockstate JSON written with one variant per `part` value now needs entries up to the parts its footprint has; one with a single variant is unaffected. (#44)
- `FootprintShape.square(tileWidth, height)` is Factorio's tile square lying on the ground, the origin at its bottom centre; `standing` builds a wall. It throws for an even or non-positive `tileWidth`, a `height` below 1, or a shape over `MAX_PARTS`. (#44)

## 0.5.4

### Players

- A **Footprint** part left in a world after its mod changed the machine's shape no longer crashes the game when you right-click it, Fast Replace over it, or aim at it holding a block that replaces. It is left an orphan, as a part whose machine is gone is: clicks on it pass, nothing replaces it, and breaking it removes it alone. (#42)

### Consumers

- `FootprintShape.originOf` answers `null` for a part number outside the shape, and `FootprintShape.hasPart` says whether a number is in it, so `Footprint.standingOrigin` answers `null` for such a part, as for one whose origin is gone, rather than throwing. Fast Replace asks no group's `ReplaceBuilder` of an orphan part, so a builder need not guard against one. (#42)

## 0.5.3

### Players

- Groundworks' item tags, `groundworks:dismantles`, `groundworks:stretches` and `groundworks:stretches_vertically`, have display names, so EMI no longer warns that they are untranslated.

## 0.5.2

### Players

- **Footprints**: a machine a mod builds on several blocks is placed whole from one item, facing you, and the preview draws it whole; it is refused whole, placing nothing, when any of its blocks is taken. Breaking any of its blocks breaks the whole machine and hands its item back once, and Rotate in Place on any of its blocks turns it whole, keeping what it holds, or tells you there is no room. Jade shows the machine's own line on every block of it. Groundworks adds no machine of its own. (#40)
- **Fast Replace** hands back what breaking the replaced block drops, as if broken with its correct tool and no enchantment: a grass block hands back dirt, stone cobblestone, a double slab both slabs, and glass nothing. No replace is a silk-touch break, so a replace group can't turn into a source of grass blocks. A full inventory still refuses a replace that drops something, though the preview no longer shows that refusal before you click. (#38)

### Consumers

- `Footprint.declare(FootprintShape, origin, part, item)` declares a **Footprint** (ADR 0009): a `FootprintShape` in the origin's own frame, `x` forward, `y` up, `z` to the side, turned with the origin's facing as the Pack's machines were, with `FootprintShape.of` and `FootprintShape.standing`, up to 26 parts; the origin, any block with `Footprint.FACING` that calls `teardown` from its own `affectNeighborsAfterRemoval` and that a piston doesn't move; a part block registered from `FootprintPartBlock`, which draws nothing, hides no neighbour's face, drops nothing of its own and doesn't move for a piston; and the item, a `FootprintItem`, or any item whose `plan` and `place` call the footprint's. A placement is refused whole as `Refusal.Footprint.BLOCKED`, and the origin takes the item's block-entity data and components. A part's break drops what the origin drops for the breaker's tool, through NeoForge's drops event, and takes the footprint down; a part forwards clicks to the origin, and the block energy, fluid and item capabilities, side unchanged, invalidating them as it is laid, turned and broken. Rotate in Place turns a footprint whole with no statement, keeping the origin's block entity, refused as `Footprint.TURN_BLOCKED`. Groundworks reads Jade, compile-only, to show a part as its origin. (#40)
- A **Replace group** without a builder hands back the old block's loot, rolled on the server with the block's entity and the player, and an unenchanted netherite pickaxe, axe, shovel, hoe or sword, the first correct for the block, or no tool where none is. Before, it was the block's item. Since loot is the server's, the room check for it is the click's alone, and the plan the client asks is never refused as `Refusal.FastReplace.NO_ROOM_TO_RETURN`. A `ReplaceBuilder`'s `refund` is unchanged. (#38)

## 0.5.1

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
- `FastReplace.group(Identifier, Predicate<? super Block>, ReplaceBuilder)` states a **Replace group** whose replace spans several blocks: a `ReplaceBuilder` plans the whole replace from the held stack, the aimed position and the old state, with every position it swaps, the state at each and which it replaces, and says what the replace hands back, by default the aimed block's item. It plans nothing where it makes no replace, and the click is the item's own. The builder decides which members replace which, the aimed block's own kind included, and its plan is used as it is, with no orientation copied. A plan that replaces a position it puts no block at is refused as `Refusal.FastReplace.LEAVES_A_GAP`, since a replace swaps and never clears; a builder's own refusal is told through its `message`. One held item is charged and one refund handed back for the whole plan, and the library lays it as it lays the item's, each position swapped in the plan's order. (#35)

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
