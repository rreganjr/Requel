https://github.com/rreganjr/Requel/issues/105

# OAuth signing key: keystore & credential handling

## Summary

The embedded OAuth 2.1 authorization server signs issued JWTs with an RSA key. By default that key is
generated fresh at every startup (ephemeral), so it changes on each restart and previously-issued
tokens stop validating — fine for dev/test/CI, but a blocker for the remote MCP connector (#100),
which needs tokens that survive a restart.

When a keystore is configured, `AuthorizationServerConfig.jwkSource()` loads a **persistent** RSA key
from it and uses the key alias as a stable JWK `kid`, so tokens issued before a restart keep
validating afterwards and JWKS consumers can cache the key. When no keystore is configured, the
ephemeral behavior (with its startup warning) is unchanged.

The signing key lives in a permission-locked **PKCS12 (or JKS) keystore file on disk**. The keystore
and key passwords are **never committed or hardcoded**; they are supplied at runtime from environment
variables that developers and operators populate from the environment's native secret store.

## Configuration

Read via `Environment`, following the existing `requel.oauth.*` convention (e.g.
`requel.oauth.dcr.registrar-client-secret`):

| Property | Env var | Meaning |
| --- | --- | --- |
| `requel.oauth.jwk.keystore.location` | `REQUEL_OAUTH_JWK_KEYSTORE_LOCATION` | Spring resource: `file:` (real keys) or `classpath:` (throwaway dev keys). Blank = ephemeral key. |
| `requel.oauth.jwk.keystore.type` | `REQUEL_OAUTH_JWK_KEYSTORE_TYPE` | `PKCS12` (default) or `JKS`. |
| `requel.oauth.jwk.keystore.password` | `REQUEL_OAUTH_JWK_KEYSTORE_PASSWORD` | Keystore password. Never commit. |
| `requel.oauth.jwk.key-alias` | `REQUEL_OAUTH_JWK_KEY_ALIAS` | Alias of the signing entry; also the stable JWK `kid`. Required when a location is set. |
| `requel.oauth.jwk.key-password` | `REQUEL_OAUTH_JWK_KEY_PASSWORD` | Key password; defaults to the keystore password when unset (typical for PKCS12). |

Spring relaxed binding maps the env vars above onto the properties automatically, so the same launch
configuration works across macOS, Linux, Windows, and Docker — only the source of the password value
differs.

## Generating the keystore (keytool)

A single self-signed RSA key in a PKCS12 keystore is all the AS needs — the key is used only to sign
and verify Requel's own JWTs, so the certificate is never presented to a third party:

```bash
keytool -genkeypair \
  -alias requel-oauth-signing \
  -keyalg RSA -keysize 2048 \
  -dname "CN=Requel OAuth Signing" \
  -validity 3650 \
  -keystore requel-oauth-signing.p12 \
  -storetype PKCS12 \
  -storepass "$REQUEL_OAUTH_JWK_KEYSTORE_PASSWORD"
```

Keep `requel-oauth-signing.p12` out of source control. Mount it into the deployed instance / Docker
image at a permission-locked path (e.g. `chmod 600`, owned by the app's service user) and point
`requel.oauth.jwk.keystore.location` at it with a `file:` URL.

## Why not put the private key in the OS keychain directly?

Java can address the macOS Keychain (`KeyStore.getInstance("KeychainStore")`) and the Windows
Certificate Store (`Windows-MY`), but their support for *app-managed private keys* is limited and
historically buggy, and neither exists on Linux/Docker. To keep dev/prod parity, the **private key
lives in a portable PKCS12 file** and only the **password** is sourced from the OS secret store.

## Sourcing the password per OS

### macOS — Keychain

```bash
# store once
security add-generic-password -a requel -s requel-oauth-jwk -w 'PASSWORD'
# fetch at launch
export REQUEL_OAUTH_JWK_KEYSTORE_PASSWORD="$(security find-generic-password -s requel-oauth-jwk -w)"
```

### Linux — Secret Service (libsecret)

```bash
# store once (prompts for the value)
secret-tool store --label='Requel OAuth JWK keystore' service requel-oauth-jwk
# fetch at launch
export REQUEL_OAUTH_JWK_KEYSTORE_PASSWORD="$(secret-tool lookup service requel-oauth-jwk)"
```

Requires a running Secret Service (GNOME Keyring / KWallet). On headless servers and containers there
usually isn't one — inject `REQUEL_OAUTH_JWK_KEYSTORE_PASSWORD` via the orchestrator, Vault, a Docker
secret, or a `systemd` credential instead.

### Windows — Credential Manager (PowerShell SecretManagement)

```powershell
# one-time setup
Install-Module Microsoft.PowerShell.SecretManagement, Microsoft.PowerShell.SecretStore -Scope CurrentUser
Register-SecretVault -Name Requel -ModuleName Microsoft.PowerShell.SecretStore -DefaultVault
Set-Secret -Name requel-oauth-jwk -Secret 'PASSWORD'
# at launch
$env:REQUEL_OAUTH_JWK_KEYSTORE_PASSWORD = Get-Secret -Name requel-oauth-jwk -AsPlainText
```

## Launch example

Once the password is in the environment (from any of the sources above):

```bash
export REQUEL_OAUTH_JWK_KEYSTORE_LOCATION=file:/etc/requel/requel-oauth-signing.p12
export REQUEL_OAUTH_JWK_KEY_ALIAS=requel-oauth-signing
# REQUEL_OAUTH_JWK_KEYSTORE_PASSWORD already exported by the OS-specific step above

java -jar modules/requel-app/target/requel-app-1.2.0.jar
```

With no `REQUEL_OAUTH_JWK_KEYSTORE_LOCATION` set, the AS falls back to an ephemeral key and logs the
existing "tokens will not survive a restart" warning — so dev, tests, and CI need no keystore.
