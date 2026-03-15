package com.carsensor.platform.dto;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * DTO для ответа с ошибкой
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        String errorCode,
        Map<String, String> fieldErrors
) {
    public ErrorResponse {
        if (timestamp == null) {
            timestamp = Instant.now().toString();
        }
    }
}