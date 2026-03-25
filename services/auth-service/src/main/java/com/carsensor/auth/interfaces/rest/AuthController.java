package com.carsensor.auth.interfaces.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.carsensor.auth.application.service.AuthenticationService;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import com.carsensor.platform.dto.UserDto;
import com.carsensor.platform.exception.PlatformException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API для аутентификации пользователей")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Аутентификация пользователя и получение JWT токенов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.debug("Login request received for user: {}", loginRequest.username());
        AuthResponse response = authenticationService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токена", description = "Получение нового access token по refresh token")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest request) {
        log.debug("Запрос на обновление токена");

        if (authHeader == null) {
            log.warn("Запрос на /refresh без заголовка авторизации от IP: {}", request.getRemoteAddr());
            throw new PlatformException.MissingTokenException("Отсутствует заголовок авторизации");
        }

        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Неверный формат заголовка авторизации от IP: {}", request.getRemoteAddr());
            throw new PlatformException.InvalidTokenFormatException("Неверный формат заголовка авторизации. Должен начинаться с 'Bearer '");
        }

        String refreshToken = authHeader.substring(7);
        AuthResponse response = authenticationService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest request) {
        log.debug("Запрос на выход из системы");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Невалидный токен при выходе от IP: {}", request.getRemoteAddr());
            throw new PlatformException.MissingTokenException("Отсутствует или неверный формат токена авторизации");
        }

        authenticationService.logout(authHeader);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Информация о текущем пользователе")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized access to /me endpoint");
            throw new PlatformException.UnauthorizedException("Необходима аутентификация для доступа к ресурсу");
        }

        UserDto currentUser = authenticationService.getCurrentUser();
        return ResponseEntity.ok(currentUser);
    }

    @PostMapping("/validate")
    @Operation(summary = "Проверка валидности токена")
    public ResponseEntity<Boolean> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean isValid = authenticationService.validateToken(token);
        return ResponseEntity.ok(isValid);
    }
}