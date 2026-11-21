package com.freightfox.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String status,
        String message,
        List<String> errors,
        Instant timestamp
) {
    public static ErrorResponse of(String message) {
        return new ErrorResponse("error", message, List.of(), Instant.now());
    }

    public static ErrorResponse of(String message, List<String> errors) {
        return new ErrorResponse("error", message, errors, Instant.now());
    }
}
