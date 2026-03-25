package com.carsensor.platform.exception;

import lombok.Getter;

/**
 * Базовое исключение платформы - sealed class для контроля иерархии
 */
@Getter
public sealed class PlatformException extends RuntimeException
        permits PlatformException.CarNotFoundException,
        PlatformException.UserNotFoundException,
        PlatformException.InvalidCredentialsException,
        PlatformException.AccessDeniedException,
        PlatformException.DuplicateResourceException,
        PlatformException.ParsingException,
        PlatformException.ValidationException,
        PlatformException.MissingTokenException,
        PlatformException.InvalidTokenFormatException,
        PlatformException.InvalidTokenException,
        PlatformException.TokenExpiredException,
        PlatformException.UnauthorizedException,
        PlatformException.UserBlockedException {  // ДОБАВЬТЕ ЭТУ СТРОКУ

    private final String errorCode;
    private final String userMessage;
    private final Object[] args;

    protected PlatformException(String errorCode, String userMessage, String technicalMessage, Object[] args, Throwable cause) {
        super(technicalMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.args = args;
    }

    /**
     * Автомобиль не найден
     */
    public static final class CarNotFoundException extends PlatformException {
        public CarNotFoundException(Long id) {
            super(
                    "CAR_NOT_FOUND",
                    "Автомобиль с идентификатором " + id + " не найден",
                    "Car not found with id: " + id,
                    new Object[]{id},
                    null
            );
        }

        public CarNotFoundException(String brand, String model) {
            super(
                    "CAR_NOT_FOUND",
                    "Автомобиль " + brand + " " + model + " не найден",
                    "Car not found: " + brand + " " + model,
                    new Object[]{brand, model},
                    null
            );
        }
    }

    /**
     * Пользователь не найден
     */
    public static final class UserNotFoundException extends PlatformException {
        public UserNotFoundException(String username) {
            super(
                    "USER_NOT_FOUND",
                    "Пользователь не найден",
                    "User not found: " + username,
                    new Object[]{username},
                    null
            );
        }
    }

    /**
     * Неверные учетные данные
     */
    public static final class InvalidCredentialsException extends PlatformException {
        public InvalidCredentialsException() {
            super(
                    "INVALID_CREDENTIALS",
                    "Неверный логин или пароль",
                    "Invalid login credentials",
                    null,
                    null
            );
        }

        public InvalidCredentialsException(String message) {
            super(
                    "INVALID_CREDENTIALS",
                    message,
                    message,
                    null,
                    null
            );
        }
    }

    /**
     * Доступ запрещен
     */
    public static final class AccessDeniedException extends PlatformException {
        public AccessDeniedException(String username, String requiredRole) {
            super(
                    "ACCESS_DENIED",
                    "У вас нет прав для выполнения этого действия",
                    "Access denied for user: " + username + ", required role: " + requiredRole,
                    new Object[]{username, requiredRole},
                    null
            );
        }

        public AccessDeniedException(String username, String message, String technicalMessage) {
            super(
                    "ACCESS_DENIED",
                    message,
                    technicalMessage,
                    new Object[]{username},
                    null
            );
        }
    }

    /**
     * Дублирование ресурса
     */
    public static final class DuplicateResourceException extends PlatformException {
        public DuplicateResourceException(String resourceType, String identifier) {
            super(
                    "DUPLICATE_RESOURCE",
                    resourceType + " с такими данными уже существует",
                    resourceType + " already exists: " + identifier,
                    new Object[]{resourceType, identifier},
                    null
            );
        }
    }

    /**
     * Ошибка парсинга
     */
    public static final class ParsingException extends PlatformException {
        public ParsingException(String url, Throwable cause) {
            super(
                    "PARSING_ERROR",
                    "Ошибка при парсинге данных с сайта",
                    "Failed to parse data from: " + url,
                    new Object[]{url},
                    cause
            );
        }
    }

    /**
     * Ошибка валидации
     */
    public static final class ValidationException extends PlatformException {
        public ValidationException(String message) {
            super(
                    "VALIDATION_ERROR",
                    message,
                    "Validation error: " + message,
                    null,
                    null
            );
        }
    }

    /**
     * Отсутствует токен
     */
    public static final class MissingTokenException extends PlatformException {
        public MissingTokenException(String message) {
            super(
                    "MISSING_TOKEN",
                    message,
                    "Missing token: " + message,
                    null,
                    null
            );
        }
    }

    /**
     * Неверный формат токена
     */
    public static final class InvalidTokenFormatException extends PlatformException {
        public InvalidTokenFormatException(String message) {
            super(
                    "INVALID_TOKEN_FORMAT",
                    message,
                    "Invalid token format: " + message,
                    null,
                    null
            );
        }
    }

    /**
     * Невалидный токен
     */
    public static final class InvalidTokenException extends PlatformException {
        public InvalidTokenException(String message) {
            super(
                    "INVALID_TOKEN",
                    message,
                    "Invalid token: " + message,
                    null,
                    null
            );
        }

        public InvalidTokenException(String message, Throwable cause) {
            super(
                    "INVALID_TOKEN",
                    message,
                    "Invalid token: " + message,
                    null,
                    cause
            );
        }
    }

    /**
     * Токен истек
     */
    public static final class TokenExpiredException extends PlatformException {
        public TokenExpiredException(String message) {
            super(
                    "TOKEN_EXPIRED",
                    message,
                    "Token expired: " + message,
                    null,
                    null
            );
        }
    }

    /**
     * Пользователь заблокирован
     */
    public static final class UserBlockedException extends PlatformException {
        public UserBlockedException(String username) {
            super(
                    "USER_BLOCKED",
                    "Учетная запись заблокирована",
                    "User account is blocked: " + username,
                    new Object[]{username},
                    null
            );
        }
    }

    /**
     * Неавторизованный доступ (401 Unauthorized)
     */
    public static final class UnauthorizedException extends PlatformException {
        public UnauthorizedException(String message) {
            super(
                    "UNAUTHORIZED",
                    message,
                    "Unauthorized: " + message,
                    null,
                    null
            );
        }

        public UnauthorizedException(String message, Throwable cause) {
            super(
                    "UNAUTHORIZED",
                    message,
                    "Unauthorized: " + message,
                    null,
                    cause
            );
        }
    }
}