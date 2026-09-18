# USER_AUDIT

## Overview
- Total files importing `com.rreganjr.requel.user.User`: 13
- No property access (safe to swap types without code changes): 0
- Only identity-level methods (`getId`, `getUsername`, `getDisplayName`, Object methods): 0
- Requires broader `requel.user.User` contract: 13

## Non-Identity Methods In Use
| Method | Files |
| --- | --- |
| `getRoleForType` | 5 |
| `getName` | 3 |
| `hasRole` | 3 |
| `getEmailAddress` | 2 |
| `getPhoneNumber` | 2 |
| `getUser` | 2 |
| `UserComparator` | 2 |
| `getUserRoles` | 1 |
| `getOrganization` | 1 |
| `isPassword` | 1 |
| `grantRole` | 1 |

## Files Requiring `requel.user.User` Features
The entries below list the specific methods and line numbers that depend on data or behaviour absent from `com.rreganjr.platform.identity.User`.

- `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/AbstractStakeholder.java`
  - `getName` at `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/AbstractStakeholder.java:132`, `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/AbstractStakeholder.java:133`
  - `getEmailAddress` at `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/AbstractStakeholder.java:153`
  - `getPhoneNumber` at `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/AbstractStakeholder.java:160`
- `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/EditProjectCommandImpl.java`
  - `getRoleForType` at `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/EditProjectCommandImpl.java:187`
- `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/EditUserStakeholderCommandImpl.java`
  - `getRoleForType` at `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/EditUserStakeholderCommandImpl.java:185`
- `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/ImportProjectCommandImpl.java`
  - `hasRole` at `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/ImportProjectCommandImpl.java:175`
- `modules/requel-app/src/main/java/com/rreganjr/requel/ui/NavigationInitializationController.java`
  - `getUserRoles` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/NavigationInitializationController.java:66`
- `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/ProjectImportPanel.java`
  - `getUser` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/ProjectImportPanel.java:120`
- `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/ProjectOverviewPanel.java`
  - `getName` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/ProjectOverviewPanel.java:201`, `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/ProjectOverviewPanel.java:203`
  - `getUser` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/ProjectOverviewPanel.java:140`
- `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/ProjectUserNavigatorTreeNodeFactory.java`
  - `getRoleForType` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/ProjectUserNavigatorTreeNodeFactory.java:76`
- `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserCollectionNavigatorTreeNodeFactory.java`
  - `UserComparator` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserCollectionNavigatorTreeNodeFactory.java:136`, `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserCollectionNavigatorTreeNodeFactory.java:161`
- `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java`
  - `hasRole` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:205`, `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:227`, `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:272`, `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:373`, `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:380`
  - `getName` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:218`
  - `getOrganization` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:221`
  - `getEmailAddress` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:224`
  - `getPhoneNumber` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:226`
  - `getRoleForType` at `modules/requel-app/src/main/java/com/rreganjr/requel/ui/user/UserEditorPanel.java:381`
- `modules/requel-app/src/main/java/com/rreganjr/requel/user/impl/UserSetImpl.java`
  - `UserComparator` at `modules/requel-app/src/main/java/com/rreganjr/requel/user/impl/UserSetImpl.java:42`
- `modules/requel-app/src/main/java/com/rreganjr/requel/user/impl/command/LoginCommandImpl.java`
  - `isPassword` at `modules/requel-app/src/main/java/com/rreganjr/requel/user/impl/command/LoginCommandImpl.java:53`
- `modules/requel-app/src/test/java/com/rreganjr/requel/ProjectXmlRoundTripIT.java`
  - `hasRole` at `modules/requel-app/src/test/java/com/rreganjr/requel/ProjectXmlRoundTripIT.java:178`
  - `grantRole` at `modules/requel-app/src/test/java/com/rreganjr/requel/ProjectXmlRoundTripIT.java:179`
  - `getRoleForType` at `modules/requel-app/src/test/java/com/rreganjr/requel/ProjectXmlRoundTripIT.java:181`
