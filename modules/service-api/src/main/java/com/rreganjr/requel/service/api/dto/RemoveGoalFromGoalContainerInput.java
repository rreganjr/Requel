package com.rreganjr.requel.service.api.dto;

public record RemoveGoalFromGoalContainerInput(
        String projectName,
        Long goalContainerId,
        Long goalId
) {
}
