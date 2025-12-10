/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
import java.util.Objects;
import java.util.Set;

/**
 * Format-agnostic representation of an actor import payload.
 */
public class ActorImportDraft {
    private final String externalId;
    private final String createdByExternalId;
    private final String name;
    private final String description;
    private final Set<String> goalExternalIds;
    private final Set<String> annotationExternalIds;
    private final Set<String> glossaryTermExternalIds;

    private ActorImportDraft(Builder builder) {
        this.externalId = builder.externalId;
        this.createdByExternalId = builder.createdByExternalId;
        this.name = builder.name;
        this.description = builder.description;
        this.goalExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.goalExternalIds));
        this.annotationExternalIds =
                Collections.unmodifiableSet(new HashSet<>(builder.annotationExternalIds));
        this.glossaryTermExternalIds =
                Collections.unmodifiableSet(new HashSet<>(builder.glossaryTermExternalIds));
    }

    public String getExternalId() {
        return externalId;
    }

    public String getCreatedByExternalId() {
        return createdByExternalId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getGoalExternalIds() {
        return goalExternalIds;
    }

    public Set<String> getAnnotationExternalIds() {
        return annotationExternalIds;
    }

    public Set<String> getGlossaryTermExternalIds() {
        return glossaryTermExternalIds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String externalId;
        private String createdByExternalId;
        private String name;
        private String description;
        private Set<String> goalExternalIds = new HashSet<>();
        private Set<String> annotationExternalIds = new HashSet<>();
        private Set<String> glossaryTermExternalIds = new HashSet<>();

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder createdByExternalId(String createdByExternalId) {
            this.createdByExternalId = createdByExternalId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder goalExternalIds(Set<String> goalExternalIds) {
            if (goalExternalIds != null) {
                this.goalExternalIds.addAll(goalExternalIds);
            }
            return this;
        }

        public Builder annotationExternalIds(Set<String> annotationExternalIds) {
            if (annotationExternalIds != null) {
                this.annotationExternalIds.addAll(annotationExternalIds);
            }
            return this;
        }

        public Builder glossaryTermExternalIds(Set<String> glossaryTermExternalIds) {
            if (glossaryTermExternalIds != null) {
                this.glossaryTermExternalIds.addAll(glossaryTermExternalIds);
            }
            return this;
        }

        public ActorImportDraft build() {
            Objects.requireNonNull(name, "actor name is required");
            return new ActorImportDraft(this);
        }
    }
}
