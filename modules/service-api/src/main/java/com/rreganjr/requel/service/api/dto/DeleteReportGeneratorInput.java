package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for DeleteReportGenerator command.
 */
public record DeleteReportGeneratorInput(String projectName, Long reportId) {
}
