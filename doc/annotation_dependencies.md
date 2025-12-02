# Annotation Package External Dependencies (as of 2025-12-02)

The annotation model now lives entirely in `modules/requel-app`. Legacy JAXB patchers and `afterUnmarshal` hooks were removed with the old importer. Remaining cross‑module links are limited to identity lookups and lightweight adapters.

## Current dependencies

- `impl/AbstractAnnotation.java`
  - `com.rreganjr.platform.identity.User` (platform-identity)
  - `com.rreganjr.requel.user.impl.UserImpl` (user-jpa target entity)
- `impl/ArgumentImpl.java`
  - `User`, `UserImpl`
- `impl/IssueImpl.java`
  - `User`, `UserImpl`
  - `com.rreganjr.requel.user.exception.NoSuchUserException` (user-domain)
- `impl/PositionImpl.java`
  - `User`, `UserImpl`
- `impl/package-info.java`
  - `com.rreganjr.requel.user.impl.User2UserImplAdapter` (user-jpa) JAXB adapter for XML mappings

### Import-side helpers (StAX pipeline)
- `imports/AnnotationAssembler.java`
  - `UserRepository`, `User` (resolve creators)
- `imports/PositionAssembler.java`
  - `UserRepository`, `User`

## Notes on migration status

- No annotation classes reference project/domain entities; annotatable linkage is handled via the streaming import’s `AnnotationLinkRegistry`.
- JAXB-era patchers are gone; only lightweight JAXB adapters remain for XML serialization compatibility.
- Remaining coupling is expected identity lookup plus import assemblers; no repository implementations are referenced directly.
