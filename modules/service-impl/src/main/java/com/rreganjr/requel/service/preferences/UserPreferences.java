/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
