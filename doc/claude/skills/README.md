# Claude skills (backup + installable)

The working copies of project Claude skills live in `.claude/skills/`, which is **gitignored** —
so they are not version-controlled and disappear on a fresh clone. This folder is the tracked
backup and distribution point.

For each skill you'll find:

- `<name>.skill` — an installable package (a zip of the skill directory). In Cowork/Claude,
  open it and use **Save skill** to install. From Claude Code, unzip it into `.claude/skills/`.
- `<name>/SKILL.md` — the same content as plain text, so changes are diffable in git.

## issue-retro

Retroactively assigns **Story Points (Retro)** to closed GitHub issues for a release, derived from
the number of distinct days work was committed for each issue. Pairs with the helper scripts in
`scripts/` (`setup-project.sh`, `backfill-points.sh`, `set-points.sh`, `clear-open-retros.sh`,
`audit-retros.sh`, `retro-lib.sh`). See the SKILL.md for the full workflow.

### Restore into a working clone

```bash
mkdir -p .claude/skills
unzip -o doc/claude/skills/issue-retro.skill -d .claude/skills/
```

### Re-package after editing the working copy

```bash
( cd .claude/skills && zip -rq /tmp/issue-retro.skill issue-retro -x '*.DS_Store' )
cp /tmp/issue-retro.skill doc/claude/skills/issue-retro.skill
cp .claude/skills/issue-retro/SKILL.md doc/claude/skills/issue-retro/SKILL.md
```
