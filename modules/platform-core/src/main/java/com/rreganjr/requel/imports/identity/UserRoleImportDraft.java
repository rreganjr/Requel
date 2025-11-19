package com.rreganjr.requel.imports.identity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserRoleImportDraft {
    private final String roleType;
    private final Set<String> permissionNames;

    public UserRoleImportDraft(String roleType, Set<String> permissionNames) {
        this.roleType = roleType;
        this.permissionNames = permissionNames == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(permissionNames));
    }

    public String getRoleType() {
        return roleType;
    }

    public Set<String> getPermissionNames() {
        return permissionNames;
    }
}
