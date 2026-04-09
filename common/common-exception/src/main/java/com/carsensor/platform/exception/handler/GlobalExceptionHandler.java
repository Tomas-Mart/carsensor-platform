package com.carsensor.platform.exception.handler;

import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
        // Логируем с информацией о запросе
        log.error("Platform exception occurred: {} | URI: {} | Method: {}",
                ex.getMessage(),
                request.getDescription(false),
                request.getContextPath(),
                ex);

        HttpStatus status = switch (ex) {
            case PlatformException.CarNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case PlatformException.UserNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case PlatformException.InvalidCredentialsException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.AccessDeniedException ignored -> HttpStatus.FORBIDDEN;
            case PlatformException.DuplicateResourceException ignored -> HttpStatus.CONFLICT;
            case PlatformException.ValidationException ignored -> HttpStatus.BAD_REQUEST;
            case PlatformException.MissingTokenException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.InvalidTokenFormatException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.InvalidTokenException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.TokenExpiredException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.UnauthorizedException ignored -> HttpStatus.UNAUTHORIZED;
            case PlatformException.UserBlockedException ignored -> HttpStatus.FORBIDDEN;
            case PlatformException.OptimisticLockException ignored -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        // Создаем Problem Detail согласно RFC 7807
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getUserMessage());
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setType(URI.create("/errors/" + ex.getErrorCode().toLowerCase()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", ex.getErrorCode());
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception occurred: {} | URI: {}",
                ex.getMessage(),
                request.getDescription(false),
                ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error_code", "INTERNAL_ERROR");
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }
}