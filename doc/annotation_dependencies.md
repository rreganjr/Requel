# Annotation Package External Dependencies (as of 2025-10-30)


## Depends on other modules

- `impl/AbstractAnnotation.java`
  - `com.rreganjr.requel.user.UserRepository` (user-domain)
  - `com.rreganjr.requel.user.impl.UserImpl` (user-jpa)
  - `com.rreganjr.requel.utils.jaxb.JAXBAnnotationGroupedByPatcher` (requel-app)
  - `com.rreganjr.requel.utils.jaxb.JAXBCreatedEntityPatcher` (user-jpa)
- `impl/ArgumentImpl.java`
  - `com.rreganjr.requel.user.UserRepository` (user-domain)
  - `com.rreganjr.requel.user.impl.UserImpl` (user-jpa)
  - `com.rreganjr.requel.utils.jaxb.JAXBCreatedEntityPatcher` (user-jpa)
- `impl/IssueImpl.java`
  - `com.rreganjr.requel.user.UserRepository` (user-domain)
  - `com.rreganjr.requel.user.exception.NoSuchUserException` (user-domain)
  - `com.rreganjr.requel.user.impl.UserImpl` (user-jpa)
- `impl/PositionImpl.java`
  - `com.rreganjr.requel.user.UserRepository` (user-domain)
  - `com.rreganjr.requel.user.impl.UserImpl` (user-jpa)
  - `com.rreganjr.requel.utils.jaxb.JAXBCreatedEntityPatcher` (user-jpa)
