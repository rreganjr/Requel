package com.rreganjr.requel.service.api.dto;

public record RemoveScenarioFromUseCaseInput(
        String projectName,
        Long useCaseId,
        Long scenarioId
) {}
