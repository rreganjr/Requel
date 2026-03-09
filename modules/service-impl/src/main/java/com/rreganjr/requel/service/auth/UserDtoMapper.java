package com.rreganjr.requel.service.auth;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.service.api.dto.UserDto;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.requel.user.impl.UserImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps domain User entities to UserDto records.
 * Extracts role names and permission names from the domain role model.
 */
@Component
public class UserDtoMapper {

    /**
     * Map a domain User to a UserDto, including roles and permissions.
     */
    public UserDto toDto(User user) {
        List<String> roles = new ArrayList<>();
        List<String> permissions = new ArrayList<>();

        if (user instanceof com.rreganjr.requel.user.User requelUser) {
            for (UserRole role : requelUser.getUserRoles()) {
                roles.add(toRoleString(role));
                for (UserRolePermission perm : role.getAvailableUserRolePermissions()) {
                    if (role.hasUserRolePermission(perm)) {
                        permissions.add(perm.getName());
                    }
                }
            }
        }

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user instanceof com.rreganjr.requel.user.User u ? u.getEmailAddress() : null,
                user instanceof com.rreganjr.requel.user.User u ? u.getPhoneNumber() : null,
                user instanceof com.rreganjr.requel.user.User u && u.getOrganization() != null
                        ? u.getOrganization().getName() : null,
                roles,
                permissions,
                0 // version not exposed on public interface; will be available via entity metadata
        );
    }

    /**
     * Extract role and permission lists for JWT claims.
     */
    public List<String> getRoleStrings(User user) {
        List<String> roles = new ArrayList<>();
        if (user instanceof com.rreganjr.requel.user.User requelUser) {
            for (UserRole role : requelUser.getUserRoles()) {
                roles.add(toRoleString(role));
            }
        }
        return roles;
    }

    public List<String> getPermissionStrings(User user) {
        List<String> permissions = new ArrayList<>();
        if (user instanceof com.rreganjr.requel.user.User requelUser) {
            for (UserRole role : requelUser.getUserRoles()) {
                for (UserRolePermission perm : role.getAvailableUserRolePermissions()) {
                    if (role.hasUserRolePermission(perm)) {
                        permissions.add(perm.getName());
                    }
                }
            }
        }
        return permissions;
    }

    private String toRoleString(UserRole role) {
        if (role instanceof SystemAdminUserRole) {
            return "SYSTEM_ADMIN";
        }
        // Derive from class name: ProjectUserRole -> PROJECT_USER
        String name = role.getClass().getSimpleName();
        return name.replace("UserRole", "")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
    }
}
