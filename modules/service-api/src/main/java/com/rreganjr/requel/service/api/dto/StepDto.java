package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single step in a scenario's step list.
 * When isScenario=true, scenarioId points to the sub-scenario and children
 * are not populated here — navigate to /scenarios/{scenarioId} to see them.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StepDto(
    Long id,
    int version,
    String name,
    String text,
    String scenarioType,
    boolean isScenario,
    Long scenarioId
) {}
