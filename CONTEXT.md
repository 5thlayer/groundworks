# Groundworks

A mod for mass placement and **Dismantle**: it plans what a click would lay or take up, shows the plan before the click, and carries it out. It works in a vanilla game with no other mod: vanilla is a **Consumer** like any other, built into Groundworks, and tags a pack developer can change set which vanilla blocks and items it reaches (ADR 0005). Mods such as Beltworks, and packs such as FactoryWorks, add **Consumers** of their own, so that everything on one screen places, previews and dismantles one way (ADR 0002). Adding them is the pack developer's choice. Groundworks doesn't depend on them to be useful.

## Language

### Parties

**Groundworks**:
This library. It owns the plan, the drawing, the hooks, **Rotate**, **Fast Replace**, the **Stretch** and the **Dismantle**, and nothing about any particular block. Formerly placementpreview, which only drew **Placement Plans** (ADR 0002).
_Avoid_: placementpreview (its old name), preview lib, the renderer, Groundwork

**Consumer**:
Whatever plans its items' placements through Groundworks and draws its own additions through its hooks. Vanilla is one, built into Groundworks: what its blocks do is vanilla's own, and tags set which blocks and items it reaches. Beltworks and the Pack are others, added by whoever builds the pack.
_Avoid_: client (collides with the game's client side), dependent, integration

### Plans

**Placement Plan**:
What a held item would do at an aimed spot: the blocks it would put down (each a position and a blockstate), the positions among them it replaces, and a **Refusal** or none. Placing executes a plan and the preview draws one, so both ask one rule. A refused plan still carries its blocks, because a refusal is drawn where they would have gone; no plan at all means nothing to draw.
_Avoid_: ghost, build plan, preview state, placement context (vanilla's own type, one input to a plan)

**Refusal**:
Why a **Placement Plan**, or a **Dismantle**'s span, would not go through. Refusals are open: the library names only the **Vanilla** one and a span's "not the same kind", and each **Consumer** names its own.
_Avoid_: error, failure, rejection

**Vanilla Plan**:
The **Placement Plan** of an item that places as vanilla does, asked of the game rather than restated. It is drawn only for blocks a **Consumer** has **opted in**.
_Avoid_: default plan, generic plan

**Opt-in**:
A **Consumer**'s statement of which plain blocks get a **Vanilla Plan**. An item that plans its own placement needs none; it is always drawn. The library keeps no list of mods.
_Avoid_: whitelist, allowlist

### Drawing

**Placement Preview**:
What the player sees while holding a planning item and aiming at a spot: the plan's blocks drawn translucent, blue where they replace, red where the plan is refused. Always on, with no toggle. It changes nothing in the world.
_Avoid_: ghost (Factorio's ghost is an entity left for robots to build), hologram, blueprint preview

**Overlay**:
A **Consumer**'s addition drawn with a **Placement Plan**, given the plan and its tint: a pole's supply area, a splitter's belt surface.
_Avoid_: decoration, extra

**Takeover**:
A preview that replaces the **Placement Preview** for a frame, such as a **Dismantle**'s span, which Groundworks draws ahead of any **Consumer**'s. The first to draw wins; then no other Takeover draws and no plan is asked for.
_Avoid_: frame hook, override

**Marker**:
A **Consumer**'s drawing that shows whatever the aim, even while a **Takeover** draws, such as a belt stretch's stored start.
_Avoid_: always hook, indicator

**Outline**:
A red outline round given block positions, which Groundworks and **Consumers** use to mark what a **Takeover** would take up, such as a **Dismantle**'s span.
_Avoid_: dismantle outline, highlight

### Rotating

**Rotate**:
One action on one key (`R` by default) that turns a quarter at a time. It **Rotates the Plan** when the held item is rotatable -- its plan is drawn and the block it places has a facing, an axis or a rotation -- and otherwise **Rotates in Place** the block under the crosshair. That is Factorio's rule for which target the key takes, and to the player it is one verb.
_Avoid_: rotate key, turn, wrench rotate

**Reverse Rotate**:
**Rotate** the other way (`Shift+R` by default), on both targets. A separate action rather than a modifier, so it can be rebound alone.
_Avoid_: counter-rotate, rotate back

**Rotate the Plan**:
**Rotate** on the held item: its next placement turns a quarter from the way the player looks, and the **Placement Preview** redraws with it. The turn is relative to the look, not a compass direction, because the player's camera turns. It stays with the held stack until the stack's last item is placed, or until a **Stretch**'s start takes it: the start stores the look turned by it and uses it up, as it uses up the height. With a start stored, it leaves the stretch alone and turns the held stack for the next start or placement. It applies only where a **Placement Preview** is drawn, so it never turns a placement the player cannot see.
_Avoid_: rotate the preview (the preview only follows the plan), rotate the ghost, held rotate

**Rotate in Place**:
**Rotate** on a placed block: the block under the crosshair turns, and what turning means is the block's own -- a belt tile turns, a machine keeps its contents. A block that cannot take the turn is refused with its reason and nothing changes; there is no preview of it. It turns only blocks a **Consumer** has stated it turns, a statement separate from **Opt-in**: Beltworks states its own blocks, the Pack states every block.
_Avoid_: placed rotate, wrench rotate

### Replacing

**Fast Replace**:
A plain click with a block item on a placed block of a **Replace group** puts the held block in its place: one held item is charged however many blocks the replace spans, and the block it replaces is handed back into the slot the charge freed, or the click is refused with its reason when there is no room. A sneak-click places beside instead, and takes any height **Raise** or **Lower** holds: a replace ignores the height and leaves it on the stack. A turn **Rotate** holds re-orients the replace; without one the new block keeps the replaced block's orientation. The **Placement Plan** names what it replaces and the **Placement Preview** draws that in the replace tint, and the click carries out the plan the preview showed. What becomes of the replaced block's contents is the block's own. A creative player is neither charged nor handed anything.
_Avoid_: upgrade, swap, replace click

**Replace group**:
A kind of block that a **Fast Replace** swaps for another block of the same kind, such as one tier of belt tile for another. A **Consumer** states each group, as it states its **Opt-in** and what it **Rotates in Place**; a block belongs to at most one, and a block is never replaced by itself. A replace may span several blocks, a splitter's two halves or a machine's footprint, and the **Consumer** says which, and what the new blocks are and what comes back. Vanilla ships no group of its own.
_Avoid_: replace list, upgrade path

### Stretching

**Stretch**:
The blocks one drag of a held item lays, planned, charged, laid and refused whole. Its route passes through every **Anchor**, one **Leg** after another. It never changes height by itself: only **Raise** and **Lower** do. Its **Placement Preview** draws each block as it will stand once the whole stretch is laid, joined to its planned neighbours and to the world beside it.
_Avoid_: run, zoop, drag (the gesture, not what it lays)

**Anchor**:
A point of a **Stretch**'s route that the player fixed: its start, each point a sneak-click adds, and the aimed end. The aim picks only where it lies seen from above; its height is the stretch's height there, never the aimed block's. Only the start takes a height of its own, from **Raise** and **Lower**.
_Avoid_: corner (a block's shape, not a point of a route), waypoint, node

**Leg**:
The part of a **Stretch** from one **Anchor** to the next. It rises or falls by the height the player set right after its first anchor, along its first direction, then runs level to the next anchor. Seen from above it is one straight line or two joined by one turn. What a rise is built of is the item's: slopes for a belt, a straight climb for a pipe.
_Avoid_: segment, section

**Column**:
A **Stretch** that goes only straight up or down from its start, with no **Leg**: a ladder up a wall, a chain down a shaft. It is drawn by clicking the start's own column seen from above, or anywhere for an item that stretches only as a column; the aim sets nothing else. Its length is the height **Raise** and **Lower** set after the start is stored, capped only by the world's build height, never by the player's reach. Every block is placed with the look stored with the start, turned by **Rotate**, as if clicked on the block before it, so a chain stands upright and a ladder faces off the wall. It has no anchors between its ends, so a sneak-click is refused, and it never joins level legs. Items in `groundworks:stretches_vertically` stretch as a column; an item also in `groundworks:stretches` draws legs when the end is aimed off the start's column.
_Avoid_: vertical leg, climb (what a pipe's rise is built of), shaft

**Raise** / **Lower**:
The two actions (`G` and `B` by default) that move the held item's next placement one block up or down, wherever a **Placement Preview** is drawn. The height is capped by the player's reach, in whole blocks. On a single placement it moves the block straight up or down from where it would go, and it stays with the held stack until the stack's last item is placed. Storing a **Stretch**'s start uses it up as the start's height. From then on they set the rise of the **Leg** being drawn, one net height per leg, which the next stored anchor freezes into it.
_Avoid_: height gesture (the pair's old working name), elevate, lift

**Detour**:
The way a **Leg** goes round an obstacle at its own height, flat, on the side of the leg the player stands on, and never far from the straight line. It leaves and meets the leg's **Anchors** the way the leg does, so the rise stays put and the next leg heads the same way; an obstacle right beside an anchor is not gone round. What is an obstacle is the item's to say; a leg that no detour clears is refused.
_Avoid_: pathfinding, reroute, go-around

### Dismantling

**Dismantle**:
Taking up a span of one **Dismantle family** from a start to an end, both included, with a tool the start's family accepts, by default one in `groundworks:dismantles`: a sneak-click on a member stores the start on the held stack, and a click names the end. A sneak-click with a start stored queues the span to the clicked block and clears the start, or is refused and leaves the start where it is. A click confirms the queued spans, and the span it ends if a start is stored, as one pass: each is planned again, and the pass goes through whole or not at all. A pass may take spans of several families, at most two spans, and takes a position two spans share once. A sneak-use in the air clears the start and the queue. A start whose block is gone, whose family no longer counts it the same start, or that is in another dimension, is no start. An end outside the start's family is refused as not the same kind. What the span takes goes to the inventory, and what doesn't fit drops at the player's feet; a creative player is handed nothing. While a start or a queued span is stored the preview outlines every queued span and the span to the aim, or only the starts when the pass would be refused.
_Avoid_: deconstruct, mass mine, unstretch

**Dismantle family**:
A kind of connected block that a **Dismantle** takes up as one span, with its own rule for which blocks a span follows between its two ends and what the span takes. A **Consumer** supplies each one: Beltworks' belt family follows one transport line and takes the tiles' wedges and items with them, and the Pack's pipe family takes the shortest joined path. Vanilla's families are block tags under `groundworks:dismantle_family/`, one family per tag, shipped as fences, walls, bars and rails: a span takes the shortest joined path through one tag's blocks, where two touching blocks are joined if the block's own sides say they connect, or simply by touching for a block with no sides to say, and only with the tool vanilla breaks every one of them with. Groundworks runs the gesture for every family alike, and a span never crosses from one family to another.
_Avoid_: dismantle group, connected type
