package com.rreganjr.requel.imports.project;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Format-agnostic representation of a scenario or step import payload.
 */
public class ScenarioImportDraft {
    private final String externalId;
    private final String createdByExternalId;
    private final String name;
    private final String description;
    private final String scenarioType;
    private final Set<String> stepRefs;
    private final Set<String> annotationExternalIds;

    private ScenarioImportDraft(Builder builder) {
        this.externalId = builder.externalId;
        this.createdByExternalId = builder.createdByExternalId;
        this.name = builder.name;
        this.description = builder.description;
        this.scenarioType = builder.scenarioType;
        this.stepRefs = Collections.unmodifiableSet(new HashSet<>(builder.stepRefs));
        this.annotationExternalIds =
                Collections.unmodifiableSet(new HashSet<>(builder.annotationExternalIds));
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

    public String getScenarioType() { return scenarioType; }

    public Set<String> getStepRefs() {
        return stepRefs;
    }

    public Set<String> getAnnotationExternalIds() {
        return annotationExternalIds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String externalId;
        private String createdByExternalId;
        private String name;
        private String description;
        private String scenarioType;
        private Set<String> stepRefs = new HashSet<>();
        private Set<String> annotationExternalIds = new HashSet<>();

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

        public Builder scenarioType(String scenarioType) {
            this.scenarioType = scenarioType;
            return this;
        }

        public Builder stepRefs(Set<String> stepRefs) {
            if (stepRefs != null) {
                this.stepRefs.addAll(stepRefs);
            }
            return this;
        }

        public Builder annotationExternalIds(Set<String> annotationExternalIds) {
            if (annotationExternalIds != null) {
                this.annotationExternalIds.addAll(annotationExternalIds);
            }
            return this;
        }

        public ScenarioImportDraft build() {
            Objects.requireNonNull(name, "scenario name is required");
            Objects.requireNonNull(scenarioType, "scenario type is required");
            return new ScenarioImportDraft(this);
        }
    }
}
