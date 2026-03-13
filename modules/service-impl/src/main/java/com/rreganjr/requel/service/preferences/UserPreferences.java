package com.rreganjr.requel.service.preferences;

import jakarta.persistence.*;

/**
 * UI preferences for a user. Separate aggregate from User — User handles
 * identity/auth/contact, this handles UI configuration.
 * See doc/UI_DESIGN_GUIDE.md §3.2.3.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "sidebar_project_limit", nullable = false)
    private int sidebarProjectLimit = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "sidebar_project_staleness", nullable = false)
    private SidebarProjectStaleness sidebarProjectStaleness = SidebarProjectStaleness.THREE_MONTHS;

    protected UserPreferences() {}

    public UserPreferences(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public int getSidebarProjectLimit() {
        return sidebarProjectLimit;
    }

    public void setSidebarProjectLimit(int sidebarProjectLimit) {
        this.sidebarProjectLimit = sidebarProjectLimit;
    }

    public SidebarProjectStaleness getSidebarProjectStaleness() {
        return sidebarProjectStaleness;
    }

    public void setSidebarProjectStaleness(SidebarProjectStaleness sidebarProjectStaleness) {
        this.sidebarProjectStaleness = sidebarProjectStaleness;
    }
}
