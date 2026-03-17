package com.rreganjr.requel.service.api.dto;

public record CopyStoryInput(
        String projectName,
        Long storyId,
        String newStoryName
) {
}
