# #321 — One coverage aggregate across every module

https://github.com/rreganjr/Requel/issues/321

Branch `321-coverage-aggregate` from `release/2.1`, PR to `release/2.1`.

## Summary

Codecov reads per-module JaCoCo reports, each built from that module's own tests, plus the
aggregate in `requel-app`. The aggregate only covers `requel-app`'s direct dependencies, so
`assistant-api`, `assistant-core` and `gateway-api` (transitive) and `requel-cli` and
`gateway-rest-client` (not dependencies at all) still show their own-tests-only figure, and
nothing notices when a module falls out. Replace it with one aggregate built in a report-only
module that depends on every module, upload that alone, guard it in CI, and document it.

Already done since the issue was filed: CI and `release.yml` build
`-pl modules/requel-app,modules/requel-cli -am`, so the `requel-cli` and `gateway-rest-client`
tests run in CI and fail the build (AC 2).

## Locked decisions (2026-10-28)

1. **A report-only module, `modules/coverage-report`** (`pom` packaging, no code), declaring every
   module with `src/main/java` as a direct dependency and running `report-aggregate` at `verify`.
   `requel-app`'s `jacoco-report-aggregate` execution is removed.
2. **The `com/rreganjr/nlp/impl/**` exclude stays**, stated once, in `coverage-report`'s pom, with
   the reason: the legacy OpenNLP pipeline in `nlp-jpa` is barely exercised and may be removed, and
   counting it would drag every figure down. The root pom's per-module report config is unchanged
   (no longer uploaded).
3. **A CI check** in `.github/scripts/check-coverage-modules.sh`: every module directory with
   `src/main/java` must appear as a `<group name="<artifactId>">` in the aggregate XML, or the step
   fails naming the missing ones. (`.github/`, not `scripts/`: CI skips pushes that only touch
   `scripts/`.) Module directory names equal their artifactIds (checked 2026-10-28).
4. **Docs in `doc/guides/COVERAGE.md`.**
5. **The one-time Ubuntu 26.04 check left by #300** rides on this PR: a temporary
   `runs-on: ubuntu-26.04` on both jobs in one commit, reverted before merge.

## Step by step

1. `modules/coverage-report/pom.xml`: parent is the root pom; `<packaging>pom</packaging>`;
   dependencies on all 24 code modules (requel-app included) (`${project.version}`); the jacoco plugin with one
   `report-aggregate` execution at `verify` (`com/rreganjr/**` included, `com/rreganjr/nlp/impl/**`
   excluded with the reason in a comment, HTML + XML, output `target/site/jacoco-aggregate`).
   Deploy/install skipped if the build would otherwise publish it.
2. Root `pom.xml`: add `<module>modules/coverage-report</module>` last.
3. `modules/requel-app/pom.xml`: drop the `jacoco-report-aggregate` execution and its comment
   (moved, with the #313 history, to `coverage-report`).
4. `ci.yml` `Build & test`:
   - build `mvn -pl modules/coverage-report -am verify` (every module, since it depends on all);
   - a step running `.github/scripts/check-coverage-modules.sh` after the build;
   - the Codecov backend upload sends `modules/coverage-report/target/site/jacoco-aggregate/jacoco.xml`
     only, with `disable_search: true`;
   - the `jacoco-coverage` artifact includes `modules/coverage-report/target/site/jacoco-aggregate/`.
5. `release.yml`: the same `-pl modules/coverage-report -am verify`, so its test run matches CI.
   The jars it attaches are still built (both are in the reactor).
6. `doc/guides/COVERAGE.md`: what produces the number, what is uploaded, the exclude and why, how to
   read a module at 0%, and that adding a module means adding it to `coverage-report`. Linked from
   `doc/README.md` and `CLAUDE.md`'s Testing section.

## Test plan

`tmp/321-verify.sh`: `mvn clean verify -pl modules/coverage-report -am`, then the check script
against the produced XML; and a negative run of the script against a copy of the XML with one group
removed, which must fail.

- **CI on the PR:** the check step passes; Codecov receives exactly one backend JaCoCo report; the
  Codecov figure for `assistant-core` matches its group in the aggregate (AC 3).
- **Ubuntu 26.04:** both jobs green with `Runner Image` showing `ubuntu-26.04`, then the revert.

## Out of scope

- Coverage thresholds or status gating; the README badge.
- Frontend (Vitest/Playwright) coverage, which is uploaded separately and unchanged.

## Risks

- **`report-aggregate` and `requel-app`'s repackaged jar.** The goal reads reactor projects' class
  and exec directories, not the jar, so Spring Boot's repackage does not matter; the check step
  would catch a module that silently drops out.
- **Build time.** `-am` from `coverage-report` builds the same modules CI already builds for
  `requel-app` + `requel-cli`, and runs no new test suites; only the aggregate report moves.

## AC mapping

| Issue AC | Covered by |
|---|---|
| Every code module is a group in the uploaded aggregate | steps 1–2; check script |
| CI runs the `requel-cli` / `gateway-rest-client` suites | already done (#376); kept by step 4 |
| assistant-core's Codecov figure is the aggregate's | step 4 upload; checked on the PR |
| Exactly one backend JaCoCo report, `disable_search: true` | step 4 |
| CI fails when a module is missing | step 4 check + script |
| Excludes stated in one place with the reason | decision 2 |
| Coverage setup documented | step 6 |
