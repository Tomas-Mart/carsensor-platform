package com.carsensor.auth.application.service.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
import com.carsensor.platform.dto.UserDto;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    @Override
    @Transactional
    public AuthResponse authenticate(LoginRequest loginRequest) {
        log.info("=== AUTHENTICATION START ===");
        log.info("Authenticating user: {}", loginRequest.username());
        log.info("Password length: {}", loginRequest.password().length());

        try {
            log.info("Creating authentication token...");
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()
            );
            log.info("Calling authenticationManager.authenticate()...");
            log.info("AuthenticationManager class: {}", authenticationManager.getClass().getName());
            Authentication authentication = authenticationManager.authenticate(authToken);
            log.info("Authentication successful for user: {}", loginRequest.username());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();

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
            log.error("Platform exception during authentication for user: {} - {}",
                    loginRequest.username(), e.getUserMessage());
            throw e;
        } catch (BadCredentialsException e) {
            log.error("Bad credentials for user: {}", loginRequest.username());
            throw new PlatformException.InvalidCredentialsException();
        } catch (DisabledException e) {
            log.error("User is disabled: {}", loginRequest.username());
            throw new PlatformException.AccessDeniedException(
                    loginRequest.username(), "Учетная запись деактивирована"
            );
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.username(), e);
            throw new PlatformException.InvalidCredentialsException();
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Обновление токена");

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Refresh token is null or empty");
            // Используем MissingTokenException как ожидают тесты
            throw new PlatformException.MissingTokenException("Токен не предоставлен");
        }

        if (blacklistedTokens.contains(refreshToken)) {
            log.warn("Refresh token находится в черном списке");
            throw new PlatformException.InvalidTokenException("Токен отозван");
        }

        try {
            tokenProvider.validateToken(refreshToken);
        } catch (PlatformException.TokenExpiredException e) {
            // Пробрасываем TokenExpiredException как ожидают тесты
            throw e;
        } catch (PlatformException.InvalidTokenFormatException e) {
            // Пробрасываем InvalidTokenFormatException как ожидают тесты
            throw e;
        } catch (Exception e) {
            log.error("Invalid refresh token", e);
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new PlatformException.UserNotFoundException(username));

        if (!user.isActive()) {
            log.warn("Пользователь заблокирован: {}", user.getUsername());
            // Используем AccessDeniedException как ожидают тесты
            throw new PlatformException.AccessDeniedException(
                    user.getUsername(), "Учетная запись заблокирована"
            );
        }

        String newAccessToken = tokenProvider.generateAccessToken(user);
        log.info("Токен успешно обновлен для пользователя: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
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
            blacklistedTokens.add(token);
            SecurityContextHolder.clearContext();
            log.debug("User logged out successfully");
        } else {
            log.warn("Invalid or missing access token for logout");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.debug("Token is null or empty");
            return false;
        }

        if (blacklistedTokens.contains(token)) {
            log.debug("Token is blacklisted");
            return false;
        }

        try {
            return tokenProvider.validateToken(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
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
            log.warn("Attempt to get current user without authentication");
            throw new PlatformException.UnauthorizedException("Необходима аутентификация");
        }

        try {
            User user = (User) authentication.getPrincipal();

            if (user == null) {
                log.warn("User principal is null");
                throw new PlatformException.UnauthorizedException("Необходима аутентификация");
            }

            return UserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .isActive(user.isActive())
                    .roles(user.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toList()))
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();

        } catch (ClassCastException e) {
            log.error("Authentication principal is not a User object", e);
            throw new PlatformException.UnauthorizedException("Неверный формат аутентификации");
        }
    }
}