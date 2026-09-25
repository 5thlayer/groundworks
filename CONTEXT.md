# placementpreview

A library mod that shows a player what a held item would place before they click: a **Placement Plan**, drawn as a **Placement Preview**. It exists so the mods that share a screen (Beltworks and the PlanetaryFactory Pack) draw placements one way, with one renderer.

## Language

### Parties

**placementpreview**:
This library. It owns the plan, the drawing and the hooks, and nothing about any particular block.
_Avoid_: preview lib, the renderer

**Consumer**:
A mod that plans its items' placements through the library and draws its own additions through its hooks. Beltworks and the Pack are the two today.
_Avoid_: client (collides with the game's client side), dependent, integration

### Plans

**Placement Plan**:
What a held item would do at an aimed spot: the blocks it would put down (each a position and a blockstate), the positions among them it replaces, and a **Refusal** or none. Placing executes a plan and the preview draws one, so both ask one rule. A refused plan still carries its blocks, because a refusal is drawn where they would have gone; no plan at all means nothing to draw.
_Avoid_: ghost, build plan, preview state, placement context (vanilla's own type, one input to a plan)

**Refusal**:
Why a **Placement Plan** would not go through. Refusals are open: the library names only the **Vanilla** one, and each **Consumer** names its own.
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
A **Consumer**'s preview that replaces the **Placement Preview** for a frame, such as a dismantle's span. The first to draw wins; then no other Takeover draws and no plan is asked for.
_Avoid_: frame hook, override

**Marker**:
A **Consumer**'s drawing that shows whatever the aim, even while a **Takeover** draws, such as a belt stretch's stored start.
_Avoid_: always hook, indicator

**Outline**:
A red outline round given block positions, which **Consumers** use to mark what a **Takeover** would take up.
_Avoid_: dismantle outline, highlight
