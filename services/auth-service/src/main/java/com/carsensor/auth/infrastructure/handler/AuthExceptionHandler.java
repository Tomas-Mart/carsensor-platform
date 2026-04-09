package com.carsensor.auth.infrastructure.handler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

/**
 * Централизованный обработчик ошибок для auth-service.
 *
 * <p>Обрабатывает специфичные для auth-service исключения и делегирует
 * общие исключения в GlobalExceptionHandler.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    /**
     * Обработка ошибок аутентификации (неверные учетные данные).
     *
     * @param ex исключение BadCredentialsException
     * @return ProblemDetail с деталями ошибки
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(BadCredentialsException ex) {
        log.error("Ошибка аутентификации: неверный логин или пароль");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Неверный логин или пароль"
        );
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setType(URI.create("/errors/invalid-credentials"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "INVALID_CREDENTIALS");

        return problemDetail;
    }

    /**
     * Обработка ошибок доступа (недостаточно прав).
     *
     * @param ex исключение AccessDeniedException
     * @return ProblemDetail с деталями ошибки
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Ошибка доступа: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "У вас нет прав для выполнения этого действия"
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setType(URI.create("/errors/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "ACCESS_DENIED");

        return problemDetail;
    }

    /**
     * Обработка ошибок оптимистичной блокировки (JPA @Version).
     *
     * @param ex исключение OptimisticLockingFailureException
     * @return ProblemDetail с деталями ошибки
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        log.error("Ошибка оптимистичной блокировки: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Данные были изменены другим пользователем. Пожалуйста, обновите страницу и попробуйте снова."
        );
        problemDetail.setTitle("Conflict");
        problemDetail.setType(URI.create("/errors/optimistic-lock"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "OPTIMISTIC_LOCK");

        return problemDetail;
    }

    /**
     * Обработка ошибок валидации входных данных.
     *
     * @param ex исключение MethodArgumentNotValidException
     * @return ProblemDetail с деталями ошибки
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Ошибка валидации: {}", errors);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Ошибка валидации входных данных"
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("/errors/validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "VALIDATION_FAILED");
        problemDetail.setProperty("field_errors", errors);

        return problemDetail;
    }
}