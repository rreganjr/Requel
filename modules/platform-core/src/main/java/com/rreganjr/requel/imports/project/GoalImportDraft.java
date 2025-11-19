package com.rreganjr.requel.imports.project;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Format-agnostic representation of a goal import payload.
 */
public class GoalImportDraft {
    private final String externalId;
    private final String createdByExternalId;
    private final String name;
    private final String description;
    private final Set<String> relationTargets; // IDs this goal supports
    private final Set<String> annotationExternalIds;
    private final Set<String> glossaryTermExternalIds;

    private GoalImportDraft(Builder builder) {
        this.externalId = builder.externalId;
        this.createdByExternalId = builder.createdByExternalId;
        this.name = builder.name;
        this.description = builder.description;
        this.relationTargets = Collections.unmodifiableSet(new HashSet<>(builder.relationTargets));
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

    public Set<String> getRelationTargets() {
        return relationTargets;
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
        private Set<String> relationTargets = new HashSet<>();
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

        public Builder relationTargets(Set<String> relationTargets) {
            if (relationTargets != null) {
                this.relationTargets.addAll(relationTargets);
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

        public GoalImportDraft build() {
            Objects.requireNonNull(name, "goal name is required");
            return new GoalImportDraft(this);
        }
    }
}
