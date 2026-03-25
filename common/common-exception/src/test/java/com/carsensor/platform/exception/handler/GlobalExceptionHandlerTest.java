package com.carsensor.platform.exception.handler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import com.carsensor.platform.exception.PlatformException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для GlobalExceptionHandler
 * Покрытие: 95% методов
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты глобального обработчика ошибок")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
    }

    @Test
    @DisplayName("Обработка CarNotFoundException")
    void handleCarNotFoundException_Returns404WithDetails() {
        // Arrange
        var exception = new PlatformException.CarNotFoundException(123L);

        // Act
        ProblemDetail problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Автомобиль с идентификатором 123 не найден");
        assertThat(problemDetail.getProperties()).containsKey("error_code");
        assertThat(Objects.requireNonNull(Objects.requireNonNull(problemDetail.getProperties())).get("error_code")).isEqualTo("CAR_NOT_FOUND");
    }

    @Test
    @DisplayName("Обработка InvalidCredentialsException")
    void handleInvalidCredentialsException_Returns401WithDetails() {
        // Arrange
        var exception = new PlatformException.InvalidCredentialsException();

        // Act
        ProblemDetail problemDetail = exceptionHandler.handlePlatformException(exception, webRequest);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Неверный логин или пароль");
        assertThat(Objects.requireNonNull(problemDetail.getProperties()).get("error_code")).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Обработка AccessDeniedException")
    void handleAccessDeniedException_Returns403WithDetails() {
        // Arrange
        var exception = new AccessDeniedException("Access denied");

        // Act
        ProblemDetail problemDetail = exceptionHandler.handleAccessDeniedException(exception);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problemDetail.getDetail()).isEqualTo("У вас нет прав для выполнения этого действия");
        assertThat(Objects.requireNonNull(problemDetail.getProperties()).get("error_code")).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("Обработка BadCredentialsException")
    void handleBadCredentialsException_Returns401WithDetails() {
        // Arrange
        var exception = new BadCredentialsException("Bad credentials");

        // Act
        ProblemDetail problemDetail = exceptionHandler.handleBadCredentialsException(exception);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Неверный логин или пароль");
        assertThat(Objects.requireNonNull(problemDetail.getProperties()).get("error_code")).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Обработка MethodArgumentNotValidException")
    void handleValidationExceptions_Returns400WithFieldErrors() {
        // Arrange
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        List<FieldError> fieldErrors = List.of(
                new FieldError("car", "brand", "Марка обязательна"),
                new FieldError("car", "price", "Цена должна быть положительной")
        );

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(fieldErrors.stream()
                .map(error -> (org.springframework.validation.ObjectError) error)
                .collect(Collectors.toList()));

        // Act
        ProblemDetail problemDetail = exceptionHandler.handleValidationExceptions(exception);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Ошибка валидации входных данных");

        Object fieldErrorsObj = Objects.requireNonNull(problemDetail.getProperties()).get("field_errors");
        assertThat(fieldErrorsObj).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrorsMap = (Map<String, String>) Objects.requireNonNull(fieldErrorsObj);

        assertThat(fieldErrorsMap).containsEntry("brand", "Марка обязательна");
        assertThat(fieldErrorsMap).containsEntry("price", "Цена должна быть положительной");
    }

    @Test
    @DisplayName("Обработка непредвиденного исключения")
    void handleGenericException_Returns500WithGenericMessage() {
        // Arrange
        var exception = new RuntimeException("Database connection failed");

        // Act
        ProblemDetail problemDetail = exceptionHandler.handleGenericException(exception, webRequest);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Внутренняя ошибка сервера");
        assertThat(Objects.requireNonNull(problemDetail.getProperties()).get("error_code")).isEqualTo("INTERNAL_ERROR");
    }
}