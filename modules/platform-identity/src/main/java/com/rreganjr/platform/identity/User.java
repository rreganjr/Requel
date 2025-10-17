package com.rreganjr.platform.identity;

/**
 * Minimal identity projection exposed to downstream modules.
 */
public interface User {

    /**
     * @return stable database identifier, or {@code null} for transient records.
     */
    Long getId();

    /**
     * @return unique login name.
     */
    String getUsername();

    /**
     * Optional display helper; defaults to username when not overridden.
     */
    default String getDisplayName() {
        return getUsername();
    }
}
