# Fast Replace lives in the library, scoped by each Consumer

The Pack built Fast Replace for its own blocks, with its own charge-and-refund step (`ReplaceHandoff`). Beltworks' tile, splitter, loader and feeder items plan their own placement, and a held item's plan belongs to the item's own mod (ADR 0001's contract), so the Pack cannot plan a replace for them. Groundworks already charges for replaced blocks and hands them back during a **Stretch**. So **Fast Replace** moves into the library both Consumers depend on, as **Rotate** did (ADR 0003): one gesture, and one click in any pack.

A plain click with a block item on a placed block of a **Replace group** puts the held block in its place. A sneak-click places beside. The plan names what it replaces, the **Placement Preview** draws that in the replace tint, and the click carries out the plan the preview showed.

The library keeps no list of mods, so each part is scoped by what Consumers state:

- **A Replace group is a registered statement.** `FastReplace.group(id, members, builder)` is called at mod construction, on both sides like `Rotate.turnsInPlace`, because the client draws the preview from the same statement. `id` is a `ResourceLocation`, `members` a predicate over blocks, and `builder` optional. A block belongs to at most one group, the first registered, and a block is never replaced by itself. Vanilla ships no group.
- **The plan comes from the item, or from the group's builder.** By default the library asks the held item's own plan for the aimed block's position and marks it replaced, so a single-block replace needs no code. A group that spans blocks, a splitter's two halves or a machine's footprint, gives a builder that returns the whole plan with its positions, states, `replaces`, and optionally what to hand back. A plan must place a block at every position it replaces: a replace swaps and never clears, so a builder that leaves one out is refused.
- **The new block copies the old block's properties by default,** every one the two blocks share whose value the new block accepts. A **Rotate** turn held on the stack overrides the copy: the new state comes from the item's plan, turned. A builder sets its own states.
- **One held item is charged and one block handed back,** however many blocks the replace spans. The refund goes to the slot the charge freed: the held slot if the charge emptied the stack, else a stack of the same item, else the first empty slot. With no room the click is refused, with its reason, before anything changes. A creative player is neither charged nor handed anything. The charge, the room check and the laying move out of `Stretches` into a shared class, so a **Stretch**'s refund takes the freed slot too.
- **The block-entity handoff stays the block owner's.** The library swaps each planned position with one `setBlock` and nothing else: no drops, no touching a block entity's contents. Beltworks keeps its tiles' items across any `setBlock` and the Pack keeps its machine's block entity. The claim guard Rotate uses, the place event, still asks protection mods.
- **A refused replace cancels the click** and puts its reason on the action bar, drawn red. It never falls through to placing beside. The library names `NO_ROOM_TO_RETURN` and `MAY_NOT_BUILD` in a `Refusal.FastReplace`, and a builder returns its own.
- **The replaced block does not count as an obstruction** when the new block is checked: the taken and survival checks treat the replaced positions as free. An entity in the way still refuses, as in vanilla.
- **A height held by Raise or Lower does not move a replace.** The click targets the aimed block, as the Pack's replace items do today, and leaves the height on the stack. A sneak-click places beside and takes the height.

## Considered Options

- **The Pack keeps Fast Replace.** Rejected. It cannot plan for Beltworks' items, and a pack without the Pack would lose the click.
- **Beltworks takes Fast Replace.** Rejected for ADR 0003's reason: the Pack would depend on a belt mod for a pack-wide gesture.
- **A pair predicate `(aimed, held)`.** Rejected. "The same kind, another tier" is a group, Factorio speaks in groups, and a group gives vanilla a tag with no code.
- **A per-block contract like `TurnsInPlace`.** Rejected. What replaces what is a statement about a kind of block, and the old block has no say in it.
- **The library builds every plan.** Rejected. A splitter's second half and a machine's footprint are not the item's single-block plan.
- **A player toggle.** Rejected. The preview is always on, and a sneak-click already places beside.
- **The height moves the replace target,** so raising the preview onto a tile makes it blue. Rejected for now. A tile can be aimed at directly, floating or not, and it would rework how an occupied raised spot is read. It can be added later.

## Consequences

- Adding Fast Replace is an API addition under ADR 0001's semver: the group statement, the builder and `Refusal.FastReplace` become part of what Consumers compile against.
- Beltworks states its four groups and keeps its contents across a swap. The Pack's own replace items move onto the library's click and the shared charge, and `ReplaceHandoff` goes. Beltworks' ADR 0002 already says Fast Replace is no longer the Pack's.
- A stale height no longer changes what a plain click on a grouped block does. It only shifts the sneak-click that places beside.
- A **Stretch**'s refund now lands in the freed slot, where before it went where the game put it.
