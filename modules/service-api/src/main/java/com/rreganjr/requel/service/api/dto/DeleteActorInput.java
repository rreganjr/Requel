package com.rreganjr.requel.service.api.dto;

public record DeleteActorInput(
        String projectName,
        Long actorId,
        int version
) {
}
