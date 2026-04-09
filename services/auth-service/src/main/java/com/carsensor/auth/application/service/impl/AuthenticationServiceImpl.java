package com.carsensor.auth.application.service.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.dto.UserDto;
import com.carsensor.auth.application.service.AuthenticationService;
import com.carsensor.auth.application.service.internal.UserInternalService;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.infrastructure.security.JwtTokenProvider;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import com.carsensor.platform.exception.PlatformException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация сервиса аутентификации.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtTokenProvider tokenProvider;
    private final UserInternalService userInternalService;
    private final AuthenticationManager authenticationManager;

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    @Override
    @Transactional
    public AuthResponse authenticate(LoginRequest loginRequest) {
        log.info("Аутентификация пользователя: {}", loginRequest.username());

        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()
            );

            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = (User) authentication.getPrincipal();

            // Проверка на блокировку
            if (user.isLocked()) {
                log.warn("Пользователь заблокирован: {}", user.getUsername());
                throw new PlatformException.UserBlockedException(user.getUsername());
            }

            // Проверка на деактивацию
            if (!user.isActive()) {
                log.warn("Пользователь деактивирован: {}", user.getUsername());
                throw new PlatformException.AccessDeniedException(
                        user.getUsername(), "Учетная запись деактивирована"
                );
            }

            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);
            String[] rolesArray = user.getRoles()
                    .stream()
                    .map(Role::getName)
                    .toArray(String[]::new);

            log.info("Пользователь успешно аутентифицирован: {}", user.getUsername());

            return AuthResponse.of(
                    accessToken,
                    refreshToken,
                    tokenProvider.getAccessTokenValidityInSeconds(),
                    user.getUsername(),
                    rolesArray
            );

        } catch (PlatformException e) {
            throw e;
        } catch (BadCredentialsException e) {
            log.error("Неверные учетные данные для пользователя: {}", loginRequest.username());
            throw new PlatformException.InvalidCredentialsException();
        } catch (DisabledException e) {
            log.error("Пользователь деактивирован: {}", loginRequest.username());
            throw new PlatformException.AccessDeniedException(
                    loginRequest.username(), "Учетная запись деактивирована"
            );
        } catch (Exception e) {
            log.error("Ошибка аутентификации для пользователя: {}", loginRequest.username(), e);
            throw new PlatformException.InvalidCredentialsException();
        }
    }

    @Override
    public AuthResponse refreshToken(String authHeader, HttpServletRequest request) {
        log.debug("Обновление токена");

        // Валидация заголовка
        if (authHeader == null) {
            log.warn("Запрос на обновление токена без заголовка авторизации от IP: {}",
                    request.getRemoteAddr());
            throw new PlatformException.MissingTokenException("Отсутствует заголовок авторизации");
        }

        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Неверный формат заголовка авторизации от IP: {}", request.getRemoteAddr());
            throw new PlatformException.InvalidTokenFormatException(
                    "Неверный формат заголовка авторизации. Должен начинаться с 'Bearer '"
            );
        }

        String refreshToken = authHeader.substring(7);

        if (refreshToken.isBlank()) {
            log.warn("Refresh token пустой от IP: {}", request.getRemoteAddr());
            throw new PlatformException.MissingTokenException("Токен не предоставлен");
        }

        if (blacklistedTokens.contains(refreshToken)) {
            log.warn("Refresh token находится в черном списке от IP: {}", request.getRemoteAddr());
            throw new PlatformException.InvalidTokenException("Токен отозван");
        }

        try {
            tokenProvider.validateToken(refreshToken);
        } catch (PlatformException.TokenExpiredException e) {
            throw e;
        } catch (PlatformException.InvalidTokenFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Невалидный refresh token", e);
            String message = e.getMessage();
            if (message != null && message.contains("expired")) {
                throw new PlatformException.TokenExpiredException("Токен истек");
            } else if (message != null && message.contains("format")) {
                throw new PlatformException.InvalidTokenFormatException("Неверный формат токена");
            } else {
                throw new PlatformException.InvalidTokenException("Невалидный refresh token");
            }
        }

        String username = tokenProvider.extractUsername(refreshToken);
        User user = userInternalService.findUserEntityByUsername(username)
                .orElseThrow(() -> new PlatformException.UserNotFoundException(username));

        if (!user.isActive()) {
            log.warn("Пользователь деактивирован: {}", user.getUsername());
            throw new PlatformException.AccessDeniedException(
                    user.getUsername(), "Учетная запись деактивирована"
            );
        }

        String newAccessToken = tokenProvider.generateAccessToken(user);
        String[] rolesArray = user.getRoles()
                .stream()
                .map(Role::getName)
                .toArray(String[]::new);

        log.info("Токен успешно обновлен для пользователя: {}", user.getUsername());

        return AuthResponse.of(
                newAccessToken,
                refreshToken,
                tokenProvider.getAccessTokenValidityInSeconds(),
                user.getUsername(),
                rolesArray
        );
    }

    @Override
    public void logout(String authHeader, HttpServletRequest request) {
        log.debug("Выход из системы");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Невалидный токен при выходе от IP: {}", request.getRemoteAddr());
            throw new PlatformException.MissingTokenException(
                    "Отсутствует или неверный формат токена авторизации"
            );
        }

        String token = authHeader.substring(7);
        blacklistedTokens.add(token);
        SecurityContextHolder.clearContext();
        log.debug("Пользователь успешно вышел из системы");
    }

    @Override
    public boolean validateToken(String authHeader) {
        // Проверка наличия и формата заголовка
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Неверный формат заголовка для проверки токена");
            return false;
        }

        // Извлечение токена
        String token = authHeader.substring(7);

        if (token.isBlank()) {
            log.debug("Пустой токен для проверки");
            return false;
        }

        // Проверка черного списка
        if (blacklistedTokens.contains(token)) {
            log.debug("Токен находится в черном списке");
            return false;
        }

        // Валидация токена
        try {
            return tokenProvider.validateToken(token);
        } catch (Exception e) {
            log.debug("Ошибка валидации токена: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
            !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Попытка получить текущего пользователя без аутентификации");
            throw new PlatformException.UnauthorizedException("Необходима аутентификация");
        }

        try {
            User user = (User) authentication.getPrincipal();

            if (user == null) {
                log.warn("Principal пользователя равен null");
                throw new PlatformException.UnauthorizedException("Необходима аутентификация");
            }

            return UserDto.from(user);

        } catch (ClassCastException e) {
            log.error("Principal не является объектом User", e);
            throw new PlatformException.UnauthorizedException("Неверный формат аутентификации");
        }
    }
}