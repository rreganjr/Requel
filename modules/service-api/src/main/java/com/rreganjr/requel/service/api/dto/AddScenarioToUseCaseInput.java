package com.rreganjr.requel.service.api.dto;

public record AddScenarioToUseCaseInput(
        String projectName,
        Long useCaseId,
        Long scenarioId
) {}
