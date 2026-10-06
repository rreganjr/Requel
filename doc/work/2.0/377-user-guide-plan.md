# #377 User guide on the website — implementation plan

Child of the Release 2.0.0 epic #375. Review: 2026-10-06 against `release/2.0` after #376.

## Summary

A user guide for 2.0 as static pages on the GitHub Pages site
(https://rreganjr.github.io/Requel/guide/), with screenshots a script retakes for each release.

- `website/guide/`: nine pages. Contents and concepts, getting started, projects, requirements,
  stakeholders, issues and discussion, AI review, reports, API/MCP/CLI.
- `website/images/guide/`: 22 screenshots of the sample project, from
  `scripts/guide-screenshots.mjs`.
- `website/site.css`: the site's styles, moved out of `index.html` so the guide shares them.
- `website/index.html`: "What's new" as in the README, a User guide section, the sample link on
  `master`, the 2009 PDF labelled as the 1.0 guide.
- The README points at the guide.
- CI skips website-only pushes; the release steps retake the screenshots.

## Locked decisions (2026-10-06)

1. Static HTML pages on the existing Pages site, styled like `index.html`. No site generator.
2. A repeatable Playwright script takes the screenshots.
3. `ci.yml` paths-ignore gets the website's pages, styles and images. `website/integration/**`
   is not ignored: `ProjectSchemaNameLengthTest` reads the published schema.
4. The 10 stale `requel-*.png` screenshots at the repo root are deleted.
5. All eight topic pages in one change.
6. The 2009 User Guide PDF stays under Background, labelled as the 1.0 guide; the README links
   the new guide.

## Screenshot script

`node scripts/guide-screenshots.mjs` against a running 2.0 on an empty database, from the repo
root, using `requel-angular`'s Playwright.

- Setup through the UI, skipped when already done: import `doc/samples/Requel.xml`, and give the
  user `manageApiTokens` so Settings shows the token section (then log in again, since the UI
  reads permissions at login). It changes data: a throwaway install only.
- Then one shot per page in a list: name, route, and options (`full`, `element` + `hasText` for
  one issue, `before` to tick permissions without saving).
- Waits for `load`, `main` and 2 s, not `networkidle`: the live-refresh stream never goes idle.
- Settings: `REQUEL_URL`, `REQUEL_USER`, `REQUEL_PASSWORD`, `REQUEL_PROJECT`, `GUIDE_OUT`,
  `CHROMIUM_PATH`.

## Sample project cleanup

The screenshots show `doc/samples/Requel.xml`, which still had joke test data from 2025:

- glossary terms "Yellow Snow" and "Pee Pee Snow Cone" → "Stakeholder" and its alternate
  "Interested Party";
- scenario "test top level scenario" → "Create a new project";
- "underpants" → "members";
- seven notes and issues (and four positions) analysing the old scenario text removed.

`ImportProjectStreamingCommandTest` (names, counts 265 → 258 and 145 → 143) and
`ProjectXmlStreamingRoundTripIT` (glossary names) follow.

## Release steps

`RELEASE_PROCESS.md`: website pages join the commits that skip the PR, the CI table says so,
and testing the candidate includes retaking the screenshots and setting the site banner.

## Found while writing (not fixed here)

- Per-item AI review has no button in the UI or tool in MCP; the guide shows the API call.
- The resolve button on a hand-written position reads "Ignore" (`resolveLabel` default).
- Exports carry user password hashes; the guide says so.
- `requel-cli` is not attached to releases; the guide says to build it.
- Deleting the imported sample project fails on a `scenarios_annotations` foreign key.
- Re-running analysis on the sample adds a second "Ignore this word." position to issues that
  already had one.

## Test plan

- Sample-dependent tests: `ImportProjectStreamingCommandTest`, `ImportProjectCommandTest`,
  `ProjectSchemaNameLengthTest`, `ProjectXmlStreamingRoundTripIT`.
- Every local link and anchor in `website/` resolves; every screenshot is used.
- Pages render without horizontal scroll at 390 px.
- The script runs end to end against a fresh install and produces all 22 shots.
- `mvn clean verify`; `./scripts/check-doc-links.sh`.
