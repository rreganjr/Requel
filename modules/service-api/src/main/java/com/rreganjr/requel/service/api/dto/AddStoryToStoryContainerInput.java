package com.rreganjr.requel.service.api.dto;

public record AddStoryToStoryContainerInput(String projectName, Long storyContainerId, Long storyId) {}
