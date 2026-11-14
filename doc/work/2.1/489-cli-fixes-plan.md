# #489 — rc6: requel-cli typed write commands crash; login --token keeps OAuth tokens

https://github.com/rreganjr/Requel/issues/489

Branch `489-cli-fixes` from `release/2.1` (at `2.1.0-rc6`), PR to `release/2.1`. Size 2. Found
in the rc6 acceptance test (#420), section 7; the fix ships in rc7. Two fixes, one ticket.

## Summary

1. Every runtime-discovered write subcommand (`resolve-issue`, `edit-scenario-step`, `edit-goal`,
   ...) throws `DuplicateOptionAnnotationsException` before it runs. `TypedCommands.buildSubcommand`
   turns on picocli's standard help options (`-h/--help`, `-V/--version`) and then adds one option
   per input field; most Edit and Delete inputs have a `version` field (the lock version, since
   2.0), so the first one built throws, and the CLI registers them all at startup.
   `TypedCommandsTest` uses stub descriptors without a `version` field. `requel run` is unaffected.
2. `requel login --token` saves the PAT next to OAuth tokens an earlier `login --oauth` stored for
   the same URL. `CliTokenSource` prefers OAuth, so the new PAT is ignored; after a password change
   ends the OAuth sign-in (#446), every call is 401 until `requel logout`.

## Locked decisions (2026-11-13)

1. **A typed subcommand has `-h/--help` only.** It gets an explicit usage-help option instead of
   `mixinStandardHelpOptions`, so an input's `version` field is `--version`, as the issue asks. The
   CLI's own version stays on the root: `requel --version`.
2. **A field that would still clash is left off, not fatal.** A field whose option name is taken by
   the subcommand's own options (today only `--help`/`-h`) is skipped, and the subcommand's help
   says that field can be sent with `requel run`. No current input has one.
3. **The last login wins.** `CredentialStore.save` (PAT) removes the URL's OAuth tokens;
   `saveOAuth` removes its PAT.
4. **A test against the real catalog.** `TypedCommandsCatalogIT` in requel-app reads
   `/api/gateway/commands/descriptors` the way the CLI does, maps it to `CommandInfo`, and
   registers the typed subcommands; it fails if any descriptor can't be built, so a new input field
   can't break the CLI silently. requel-app gets requel-cli as a test-scoped dependency for it (the
   test lives in the `com.rreganjr.requel.cli` package, since `TypedCommands` is package-private).

## Changes

requel-cli
- `TypedCommands.buildSubcommand`: an explicit `-h/--help` usage-help option instead of the
  standard mixin; skip a field whose option is already taken, with a help note.
- `CredentialStore.save` / `saveOAuth`: each clears the other kind for the URL.

requel-app (test)
- `pom.xml`: requel-cli, test scope.
- `com.rreganjr.requel.cli.TypedCommandsCatalogIT` (new).

## Test plan

`tmp/489-verify.sh` runs the backend verify and the coverage-modules check. No Angular change.

- `TypedCommandsTest`:
  - a descriptor with a `version` field builds, and `--version 3` reaches the input as 3;
  - `-h`/`--help` still prints the subcommand's usage;
  - a field named `help` is skipped and the subcommand still builds.
- `CredentialStoreTest`: `save` after `saveOAuth` leaves only the PAT; `saveOAuth` after `save`
  leaves only the OAuth tokens. `CliTokenSourceTest`: after `login --oauth` then `login --token`,
  the PAT is sent.
- `TypedCommandsCatalogIT`: every write descriptor builds; `resolve-issue`,
  `edit-scenario-step` and `edit-goal --version 1` parse.
- On the rc stack with a local build of the CLI: `resolve-issue` and `edit-scenario-step`
  against rc5-acceptance, and `login --token` after a stored OAuth login.

## Out of scope

- The CLI's other built-ins (unchanged; they never had the clash).
- Server-side changes: the catalog and inputs stay as they are.

## AC mapping

| AC | Covered by |
|---|---|
| `resolve-issue`, `edit-scenario-step`, `edit-goal --version N` run | TypedCommandsTest, TypedCommandsCatalogIT, rc-stack check |
| After `login --oauth` then `login --token`, calls use the token | CredentialStoreTest, CliTokenSourceTest, rc-stack check |
| Unit tests for both; a typed-command test from the real catalog | the above |
