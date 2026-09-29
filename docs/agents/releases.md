# Releases

A release reaches Consumers through the local maven repository (`~/.m2`) at a version of its own. Consumers read the Library from there by version, and the Pack (adamico/planetary-factory) pins the jar's sha256. That only works if the jar published at a version is the one jar that version ever names.

Nothing publishes anywhere else. CI builds and tests on every push but never publishes.

## The changelog

Each change adds its line under `## Unreleased` in `CHANGELOG.md` when it lands, with its issue number: under `### Players` what a player or pack developer sees, in plain words, and under `### Consumers` what a mod building against Groundworks can use or will notice, in the glossary's terms. A change may need a line in both (ADR 0007). A release ships what Unreleased lists, so the changelog is written as the work is done and never reconstructed from commits.

## Cutting a release

`scripts/release.sh <version>` from a clean main. libworks' ADR 0001 (https://github.com/5thlayer/libworks/blob/main/docs/adr/0001-below-1-0-an-addition-bumps-the-patch.md) sets the semver: below 1.0 only a breaking change bumps the minor version, and an addition or a fix is the next patch. The script refuses a version that is already tagged or in `~/.m2`, and an empty Unreleased. It then:

1. sets `mod_version` and turns `## Unreleased` into `## <version>` under a fresh, empty Unreleased
2. runs the build and the game tests, putting both files back if either fails
3. commits `chore: release <version>`, runs `publishToMavenLocal`, and tags `v<version>` with the jar's sha256

It pushes nothing, and ends by printing the push command. To try the script out, set `MAVEN_REPO_LOCAL` to a scratch folder and run it in a throwaway clone: a version published to the real `~/.m2` is permanent.

A release that must reach another Library or the Pack follows the `release-train` skill.

## A published version is final

A version in `~/.m2` never changes. A fix is the next patch version. `publishToMavenLocal` refuses a version that is already there (`build.gradle`), and nothing is deleted or overwritten by hand to get past it.

## Tags

A release is tagged `v<version>`, annotated with its jar's sha256. `git tag -l 'v*' -n9` lists them.

## Versions so far

`0.1.0` to `0.3.0` predate the script. Each was bumped inside a feature commit and published by hand, and was tagged afterwards on the commit whose rebuild matches the jar in `~/.m2` byte for byte:

- `v0.1.0`: `a71f87d` (#5)
- `v0.2.0`: `0775ef2` (#11)
- `v0.3.0`: `19a1f54` (#13)
