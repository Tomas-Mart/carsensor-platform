package com.carsensor.platform.exception.handler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import com.carsensor.platform.exception.PlatformException;
import lombok.extern.slf4j.Slf4j;

/**
 * Централизованный обработчик ошибок с поддержкой RFC 7807 (Problem Details)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PlatformException.class)
    public ProblemDetail handlePlatformException(PlatformException ex, WebRequest request) {
        // Технические детали логируем на английском
        log.error("Platform exception occurred: {}", ex.getMessage(), ex);

        HttpStatus status = switch (ex) {
            case PlatformException.CarNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case PlatformException.UserNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case PlatformException.InvalidCredentialsException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.AccessDeniedException ignored -> HttpStatus.FORBIDDEN;
            case PlatformException.DuplicateResourceException ignored -> HttpStatus.CONFLICT;
            case PlatformException.ParsingException ignored -> HttpStatus.INTERNAL_SERVER_ERROR;
            case PlatformException.ValidationException ignored -> HttpStatus.BAD_REQUEST;
            case PlatformException.MissingTokenException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.InvalidTokenFormatException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.InvalidTokenException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.TokenExpiredException ignored -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        // Создаем Problem Detail согласно RFC 7807
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getUserMessage());
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setType(URI.create("/errors/" + ex.getErrorCode().toLowerCase()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", ex.getErrorCode());

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

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(BadCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Неверный логин или пароль"
        );
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setType(URI.create("/errors/invalid-credentials"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "INVALID_CREDENTIALS");

        log.error("Authentication failed: {}", ex.getMessage() != null ? ex.getMessage() : "Unknown error");

        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "У вас нет прав для выполнения этого действия"
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setType(URI.create("/errors/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "ACCESS_DENIED");

        log.error("Access denied: {}", ex.getMessage() != null ? ex.getMessage() : "Access denied");

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception occurred", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "INTERNAL_ERROR");

        return problemDetail;
    }
}