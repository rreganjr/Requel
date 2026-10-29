# Test coverage

How the backend coverage number on Codecov is produced, what it leaves out, and how to read it.
Set up in #321.

## Summary

- One JaCoCo report counts: the aggregate built by `modules/coverage-report`, at
  `modules/coverage-report/target/site/jacoco-aggregate/jacoco.xml`. CI uploads it, and only it, to
  Codecov under the `backend` flag.
- It merges every module's execution data (unit tests in each module, and the integration tests,
  most of them in `requel-app`) against every module's classes, so a class exercised only by a test
  in another module is counted where it lives.
- One package is excluded: `com/rreganjr/nlp/impl/**`. The reason is below.
- CI fails if a module with main code is missing from the aggregate.

## How it is built

Every module gets JaCoCo's agent from the root `pom.xml` (`prepare-agent`), so surefire and
failsafe write `target/jacoco.exec` in the module whose tests ran. A test in `requel-app` that
drives `assistant-core` records that coverage in `requel-app`'s exec file, not in
`assistant-core`'s.

`modules/coverage-report` is a `pom`-packaged module with no code. It declares every module that
has `src/main/java` as a direct dependency and runs `jacoco:report-aggregate` at `verify`. That goal
reads the exec files of all those modules and reports each module's classes against all of them, as
one `<group>` per module (named by artifactId).

CI and `release.yml` build it with:

```bash
mvn -pl modules/coverage-report -am verify
```

which builds and tests every module it depends on first. Locally, the same command leaves the report
in `modules/coverage-report/target/site/jacoco-aggregate/index.html`.

## What is excluded, and why

`com/rreganjr/nlp/impl/**`: the legacy OpenNLP pipeline in `nlp-jpa`. It is barely exercised and
may be removed, and counting it would pull every figure down. The exclusion is stated in one place,
`modules/coverage-report/pom.xml`. #313 narrowed it from `com/rreganjr/nlp/**`, which also hid
`com.rreganjr.nlp.dictionary` (the WordNet entities and the dictionary repository).

The frontend is reported separately (Vitest and Playwright results, the `frontend-*` flags) and is
not part of this number.

## Reading a module at 0%

- **In Codecov:** the backend figure is the aggregate's. A module at 0% there has no test, in any
  module, that runs its code.
- **In a module's own `target/site/jacoco/`:** each module also writes its own report from its own
  exec file only. Those are not uploaded and routinely read low or 0% for code covered by
  `requel-app`'s integration tests. Use the aggregate.
- **A module missing entirely:** the `Check every module is in the coverage aggregate` step in CI
  (`.github/scripts/check-coverage-modules.sh`) fails and names it.

## Adding a module

Add the new module as a dependency of `modules/coverage-report/pom.xml`. `report-aggregate` covers
only the modules that pom declares directly; one reached only transitively is left out, which is how
`assistant-api`, `assistant-core` and `gateway-api` fell out of the old aggregate in `requel-app`.
The CI check catches a module that is forgotten.
