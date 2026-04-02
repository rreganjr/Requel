package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDto(
        Long id,
        int version,
        String name,
        String description,
        String organizationName,
        String createdBy,
        String status,
        int stakeholderCount,
        int goalCount,
        int storyCount,
        int actorCount,
        int useCaseCount,
        int scenarioCount,
        int glossaryTermCount,
        int reportGeneratorCount
) {
}
