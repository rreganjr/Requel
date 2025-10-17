package com.rreganjr.platform.identity;

/**
 * Represents a coarse-grained capability assigned to a {@link User}.
 */
public interface Role {

    /**
     * @return canonical role name (e.g. {@code ROLE_SYSTEM_ADMIN}).
     */
    String getName();
}
