package com.rreganjr.requel.service.api.dto;

public record EditUseCaseInput(
        String projectName,
        Long useCaseId,
        String name,
        String text,
        String primaryActorName,
        Integer version
) {}
