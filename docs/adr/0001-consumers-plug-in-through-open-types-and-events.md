# Consumers plug in through open types and events

Beltworks' ADR 0010 made placementpreview one library that Beltworks and the PlanetaryFactory Pack both draw through, and it keeps nothing about any particular block. Three choices set how a **Consumer** plugs in, and they are hard to change once two Consumers compile against them:

- **A Refusal is an interface with no methods.** The library ships only its **Vanilla** refusal, and each Consumer's own enum implements it. The renderer only asks whether a plan is refused. A Consumer's checks keep comparing its own enum values, and its reasons stay checked by the compiler.
- **Opt-in is a predicate over blocks,** registered at mod construction and read on both sides. A Consumer states its rule (the Pack's is "my namespace") and the library keeps no list of mods.
- **Overlays, Takeovers and Markers are NeoForge events** on the game bus. A Consumer that isn't loaded doesn't subscribe, so the renderer never checks which mods are present. On each frame Markers fire first. Then the first Takeover to draw cancels the rest and the plan. Otherwise Overlays fire for every plan drawn, refused or not, with its tint. The library promises no order between Consumers' Takeovers, and each claims only its own aim.

Every Consumer keeps one contract the library can't enforce: an item that plans its own placement **executes the plan it returned** when clicked, and the server's answer is the authority. A preview that disagrees with the click is worse than none, because a player builds against it.

## Considered Options

- **A Refusal as a namespaced id or a string.** Rejected. The Pack's checks would stop comparing enum values and start matching text, and a mistyped reason would compile.
- **Opt-in by namespace string or block tag.** A namespace is less expressive than a predicate for no gain. A tag can't cover a whole namespace, so the Pack would lose "a new block previews with no code".
- **Static registration methods for the hooks.** Rejected. It isn't how NeoForge mods take part in each other's rendering, and it would make the order of registration a contract.

## Consequences

- The library's API is semver from 0.1.0: a breaking change bumps the minor version below 1.0. Consumers declare a version range, because the jar nested in Beltworks and the one the Pack depends on resolve to a single loaded version.
