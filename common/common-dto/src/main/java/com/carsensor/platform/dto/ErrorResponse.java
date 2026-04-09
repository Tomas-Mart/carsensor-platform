package com.carsensor.platform.dto;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для ответа с ошибкой
 * Не зависит от Spring — только чистый Java
 */
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
    // Компактный конструктор для нормализации
    public ErrorResponse {
        if (timestamp == null || timestamp.isBlank()) {
            timestamp = Instant.now().toString();
        }
    }

    // ===== Фабричные методы (без Spring HttpStatus) =====

    /**
     * Создает ответ с ошибкой
     *
     * @param statusCode HTTP статус код (например, 400, 404, 500)
     * @param statusText HTTP статус текст (например, "Bad Request")
     * @param message    детальное сообщение об ошибке
     */
    public static ErrorResponse of(int statusCode, String statusText, String message) {
        return new ErrorResponse(
                null,
                statusCode,
                statusText,
                message,
                null,
                null,
                null
        );
    }

    public static ErrorResponse of(int statusCode, String statusText, String message, String path) {
        return new ErrorResponse(
                null,
                statusCode,
                statusText,
                message,
                path,
                null,
                null
        );
    }

    public static ErrorResponse of(int statusCode, String statusText, String message, String path, String errorCode) {
        return new ErrorResponse(
                null,
                statusCode,
                statusText,
                message,
                path,
                errorCode,
                null
        );
    }

    /**
     * Создает ответ для ошибки валидации (400 Bad Request)
     */
    public static ErrorResponse validationError(Map<String, String> fieldErrors, String path) {
        return new ErrorResponse(
                null,
                400,
                "Bad Request",
                "Ошибка валидации входных данных",
                path,
                "VALIDATION_FAILED",
                fieldErrors
        );
    }

    /**
     * Создает ответ для ошибки 401 Unauthorized
     */
    public static ErrorResponse unauthorized(String message, String path) {
        return new ErrorResponse(
                null,
                401,
                "Unauthorized",
                message,
                path,
                "UNAUTHORIZED",
                null
        );
    }

    /**
     * Создает ответ для ошибки 403 Forbidden
     */
    public static ErrorResponse forbidden(String message, String path) {
        return new ErrorResponse(
                null,
                403,
                "Forbidden",
                message,
                path,
                "ACCESS_DENIED",
                null
        );
    }

    /**
     * Создает ответ для ошибки 404 Not Found
     */
    public static ErrorResponse notFound(String message, String path) {
        return new ErrorResponse(
                null,
                404,
                "Not Found",
                message,
                path,
                "NOT_FOUND",
                null
        );
    }

    /**
     * Создает ответ для ошибки 500 Internal Server Error
     */
    public static ErrorResponse internalServerError(String message, String path) {
        return new ErrorResponse(
                null,
                500,
                "Internal Server Error",
                message,
                path,
                "INTERNAL_ERROR",
                null
        );
    }
}