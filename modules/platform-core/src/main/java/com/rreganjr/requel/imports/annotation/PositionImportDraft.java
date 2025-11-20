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
    private final java.util.List<ArgumentDraft> arguments;

    private PositionImportDraft(Builder builder) {
        this.externalId = builder.externalId;
        this.createdByExternalId = builder.createdByExternalId;
        this.text = builder.text;
        this.argumentExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.argumentExternalIds));
        this.arguments = java.util.List.copyOf(builder.arguments);
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

    public java.util.List<ArgumentDraft> getArguments() {
        return arguments;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String externalId;
        private String createdByExternalId;
        private String text;
        private Set<String> argumentExternalIds = new HashSet<>();
        private java.util.List<ArgumentDraft> arguments = new java.util.ArrayList<>();

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

        public Builder arguments(java.util.List<ArgumentDraft> arguments) {
            if (arguments != null) {
                this.arguments.addAll(arguments);
            }
            return this;
        }

        public Builder addArgument(ArgumentDraft argument) {
            if (argument != null) {
                this.arguments.add(argument);
            }
            return this;
        }
        public PositionImportDraft build() {
            Objects.requireNonNull(text, "position text is required");
            return new PositionImportDraft(this);
        }
    }

    public static class ArgumentDraft {
        private final String externalId;
        private final String createdByExternalId;
        private final String text;
        private final String supportLevel;

        public ArgumentDraft(String externalId, String createdByExternalId, String text, String supportLevel) {
            this.externalId = externalId;
            this.createdByExternalId = createdByExternalId;
            this.text = text;
            this.supportLevel = supportLevel;
        }

        public String getExternalId() { return externalId; }
        public String getCreatedByExternalId() { return createdByExternalId; }
        public String getText() { return text; }
        public String getSupportLevel() { return supportLevel; }
    }
}
