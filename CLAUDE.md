## Purpose

Groundworks is a mod that works in a vanilla game with no other mod: mass placement, its preview, Rotate, Raise and Lower, the Stretch and the Dismantle, for vanilla blocks. It is not a library that waits for other mods. Vanilla is a Consumer like any other. Its features ship switched off, and a pack developer switches them on for the blocks and items they choose, through data, with no code (ADR 0005). Adding Consumer mods, such as Beltworks, is also the pack developer's choice. Read `CONTEXT.md` before judging what Groundworks is for, what it should ship, or where it is published.

The code doesn't fully match this yet. Most features still wait for a Consumer's statement made in code (Opt-in, Rotate in Place, a Stretch's builder, a Dismantle family), as ADR 0001 decided and ADR 0005 replaces. Say so when it matters. Don't treat that gap as the purpose.

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
