# #396: rc5 acceptance fixes (arm64 image, stale UI after upgrade, lenient AI replies), plan

Issue: https://github.com/rreganjr/Requel/issues/396 · branch `396-arm64-image` · epic #375 · found by #378 on rc5.

## The problem

`container-publish.yml` built the image with the Maven `docker-image` profile, a plain `docker build` on an amd64 runner, and pushed that. So `rreganjr/requel` was `linux/amd64` only. On Apple Silicon, `docker compose up` warns that the platform doesn't match and runs Requel under emulation, which is slower to start and run.

## The fix

- `container-publish.yml`:
  - The Maven build, tests, re-tag and amd64 smoke test are unchanged.
  - After logging in, set up QEMU and Buildx.
  - One step replaces the two `docker push` steps. It runs `docker buildx build --platform linux/amd64,linux/arm64` from the jar the smoke test ran, pushes `:VERSION` (plus `:latest` for a final release, the same no-dash rule as before), then prints `docker buildx imagetools inspect` so the run log shows both platforms.
- The `Dockerfile` is unchanged. It copies the platform-neutral jar onto `eclipse-temurin:17-jre-jammy`, which is published for both platforms, and has no `RUN` steps, so the arm64 half needs no work under QEMU.
- `RELEASE_PROCESS.md`:
  - "Verifying a published image" checks for both platforms with `imagetools inspect`, and for no platform warning on Apple Silicon.
  - The manual fallback uses `buildx --push`.

## Tests

- `actionlint` passes on the workflow.
- The new step's build ran locally against the rc4 jar for both platforms, without `--push`.
- The real test is rc6:
  - `docker buildx imagetools inspect rreganjr/requel:2.0.0-rc6` lists both platforms;
  - `docker compose up` on an Apple Silicon Mac pulls the arm64 image with no warning, and Requel starts and logs in.

## Folded in from the rc5 Ollama pass

### Blank page after an upgrade

Spring Boot served the UI with no `Cache-Control` and the jar's 1980 entry time as `Last-Modified`. Chrome therefore kept `index.html` for years by heuristic. After moving from an older Requel to rc5 on the same port, it showed the old page, which asks for `main-<oldhash>.js`. The new jar 404s that file, so the page was blank until the cache was cleared. `/login?x` worked because that URL wasn't cached.

- `StaticCacheHeadersFilter` (service-impl, `config`), GET and HEAD only, skipping `/api/`, `/actuator/`, `/oauth2/`, `/connect/`, `/.well-known/`, `/userinfo` and `/login/`:
  - Angular content-hashed files (`-XXXXXXXX.ext`) get `public, max-age=31536000, immutable`.
  - Everything else the UI serves (`/`, `/index.html`, the routes `SpaController` forwards, `favicon.ico`) gets `no-cache`.
- `spring.web.resources.cache.use-last-modified=false`. With the 1980 date, a revalidation of a changed `index.html` got a 304.
- Test: `StaticCacheHeadersIT` covers the UI routes (no-cache), a hashed file (a year, no `Last-Modified`), an unhashed file (no-cache, no `Last-Modified`), and the API left alone.

### AI replies from small models

`qwen2.5:3b` on Ollama sometimes ignored the output schema:

- **No `summary`:** the whole run failed with "structured output missing summary", findings and all (6 runs).
- **Severities outside `LOW|MEDIUM|HIGH`:** `MODERATE` and `MAJOR`. These were kept, but at the kind's default severity.

The fixes:

- `ReviewResultMapper.validate` fails a missing summary only when there are no findings either. `toResponse` records `NO_SUMMARY` ("(The model returned findings without a summary.)") as the run summary. Test: `ReviewResultMapperTest.findingsWithoutASummaryAreKept`.
- `CommandBackedAssistantResultApplicator.issueSeverity` maps synonyms for assistant output only: `MINOR` and `TRIVIAL` to LOW; `MODERATE`, `MED` and `NORMAL` to MEDIUM; `MAJOR`, `SEVERE` and `CRITICAL` to HIGH. Anything else is still logged and left at the default. The API and import still accept only the three names. Test: `severitySynonymsFromAModelMapToTheThreeSeverities`.

