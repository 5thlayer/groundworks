## Purpose

Groundworks is a mod that works in a vanilla game with no other mod: mass placement, its preview, Rotate, Raise and Lower, the Stretch and the Dismantle, for vanilla blocks. It is not a library that waits for other mods. Vanilla is a Consumer like any other, built into Groundworks and wired the way Beltworks is. What each vanilla block does is vanilla's own logic; which blocks and items it reaches is set by one tag per feature, shipped with defaults, which a pack developer changes through data, with no code (ADR 0005). Adding Consumer mods, such as Beltworks, is also the pack developer's choice. Read `GLOSSARY.md` before judging what Groundworks is for, what it should ship, or where it is published.

The vanilla Consumer (`VanillaConsumer`) makes its statements through data: its Opt-in through the `groundworks:plan_opt_in` block tag with a namespace config, Rotate in Place through the `groundworks:rotates_in_place` block tag, its Stretch builder through the `groundworks:stretches` item tag and its Columns through `groundworks:stretches_vertically`, its Dismantle families through the block tags under `groundworks:dismantle_family/` (5thlayer/groundworks#22), and its Replace groups through the block tags under `groundworks:replace_group/`, of which it ships none (5thlayer/groundworks#34).

## Workflow

Commit on the current branch; open a feature branch only when the user asks for one. Nothing is pushed without the user's word.

Anything that changes Groundworks' behaviour gets a `/code-review`: the project skill in `.claude/skills/code-review`, from mattpocock/skills, never the built-in review of the same name. Doc and plumbing changes skip it: that covers `CLAUDE.md`, `GLOSSARY.md`, ADRs, `docs/`, `.claude/`, submodule bumps, and tooling or CI config.

## Commits

Conventional commits: `<type>(<optional scope>): <summary>`, with the summary in the imperative and lower case. The types in use are `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci` and `chore`. A breaking change marks its type with `!` (`feat!: ...`), and its release bumps the minor (ADR 0001). A commit that closes an issue ends its body with `Closes #<n>`.

## Testing

`sh ./gradlew build` runs the JUnit tests, on a plain JVM with no Minecraft. `sh ./gradlew runGameTestServer` runs the game tests headless, a real player on a real server, and names each one it ran; it fails if it ran none. `python3 -m unittest discover scripts/tests` tests the upload step against a stand-in server on localhost. A new game test class is registered by a line in `GroundworksGameTests.registerTests`, and its tests stand on the `gametest/platform` structure that `scripts/build-gametest-structures.py` writes. CI (`.github/workflows/ci.yml`) runs all three on every push and never publishes.

The `skillworks:quicklaunch` skill opens the dev client into the most recent save in `run/saves`, one client per checkout.

## Releases

A change adds its line under `## Unreleased` in `CHANGELOG.md` as it lands, under Players, Consumers, or both (ADR 0007). Before bumping `mod_version`, publishing to `~/.m2`, tagging a release or uploading to Modrinth or CurseForge, read `docs/agents/releases.md`: releases go through `scripts/release.sh`, which uploads last with `scripts/upload.py`, and a published version never changes, in `~/.m2` or on either site. ADR 0001, inherited from 5thlayer/libworks, sets the version bumps.

A release that must reach Beltworks or the Pack follows the `release-train` skill: one owning session per checkout, releases in order Groundworks → Beltworks → Pack, and pushes only on the user's word.

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, used verbatim as label strings. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
