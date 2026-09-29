# Groundworks is published on Modrinth and CurseForge as a mod

Since ADR 0005 Groundworks is useful in a vanilla game with no other mod, and that ADR foresaw a standalone page. Until now Groundworks reached players only nested in Beltworks, and libworks' `docs/agents/publishing.md` named it as the Library that never goes to the sites.

**Decision.** Groundworks gets a project on Modrinth and one on CurseForge, as a mod players install:

- **Beltworks keeps nesting it.** NeoForge loads the highest Groundworks nested or installed, so a player who also installs Groundworks gets the newer one, Beltworks stays one download, and the Pack still pins one jar. Moving Beltworks to a declared dependency is Beltworks' decision, later.
- **Uploads as Beltworks does.** `scripts/release.sh` uploads each release through a port of Beltworks' `scripts/upload.py`, with `--no-upload` for the release train, using the same 1Password items as Beltworks, since both tokens belong to the account, not a project.
- **Release files below 1.0.** `upload_release_type = release`, since CurseForge's app syncs only a project with a Release file, and ADR 0001's versions already say pre-1.0.
- **The first upload is the next release,** not 0.5.0, once the page has its README description and icon.
- **The changelog has two audiences.** Each version's section splits into Players, what a player or pack developer sees, first, and Consumers, what a mod building against Groundworks can use or will notice.
- **AI use is disclosed.** Much of Groundworks' code and page is AI output, so its Modrinth project carries the "Contains AI-generated content" disclosure (Modrinth Content Rules §6.1). No image on either page is AI output (§6.2): the gallery is in-game screenshots, and the icon is drawn by the author, from a written brief at most.

## Considered Options

- **Stay a library, nested only.** Rejected: a mod useful on its own that players can't find.
- **Beltworks declares Groundworks as a dependency** instead of nesting it. Deferred: it splits Beltworks into two downloads and changes what the Pack pins.
- **Beta files below 1.0,** Beltworks' default. Rejected: the CurseForge app would never list Groundworks.
- **Upload 0.5.0 now.** Rejected: its notes are written for mod authors, and the page has no description or icon yet.
- **Separate tokens per mod.** Rejected: nothing to gain while one account owns every project.

## Consequences

- libworks' `publishing.md` drops Groundworks as its example of a Library that skips the sites.
- `mod_description` becomes the page's summary, written for players.
- Groundworks gains a `README.md`, its page description, and the release docs gain the upload step.
- Every Groundworks release after the first upload is public on two sites, where, as in `~/.m2`, a version is final.
