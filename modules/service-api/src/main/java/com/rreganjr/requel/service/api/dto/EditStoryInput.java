package com.rreganjr.requel.service.api.dto;

public record EditStoryInput(
        String projectName,
        String name,
        String text,
        String storyTypeName,
        Integer version
) {
}
