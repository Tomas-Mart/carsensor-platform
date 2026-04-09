package com.carsensor.platform.dto;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты для ErrorResponse.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Сериализация/десериализация JSON</li>
 *   <li>Компактный конструктор (нормализация и валидация)</li>
 *   <li>Фабричные методы</li>
 * </ul>
 *
 * @see ErrorResponse
 * @since 1.0
 */
@DisplayName("Тесты ErrorResponse")
class ErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================
    // ТЕСТЫ СЕРИАЛИЗАЦИИ
    // ============================================================

    @Nested
    @DisplayName("Сериализация и десериализация JSON")
    class SerializationTests {

        @Test
        @DisplayName("✅ Сериализация ErrorResponse в JSON и обратно")
        void should_SerializeAndDeserialize_Correctly() throws Exception {
            // given
            var original = new ErrorResponse(
                    Instant.now().toString(),
                    400,
                    "Bad Request",
                    "Invalid input",
                    "/api/v1/cars",
                    "VALIDATION_FAILED",
                    Map.of("brand", "Марка не может быть пустой")
            );

            // when
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, ErrorResponse.class);

            // then
            assertThat(deserialized)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.status()).isEqualTo(400);
                        assertThat(r.error()).isEqualTo("Bad Request");
                        assertThat(r.message()).isEqualTo("Invalid input");
                        assertThat(r.path()).isEqualTo("/api/v1/cars");
                        assertThat(r.errorCode()).isEqualTo("VALIDATION_FAILED");
                        assertThat(r.fieldErrors()).containsEntry("brand", "Марка не может быть пустой");
                    });
        }
    }

    // ============================================================
    // ТЕСТЫ КОМПАКТНОГО КОНСТРУКТОРА
    // ============================================================

    @Nested
    @DisplayName("Компактный конструктор (нормализация и валидация)")
    class CompactConstructorTests {

        @Test
        @DisplayName("✅ Null timestamp заменяется на текущее время")
        void should_SetCurrentTimestamp_WhenTimestampIsNull() {
            // when
            var response = new ErrorResponse(
                    null,
                    401,
                    "Unauthorized",
                    "Invalid credentials",
                    "/api/v1/auth/login",
                    null,
                    null
            );

            // then
            assertThat(response.timestamp()).isNotBlank();
            assertThat(response.status()).isEqualTo(401);
            assertThat(response.error()).isEqualTo("Unauthorized");
            assertThat(response.message()).isEqualTo("Invalid credentials");
            assertThat(response.path()).isEqualTo("/api/v1/auth/login");
            assertThat(response.errorCode()).isNull();
            assertThat(response.fieldErrors()).isNull();
        }

        @Test
        @DisplayName("✅ Blank timestamp заменяется на текущее время")
        void should_SetCurrentTimestamp_WhenTimestampIsBlank() {
            // when
            var response = new ErrorResponse(
                    "   ",
                    400,
                    "Bad Request",
                    "Invalid input",
                    null,
                    null,
                    null
            );

            // then
            assertThat(response.timestamp()).isNotBlank();
        }

        @Test
        @DisplayName("✅ Корректный timestamp остается без изменений")
        void should_KeepTimestamp_WhenTimestampIsValid() {
            // given
            var timestamp = "2026-03-27T10:30:00Z";

            // when
            var response = new ErrorResponse(
                    timestamp,
                    400,
                    "Bad Request",
                    "Invalid input",
                    null,
                    null,
                    null
            );

            // then
            assertThat(response.timestamp()).isEqualTo(timestamp);
        }
    }

    // ============================================================
    // ТЕСТЫ ФАБРИЧНЫХ МЕТОДОВ
    // ============================================================

    @Nested
    @DisplayName("Фабричные методы")
    class FactoryMethodTests {

        @Test
        @DisplayName("✅ of(statusCode, statusText, message)")
        void should_CreateErrorResponse_WithThreeParams() {
            // when
            var response = ErrorResponse.of(404, "Not Found", "Resource not found");

            // then
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.error()).isEqualTo("Not Found");
            assertThat(response.message()).isEqualTo("Resource not found");
            assertThat(response.timestamp()).isNotBlank();
            assertThat(response.path()).isNull();
            assertThat(response.errorCode()).isNull();
            assertThat(response.fieldErrors()).isNull();
        }

        @Test
        @DisplayName("✅ of(statusCode, statusText, message, path)")
        void should_CreateErrorResponse_WithFourParams() {
            // when
            var response = ErrorResponse.of(404, "Not Found", "Resource not found", "/api/v1/cars/999");

            // then
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.error()).isEqualTo("Not Found");
            assertThat(response.message()).isEqualTo("Resource not found");
            assertThat(response.path()).isEqualTo("/api/v1/cars/999");
        }

        @Test
        @DisplayName("✅ of(statusCode, statusText, message, path, errorCode)")
        void should_CreateErrorResponse_WithFiveParams() {
            // when
            var response = ErrorResponse.of(404, "Not Found", "Resource not found", "/api/v1/cars/999", "NOT_FOUND");

            // then
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.errorCode()).isEqualTo("NOT_FOUND");
        }

        @Test
        @DisplayName("✅ validationError()")
        void should_CreateValidationErrorResponse() {
            // given
            var fieldErrors = Map.of("brand", "Марка не может быть пустой", "year", "Год должен быть от 1900 до 2026");

            // when
            var response = ErrorResponse.validationError(fieldErrors, "/api/v1/cars");

            // then
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.error()).isEqualTo("Bad Request");
            assertThat(response.message()).isEqualTo("Ошибка валидации входных данных");
            assertThat(response.path()).isEqualTo("/api/v1/cars");
            assertThat(response.errorCode()).isEqualTo("VALIDATION_FAILED");
            assertThat(response.fieldErrors()).containsAllEntriesOf(fieldErrors);
        }

        @Test
        @DisplayName("✅ unauthorized()")
        void should_CreateUnauthorizedResponse() {
            // when
            var response = ErrorResponse.unauthorized("Invalid token", "/api/v1/auth/me");

            // then
            assertThat(response.status()).isEqualTo(401);
            assertThat(response.error()).isEqualTo("Unauthorized");
            assertThat(response.message()).isEqualTo("Invalid token");
            assertThat(response.path()).isEqualTo("/api/v1/auth/me");
            assertThat(response.errorCode()).isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("✅ forbidden()")
        void should_CreateForbiddenResponse() {
            // when
            var response = ErrorResponse.forbidden("Access denied", "/api/v1/admin");

            // then
            assertThat(response.status()).isEqualTo(403);
            assertThat(response.error()).isEqualTo("Forbidden");
            assertThat(response.message()).isEqualTo("Access denied");
            assertThat(response.errorCode()).isEqualTo("ACCESS_DENIED");
        }

        @Test
        @DisplayName("✅ notFound()")
        void should_CreateNotFoundResponse() {
            // when
            var response = ErrorResponse.notFound("Car not found", "/api/v1/cars/999");

            // then
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.error()).isEqualTo("Not Found");
            assertThat(response.message()).isEqualTo("Car not found");
            assertThat(response.errorCode()).isEqualTo("NOT_FOUND");
        }

        @Test
        @DisplayName("✅ internalServerError()")
        void should_CreateInternalServerErrorResponse() {
            // when
            var response = ErrorResponse.internalServerError("Unexpected error", "/api/v1/cars");

            // then
            assertThat(response.status()).isEqualTo(500);
            assertThat(response.error()).isEqualTo("Internal Server Error");
            assertThat(response.message()).isEqualTo("Unexpected error");
            assertThat(response.errorCode()).isEqualTo("INTERNAL_ERROR");
        }
    }
}