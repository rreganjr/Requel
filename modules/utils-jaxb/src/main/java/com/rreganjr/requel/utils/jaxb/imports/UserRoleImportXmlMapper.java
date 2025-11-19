package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.identity.UserRoleImportDraft;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserRoleImportXmlMapper {

    public List<UserRoleImportDraft> toDrafts(UserRoleImportXml xml) {
        if (xml == null) {
            return List.of();
        }
        List<UserRoleImportDraft> drafts = new ArrayList<>();
        for (Object roleObj : xml.getRoles()) {
            if (roleObj instanceof UserRoleImportXml.ProjectUserRoleXml projectRole) {
                drafts.add(new UserRoleImportDraft(
                        "com.rreganjr.requel.project.ProjectUserRole",
                        permissionNames(projectRole.getUserPermissions())));
            } else if (roleObj instanceof UserRoleImportXml.SystemAdminUserRoleXml) {
                drafts.add(new UserRoleImportDraft(
                        "com.rreganjr.requel.user.impl.SystemAdminUserRole",
                        Set.of()));
            } else {
                // unknown role type; skip
            }
        }
        return drafts;
    }

    private Set<String> permissionNames(UserRoleImportXml.UserPermissionContainer container) {
        Set<String> names = new HashSet<>();
        if (container != null) {
            container.getPermissions().forEach(p -> {
                if (p.getName() != null) {
                    names.add(p.getName());
                }
            });
        }
        return names;
    }
}
