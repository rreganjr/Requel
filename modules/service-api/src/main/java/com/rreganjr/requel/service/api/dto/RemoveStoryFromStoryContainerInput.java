package com.rreganjr.requel.service.api.dto;

public record RemoveStoryFromStoryContainerInput(String projectName, Long storyContainerId, Long storyId) {}
