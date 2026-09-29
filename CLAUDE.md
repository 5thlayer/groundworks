## Purpose

Groundworks is a mod that works in a vanilla game with no other mod: mass placement, its preview, Rotate, Raise and Lower, the Stretch and the Dismantle, for vanilla blocks. It is not a library that waits for other mods. Vanilla is a Consumer like any other, built into Groundworks and wired the way Beltworks is. What each vanilla block does is vanilla's own logic; which blocks and items it reaches is set by one tag per feature, shipped with defaults, which a pack developer changes through data, with no code (ADR 0005). Adding Consumer mods, such as Beltworks, is also the pack developer's choice. Read `CONTEXT.md` before judging what Groundworks is for, what it should ship, or where it is published.

The vanilla Consumer (`VanillaConsumer`) makes all four statements through data: its Opt-in through the `groundworks:plan_opt_in` block tag with a namespace config, Rotate in Place through the `groundworks:rotates_in_place` block tag, its Stretch builder through the `groundworks:stretches` item tag, and its Dismantle families through the block tags under `groundworks:dismantle_family/` (5thlayer/groundworks#22).

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
