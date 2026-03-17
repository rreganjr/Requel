package com.rreganjr.requel.service.api.dto;

public record DeleteGoalRelationInput(
        String projectName,
        Long goalRelationId,
        Integer version
) {
}
