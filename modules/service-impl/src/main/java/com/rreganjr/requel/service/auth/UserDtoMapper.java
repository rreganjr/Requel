package com.rreganjr.requel.service.auth;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.service.api.dto.UserDto;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import org.hibernate.Hibernate;
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
                user instanceof com.rreganjr.requel.user.User u ? u.getName() : user.getDisplayName(),
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

    /**
     * Resolve the real (non-proxied) class of a role. Handles both Hibernate proxies
     * and Spring AOP CGLIB proxies (DomainObjectWrappingAdvice).
     */
    private static Class<?> unproxyRoleClass(UserRole role) {
        Class<?> clazz = Hibernate.getClass(role);
        // If still a CGLIB proxy, walk up to the first non-proxy superclass
        while (clazz != null && clazz.getSimpleName().contains("$")) {
            clazz = clazz.getSuperclass();
        }
        return clazz != null ? clazz : role.getClass();
    }

    private String toRoleString(UserRole role) {
        // Return class simple name — matches what /api/users/roles returns as roleName
        // and what EditUserCommandImpl.updateRoles() expects in userRoleNames
        return unproxyRoleClass(role).getSimpleName();
    }
}
