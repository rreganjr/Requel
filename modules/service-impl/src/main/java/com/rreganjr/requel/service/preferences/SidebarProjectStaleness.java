package com.rreganjr.requel.service.preferences;

/**
 * How old a project's last activity can be before it's hidden from the sidebar tree.
 */
public enum SidebarProjectStaleness {
    ONE_MONTH,
    THREE_MONTHS,
    SIX_MONTHS,
    NINE_MONTHS,
    TWELVE_MONTHS,
    ALWAYS
}
