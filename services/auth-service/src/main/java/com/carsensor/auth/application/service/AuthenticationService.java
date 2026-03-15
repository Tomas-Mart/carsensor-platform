package com.carsensor.auth.application.service;

import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;

/**
 * Интерфейс сервиса аутентификации
 */
public interface AuthenticationService {

    /**
     * Аутентификация пользователя и получение JWT токенов
     *
     * @param loginRequest запрос с логином и паролем
     * @return ответ с токенами
     */
    AuthResponse authenticate(LoginRequest loginRequest);

    /**
     * Обновление access token с использованием refresh token
     *
     * @param refreshToken refresh token
     * @return новые токены
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Выход пользователя из системы
     *
     * @param accessToken access token для инвалидации
     */
    void logout(String accessToken);

    /**
     * Проверка валидности токена
     *
     * @param token JWT токен
     * @return true если токен валиден
     */
    boolean validateToken(String token);

    /**
     * Получение информации о текущем аутентифицированном пользователе
     *
     * @return информация о пользователе
     */
    CurrentUser getCurrentUser();

    /**
     * Информация о текущем пользователе
     */
    record CurrentUser(
            Long id,
            String username,
            String email,
            String[] roles
    ) {
    }
}