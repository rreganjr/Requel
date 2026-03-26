package com.rreganjr.requel.service.api.dto;

public record DeleteUseCaseInput(String projectName, Long useCaseId, int version) {}
