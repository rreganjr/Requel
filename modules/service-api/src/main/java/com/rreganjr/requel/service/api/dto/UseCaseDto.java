package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UseCaseDto(
        Long id,
        int version,
        String name,
        String text,
        String primaryActorName,
        String createdBy,
        Long scenarioId,
        String scenarioName,
        Integer scenarioStepCount,
        List<GoalDto> goals,
        List<ActorDto> actors,
        List<StoryDto> stories,
        List<ScenarioDto> additionalScenarios
) {}
