/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.imports.project;

/**
 * Minimal stakeholder draft linking a user into a project.
 */
public class StakeholderImportDraft {
    private final String externalId;
    private final String createdByExternalId;
    private final String userExternalId;
    private final java.util.Set<String> annotationExternalIds;
    private final String type;
    private final String name;
    private final String text;

    public StakeholderImportDraft(String externalId, String createdByExternalId, String userExternalId,
                                  java.util.Set<String> annotationExternalIds) {
        this.externalId = externalId;
        this.createdByExternalId = createdByExternalId;
        this.userExternalId = userExternalId;
        this.annotationExternalIds = annotationExternalIds != null
                ? java.util.Collections.unmodifiableSet(new java.util.HashSet<>(annotationExternalIds))
                : java.util.Collections.emptySet();
        this.type = "USER";
        this.name = null;
        this.text = null;
    }

    public StakeholderImportDraft(String externalId, String createdByExternalId,
                                  java.util.Set<String> annotationExternalIds) {
        this.externalId = externalId;
        this.createdByExternalId = createdByExternalId;
        this.userExternalId = null;
        this.annotationExternalIds = annotationExternalIds != null
                ? java.util.Collections.unmodifiableSet(new java.util.HashSet<>(annotationExternalIds))
                : java.util.Collections.emptySet();
        this.type = "NON_USER";
        this.name = null;
        this.text = null;
    }

    public StakeholderImportDraft(String externalId, String createdByExternalId,
                                  String name, String text, java.util.Set<String> annotationExternalIds) {
        this.externalId = externalId;
        this.createdByExternalId = createdByExternalId;
        this.userExternalId = null;
        this.annotationExternalIds = annotationExternalIds != null
                ? java.util.Collections.unmodifiableSet(new java.util.HashSet<>(annotationExternalIds))
                : java.util.Collections.emptySet();
        this.type = "NON_USER";
        this.name = name;
        this.text = text;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getCreatedByExternalId() {
        return createdByExternalId;
    }

    public String getUserExternalId() {
        return userExternalId;
    }

    public java.util.Set<String> getAnnotationExternalIds() {
        return annotationExternalIds;
    }

    public boolean isUserStakeholder() {
        return "USER".equals(type);
    }

    public boolean isNonUserStakeholder() {
        return "NON_USER".equals(type);
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }
}
