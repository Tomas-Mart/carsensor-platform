package com.carsensor.platform.dto;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * DTO для ответа с ошибкой
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Стандартный ответ с ошибкой (Problem Details for HTTP APIs RFC 7807)")
public record ErrorResponse(
        @Schema(description = "Временная метка ошибки в формате ISO 8601",
                example = "2026-03-16T14:30:00Z")
        String timestamp,

        @Schema(description = "HTTP статус код",
                example = "400",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int status,

        @Schema(description = "Краткое описание ошибки",
                example = "Bad Request",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String error,

        @Schema(description = "Детальное сообщение об ошибке",
                example = "Invalid request parameters",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String message,

        @Schema(description = "Путь запроса, на котором произошла ошибка",
                example = "/api/v1/cars")
        String path,

        @JsonProperty("error_code")
        @Schema(description = "Код ошибки для клиента",
                example = "VALIDATION_FAILED")
        String errorCode,

        @JsonProperty("field_errors")
        @Schema(description = "Ошибки по конкретным полям",
                example = "{\"brand\": \"Марка не может быть пустой\", \"year\": \"Год должен быть от 1900 до 2026\"}")
        Map<String, String> fieldErrors
) {
    public ErrorResponse {
        if (timestamp == null) {
            timestamp = Instant.now().toString();
        }
    }
}