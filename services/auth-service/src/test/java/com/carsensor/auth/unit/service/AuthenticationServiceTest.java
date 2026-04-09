package com.carsensor.auth.unit.service;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import com.carsensor.auth.application.service.AuthenticationService;
import com.carsensor.auth.application.service.impl.AuthenticationServiceImpl;
import com.carsensor.auth.application.service.internal.UserInternalService;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.infrastructure.security.JwtTokenProvider;
import com.carsensor.platform.dto.LoginRequest;
import com.carsensor.platform.exception.PlatformException;
import com.carsensor.platform.exception.PlatformException.AccessDeniedException;
import com.carsensor.platform.exception.PlatformException.InvalidCredentialsException;
import com.carsensor.platform.exception.PlatformException.InvalidTokenException;
import com.carsensor.platform.exception.PlatformException.InvalidTokenFormatException;
import com.carsensor.platform.exception.PlatformException.MissingTokenException;
import com.carsensor.platform.exception.PlatformException.TokenExpiredException;
import com.carsensor.platform.exception.PlatformException.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты для сервиса аутентификации.
 *
 * <p>Проверяют работу методов:
 * <ul>
 *   <li>{@link AuthenticationService#authenticate(LoginRequest)}</li>
 *   <li>{@link AuthenticationService#refreshToken(String, HttpServletRequest)}</li>
 *   <li>{@link AuthenticationService#logout(String, HttpServletRequest)}</li>
 *   <li>{@link AuthenticationService#validateToken(String)}</li>
 *   <li>{@link AuthenticationService#getCurrentUser()}</li>
 * </ul>
 *
 * <p><b>Особенности:</b>
 * <ul>
 *   <li>Использует Mockito для изоляции зависимостей</li>
 *   <li>Java 21 features: {@code var} и текстовые блоки</li>
 *   <li>Полное покрытие всех сценариев</li>
 * </ul>
 *
 * @see AuthenticationServiceImpl
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса аутентификации")
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationServiceTest {

    // ============================================================
    // КОНСТАНТЫ ТЕСТОВЫХ ДАННЫХ
    // ============================================================

    /**
     * Имя тестового пользователя
     */
    private static final String TEST_USERNAME = "admin";

    /**
     * Пароль тестового пользователя
     */
    private static final String TEST_PASSWORD = "admin123";

    /**
     * Email тестового пользователя
     */
    private static final String TEST_EMAIL = "admin@test.com";

    /**
     * Имя роли пользователя
     */
    private static final String ROLE_USER = "ROLE_USER";

    /**
     * Пример access токена
     */
    private static final String ACCESS_TOKEN = "access.jwt.token";

    /**
     * Пример refresh токена
     */
    private static final String REFRESH_TOKEN = "refresh.jwt.token";

    /**
     * Валидный refresh токен
     */
    private static final String VALID_REFRESH_TOKEN = "valid.refresh.token";

    /**
     * Новый access токен
     */
    private static final String NEW_ACCESS_TOKEN = "new.access.token";

    /**
     * Время жизни токена в секундах
     */
    private static final long EXPIRES_IN = 900L;

    /**
     * Префикс Bearer для токена
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Валидный токен с Bearer префиксом
     */
    private static final String VALID_BEARER_TOKEN = BEARER_PREFIX + "valid.jwt.token";

    /**
     * Значение токена без префикса
     */
    private static final String TOKEN_VALUE = "valid.jwt.token";

    // ============================================================
    // ПОЛЯ И ЗАВИСИМОСТИ
    // ============================================================

    /**
     * Мок менеджера аутентификации Spring Security
     */
    @Mock
    private AuthenticationManager authenticationManager;

    /**
     * Мок провайдера JWT токенов
     */
    @Mock
    private JwtTokenProvider tokenProvider;

    /**
     * Мок внутреннего сервиса пользователей (для поиска entity)
     */
    @Mock
    private UserInternalService userInternalService;

    /**
     * Мок HTTP запроса (для получения IP адреса)
     */
    @Mock
    private HttpServletRequest httpServletRequest;

    /**
     * Мок объекта аутентификации Spring Security
     */
    @Mock
    private Authentication authentication;

    /**
     * Захватчик аргументов для токена аутентификации
     */
    @Captor
    private ArgumentCaptor<UsernamePasswordAuthenticationToken> authTokenCaptor;

    /**
     * Тестируемый сервис
     */
    private AuthenticationService authenticationService;

    /**
     * Валидный запрос на вход
     */
    private LoginRequest validLoginRequest;

    /**
     * Тестовый пользователь
     */
    private User testUser;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    /**
     * Инициализация тестовых данных перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

        var userRole = Role.builder()
                .id(1L)
                .name(ROLE_USER)
                .build();

        testUser = User.builder()
                .id(1L)
                .username(TEST_USERNAME)
                .password("encodedPassword")
                .email(TEST_EMAIL)
                .isActive(true)
                .roles(Set.of(userRole))
                .build();

        authenticationService = new AuthenticationServiceImpl(
                tokenProvider,
                userInternalService,
                authenticationManager
        );
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    // ============================================================
    // ТЕСТЫ МЕТОДА authenticate()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода authenticate() - аутентификация пользователя")
    class AuthenticateTests {

        @Test
        @DisplayName("✅ Успешная аутентификация с валидными credentials")
        void authenticate_ValidCredentials_ReturnsAuthResponse() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(tokenProvider.generateAccessToken(testUser)).thenReturn(ACCESS_TOKEN);
            when(tokenProvider.generateRefreshToken(testUser)).thenReturn(REFRESH_TOKEN);
            when(tokenProvider.getAccessTokenValidityInSeconds()).thenReturn(EXPIRES_IN);

            var response = authenticationService.authenticate(validLoginRequest);

            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.accessToken()).isEqualTo(ACCESS_TOKEN);
                        assertThat(r.refreshToken()).isEqualTo(REFRESH_TOKEN);
                        assertThat(r.expiresIn()).isEqualTo(EXPIRES_IN);
                        assertThat(r.username()).isEqualTo(TEST_USERNAME);
                        assertThat(r.roles()).containsExactly(ROLE_USER);
                    });

            verify(authenticationManager).authenticate(authTokenCaptor.capture());
            var capturedToken = authTokenCaptor.getValue();
            assertThat(capturedToken.getPrincipal()).isEqualTo(TEST_USERNAME);
            assertThat(capturedToken.getCredentials()).isEqualTo(TEST_PASSWORD);
        }

        @Test
        @DisplayName("❌ Аутентификация с неверным паролем выбрасывает InvalidCredentialsException")
        void authenticate_InvalidPassword_ThrowsException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .extracting("errorCode")
                    .isEqualTo("INVALID_CREDENTIALS");

            verify(authenticationManager).authenticate(any());
            verifyNoInteractions(tokenProvider);
        }

        @Test
        @DisplayName("❌ Аутентификация с неактивным пользователем выбрасывает AccessDeniedException")
        void authenticate_InactiveUser_ThrowsException() {
            testUser = User.builder()
                    .id(1L)
                    .username(TEST_USERNAME)
                    .password("encodedPassword")
                    .email(TEST_EMAIL)
                    .isActive(false)
                    .roles(Set.of(Role.builder().id(1L).name(ROLE_USER).build()))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Учетная запись деактивирована");

            verify(tokenProvider, never()).generateAccessToken(any());
            verify(tokenProvider, never()).generateRefreshToken(any());
        }

        @Test
        @DisplayName("❌ Аутентификация с DisabledException выбрасывает AccessDeniedException")
        void authenticate_DisabledException_ThrowsAccessDeniedException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("User is disabled"));

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Учетная запись деактивирована");
        }

        @Test
        @DisplayName("❌ Аутентификация с заблокированным пользователем (isLocked) выбрасывает UserBlockedException")
        void authenticate_LockedUser_ThrowsUserBlockedException() {
            testUser = User.builder()
                    .id(1L)
                    .username(TEST_USERNAME)
                    .password("encodedPassword")
                    .email(TEST_EMAIL)
                    .isActive(true)
                    .isLocked(true)
                    .roles(Set.of(Role.builder().id(1L).name(ROLE_USER).build()))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(PlatformException.UserBlockedException.class)
                    .hasMessageContaining("User account is blocked: admin");
        }
    }

    // ============================================================
    // ТЕСТЫ МЕТОДА refreshToken()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода refreshToken() - обновление токена")
    class RefreshTokenTests {

        @Test
        @DisplayName("✅ Успешное обновление токена с валидным refresh token")
        void refreshToken_ValidToken_ReturnsNewAccessToken() {
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(tokenProvider.validateToken(TOKEN_VALUE)).thenReturn(true);
            when(tokenProvider.extractUsername(TOKEN_VALUE)).thenReturn(TEST_USERNAME);
            when(userInternalService.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
            when(tokenProvider.generateAccessToken(testUser)).thenReturn(NEW_ACCESS_TOKEN);
            when(tokenProvider.getAccessTokenValidityInSeconds()).thenReturn(EXPIRES_IN);

            var response = authenticationService.refreshToken(VALID_BEARER_TOKEN, httpServletRequest);

            assertThat(response)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
                        assertThat(r.refreshToken()).isEqualTo(TOKEN_VALUE);
                        assertThat(r.username()).isEqualTo(TEST_USERNAME);
                    });

            verify(tokenProvider).validateToken(TOKEN_VALUE);
            verify(tokenProvider).extractUsername(TOKEN_VALUE);
            verify(userInternalService).findUserEntityByUsername(TEST_USERNAME);
        }

        @Test
        @DisplayName("❌ Обновление с невалидным refresh token выбрасывает InvalidTokenException")
        void refreshToken_InvalidToken_ThrowsException() {
            var invalidToken = "Bearer invalid.token";
            var expectedMessage = "Невалидный refresh token";

            doThrow(new InvalidTokenException(expectedMessage))
                    .when(tokenProvider).validateToken("invalid.token");

            assertThatThrownBy(() -> authenticationService.refreshToken(invalidToken, httpServletRequest))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining(expectedMessage);

            verify(tokenProvider).validateToken("invalid.token");
            verifyNoMoreInteractions(tokenProvider);
            verifyNoInteractions(userInternalService);
        }

        @Test
        @DisplayName("❌ Обновление с пустым заголовком выбрасывает MissingTokenException")
        void refreshToken_EmptyHeader_ThrowsException() {
            assertThatThrownBy(() -> authenticationService.refreshToken(null, httpServletRequest))
                    .isInstanceOf(MissingTokenException.class)
                    .hasMessageContaining("Отсутствует заголовок авторизации");

            verifyNoInteractions(tokenProvider);
        }

        @Test
        @DisplayName("❌ Обновление с неверным форматом заголовка выбрасывает InvalidTokenFormatException")
        void refreshToken_InvalidHeaderFormat_ThrowsException() {
            var invalidHeader = "InvalidFormat token";

            assertThatThrownBy(() -> authenticationService.refreshToken(invalidHeader, httpServletRequest))
                    .isInstanceOf(InvalidTokenFormatException.class)
                    .hasMessageContaining("Неверный формат заголовка авторизации");
        }

        @Test
        @DisplayName("❌ Обновление с истекшим токеном выбрасывает TokenExpiredException")
        void refreshToken_ExpiredToken_ThrowsException() {
            var expiredToken = "Bearer expired.token";
            var expectedMessage = "Срок действия токена истек";

            doThrow(new TokenExpiredException(expectedMessage))
                    .when(tokenProvider).validateToken("expired.token");

            assertThatThrownBy(() -> authenticationService.refreshToken(expiredToken, httpServletRequest))
                    .isInstanceOf(TokenExpiredException.class)
                    .hasMessageContaining(expectedMessage);
        }

        @Test
        @DisplayName("❌ Обновление для несуществующего пользователя выбрасывает UserNotFoundException")
        void refreshToken_UserNotFound_ThrowsException() {
            var nonExistentUser = "nonexistent";

            when(tokenProvider.validateToken(TOKEN_VALUE)).thenReturn(true);
            when(tokenProvider.extractUsername(TOKEN_VALUE)).thenReturn(nonExistentUser);
            when(userInternalService.findUserEntityByUsername(nonExistentUser)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.refreshToken(VALID_BEARER_TOKEN, httpServletRequest))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("❌ Обновление для неактивного пользователя выбрасывает AccessDeniedException")
        void refreshToken_InactiveUser_ThrowsAccessDeniedException() {
            testUser = User.builder()
                    .id(1L)
                    .username(TEST_USERNAME)
                    .password("encodedPassword")
                    .email(TEST_EMAIL)
                    .isActive(false)
                    .roles(Set.of(Role.builder().id(1L).name(ROLE_USER).build()))
                    .build();

            when(tokenProvider.validateToken(TOKEN_VALUE)).thenReturn(true);
            when(tokenProvider.extractUsername(TOKEN_VALUE)).thenReturn(TEST_USERNAME);
            when(userInternalService.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authenticationService.refreshToken(VALID_BEARER_TOKEN, httpServletRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Учетная запись деактивирована");  // ✅ исправлено сообщение

            verify(tokenProvider, never()).generateAccessToken(any());
        }
    }

    // ============================================================
    // ТЕСТЫ МЕТОДА logout()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода logout() - выход из системы")
    class LogoutTests {

        @Test
        @DisplayName("✅ Logout с валидным токеном очищает контекст")
        void logout_ValidToken_ClearsContext() {
            authenticationService.logout(VALID_BEARER_TOKEN, httpServletRequest);
            var isValid = authenticationService.validateToken(VALID_BEARER_TOKEN);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("❌ Logout с null токеном выбрасывает MissingTokenException")
        void logout_NullToken_ThrowsException() {
            assertThatThrownBy(() -> authenticationService.logout(null, httpServletRequest))
                    .isInstanceOf(MissingTokenException.class)
                    .hasMessageContaining("Отсутствует или неверный формат токена авторизации");
        }

        @Test
        @DisplayName("❌ Logout с неверным форматом токена выбрасывает MissingTokenException")
        void logout_InvalidTokenFormat_ThrowsException() {
            assertThatThrownBy(() -> authenticationService.logout("invalid-format", httpServletRequest))
                    .isInstanceOf(MissingTokenException.class)
                    .hasMessageContaining("Отсутствует или неверный формат токена авторизации");
        }
    }

    // ============================================================
    // ТЕСТЫ МЕТОДА validateToken()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода validateToken() - валидация токена")
    class ValidateTokenTests {

        @Test
        @DisplayName("✅ Валидация валидного токена возвращает true")
        void validateToken_ValidToken_ReturnsTrue() {
            when(tokenProvider.validateToken(TOKEN_VALUE)).thenReturn(true);

            var result = authenticationService.validateToken(VALID_BEARER_TOKEN);

            assertThat(result).isTrue();
            verify(tokenProvider).validateToken(TOKEN_VALUE);
        }

        @Test
        @DisplayName("❌ Валидация невалидного токена возвращает false")
        void validateToken_InvalidToken_ReturnsFalse() {
            when(tokenProvider.validateToken("invalid.token")).thenReturn(false);

            var result = authenticationService.validateToken("Bearer invalid.token");

            assertThat(result).isFalse();
            verify(tokenProvider).validateToken("invalid.token");
        }

        @Test
        @DisplayName("❌ Валидация null токена возвращает false")
        void validateToken_NullToken_ReturnsFalse() {
            var result = authenticationService.validateToken(null);

            assertThat(result).isFalse();
            verifyNoInteractions(tokenProvider);
        }

        @Test
        @DisplayName("❌ Валидация токена без Bearer префикса возвращает false")
        void validateToken_WithoutBearerPrefix_ReturnsFalse() {
            var result = authenticationService.validateToken("token.without.bearer");

            assertThat(result).isFalse();
            verifyNoInteractions(tokenProvider);
        }
    }

    // ============================================================
    // ТЕСТЫ МЕТОДА getCurrentUser()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода getCurrentUser() - получение текущего пользователя")
    class GetCurrentUserTests {

        @Test
        @DisplayName("✅ Получение текущего пользователя с аутентификацией")
        void getCurrentUser_WithAuthentication_ReturnsUserDto() {
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(authentication.isAuthenticated()).thenReturn(true);

            org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            var result = authenticationService.getCurrentUser();

            assertThat(result).isNotNull();
            assertThat(result.username()).isEqualTo(TEST_USERNAME);
            assertThat(result.email()).isEqualTo(TEST_EMAIL);

            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("❌ Получение текущего пользователя без аутентификации выбрасывает исключение")
        void getCurrentUser_NoAuthentication_ThrowsException() {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> authenticationService.getCurrentUser())
                    .isInstanceOf(PlatformException.UnauthorizedException.class)
                    .hasMessageContaining("Необходима аутентификация");
        }

        @Test
        @DisplayName("❌ Получение текущего пользователя с anonymousUser выбрасывает исключение")
        void getCurrentUser_AnonymousUser_ThrowsException() {
            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            when(authentication.isAuthenticated()).thenReturn(true);

            org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            assertThatThrownBy(() -> authenticationService.getCurrentUser())
                    .isInstanceOf(PlatformException.UnauthorizedException.class)
                    .hasMessageContaining("Необходима аутентификация");

            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}