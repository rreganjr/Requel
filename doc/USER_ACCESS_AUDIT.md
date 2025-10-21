# User Access Audit

This checklist captures every remaining call site where project-layer code reaches past a stakeholder into `com.rreganjr.requel.user.User`. The goal is to decide which usages are legitimate identity bridges and which can be refactored to stakeholder-centric helpers.

## Commands & Services

| Location | Purpose | Recommendation |
| -------- | ------- | -------------- |
| `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/DeleteStakeholderCommandImpl.java:77` | Delegates to `Stakeholder.removeFromProject(...)`, which handles user-role cleanup internally. | **Keep** – identity work is now encapsulated in the stakeholder implementation. |
| `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/ImportProjectCommandImpl.java` | Import flow reconciles stakeholders and now relies on `matchesUser`/`ensureProjectMembership` helpers rather than touching `User` directly. | **Keep** – identity lookups remain necessary, but encapsulated through stakeholder APIs. |
| `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/UserStakeholderImpl.java:281-293` | JAXB patcher persists or reuses existing `UserImpl` instances during import. | **Keep** – persistence-level identity mapping. |
| `modules/requel-app/src/main/java/com/rreganjr/requel/project/ProjectUserRole.java` (constructors, equality, logging) | Associates a system role with the owning user and enforces uniqueness. | **Keep** – core identity concept. |

## Project/Domain Utilities

| Location | Purpose | Recommendation |
| -------- | ------- | -------------- |
| `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/ProjectImpl.java:185` | Looks up the stakeholder for a given system user (`Project.getUserStakeholder(User)`). | **Keep** – essential bridge from authenticated user to project context. |

## UI (post-refactor verification)

| Location | Purpose | Recommendation |
| -------- | ------- | -------------- |
| `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/UserStakeholderEditorPanel.java` | Reads the current session user to determine grantable permissions and audit edits. | **Keep** – still needs the identity object. |

## Test Coverage References

| Location | Notes |
| -------- | ----- |
| `modules/requel-app/src/test/java/com/rreganjr/requel/ProjectXmlRoundTripIT.java` | Still asserts against `stakeholder.getUser().getUsername()` to ensure import/export consistency. |

## Follow-up Opportunities

- Where backend code only needs display information (e.g., logging), route through the new stakeholder `getDisplay*` helpers to avoid identity coupling.
- Consider introducing a small adapter around `ProjectUserRole` to encapsulate the `getUser()` manipulation once the identity module is extracted.
