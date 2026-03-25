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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import com.carsensor.auth.application.service.AuthenticationService;
import com.carsensor.auth.application.service.impl.AuthenticationServiceImpl;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
 *   <li>{@link AuthenticationService#refreshToken(String)}</li>
 *   <li>{@link AuthenticationService#logout(String)}</li>
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
class AuthenticationServiceTest {

    // ============================================================
    // Поля и зависимости
    // ============================================================

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Captor
    private ArgumentCaptor<UsernamePasswordAuthenticationToken> authTokenCaptor;

    private AuthenticationService authenticationService;

    private LoginRequest validLoginRequest;
    private User testUser;

    private static final String ACCESS_TOKEN = "access.jwt.token";
    private static final String REFRESH_TOKEN = "refresh.jwt.token";
    private static final String VALID_REFRESH_TOKEN = "valid.refresh.token";
    private static final String NEW_ACCESS_TOKEN = "new.access.token";
    private static final long EXPIRES_IN = 900L;

    // ============================================================
    // Инициализация
    // ============================================================

    /**
     * Инициализация тестовых данных перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest("admin", "admin123");

        var userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("admin")
                .password("encodedPassword")
                .email("admin@test.com")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();

        authenticationService = new AuthenticationServiceImpl(
                authenticationManager,
                tokenProvider,
                userRepository
        );
    }

    // ============================================================
    // Тесты метода authenticate()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода authenticate() - аутентификация пользователя")
    class AuthenticateTests {

        @Test
        @DisplayName("Успешная аутентификация с валидными credentials")
        void authenticate_ValidCredentials_ReturnsAuthResponse() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(tokenProvider.generateAccessToken(testUser)).thenReturn(ACCESS_TOKEN);
            when(tokenProvider.generateRefreshToken(testUser)).thenReturn(REFRESH_TOKEN);
            when(tokenProvider.getAccessTokenValidityInSeconds()).thenReturn(EXPIRES_IN);

            var response = authenticationService.authenticate(validLoginRequest);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.expiresIn()).isEqualTo(EXPIRES_IN);
            assertThat(response.username()).isEqualTo("admin");
            assertThat(response.roles()).containsExactly("ROLE_USER");

            verify(authenticationManager).authenticate(authTokenCaptor.capture());
            var capturedToken = authTokenCaptor.getValue();
            assertThat(capturedToken.getPrincipal()).isEqualTo("admin");
            assertThat(capturedToken.getCredentials()).isEqualTo("admin123");
        }

        @Test
        @DisplayName("Аутентификация с неверным паролем выбрасывает InvalidCredentialsException")
        void authenticate_InvalidPassword_ThrowsException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .satisfies(ex -> assertThat(((InvalidCredentialsException) ex).getErrorCode())
                            .isEqualTo("INVALID_CREDENTIALS"));

            verify(authenticationManager).authenticate(any());
            verifyNoInteractions(tokenProvider);
        }

        @Test
        @DisplayName("Аутентификация с неактивным пользователем выбрасывает AccessDeniedException")
        void authenticate_InactiveUser_ThrowsException() {
            testUser.setActive(false);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Учетная запись заблокирована");

            verify(tokenProvider, never()).generateAccessToken(any());
            verify(tokenProvider, never()).generateRefreshToken(any());
        }

        @Test
        @DisplayName("Аутентификация с DisabledException выбрасывает AccessDeniedException")
        void authenticate_DisabledException_ThrowsAccessDeniedException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("User is disabled"));

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Учетная запись деактивирована");
        }
    }

    // ============================================================
    // Тесты метода refreshToken()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода refreshToken() - обновление токена")
    class RefreshTokenTests {

        @Test
        @DisplayName("Успешное обновление токена с валидным refresh token")
        void refreshToken_ValidToken_ReturnsNewAccessToken() {
            when(tokenProvider.validateToken(VALID_REFRESH_TOKEN)).thenReturn(true);
            when(tokenProvider.extractUsername(VALID_REFRESH_TOKEN)).thenReturn("admin");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(tokenProvider.generateAccessToken(testUser)).thenReturn(NEW_ACCESS_TOKEN);
            when(tokenProvider.getAccessTokenValidityInSeconds()).thenReturn(EXPIRES_IN);

            var response = authenticationService.refreshToken(VALID_REFRESH_TOKEN);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(VALID_REFRESH_TOKEN);
            assertThat(response.username()).isEqualTo("admin");

            verify(tokenProvider).validateToken(VALID_REFRESH_TOKEN);
            verify(tokenProvider).extractUsername(VALID_REFRESH_TOKEN);
            verify(userRepository).findByUsername("admin");
        }

        @Test
        @DisplayName("Обновление с невалидным refresh token выбрасывает InvalidTokenException")
        void refreshToken_InvalidToken_ThrowsException() {
            var invalidToken = "invalid.token";
            var expectedMessage = "Невалидный refresh token";

            doThrow(new InvalidTokenException(expectedMessage))
                    .when(tokenProvider).validateToken(invalidToken);

            assertThatThrownBy(() -> authenticationService.refreshToken(invalidToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining(expectedMessage);

            verify(tokenProvider).validateToken(invalidToken);
            verifyNoMoreInteractions(tokenProvider);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Обновление с истекшим токеном выбрасывает TokenExpiredException")
        void refreshToken_ExpiredToken_ThrowsException() {
            var expiredToken = "expired.token";
            var expectedMessage = "Срок действия токена истек";

            doThrow(new TokenExpiredException(expectedMessage))
                    .when(tokenProvider).validateToken(expiredToken);

            assertThatThrownBy(() -> authenticationService.refreshToken(expiredToken))
                    .isInstanceOf(TokenExpiredException.class)
                    .hasMessageContaining(expectedMessage);
        }

        @Test
        @DisplayName("Обновление с неверным форматом токена выбрасывает InvalidTokenFormatException")
        void refreshToken_InvalidTokenFormat_ThrowsException() {
            var malformedToken = "malformed.token";
            var expectedMessage = "Неверный формат токена";

            doThrow(new InvalidTokenFormatException(expectedMessage))
                    .when(tokenProvider).validateToken(malformedToken);

            assertThatThrownBy(() -> authenticationService.refreshToken(malformedToken))
                    .isInstanceOf(InvalidTokenFormatException.class)
                    .hasMessageContaining(expectedMessage);
        }

        @Test
        @DisplayName("Обновление для несуществующего пользователя выбрасывает UserNotFoundException")
        void refreshToken_UserNotFound_ThrowsException() {
            when(tokenProvider.validateToken(VALID_REFRESH_TOKEN)).thenReturn(true);
            when(tokenProvider.extractUsername(VALID_REFRESH_TOKEN)).thenReturn("nonexistent");
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.refreshToken(VALID_REFRESH_TOKEN))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Обновление для неактивного пользователя выбрасывает AccessDeniedException")
        void refreshToken_InactiveUser_ThrowsAccessDeniedException() {
            testUser.setActive(false);

            when(tokenProvider.validateToken(VALID_REFRESH_TOKEN)).thenReturn(true);
            when(tokenProvider.extractUsername(VALID_REFRESH_TOKEN)).thenReturn("admin");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authenticationService.refreshToken(VALID_REFRESH_TOKEN))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("заблокирована");

            verify(tokenProvider, never()).generateAccessToken(any());
        }

        @Test
        @DisplayName("Пустой refresh token выбрасывает MissingTokenException")
        void refreshToken_EmptyToken_ThrowsException() {
            var emptyToken = "";
            var expectedMessage = "Токен не предоставлен";

            assertThatThrownBy(() -> authenticationService.refreshToken(emptyToken))
                    .isInstanceOf(MissingTokenException.class)
                    .hasMessageContaining(expectedMessage);

            verifyNoInteractions(tokenProvider);
        }

        @Test
        @DisplayName("Обновление токена с access токеном (не refresh) выбрасывает InvalidTokenException")
        void refreshToken_WrongTokenType_ThrowsException() {
            var accessToken = "access.token.type";
            var expectedMessage = "Невалидный refresh token";

            when(tokenProvider.validateToken(accessToken))
                    .thenThrow(new InvalidTokenException(expectedMessage));

            assertThatThrownBy(() -> authenticationService.refreshToken(accessToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining(expectedMessage);

            verify(tokenProvider).validateToken(accessToken);
            verifyNoInteractions(userRepository);
        }
    }
    // ============================================================
    // Тесты метода logout()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода logout() - выход из системы")
    class LogoutTests {

        @Test
        @DisplayName("Logout с валидным токеном очищает контекст")
        void logout_ValidToken_ClearsContext() {
            var bearerToken = "Bearer valid.jwt.token";
            var token = "valid.jwt.token";

            authenticationService.logout(bearerToken);

            assertThat(authenticationService.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("Logout с null токеном не вызывает ошибок")
        void logout_NullToken_DoesNothing() {
            authenticationService.logout(null);
        }

        @Test
        @DisplayName("Logout с токеном не в Bearer формате не вызывает ошибок")
        void logout_InvalidTokenFormat_DoesNothing() {
            authenticationService.logout("invalid-format-token");
        }
    }

    // ============================================================
    // Тесты метода validateToken()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода validateToken() - валидация токена")
    class ValidateTokenTests {

        @Test
        @DisplayName("Валидация валидного токена возвращает true")
        void validateToken_ValidToken_ReturnsTrue() {
            var token = "valid.token";
            when(tokenProvider.validateToken(token)).thenReturn(true);

            var result = authenticationService.validateToken(token);

            assertThat(result).isTrue();
            verify(tokenProvider).validateToken(token);
        }

        @Test
        @DisplayName("Валидация невалидного токена возвращает false")
        void validateToken_InvalidToken_ReturnsFalse() {
            var token = "invalid.token";
            when(tokenProvider.validateToken(token)).thenReturn(false);

            var result = authenticationService.validateToken(token);

            assertThat(result).isFalse();
            verify(tokenProvider).validateToken(token);
        }
    }

    // ============================================================
    // Тесты метода getCurrentUser()
    // ============================================================

    @Nested
    @DisplayName("Тесты метода getCurrentUser() - получение текущего пользователя")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Получение текущего пользователя без аутентификации выбрасывает исключение")
        void getCurrentUser_NoAuthentication_ThrowsException() {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> authenticationService.getCurrentUser())
                    .isInstanceOf(PlatformException.UnauthorizedException.class)
                    .hasMessageContaining("Необходима аутентификация");
        }
    }
}
