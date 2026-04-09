package com.carsensor.platform.exception.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;
import com.carsensor.platform.exception.PlatformException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для GlobalExceptionHandler.
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Обработка всех типов PlatformException</li>
 *   <li>Формирование ProblemDetail согласно RFC 7807</li>
 *   <li>Логирование информации о запросе</li>
 * </ul>
 *
 * @see GlobalExceptionHandler
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Тесты глобального обработчика ошибок")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/users");
        when(webRequest.getContextPath()).thenReturn("");
    }

    // ============================================================
    // ТЕСТЫ ОБРАБОТКИ ИСКЛЮЧЕНИЙ (404)
    // ============================================================

    @Nested
    @DisplayName("Обработка 404 Not Found исключений")
    class NotFoundExceptionTests {

        @Test
        @DisplayName("✅ CarNotFoundException возвращает 404 с деталями")
        void handleCarNotFoundException_Returns404WithDetails() {
            // given
            var exception = new PlatformException.CarNotFoundException(123L);

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Автомобиль с идентификатором 123 не найден");
            assertThat(problemDetail.getProperties())
                    .containsEntry("error_code", "CAR_NOT_FOUND")
                    .containsKey("timestamp")
                    .containsKey("path");
        }

        @Test
        @DisplayName("✅ UserNotFoundException возвращает 404 с деталями")
        void handleUserNotFoundException_Returns404WithDetails() {
            // given
            var exception = new PlatformException.UserNotFoundException("admin");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Пользователь не найден");
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "USER_NOT_FOUND");
        }
    }

    // ============================================================
    // ТЕСТЫ ОБРАБОТКИ ИСКЛЮЧЕНИЙ (400)
    // ============================================================

    @Nested
    @DisplayName("Обработка 400 Bad Request исключений")
    class BadRequestExceptionTests {

        @Test
        @DisplayName("✅ ValidationException возвращает 400 с деталями")
        void handleValidationException_Returns400WithDetails() {
            // given
            var exception = new PlatformException.ValidationException("Некорректный email");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Некорректный email");
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "VALIDATION_ERROR");
        }
    }

    // ============================================================
    // ТЕСТЫ ОБРАБОТКИ ИСКЛЮЧЕНИЙ (401)
    // ============================================================

    @Nested
    @DisplayName("Обработка 401 Unauthorized исключений")
    class UnauthorizedExceptionTests {

        @Test
        @DisplayName("✅ InvalidCredentialsException возвращает 401 с деталями")
        void handleInvalidCredentialsException_Returns401WithDetails() {
            // given
            var exception = new PlatformException.InvalidCredentialsException();

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Неверный логин или пароль");
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "INVALID_CREDENTIALS");
        }

        @Test
        @DisplayName("✅ MissingTokenException возвращает 401 с деталями")
        void handleMissingTokenException_Returns401WithDetails() {
            // given
            var exception = new PlatformException.MissingTokenException("Токен отсутствует");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "MISSING_TOKEN");
        }

        @Test
        @DisplayName("✅ InvalidTokenFormatException возвращает 401 с деталями")
        void handleInvalidTokenFormatException_Returns401WithDetails() {
            // given
            var exception = new PlatformException.InvalidTokenFormatException("Неверный формат токена");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "INVALID_TOKEN_FORMAT");
        }

        @Test
        @DisplayName("✅ InvalidTokenException возвращает 401 с деталями")
        void handleInvalidTokenException_Returns401WithDetails() {
            // given
            var exception = new PlatformException.InvalidTokenException("Невалидный токен");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "INVALID_TOKEN");
        }

        @Test
        @DisplayName("✅ TokenExpiredException возвращает 401 с деталями")
        void handleTokenExpiredException_Returns401WithDetails() {
            // given
            var exception = new PlatformException.TokenExpiredException("Токен истек");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "TOKEN_EXPIRED");
        }

        @Test
        @DisplayName("✅ UnauthorizedException возвращает 401 с деталями")
        void handleUnauthorizedException_Returns401WithDetails() {
            // given
            var exception = new PlatformException.UnauthorizedException("Неавторизованный доступ");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "UNAUTHORIZED");
        }
    }

    // ============================================================
    // ТЕСТЫ ОБРАБОТКИ ИСКЛЮЧЕНИЙ (403)
    // ============================================================

    @Nested
    @DisplayName("Обработка 403 Forbidden исключений")
    class ForbiddenExceptionTests {

        @Test
        @DisplayName("✅ AccessDeniedException возвращает 403 с деталями")
        void handleAccessDeniedException_Returns403WithDetails() {
            // given
            var exception = new PlatformException.AccessDeniedException("user", "ROLE_ADMIN");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(problemDetail.getDetail()).isEqualTo("У вас нет прав для выполнения этого действия");
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "ACCESS_DENIED");
        }

        @Test
        @DisplayName("✅ UserBlockedException возвращает 403 с деталями")
        void handleUserBlockedException_Returns403WithDetails() {
            // given
            var exception = new PlatformException.UserBlockedException("admin");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Учетная запись заблокирована");
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "USER_BLOCKED");
        }
    }

    // ============================================================
    // ТЕСТЫ ОБРАБОТКИ ИСКЛЮЧЕНИЙ (409)
    // ============================================================

    @Nested
    @DisplayName("Обработка 409 Conflict исключений")
    class ConflictExceptionTests {

        @Test
        @DisplayName("✅ DuplicateResourceException возвращает 409 с деталями")
        void handleDuplicateResourceException_Returns409WithDetails() {
            // given
            var exception = new PlatformException.DuplicateResourceException("User", "username: admin");

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(problemDetail.getDetail()).isEqualTo("User с такими данными уже существует");
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "DUPLICATE_RESOURCE");
        }

        @Test
        @DisplayName("✅ OptimisticLockException возвращает 409 с деталями")
        void handleOptimisticLockException_Returns409WithDetails() {
            // given
            var exception = new PlatformException.OptimisticLockException("User", 1L, 1L, 2L);

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(problemDetail.getDetail()).isEqualTo(
                    "Данные были изменены другим пользователем. Пожалуйста, обновите страницу и попробуйте снова.");
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "OPTIMISTIC_LOCK");
        }
    }

    // ============================================================
    // ТЕСТЫ ОБРАБОТКИ ИСКЛЮЧЕНИЙ (500)
    // ============================================================

    @Nested
    @DisplayName("Обработка 500 Internal Server Error исключений")
    class InternalServerErrorTests {

        @Test
        @DisplayName("✅ ParsingException возвращает 500 с деталями")
        void handleParsingException_Returns500WithDetails() {
            // given
            var exception = new PlatformException.ParsingException("https://example.com", new RuntimeException());

            // when
            var problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Ошибка при парсинге данных с сайта");
            assertThat(problemDetail.getProperties()).containsEntry("error_code", "PARSING_ERROR");
        }
    }

    // ============================================================
    // ТЕСТЫ ОБЩЕГО ОБРАБОТЧИКА
    // ============================================================

    @Nested
    @DisplayName("Общий обработчик Exception")
    class GenericExceptionTests {

        @Test
        @DisplayName("✅ Непредвиденное исключение возвращает 500 с общим сообщением")
        void handleGenericException_Returns500WithGenericMessage() {
            // given
            var exception = new RuntimeException("Database connection failed");

            // when
            var problemDetail = exceptionHandler.handleGenericException(exception, webRequest);

            // then
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Внутренняя ошибка сервера");
            assertThat(problemDetail.getProperties())
                    .containsEntry("error_code", "INTERNAL_ERROR")
                    .containsKey("timestamp")
                    .containsKey("path");
        }
    }
}