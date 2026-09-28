## Purpose

Groundworks is a mod that works in a vanilla game with no other mod: mass placement, its preview, Rotate, Raise and Lower, the Stretch and the Dismantle, for vanilla blocks. It is not a library that waits for other mods. Vanilla is a Consumer like any other, built into Groundworks and wired the way Beltworks is. What each vanilla block does is vanilla's own logic; which blocks and items it reaches is set by one tag per feature, shipped with defaults, which a pack developer changes through data, with no code (ADR 0005). Adding Consumer mods, such as Beltworks, is also the pack developer's choice. Read `CONTEXT.md` before judging what Groundworks is for, what it should ship, or where it is published.

The code doesn't fully match this yet. The vanilla Consumer (`VanillaConsumer`) makes only its Opt-in and its Stretch builder so far, through the `groundworks:plan_opt_in` block tag with a namespace config, and the `groundworks:stretches` item tag. Rotate in Place and a Dismantle family still wait for a Consumer's statement made in code, as ADR 0001 decided and ADR 0005 replaces (5thlayer/groundworks#22). Say so when it matters. Don't treat that gap as the purpose.

## Releases

A change a Consumer can use or will notice adds its line under `## Unreleased` in `CHANGELOG.md` as it lands. Before bumping `mod_version`, publishing to `~/.m2` or tagging a release, read `docs/agents/releases.md`: releases go through `scripts/release.sh`, and a published version never changes.

A release that must reach Beltworks or the Pack follows the `release-train` skill: one owning session per checkout, releases in order Groundworks → Beltworks → Pack, and pushes only on the user's word.

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, used verbatim as label strings. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
