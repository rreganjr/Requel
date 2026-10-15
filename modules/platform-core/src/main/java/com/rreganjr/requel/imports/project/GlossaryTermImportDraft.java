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
import java.util.Objects;
import java.util.Set;

public class GlossaryTermImportDraft {
    private final String externalId;
    private final String createdByExternalId;
    private final String name;
    private final String text;
    private final String canonicalTermExternalId;
    private final Set<String> annotationExternalIds;

    private GlossaryTermImportDraft(Builder builder) {
        this.externalId = builder.externalId;
        this.createdByExternalId = builder.createdByExternalId;
        this.name = builder.name;
        this.text = builder.text;
        this.canonicalTermExternalId = builder.canonicalTermExternalId;
        this.annotationExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.annotationExternalIds));
    }

    public String getExternalId() { return externalId; }
    public String getCreatedByExternalId() { return createdByExternalId; }
    public String getName() { return name; }
    public String getText() { return text; }
    public String getCanonicalTermExternalId() { return canonicalTermExternalId; }
    public Set<String> getAnnotationExternalIds() { return annotationExternalIds; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String externalId;
        private String createdByExternalId;
        private String name;
        private String text;
        private String canonicalTermExternalId;
        private Set<String> annotationExternalIds = new HashSet<>();

        public Builder externalId(String externalId) {
            this.externalId = externalId; return this;
        }
        public Builder createdByExternalId(String createdByExternalId) {
            this.createdByExternalId = createdByExternalId; return this;
        }
        public Builder name(String name) {
            this.name = name; return this;
        }
        public Builder text(String text) {
            this.text = text; return this;
        }
        public Builder canonicalTermExternalId(String canonicalTermExternalId) {
            this.canonicalTermExternalId = canonicalTermExternalId; return this;
        }
        public Builder annotationExternalIds(Set<String> ids) {
            if (ids != null) {
                this.annotationExternalIds.addAll(ids);
            }
            return this;
        }
        public GlossaryTermImportDraft build() {
            Objects.requireNonNull(name, "glossary term name is required");
            return new GlossaryTermImportDraft(this);
        }
    }
}
