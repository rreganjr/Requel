# Platform Identity Notes

## User ↔ Domain Adapter

`com.rreganjr.requel.user.impl.User2UserImplAdapter` is the single bridge between the
platform identity API (`com.rreganjr.platform.identity.User`) and the richer Requel
domain user (`com.rreganjr.requel.user.User`/`UserImpl`). JAXB uses this adapter for
all `@XmlJavaTypeAdapter` bindings on project/annotation entities, and several runtime
patchers rely on its helper methods.

### Key methods

| Method | Use it when… | Returns |
|--------|--------------|---------|
| `registerReplacement(identityUser, managedUser)` | an import/export flow has looked up the managed `UserImpl` for a JAXB instance and wants future lookups to reuse it | nothing (stores the mapping in a synchronized identity map) |
| `resolveIdentity(user)` | you only need the platform identity projection (e.g., CreatedEntity getters, Spring Security) | the registered managed `UserImpl` cast to the identity interface, or the original argument if no mapping exists |
| `resolveDomain(identityUser)` | domain code needs the richer `com.rreganjr.requel.user.User` view | the mapped domain instance or `null` if none |
| `clearReplacements()` | starting a new import/export run | empties the map to avoid cross-run leakage |

### Usage guidelines

* JAXB-bound getters/setters should declare the platform identity interface when possible
  and call `resolveIdentity()` before persisting or returning values.
* Domain logic that compares users, inspects roles, or touches stakeholder associations
  must immediately call `resolveDomain()` before operating on the instance.
* Avoid calling `persist(...)` on JAXB-created users; always resolve the managed instance
  and `merge(...)` if a new row must be inserted. This prevents the
  “detached entity passed to persist” loop that occurs when the same XML object is reused.
* When adding new import/export patchers, register replacements as soon as you locate the
  managed user so downstream patchers see the attached entity.

Keeping these rules in mind allows us to move more classes to the lightweight
identity interface without breaking Hibernate’s session management.
