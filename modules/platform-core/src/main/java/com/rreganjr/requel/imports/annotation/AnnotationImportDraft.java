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
package com.rreganjr.requel.imports.annotation;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AnnotationImportDraft {
    public enum Type { NOTE, ISSUE, LEXICAL_ISSUE }

    private final String externalId;
    private final String createdByExternalId;
    private final String text;
    private final Type type;
    private final boolean mustBeResolved;
    private final String severity;
    private final Set<String> positionExternalIds;
    private final Set<String> annotatableExternalIds;
    private final String word;
    private final String annotatablePropertyName;
    private final String resolvedByPositionExternalId;
    private final String resolvedByUserExternalId;
    private final Date resolvedDate;

    private AnnotationImportDraft(Builder builder) {
        this.externalId = builder.externalId;
        this.createdByExternalId = builder.createdByExternalId;
        this.text = builder.text;
        this.type = builder.type;
        this.mustBeResolved = builder.mustBeResolved;
        this.severity = builder.severity;
        this.positionExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.positionExternalIds));
        this.annotatableExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.annotatableExternalIds));
        this.word = builder.word;
        this.annotatablePropertyName = builder.annotatablePropertyName;
        this.resolvedByPositionExternalId = builder.resolvedByPositionExternalId;
        this.resolvedByUserExternalId = builder.resolvedByUserExternalId;
        this.resolvedDate = builder.resolvedDate;
    }

    public String getExternalId() { return externalId; }
    public String getCreatedByExternalId() { return createdByExternalId; }
    public String getText() { return text; }
    public Type getType() { return type; }
    public boolean isMustBeResolved() { return mustBeResolved; }
    /**
     * The issue's severity name as written in the file, or null when the file has none (exports
     * before #271). Parsed by the assembler; a String because this module sits below
     * annotation-domain.
     */
    public String getSeverity() { return severity; }
    public Set<String> getPositionExternalIds() { return positionExternalIds; }
    public Set<String> getAnnotatableExternalIds() { return annotatableExternalIds; }
    public String getWord() { return word; }
    public String getAnnotatablePropertyName() { return annotatablePropertyName; }
    /** The XML id of the position that resolved this issue, or null when it is open (#320). */
    public String getResolvedByPositionExternalId() { return resolvedByPositionExternalId; }
    public String getResolvedByUserExternalId() { return resolvedByUserExternalId; }
    public Date getResolvedDate() { return resolvedDate; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String externalId;
        private String createdByExternalId;
        private String text;
        private Type type = Type.NOTE;
        private boolean mustBeResolved = false;
        private String severity;
        private Set<String> positionExternalIds = new HashSet<>();
        private Set<String> annotatableExternalIds = new HashSet<>();
        private String word;
        private String annotatablePropertyName;
        private String resolvedByPositionExternalId;
        private String resolvedByUserExternalId;
        private Date resolvedDate;

        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder createdByExternalId(String createdByExternalId) { this.createdByExternalId = createdByExternalId; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder type(Type type) { this.type = type; return this; }
        public Builder mustBeResolved(boolean mustBeResolved) { this.mustBeResolved = mustBeResolved; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder positionExternalIds(Set<String> ids) { if (ids != null) this.positionExternalIds.addAll(ids); return this; }
        public Builder annotatableExternalIds(Set<String> ids) { if (ids != null) this.annotatableExternalIds.addAll(ids); return this; }
        public Builder word(String word) { this.word = word; return this; }
        public Builder annotatablePropertyName(String name) { this.annotatablePropertyName = name; return this; }
        public Builder resolvedByPositionExternalId(String id) { this.resolvedByPositionExternalId = id; return this; }
        public Builder resolvedByUserExternalId(String id) { this.resolvedByUserExternalId = id; return this; }
        public Builder resolvedDate(Date date) { this.resolvedDate = date; return this; }
        public AnnotationImportDraft build() {
            Objects.requireNonNull(text, "annotation text is required");
            Objects.requireNonNull(type, "annotation type is required");
            return new AnnotationImportDraft(this);
        }
    }
}
