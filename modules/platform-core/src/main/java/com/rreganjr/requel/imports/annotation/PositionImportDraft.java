package com.rreganjr.requel.imports.annotation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PositionImportDraft {
    private final String externalId;
    private final String createdByExternalId;
    private final String text;
    private final Set<String> argumentExternalIds;

    private PositionImportDraft(Builder builder) {
        this.externalId = builder.externalId;
        this.createdByExternalId = builder.createdByExternalId;
        this.text = builder.text;
        this.argumentExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.argumentExternalIds));
    }

    public String getExternalId() {
        return externalId;
    }

    public String getCreatedByExternalId() {
        return createdByExternalId;
    }

    public String getText() {
        return text;
    }

    public Set<String> getArgumentExternalIds() {
        return argumentExternalIds;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String externalId;
        private String createdByExternalId;
        private String text;
        private Set<String> argumentExternalIds = new HashSet<>();

        public Builder externalId(String externalId) {
            this.externalId = externalId; return this;
        }
        public Builder createdByExternalId(String createdByExternalId) {
            this.createdByExternalId = createdByExternalId; return this;
        }
        public Builder text(String text) {
            this.text = text; return this;
        }
        public Builder argumentExternalIds(Set<String> ids) {
            if (ids != null) this.argumentExternalIds.addAll(ids);
            return this;
        }
        public PositionImportDraft build() {
            Objects.requireNonNull(text, "position text is required");
            return new PositionImportDraft(this);
        }
    }
}
