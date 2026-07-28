https://github.com/rreganjr/Requel/issues/105

# Keystore & credential handling

## Summary

The signing/TLS keystore is a permission-locked **PKCS12 file on disk** (configurable via
`requel.keystore.location`, accepting both `file:` and `classpath:` resources — use `file:` for real
keys, `classpath:` only for throwaway dev keys). The keystore **password is never committed or
hardcoded**; it is supplied at runtime from an environment variable that developers and operators
populate from the environment's native secret store.

Spring relaxed binding maps the env var `REQUEL_KEYSTORE_PASSWORD` onto the config property
automatically, so the same launch configuration works unchanged across macOS, Linux, Windows, and
Docker — only the source of the password value differs.

## Why not put the private key in the OS keychain directly?

Java can address the macOS Keychain (`KeyStore.getInstance("KeychainStore")`) and the Windows
Certificate Store (`Windows-MY`), but their support for *app-managed private keys* is limited and
historically buggy, and neither exists on Linux/Docker. To keep dev/prod parity, the **private key
lives in a portable PKCS12 file** and only the **password** is sourced from the OS secret store.

## Acceptance criteria

- No keystore password in source, `application.properties`, or the built artifact.
- Password sourced from the `REQUEL_KEYSTORE_PASSWORD` env var at launch.
- Documentation covers macOS, Linux, and Windows.
- Headless/Docker fallback: env var injected by the orchestrator (Vault / Docker secret /
  `systemd` credential).

## Populating the password per OS

### macOS — Keychain

```bash
# store once
security add-generic-password -a requel -s requel-keystore -w 'PASSWORD'
# fetch at launch
export REQUEL_KEYSTORE_PASSWORD="$(security find-generic-password -s requel-keystore -w)"
```

### Linux — Secret Service (libsecret)

```bash
# store once (prompts for the value)
secret-tool store --label='Requel keystore' service requel-keystore
# fetch at launch
export REQUEL_KEYSTORE_PASSWORD="$(secret-tool lookup service requel-keystore)"
```

Requires a running Secret Service (GNOME Keyring / KWallet). On headless servers and containers there
usually isn't one — inject `REQUEL_KEYSTORE_PASSWORD` via the orchestrator, Vault, or a `systemd`
credential instead.

### Windows — Credential Manager (PowerShell SecretManagement)

```powershell
# one-time setup
Install-Module Microsoft.PowerShell.SecretManagement, Microsoft.PowerShell.SecretStore -Scope CurrentUser
Register-SecretVault -Name Requel -ModuleName Microsoft.PowerShell.SecretStore -DefaultVault
Set-Secret -Name requel-keystore -Secret 'PASSWORD'
# at launch
$env:REQUEL_KEYSTORE_PASSWORD = Get-Secret -Name requel-keystore -AsPlainText
```

## Launch example

Once `REQUEL_KEYSTORE_PASSWORD` is set in the environment:

```bash
java -jar modules/requel-app/target/requel-app-1.2.0.jar \
  --server.ssl.key-store=file:/path/to/keystore.p12 \
  --server.ssl.key-store-type=PKCS12 \
  --server.ssl.key-store-password=${REQUEL_KEYSTORE_PASSWORD}
```
