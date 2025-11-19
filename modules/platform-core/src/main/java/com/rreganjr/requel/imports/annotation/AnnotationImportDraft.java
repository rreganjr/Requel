package com.rreganjr.requel.imports.annotation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AnnotationImportDraft {
    public enum Type { NOTE, ISSUE }

    private final String externalId;
    private final String createdByExternalId;
    private final String text;
    private final Type type;
    private final boolean mustBeResolved;
    private final Set<String> positionExternalIds;
    private final Set<String> annotatableExternalIds;
    private final String annotatableDiscriminator;

    private AnnotationImportDraft(Builder builder) {
        this.externalId = builder.externalId;
        this.createdByExternalId = builder.createdByExternalId;
        this.text = builder.text;
        this.type = builder.type;
        this.mustBeResolved = builder.mustBeResolved;
        this.positionExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.positionExternalIds));
        this.annotatableExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.annotatableExternalIds));
        this.annotatableDiscriminator = builder.annotatableDiscriminator;
    }

    public String getExternalId() { return externalId; }
    public String getCreatedByExternalId() { return createdByExternalId; }
    public String getText() { return text; }
    public Type getType() { return type; }
    public boolean isMustBeResolved() { return mustBeResolved; }
    public Set<String> getPositionExternalIds() { return positionExternalIds; }
    public Set<String> getAnnotatableExternalIds() { return annotatableExternalIds; }
    public String getAnnotatableDiscriminator() { return annotatableDiscriminator; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String externalId;
        private String createdByExternalId;
        private String text;
        private Type type = Type.NOTE;
        private boolean mustBeResolved = false;
        private Set<String> positionExternalIds = new HashSet<>();
        private Set<String> annotatableExternalIds = new HashSet<>();
        private String annotatableDiscriminator;

        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder createdByExternalId(String createdByExternalId) { this.createdByExternalId = createdByExternalId; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder type(Type type) { this.type = type; return this; }
        public Builder mustBeResolved(boolean mustBeResolved) { this.mustBeResolved = mustBeResolved; return this; }
        public Builder positionExternalIds(Set<String> ids) { if (ids != null) this.positionExternalIds.addAll(ids); return this; }
        public Builder annotatableExternalIds(Set<String> ids) { if (ids != null) this.annotatableExternalIds.addAll(ids); return this; }
        public Builder annotatableDiscriminator(String discriminator) { this.annotatableDiscriminator = discriminator; return this; }
        public AnnotationImportDraft build() {
            Objects.requireNonNull(text, "annotation text is required");
            Objects.requireNonNull(type, "annotation type is required");
            return new AnnotationImportDraft(this);
        }
    }
}
