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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NonUserStakeholderImportDraft {
    private final String externalId;
    private final String createdByExternalId;
    private final String name;
    private final String text;
    private final Set<String> annotationExternalIds;

    public NonUserStakeholderImportDraft(String externalId, String createdByExternalId,
                                         String name, String text, Set<String> annotationExternalIds) {
        this.externalId = externalId;
        this.createdByExternalId = createdByExternalId;
        this.name = name;
        this.text = text;
        this.annotationExternalIds = annotationExternalIds != null
                ? Collections.unmodifiableSet(new HashSet<>(annotationExternalIds))
                : Collections.emptySet();
    }

    public String getExternalId() { return externalId; }
    public String getCreatedByExternalId() { return createdByExternalId; }
    public String getName() { return name; }
    public String getText() { return text; }
    public Set<String> getAnnotationExternalIds() { return annotationExternalIds; }
}
