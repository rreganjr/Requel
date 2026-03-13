package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for the ImportProject command.
 * The XML file itself is sent as a separate multipart part ("file"),
 * not as part of this JSON input.
 *
 * @param name            optional name override for the imported project
 * @param enableAnalysis  whether to run NLP analysis after import (default false)
 */
public record ImportProjectInput(
        String name,
        Boolean enableAnalysis
) {
}
