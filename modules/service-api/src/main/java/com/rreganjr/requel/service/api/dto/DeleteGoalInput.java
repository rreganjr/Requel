package com.rreganjr.requel.service.api.dto;

public record DeleteGoalInput(
        String projectName,
        Long goalId,
        Integer version
) {
}
