package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        Instant timestamp
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, Instant.now());
    }
}
