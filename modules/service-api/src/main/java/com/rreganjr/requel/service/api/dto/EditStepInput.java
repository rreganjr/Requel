package com.rreganjr.requel.service.api.dto;

/**
 * One entry in the steps list when saving a scenario.
 * stepId=null means create new. isScenario=true means this step is a sub-scenario;
 * the registrar will use EditScenarioCommand instead of EditScenarioStepCommand.
 */
public record EditStepInput(
    Long stepId,
    String name,
    String text,
    String scenarioTypeName,
    boolean isScenario
) {}
