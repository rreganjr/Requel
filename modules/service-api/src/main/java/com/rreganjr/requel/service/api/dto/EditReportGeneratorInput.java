package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for EditReportGenerator command.
 * reportId is null when creating a new report generator.
 */
public record EditReportGeneratorInput(String projectName, Long reportId, String name, String text) {
}
