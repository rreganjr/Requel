package com.rreganjr.requel.service.api.dto;

public record CopyGoalInput(
        String projectName,
        Long goalId,
        String newGoalName
) {
}
