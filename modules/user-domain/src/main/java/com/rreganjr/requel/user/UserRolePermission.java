package com.rreganjr.requel.user;

import java.io.Serializable;

/**
 * Marker interface for user role permissions exposed to the domain layer.
 */
public interface UserRolePermission extends Comparable<UserRolePermission>, Serializable {

    /**
     * @return human-readable permission name.
     */
    String getName();

    /**
     * @return fully qualified role type that owns this permission.
     */
    String getUserRoleTypeName();
}
