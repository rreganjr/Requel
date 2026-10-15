# #392: rc2 acceptance fixes, plan

Issue: https://github.com/rreganjr/Requel/issues/392 · branch `392-rc2-fixes` · epic #375 · found by #378 on a default rc2 install.

Decided 2026-10-07 (his calls): seed the CLI client by default; document MCP client registration rather than turn it on; give the built-in admin `manageApiTokens`; one ticket, then rc3.

## 1. `requel-cli login --oauth` on a default install

- `application.properties`: `requel.oauth.seed-cli-client=true`. It is a public PKCE client with a loopback redirect, `mcp` scope and consent required.
- `application-test.properties` sets it back to `false`: test contexts run on H2 without Flyway, so the `oauth2_registered_client` table only exists in ITs that load `oauth2-schema-h2.sql`.
- Test: `RequelCliOAuthClientTest.isSeededByDefault` reads the shipped `application.properties`. By hand: a fresh install runs `requel-cli login --oauth`, then `projects`.

## 2. Connecting AI clients

- INSTALL.md: a new "Connect the CLI or an AI client" section covering a PAT (Settings → Personal Access Tokens), `requel-cli login --oauth`, and MCP over OAuth. For MCP it gives the loopback-registration switch for jar installs (and says it doesn't apply under Docker, since the filter checks the peer address) and the gated registrar setup, linking to `mcp_remote_connection.md` for the client recipes.
- Configuration reference: rows for `REQUEL_OAUTH_SEED_CLI_CLIENT`, `REQUEL_OAUTH_DCR_ALLOW_ANONYMOUS_LOOPBACK`, `REQUEL_OAUTH_DCR_ENABLED` / `_REGISTRAR_CLIENT_SECRET`.

## 3. The built-in admin and `manageApiTokens`

- `AdminProjectRoleInitializer` (fresh installs) grants `manageApiTokens` alongside `createProjects`.
- Flyway `V38__admin_manage_api_tokens.sql` (existing installs where admin already has `ProjectUserRole`): creates the permission row if startup hasn't yet (`INSERT IGNORE` on the `(name, role_type)` unique key), then links it to admin's role if missing. Only `admin`. It runs once, so an admin who later removes it from themselves keeps it removed.
- Tests: `AdminApiTokenPermissionIT` boots the app on a fresh H2 database and checks admin has both permissions (fails without the initializer change). `AdminApiTokensMigrationMySqlIT` migrates a V37 database holding admin and another user to the latest version, then checks only admin gained it and the row was created once.

## 4. Edit-mode user label

- `stakeholder-editor.ts`: `userLabel()` drops the stakeholder name's ` [username]` suffix, so edit mode shows the user's name like the create wizard does. This puts the committed `stakeholder-permissions.png` guide screenshot back as it was.
- Test: `userLabel` specs; the edit-mode spec's stakeholder name now carries the suffix.
