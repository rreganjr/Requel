package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScenarioDto(
    Long id,
    int version,
    String name,
    String text,
    String scenarioType,
    String createdBy,
    List<StepDto> steps
) {}
