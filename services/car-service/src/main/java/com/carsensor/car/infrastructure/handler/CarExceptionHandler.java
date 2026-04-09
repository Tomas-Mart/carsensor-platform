package com.carsensor.car.infrastructure.handler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import com.carsensor.platform.exception.PlatformException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class CarExceptionHandler {

    /**
     * Обработка ошибки доступа от Spring Security (@PreAuthorize).
     * Возвращает 403 Forbidden вместо 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleSpringSecurityAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Spring Security Access denied: {} | URI: {}", ex.getMessage(), request.getDescription(false));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "У вас нет прав для выполнения этого действия"
        );
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(URI.create("/errors/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "ACCESS_DENIED");
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }

    /**
     * Обработка ошибки доступа от бизнес-логики (PlatformException.AccessDeniedException).
     */
    @ExceptionHandler(PlatformException.AccessDeniedException.class)
    public ProblemDetail handleBusinessAccessDeniedException(PlatformException.AccessDeniedException ex, WebRequest request) {
        log.warn("Business Access denied: {} | URI: {}", ex.getMessage(), request.getDescription(false));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getUserMessage()
        );
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(URI.create("/errors/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", ex.getErrorCode());
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }

    /**
     * Обработка ошибок оптимистичной блокировки (JPA @Version)
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        log.error("Optimistic locking failure: {}", ex.getMessage(), ex);

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Ошибка валидации входных данных"
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("/errors/validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "VALIDATION_FAILED");
        problemDetail.setProperty("field_errors", errors);

        log.error("Validation error: {}", errors);

        return problemDetail;
    }
}