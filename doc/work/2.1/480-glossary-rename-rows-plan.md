# #480 — rc3: renaming a glossary term can drop other terms out of the project's glossary

https://github.com/rreganjr/Requel/issues/480

Branch `480-glossary-rename-rows` from `release/2.1` (at `2.1.0-rc3`), PR to `release/2.1`.
Size 1. Found in the rc3 acceptance test (#420); the fix ships in rc4.

## Summary

After a rename, editing a synonym of another term was refused with "GlossaryTerm not found: 1".
Term 1 was still in the project (`terms.projectordomain_id`), but its row in
`pods_glossary_terms`, the join table behind a project's glossary, was gone. The API finds terms
through the glossary.

## Cause

- A project's glossary is a `SortedSet` ordered by name (`GlossaryTermImpl.compareTo`).
- `EditGlossaryTerm` through the API loads that set to find the term, then the merge renames a
  term in it. The renamed term stays where its old name sorted.
- #348 then adds the old name as a synonym, which dirties the set. At flush Hibernate compares
  the set with its snapshot (a `TreeMap` keyed by the same, now out-of-order, objects), misses
  some terms, and deletes their join rows: the renamed term's own, or another's.
- In the rc3 database, renaming "Pee Pee Snow Cone" (a synonym of "Yellow Snow") to "Zoiniks"
  deleted Yellow Snow's row.

Before 2.1 a rename never changed the set, so Hibernate never compared it.

## Locked decisions (2026-11-09)

1. **Re-sort the project's glossary after a rename.** `AbstractProjectOrDomain.resortGlossaryTerms()`
   replaces the set with a new `TreeSet` built one element at a time, so it is in name order
   again (not `new TreeSet<>(sortedSet)`, which copies the order as it stands). Hibernate treats a
   replaced collection as new and rewrites its rows from it. `EditGlossaryTerm` calls it right
   after the merge when the name changed, before the synonym is added.
2. **Repair the data in `V44`.** It inserts the `pods_glossary_terms` row of every term that has
   none, from the term's own `projectordomain_id`. `glossary_terms_id` is unique, so a term that
   has a row is left alone.
3. **The referers' glossary sets are left as they are.** No caller renames a term and changes its
   referers in one command: the API's `EditGlossaryTermInput` has no referers, and the only
   caller of `setAddReferers`, the add-to-glossary position, doesn't rename.

## Changes

project-jpa
- `AbstractProjectOrDomain.resortGlossaryTerms()`.
- `EditGlossaryTermCommandImpl`: calls it after the merge when the name changed.

requel-app
- `V44__restore_project_glossary_rows.sql`.

## Test plan

`tmp/480-verify.sh` runs the backend `mvn clean verify -pl modules/coverage-report -am` and the
coverage-modules check. No Angular change.

- **`GlossaryRenameApiIT`** (new), through `/api/commands/EditGlossaryTerm`, the way the term
  editor sends it, so the glossary is loaded before the rename:
  - Renaming a synonym ("Pee Pee Snow Cone" → "Zoiniks") leaves all three terms in the
    project's glossary, and the synonym can then be edited with its canonical term set. (This
    is the rc3 report.)
  - Renaming a canonical term leaves every term in the glossary.
  - Both fail without the fix.
- **`ProjectGlossaryRowsMigrationMySqlIT`** (new): at V43, a term with no row and three with rows,
  in two projects; after V44 every term has exactly one row, in its own project.
- The existing `GlossaryRenameIT` and `ReplaceTermTextIT` run unchanged.

## Out of scope

- Other sorted sets whose sort key can change. None in 2.1 has a rename that also changes the
  set in the same command.

## Risks

- **A rename rewrites the project's glossary rows** (delete them all, insert them all). It's one
  statement plus one insert per term, only on a rename.

## AC mapping

| AC | Covered by |
|---|---|
| A rename of a synonym or a canonical term leaves every term in the glossary | GlossaryRenameApiIT |
| After renaming a synonym, it can still be edited with its canonical term | GlossaryRenameApiIT |
| Upgrading restores a missing glossary row, with no duplicates | ProjectGlossaryRowsMigrationMySqlIT |
