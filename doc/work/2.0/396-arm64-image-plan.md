# #396: publish the Docker image for linux/arm64 too, plan

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
