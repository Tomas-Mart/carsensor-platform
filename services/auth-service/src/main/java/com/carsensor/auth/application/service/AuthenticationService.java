package com.carsensor.auth.application.service;

import com.carsensor.auth.application.dto.UserDto;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Интерфейс сервиса аутентификации.
 *
 * <p>Содержит бизнес-логику аутентификации, работы с JWT токенами
 * и управления сессиями пользователей.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
public interface AuthenticationService {

    /**
     * Аутентификация пользователя и получение JWT токенов.
     *
     * @param loginRequest запрос с логином и паролем
     * @return ответ с access_token и refresh_token
     */
    AuthResponse authenticate(LoginRequest loginRequest);

    /**
     * Обновление access token с использованием refresh token.
     *
     * <p>Выполняет валидацию refresh token, проверку его наличия в черном списке,
     * извлечение пользователя и генерацию нового access token.
     *
     * @param authHeader заголовок Authorization (содержит Bearer refresh_token)
     * @param request    HTTP запрос для логирования IP адреса
     * @return новые токены (access_token и тот же refresh_token)
     * @throws com.carsensor.platform.exception.PlatformException.MissingTokenException       если токен отсутствует
     * @throws com.carsensor.platform.exception.PlatformException.InvalidTokenFormatException если формат неверный
     * @throws com.carsensor.platform.exception.PlatformException.TokenExpiredException       если токен истек
     */
    AuthResponse refreshToken(String authHeader, HttpServletRequest request);

    /**
     * Выход пользователя из системы.
     *
     * <p>Добавляет access token в черный список и очищает контекст безопасности.
     *
     * @param authHeader заголовок Authorization (содержит Bearer access_token)
     * @param request    HTTP запрос для логирования IP адреса
     * @throws com.carsensor.platform.exception.PlatformException.MissingTokenException если токен отсутствует
     */
    void logout(String authHeader, HttpServletRequest request);

    /**
     * Проверка валидности JWT токена.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Наличие и формат заголовка</li>
     *   <li>Отсутствие токена в черном списке</li>
     *   <li>Подпись и срок действия токена</li>
     * </ul>
     *
     * @param authHeader заголовок Authorization (содержит Bearer token)
     * @return true если токен валиден, false в противном случае
     */
    boolean validateToken(String authHeader);

    /**
     * Получение информации о текущем аутентифицированном пользователе.
     *
     * <p>Извлекает пользователя из SecurityContextHolder и преобразует в DTO.
     *
     * @return DTO текущего пользователя
     * @throws com.carsensor.platform.exception.PlatformException.UnauthorizedException если пользователь не аутентифицирован
     */
    UserDto getCurrentUser();
}