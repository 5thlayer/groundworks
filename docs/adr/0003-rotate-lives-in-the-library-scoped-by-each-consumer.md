# Rotate lives in the library, scoped by each Consumer

The Pack built **Rotate** (its ADR-0083 and ADR-0087): one key that **Rotates the Plan** of a held rotatable item, or else **Rotates in Place** the block under the crosshair, as Factorio's `R` does. Beltworks' ADR 0002 left it in the Pack, rejecting "the Mod takes Rotate as well" because the Pack would end up with two `R` bindings, so Beltworks on its own places tiles only by where the player looks. That costs Beltworks the key where it helps most: turning a stretch's start before the first sneak-click, and turning a placed tile. It also left Beltworks' own exceptions to Rotate in Place (the splitter, a sloped tile, the wedge, the refit of a level tile) in the Pack, since Beltworks could not depend on the Pack's contract.

So Rotate moves, whole, into the library that both Consumers already depend on: the key and Reverse Rotate, the turn the held stack carries, the look it turns, the placed block's own contract to turn, vanilla's turn as the default, and the claim guard through the place event. One library means one binding. Beltworks bundles the library, so a Beltworks install with nothing else gets Rotate.

The library keeps no list of mods, so each half is scoped by what Consumers state:

- **Rotate the Plan applies only where a Placement Preview is drawn**: an item that plans its own placement, or a block some Consumer has opted in. R never turns a placement the player cannot see. An oriented block that is not drawn is not rotatable, so R falls through to the aimed block.
- **Rotate in Place turns only blocks a Consumer has stated it turns**, a registered predicate separate from Opt-in. Beltworks states its own blocks. The Pack states every block, as it does today. Standalone Beltworks never runs vanilla's turn on another mod's block.
- **A block that must not turn, or turns its own way, says so through the library's contract to turn in place**, refused with its reason. Beltworks' splitter, sloped tile and wedge refuse, and its level tile refits. The Pack's deny list is gone. A Consumer that doesn't want a foreign block turned leaves it out of its statement, and R ignores it, as Factorio ignores an entity that cannot rotate.

The move lands after the rename to Groundworks, so the key, its category, the held turn and the packet are born under the new name and the Pack's players lose their binding and any half-turned stack once, not twice.

## Considered Options

- **Beltworks takes Rotate.** Rejected, for Beltworks ADR 0002's reason: the Pack would have two `R` bindings, or would depend on a belt mod for a pack-wide key.
- **The library takes only Rotate the Plan and fires an event for the placed half.** Rejected. Factorio's target rule, which takes the held item before the aimed block, would be split across two mods, and Beltworks on its own would have no Rotate in Place.
- **Rotate the Plan on every held oriented block, drawn or not,** as the Pack does today. Rejected. The turn is invisible, and holding an undrawn block takes R away from the aimed one.
- **Rotate in Place on every block in any install.** Rejected. Beltworks would run vanilla's turn on multiblocks and kinetic blocks in packs that never asked for it.
- **Keep a registered deny list with reasons.** Rejected. No foreign block was ever on it, and a block that should explain itself can do so through its own contract.

## Consequences

- The Pack widens its Opt-in from its namespace to every block with an orientation, so R keeps working on the vanilla and foreign blocks it turns today, now with a preview. It accepts the Vanilla Plan's quirks there: a two-part block such as a door or bed is drawn as one half, and a block that sets its state after placement is drawn in the state it won't keep.
- Beltworks ADR 0002 is amended: Rotate is no longer the Pack's, and a tile turns by Rotate in any pack. The Pack's ADR-0083 and ADR-0087 point here for the mechanism.
- R while a stretch's start is stored still leaves the stretch alone. It turns the held stack, which the next start or tile takes.
- Adding Rotate is an API addition under ADR 0001's semver, and the contract to turn in place becomes part of what Consumers compile against.
