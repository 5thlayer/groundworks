## Releases

A release bumps `mod_version` in `gradle.properties`, then runs `./gradlew publishToMavenLocal`. Each version is published once, so a fix ships as the next patch version. Before publishing, confirm the new version has no folder under `~/.m2/repository/io/github/5thlayer/groundworks/`. ADR 0001 sets the API's semver: below 1.0 a breaking change bumps the minor version.

Why: Beltworks and the Pack (adamico/planetary-factory) read Groundworks from `~/.m2` by version. Republishing replaces the jar under the same coordinate. A Consumer resolved to that version then gets different code with no signal, and the Pack's drift check reads it as a jar nobody chose.

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, used verbatim as label strings. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
