package com.carsensor.auth.interfaces.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.carsensor.auth.application.dto.UserDto;
import com.carsensor.auth.application.service.AuthenticationService;
import com.carsensor.auth.application.service.command.UserCommandService;
import com.carsensor.platform.dto.AuthResponse;
import com.carsensor.platform.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Контроллер для аутентификации и регистрации пользователей.
 *
 * <p>Тонкий контроллер - вся бизнес-логика делегируется сервисам.
 * Предоставляет endpoints для:
 * <ul>
 *   <li>Входа в систему (JWT токены)</li>
 *   <li>Регистрации новых пользователей</li>
 *   <li>Обновления токенов</li>
 *   <li>Выхода из системы</li>
 *   <li>Получения информации о текущем пользователе</li>
 *   <li>Проверки валидности токена</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API для аутентификации пользователей")
public class AuthController {

    private final UserCommandService userCommandService;
    private final AuthenticationService authenticationService;

    /**
     * Аутентификация пользователя и получение JWT токенов.
     *
     * @param loginRequest запрос с логином и паролем
     * @return ответ с access_token и refresh_token
     */
    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Аутентификация пользователя и получение JWT токенов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.debug("Запрос на вход для пользователя: {}", loginRequest.username());
        AuthResponse response = authenticationService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Регистрация нового пользователя.
     *
     * @param userDto данные для регистрации
     * @return созданный пользователь
     */
    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
    })
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserDto userDto) {
        log.debug("Запрос на регистрацию пользователя: {}", userDto.username());
        UserDto createdUser = userCommandService.register(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Обновление access token с использованием refresh token.
     *
     * @param authHeader заголовок Authorization с Bearer refresh_token
     * @param request    HTTP запрос для получения IP
     * @return новые токены
     */
    @PostMapping("/refresh")
    @Operation(summary = "Обновление токена", description = "Получение нового access token по refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токен успешно обновлен"),
            @ApiResponse(responseCode = "401", description = "Невалидный или отсутствующий refresh token")
    })
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest request
    ) {
        log.debug("Запрос на обновление токена");
        AuthResponse response = authenticationService.refreshToken(authHeader, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Выход пользователя из системы (инвалидация токена).
     *
     * @param authHeader заголовок Authorization с Bearer access_token
     * @param request    HTTP запрос для получения IP
     * @return пустой ответ
     */
    @PostMapping("/logout")
    @Operation(summary = "Выход из системы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный выход"),
            @ApiResponse(responseCode = "401", description = "Отсутствует или невалидный токен")
    })
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest request
    ) {
        log.debug("Запрос на выход из системы");
        authenticationService.logout(authHeader, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Получение информации о текущем аутентифицированном пользователе.
     *
     * @return данные текущего пользователя
     */
    @GetMapping("/me")
    @Operation(summary = "Информация о текущем пользователе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение данных"),
            @ApiResponse(responseCode = "401", description = "Необходима аутентификация")
    })
    public ResponseEntity<UserDto> getCurrentUser() {
        log.debug("Запрос на получение текущего пользователя");
        UserDto currentUser = authenticationService.getCurrentUser();
        return ResponseEntity.ok(currentUser);
    }

    /**
     * Проверка валидности JWT токена.
     *
     * @param authHeader заголовок Authorization с Bearer token
     * @return true если токен валиден, false в противном случае
     */
    @PostMapping("/validate")
    @Operation(summary = "Проверка валидности токена")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Проверка выполнена")
    })
    public ResponseEntity<Boolean> validateToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        log.debug("Запрос на проверку токена");
        boolean isValid = authenticationService.validateToken(authHeader);
        return ResponseEntity.ok(isValid);
    }
}