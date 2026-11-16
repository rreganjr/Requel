# #493 — Hotfix: minimatch alerts in requel-angular dev tooling

https://github.com/rreganjr/Requel/issues/493

Hotfix branch `493-minimatch-hotfix` from `origin/master` (at 2.1.0 and #492), PR to `master`.
Size 2. Milestone `v2.1.1`, but **no release**: nothing users run changes.

## Summary

Dependabot reports two minimatch alerts on `master`, both in requel-angular's dev tooling:

1. **minimatch 10.2.6** (alert 217; affected >= 7.0.0 < 11.0.1, patched 11.0.1): pattern
   amplification in `firstPhasePreProcess()`. It came in through typescript-eslint 8.71
   (`@typescript-eslint/typescript-estree` asks for `^10.2.2`) and through @angular/cli 21.2's npm
   code: `glob` 13, `ignore-walk` 8 and `@tufjs/models` 4. Even their latest releases still ask for
   `^10`.
2. **minimatch 3.1.5**: came in through ESLint 9 (`eslint`, `@eslint/eslintrc`,
   `@eslint/config-array` ask for `^3.1.5`). Dependabot's only resolution downgraded
   typescript-eslint, so its run failed ("security_update_not_possible").

minimatch is not in the production bundle, the jar or the image: the production build from this
branch is byte-identical to the one from `master` (checked with `diff -r` of `dist/`).

## Locked decisions (2026-11-16)

1. **Hotfix on `master`, no release** (Ron). No version bump, no tag, no Docker rebuild, no
   user-facing docs. Dependabot measures `master`, so the alerts close when the PR merges.
2. **ESLint 10.** `eslint` and `@eslint/js` go to `^10` (ESLint 10.15 uses `minimatch ^11` and no
   longer pulls in `@eslint/eslintrc`). The config (`eslint.config.mjs`) is already flat and needs
   no change.
3. **typescript-eslint 8.73**, whose estree asks for `minimatch ^11.0.1`.
4. **An npm override for the Angular CLI's paths**: `overrides` forces `minimatch ^11.0.1` under
   `glob`, `ignore-walk` and `@tufjs/models`. A top-level `//overrides` note in `package.json` says
   why and when to drop each entry (once that package asks for `^11` itself). JSON has no comments,
   and npm ignores `//` keys.

## Changes

requel-angular
- `package.json`: `eslint` ^10.15.0, `@eslint/js` ^10.0.1, `typescript-eslint` ^8.73.0;
  `overrides` and its `//overrides` note.
- `package-lock.json`: re-resolved. The three nested `minimatch` 10.2.6 entries were removed so npm
  resolves them again under the override; `npm ls minimatch` now shows only 11.0.8.

## Test plan

`tmp/493-verify.sh` runs `npm ci`, `npm ls minimatch`, lint, the Angular typechecks, dev and
production builds and the full unit suite. No backend change, so no Maven verify.

- `npm ls minimatch`: six copies, all 11.0.8; `npm ls --all` reports nothing invalid.
- `npm run lint` passes on ESLint 10, and still fires: a probe file importing the bare `primeng`
  barrel fails with the #147 message.
- `tsc` (app and spec), `ng build --configuration development`, `ng build`, and the full Vitest
  suite (134 files) pass.
- The production `dist/` is identical to `master`'s.
- CI, e2e included.

## Out of scope

- Upgrading @angular/cli itself (22 is out, but that's a framework upgrade for 2.2).
- The other Dependabot-reported issues, if any remain after this merges.

## Risks

- **The override moves Angular CLI internals onto minimatch 11.** It's used by `ng add`/`ng update`
  (pacote, npm-packlist) and sigstore verification, which builds and tests don't exercise. minimatch
  11 keeps the `minimatch`/`Minimatch` exports those packages use; the change from 10 is the Node
  engine range (20 or 22+), and the repo needs Node 22.

## AC mapping

| AC | Covered by |
|---|---|
| `npm ls minimatch` shows nothing below 11.0.1 | `npm ls`, verify script |
| `npm ci`, lint, unit suite, dev and prod builds pass | verify script, CI |
| CI green, e2e included | CI |
| The minimatch alerts close after the merge | Dependabot, after merge |
