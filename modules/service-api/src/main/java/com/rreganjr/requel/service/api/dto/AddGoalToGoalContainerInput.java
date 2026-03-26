package com.rreganjr.requel.service.api.dto;

public record AddGoalToGoalContainerInput(
        String projectName,
        Long goalContainerId,
        Long goalId,
        String containerType
) {
}
