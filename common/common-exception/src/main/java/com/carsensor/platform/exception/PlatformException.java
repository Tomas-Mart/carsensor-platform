package com.carsensor.platform.exception;

import lombok.Getter;

/**
 * Базовое исключение платформы - sealed class для контроля иерархии.
 *
 * <p>Предоставляет единую иерархию исключений для всех микросервисов платформы.
 * Каждый тип исключения имеет свой уникальный код ошибки и пользовательское сообщение.
 *
 * <p><b>Коды ошибок:</b>
 * <ul>
 *   <li>CAR_NOT_FOUND - автомобиль не найден</li>
 *   <li>USER_NOT_FOUND - пользователь не найден</li>
 *   <li>INVALID_CREDENTIALS - неверные учетные данные</li>
 *   <li>ACCESS_DENIED - доступ запрещен</li>
 *   <li>DUPLICATE_RESOURCE - дублирование ресурса</li>
 *   <li>PARSING_ERROR - ошибка парсинга</li>
 *   <li>VALIDATION_ERROR - ошибка валидации</li>
 *   <li>MISSING_TOKEN - отсутствует токен</li>
 *   <li>INVALID_TOKEN_FORMAT - неверный формат токена</li>
 *   <li>INVALID_TOKEN - невалидный токен</li>
 *   <li>TOKEN_EXPIRED - токен истек</li>
 *   <li>UNAUTHORIZED - неавторизованный доступ</li>
 *   <li>USER_BLOCKED - пользователь заблокирован</li>
 *   <li>OPTIMISTIC_LOCK - ошибка оптимистичной блокировки</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
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
        PlatformException.UserBlockedException,
        PlatformException.OptimisticLockException {

    private final String errorCode;
    private final String userMessage;
    private final Object[] args;

    /**
     * Конструктор базового исключения.
     *
     * @param errorCode        код ошибки
     * @param userMessage      сообщение для пользователя
     * @param technicalMessage техническое сообщение для логов
     * @param args             аргументы сообщения
     * @param cause            причина исключения
     */
    protected PlatformException(String errorCode, String userMessage, String technicalMessage, Object[] args, Throwable cause) {
        super(technicalMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.args = args;
    }

    // ============================================================
    // Исключения для работы с автомобилями
    // ============================================================

    /**
     * Автомобиль не найден.
     *
     * <p>Выбрасывается при попытке получить, обновить или удалить автомобиль,
     * который не существует в базе данных.
     */
    public static final class CarNotFoundException extends PlatformException {

        /**
         * Создает исключение для несуществующего ID.
         *
         * @param id идентификатор автомобиля
         */
        public CarNotFoundException(Long id) {
            super(
                    "CAR_NOT_FOUND",
                    "Автомобиль с идентификатором " + id + " не найден",
                    "Car not found with id: " + id,
                    new Object[]{id},
                    null
            );
        }

        /**
         * Создает исключение для несуществующей комбинации марки и модели.
         *
         * @param brand марка автомобиля
         * @param model модель автомобиля
         */
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

    // ============================================================
    // Исключения для работы с пользователями
    // ============================================================

    /**
     * Пользователь не найден.
     *
     * <p>Выбрасывается при попытке аутентификации или получения данных
     * пользователя, который не существует.
     */
    public static final class UserNotFoundException extends PlatformException {

        /**
         * Создает исключение для несуществующего пользователя.
         *
         * @param username имя пользователя
         */
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
     * Пользователь заблокирован.
     *
     * <p>Выбрасывается при попытке аутентификации заблокированного пользователя.
     */
    public static final class UserBlockedException extends PlatformException {

        /**
         * Создает исключение для заблокированного пользователя.
         *
         * @param username имя пользователя
         */
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

    // ============================================================
    // Исключения аутентификации и авторизации
    // ============================================================

    /**
     * Неверные учетные данные.
     *
     * <p>Выбрасывается при неверном логине или пароле.
     */
    public static final class InvalidCredentialsException extends PlatformException {

        /**
         * Создает исключение с сообщением по умолчанию.
         */
        public InvalidCredentialsException() {
            super(
                    "INVALID_CREDENTIALS",
                    "Неверный логин или пароль",
                    "Invalid login credentials",
                    null,
                    null
            );
        }

        /**
         * Создает исключение с пользовательским сообщением.
         *
         * @param message сообщение об ошибке
         */
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
     * Доступ запрещен.
     *
     * <p>Выбрасывается когда у пользователя недостаточно прав для выполнения операции.
     */
    public static final class AccessDeniedException extends PlatformException {

        /**
         * Создает исключение для отсутствия необходимой роли.
         *
         * @param username     имя пользователя
         * @param requiredRole требуемая роль
         */
        public AccessDeniedException(String username, String requiredRole) {
            super(
                    "ACCESS_DENIED",
                    "У вас нет прав для выполнения этого действия",
                    "Access denied for user: " + username + ", required role: " + requiredRole,
                    new Object[]{username, requiredRole},
                    null
            );
        }

        /**
         * Создает исключение с пользовательскими сообщениями.
         *
         * @param username         имя пользователя
         * @param message          сообщение для пользователя
         * @param technicalMessage техническое сообщение
         */
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
     * Неавторизованный доступ (401 Unauthorized).
     *
     * <p>Выбрасывается когда отсутствует или невалиден JWT токен.
     */
    public static final class UnauthorizedException extends PlatformException {

        /**
         * Создает исключение с сообщением.
         *
         * @param message сообщение об ошибке
         */
        public UnauthorizedException(String message) {
            super(
                    "UNAUTHORIZED",
                    message,
                    "Unauthorized: " + message,
                    null,
                    null
            );
        }

        /**
         * Создает исключение с сообщением и причиной.
         *
         * @param message сообщение об ошибке
         * @param cause   причина исключения
         */
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

    // ============================================================
    // JWT токен исключения
    // ============================================================

    /**
     * Отсутствует токен.
     *
     * <p>Выбрасывается когда в запросе не передан JWT токен.
     */
    public static final class MissingTokenException extends PlatformException {

        /**
         * Создает исключение с сообщением.
         *
         * @param message сообщение об ошибке
         */
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
     * Неверный формат токена.
     *
     * <p>Выбрасывается когда JWT токен имеет некорректный формат.
     */
    public static final class InvalidTokenFormatException extends PlatformException {

        /**
         * Создает исключение с сообщением.
         *
         * @param message сообщение об ошибке
         */
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
     * Невалидный токен.
     *
     * <p>Выбрасывается когда JWT токен имеет неверную подпись или другие проблемы.
     */
    public static final class InvalidTokenException extends PlatformException {

        /**
         * Создает исключение с сообщением.
         *
         * @param message сообщение об ошибке
         */
        public InvalidTokenException(String message) {
            super(
                    "INVALID_TOKEN",
                    message,
                    "Invalid token: " + message,
                    null,
                    null
            );
        }

        /**
         * Создает исключение с сообщением и причиной.
         *
         * @param message сообщение об ошибке
         * @param cause   причина исключения
         */
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
     * Токен истек.
     *
     * <p>Выбрасывается когда срок действия JWT токена истек.
     */
    public static final class TokenExpiredException extends PlatformException {

        /**
         * Создает исключение с сообщением.
         *
         * @param message сообщение об ошибке
         */
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

    // ============================================================
    // Исключения бизнес-логики
    // ============================================================

    /**
     * Дублирование ресурса.
     *
     * <p>Выбрасывается при попытке создать ресурс с уже существующим уникальным полем.
     */
    public static final class DuplicateResourceException extends PlatformException {

        /**
         * Создает исключение для дублирующегося ресурса.
         *
         * @param resourceType тип ресурса
         * @param identifier   идентификатор ресурса
         */
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
     * Ошибка оптимистичной блокировки.
     *
     * <p>Выбрасывается при попытке обновить запись, которая была изменена
     * другим пользователем (несовпадение версий).
     */
    public static final class OptimisticLockException extends PlatformException {

        /**
         * Создает исключение для ошибки оптимистичной блокировки.
         *
         * @param entityType      тип сущности
         * @param id              идентификатор сущности
         * @param expectedVersion ожидаемая версия
         * @param actualVersion   фактическая версия
         */
        public OptimisticLockException(String entityType, Long id, Long expectedVersion, Long actualVersion) {
            super(
                    "OPTIMISTIC_LOCK",
                    "Данные были изменены другим пользователем. Пожалуйста, обновите страницу и попробуйте снова.",
                    "Optimistic lock exception for " + entityType + " with id " + id +
                    ". Expected version: " + expectedVersion + ", actual version: " + actualVersion,
                    new Object[]{entityType, id, expectedVersion, actualVersion},
                    null
            );
        }

        /**
         * Создает исключение с пользовательским сообщением.
         *
         * @param message сообщение об ошибке
         */
        public OptimisticLockException(String message) {
            super(
                    "OPTIMISTIC_LOCK",
                    message,
                    "Optimistic lock exception: " + message,
                    null,
                    null
            );
        }

        /**
         * Создает исключение с сообщением и причиной.
         *
         * @param message сообщение об ошибке
         * @param cause   причина исключения
         */
        public OptimisticLockException(String message, Throwable cause) {
            super(
                    "OPTIMISTIC_LOCK",
                    message,
                    "Optimistic lock exception: " + message,
                    null,
                    cause
            );
        }
    }

    // ============================================================
    // Исключения технического характера
    // ============================================================

    /**
     * Ошибка парсинга.
     *
     * <p>Выбрасывается при ошибках парсинга данных с внешних сайтов.
     */
    public static final class ParsingException extends PlatformException {

        /**
         * Создает исключение для ошибки парсинга.
         *
         * @param url   URL источника данных
         * @param cause причина исключения
         */
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
     * Ошибка валидации.
     *
     * <p>Выбрасывается при несоответствии входных данных требованиям валидации.
     */
    public static final class ValidationException extends PlatformException {

        /**
         * Создает исключение для ошибки валидации.
         *
         * @param message сообщение об ошибке
         */
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
}