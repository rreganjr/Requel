package com.rreganjr.requel.service.api.dto;

public record DeleteStoryInput(
        String projectName,
        Long storyId,
        Integer version
) {
}
