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
import com.carsensor.auth.application.service.AuthenticationService;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.repository.UserRepository;
import com.carsensor.auth.infrastructure.security.JwtTokenProvider;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация сервиса аутентификации
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    // Хранилище инвалидированных токенов (в реальном проекте использовать Redis)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public AuthResponse authenticate(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.username());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();

            // Проверка активности пользователя
            if (!user.isActive()) {
                log.warn("User is blocked: {}", user.getUsername());
                throw new PlatformException.AccessDeniedException(
                        user.getUsername(), "Учетная запись заблокирована"
                );
            }

            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            log.info("User authenticated successfully: {}", user.getUsername());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(tokenProvider.getAccessTokenValidityInSeconds())
                    .username(user.getUsername())
                    .roles(user.getRoles().stream()
                            .map(Role::getName)
                            .toArray(String[]::new))
                    .build();

        } catch (PlatformException e) {
            throw e;
        } catch (BadCredentialsException e) {
            log.error("Bad credentials for user: {}", loginRequest.username());
            throw new PlatformException.InvalidCredentialsException();
        } catch (DisabledException e) { // <-- ДОБАВИТЬ конкретный catch
            log.error("User is disabled: {}", loginRequest.username());
            throw new PlatformException.AccessDeniedException(
                    loginRequest.username(), "Учетная запись деактивирована"
            );
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.username(), e);
            throw new PlatformException.InvalidCredentialsException();
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Обновление токена");

        // Проверка не в черном ли списке
        if (blacklistedTokens.contains(refreshToken)) {
            log.warn("Refresh token находится в черном списке");
            throw new PlatformException.InvalidTokenException("Токен отозван");
        }

        // Валидация токена - JwtTokenProvider сам кидает нужные исключения
        tokenProvider.validateToken(refreshToken);

        String username = tokenProvider.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new PlatformException.UserNotFoundException(username));

        // Проверка активности пользователя
        if (!user.isActive()) {
            log.warn("Пользователь заблокирован: {}", user.getUsername());
            throw new PlatformException.AccessDeniedException(
                    user.getUsername(), "Учетная запись заблокирована"
            );
        }

        String newAccessToken = tokenProvider.generateAccessToken(user);
        log.info("Токен успешно обновлен для пользователя: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // <-- ВОПРОС: может генерировать новый refresh token?
                .expiresIn(tokenProvider.getAccessTokenValidityInSeconds())
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .toArray(String[]::new))
                .build();
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        log.debug("Logging out user");

        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            String token = accessToken.substring(7);

            // Добавляем токен в черный список
            blacklistedTokens.add(token);

            // Очищаем контекст безопасности
            SecurityContextHolder.clearContext();

            log.debug("User logged out successfully");
        }
    }

    @Override
    public boolean validateToken(String token) {
        // Проверка не в черном ли списке
        if (blacklistedTokens.contains(token)) {
            log.debug("Token is blacklisted");
            return false;
        }

        return tokenProvider.validateToken(token);
    }

    @Override
    public CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new PlatformException.InvalidCredentialsException();
        }

        User user = (User) authentication.getPrincipal();

        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toArray(String[]::new)
        );
    }
}