package com.rreganjr.requel.imports.project;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Format-agnostic representation of a use case import payload.
 */
public class UseCaseImportDraft {
    private final String externalId;
    private final String createdByExternalId;
    private final String name;
    private final String description;
    private final String primaryActorExternalId;
    private final String scenarioExternalId;
    private final Set<String> storyExternalIds;
    private final Set<String> goalExternalIds;
    private final Set<String> actorExternalIds;
    private final Set<String> annotationExternalIds;

    private UseCaseImportDraft(Builder builder) {
        this.externalId = builder.externalId;
        this.createdByExternalId = builder.createdByExternalId;
        this.name = builder.name;
        this.description = builder.description;
        this.primaryActorExternalId = builder.primaryActorExternalId;
        this.scenarioExternalId = builder.scenarioExternalId;
        this.storyExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.storyExternalIds));
        this.goalExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.goalExternalIds));
        this.actorExternalIds = Collections.unmodifiableSet(new HashSet<>(builder.actorExternalIds));
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

    public String getPrimaryActorExternalId() {
        return primaryActorExternalId;
    }

    public String getScenarioExternalId() {
        return scenarioExternalId;
    }

    public Set<String> getStoryExternalIds() {
        return storyExternalIds;
    }

    public Set<String> getGoalExternalIds() {
        return goalExternalIds;
    }

    public Set<String> getActorExternalIds() {
        return actorExternalIds;
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
        private String primaryActorExternalId;
        private String scenarioExternalId;
        private Set<String> storyExternalIds = new HashSet<>();
        private Set<String> goalExternalIds = new HashSet<>();
        private Set<String> actorExternalIds = new HashSet<>();
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

        public Builder primaryActorExternalId(String primaryActorExternalId) {
            this.primaryActorExternalId = primaryActorExternalId;
            return this;
        }

        public Builder scenarioExternalId(String scenarioExternalId) {
            this.scenarioExternalId = scenarioExternalId;
            return this;
        }

        public Builder storyExternalIds(Set<String> storyExternalIds) {
            if (storyExternalIds != null) {
                this.storyExternalIds.addAll(storyExternalIds);
            }
            return this;
        }

        public Builder goalExternalIds(Set<String> goalExternalIds) {
            if (goalExternalIds != null) {
                this.goalExternalIds.addAll(goalExternalIds);
            }
            return this;
        }

        public Builder actorExternalIds(Set<String> actorExternalIds) {
            if (actorExternalIds != null) {
                this.actorExternalIds.addAll(actorExternalIds);
            }
            return this;
        }

        public Builder annotationExternalIds(Set<String> annotationExternalIds) {
            if (annotationExternalIds != null) {
                this.annotationExternalIds.addAll(annotationExternalIds);
            }
            return this;
        }

        public UseCaseImportDraft build() {
            Objects.requireNonNull(name, "use case name is required");
            return new UseCaseImportDraft(this);
        }
    }
}
