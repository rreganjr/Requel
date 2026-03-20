package com.rreganjr.requel.service.api.dto;

import java.util.List;

public record EditScenarioInput(
    String projectName,
    Long scenarioId,
    String name,
    String text,
    String scenarioTypeName,
    Integer version,
    List<EditStepInput> steps
) {}
