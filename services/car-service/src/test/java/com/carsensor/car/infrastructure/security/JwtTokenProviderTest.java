package com.carsensor.car.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import com.carsensor.platform.exception.PlatformException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты для JwtTokenProvider (car-service).
 *
 * <p><b>Проверяемые аспекты:</b>
 * <ul>
 *   <li>Валидация JWT токенов (подпись, срок действия, формат)</li>
 *   <li>Извлечение данных из токена (username, expiration, roles, isActive)</li>
 *   <li>Проверка валидности токена для пользователя</li>
 * </ul>
 *
 * <p><b>Java 21 features:</b>
 * <ul>
 *   <li>var для локальных переменных</li>
 *   <li>Duration API для временных интервалов</li>
 *   <li>Текстовые блоки для сообщений</li>
 * </ul>
 *
 * @see JwtTokenProvider
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты JWT токен провайдера (car-service)")
class JwtTokenProviderTest {

    // ============================================================
    // КОНСТАНТЫ ТЕСТОВЫХ ДАННЫХ
    // ============================================================

    private static final String TEST_USERNAME = "testuser";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String SECRET_KEY = "mySuperSecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026";

    private static final Duration TOKEN_EXPIRATION = Duration.ofHours(1);
    private static final Duration SHORT_EXPIRATION = Duration.ofSeconds(1);

    // ============================================================
    // ПОЛЯ КЛАССА
    // ============================================================

    private JwtTokenProvider tokenProvider;
    private UserDetails testUserDetails;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", SECRET_KEY);

        testUserDetails = User.builder()
                .username(TEST_USERNAME)
                .password("")
                .authorities(() -> ROLE_USER)
                .build();
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    private String generateValidToken() {
        return Jwts.builder()
                .subject(TEST_USERNAME)
                .claim("roles", List.of(ROLE_USER))
                .claim("is_active", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION.toMillis()))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private String generateExpiredToken() throws InterruptedException {
        var token = Jwts.builder()
                .subject(TEST_USERNAME)
                .claim("roles", List.of(ROLE_USER))
                .claim("is_active", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + SHORT_EXPIRATION.toMillis()))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();

        TimeUnit.MILLISECONDS.sleep(SHORT_EXPIRATION.toMillis() + 100);
        return token;
    }

    private String generateTokenWithInactiveUser() {
        return Jwts.builder()
                .subject(TEST_USERNAME)
                .claim("roles", List.of(ROLE_USER))
                .claim("is_active", false)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION.toMillis()))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseTokenClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ============================================================
    // ТЕСТЫ ИЗВЛЕЧЕНИЯ ДАННЫХ ИЗ ТОКЕНА
    // ============================================================

    @Nested
    @DisplayName("Тесты извлечения данных из токена")
    class TokenExtractionTests {

        private String validToken;

        @BeforeEach
        void setUp() {
            validToken = generateValidToken();
        }

        @Test
        @DisplayName("✅ Извлечение username из валидного токена")
        void extractUsername_ValidToken_ReturnsUsername() {
            var username = tokenProvider.extractUsername(validToken);
            assertThat(username).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("✅ Извлечение expiration date из валидного токена")
        void extractExpiration_ValidToken_ReturnsFutureDate() {
            var expiration = tokenProvider.extractExpiration(validToken);
            assertThat(expiration).isAfter(new Date());
        }

        @Test
        @DisplayName("✅ Извлечение ролей из валидного токена")
        void extractRoles_ValidToken_ReturnsRoles() {
            var roles = tokenProvider.extractRoles(validToken);
            assertThat(roles).containsExactly(ROLE_USER);
        }

        @Test
        @DisplayName("✅ Извлечение статуса активности из валидного токена")
        void isUserActive_ValidToken_ReturnsTrue() {
            var isActive = tokenProvider.isUserActive(validToken);
            assertThat(isActive).isTrue();
        }

        @Test
        @DisplayName("✅ Извлечение статуса активности из токена с inactive пользователем")
        void isUserActive_TokenWithInactiveUser_ReturnsFalse() {
            var token = generateTokenWithInactiveUser();
            var isActive = tokenProvider.isUserActive(token);
            assertThat(isActive).isFalse();
        }
    }

    // ============================================================
    // ТЕСТЫ ВАЛИДАЦИИ ТОКЕНОВ
    // ============================================================

    @Nested
    @DisplayName("Тесты валидации токенов")
    class TokenValidationTests {

        private String validToken;

        @BeforeEach
        void setUp() {
            validToken = generateValidToken();
        }

        @Test
        @DisplayName("✅ Валидация корректного токена возвращает true")
        void validateToken_ValidToken_ReturnsTrue() {
            var result = tokenProvider.validateToken(validToken);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("❌ Валидация токена с неверной подписью выбрасывает InvalidTokenFormatException")
        void validateToken_InvalidSignature_ThrowsException() {
            var invalidKey = Keys.hmacShaKeyFor("differentSecretKey12345678901234567890".getBytes());
            var invalidToken = Jwts.builder()
                    .subject(TEST_USERNAME)
                    .signWith(invalidKey)
                    .compact();

            assertThatThrownBy(() -> tokenProvider.validateToken(invalidToken))
                    .isInstanceOf(PlatformException.InvalidTokenFormatException.class)
                    .hasMessageContaining("Неверная подпись токена");
        }

        @Test
        @DisplayName("❌ Валидация истекшего токена выбрасывает TokenExpiredException")
        void validateToken_ExpiredToken_ThrowsException() throws InterruptedException {
            var expiredToken = generateExpiredToken();

            assertThatThrownBy(() -> tokenProvider.validateToken(expiredToken))
                    .isInstanceOf(PlatformException.TokenExpiredException.class)
                    .hasMessageContaining("Срок действия токена истек");
        }

        @Test
        @DisplayName("❌ Валидация malformed токена выбрасывает InvalidTokenFormatException")
        void validateToken_MalformedToken_ThrowsException() {
            var malformedToken = "malformed.token.string";

            assertThatThrownBy(() -> tokenProvider.validateToken(malformedToken))
                    .isInstanceOf(PlatformException.InvalidTokenFormatException.class)
                    .hasMessageContaining("Неверный формат токена");
        }

        @Test
        @DisplayName("❌ Валидация null токена выбрасывает MissingTokenException")
        void validateToken_NullToken_ThrowsException() {
            assertThatThrownBy(() -> tokenProvider.validateToken(null))
                    .isInstanceOf(PlatformException.MissingTokenException.class)
                    .hasMessageContaining("Токен отсутствует");
        }

        @Test
        @DisplayName("❌ Валидация пустого токена выбрасывает MissingTokenException")
        void validateToken_EmptyToken_ThrowsException() {
            assertThatThrownBy(() -> tokenProvider.validateToken(""))
                    .isInstanceOf(PlatformException.MissingTokenException.class)
                    .hasMessageContaining("Токен отсутствует");
        }
    }

    // ============================================================
    // ТЕСТЫ ПРОВЕРКИ ВАЛИДНОСТИ ТОКЕНА ДЛЯ ПОЛЬЗОВАТЕЛЯ
    // ============================================================

    @Nested
    @DisplayName("Тесты проверки валидности токена для пользователя")
    class TokenUserValidationTests {

        private String validToken;

        @BeforeEach
        void setUp() {
            validToken = generateValidToken();
        }

        @Test
        @DisplayName("✅ Проверка валидности токена для корректного пользователя")
        void isTokenValid_CorrectUser_ReturnsTrue() {
            var result = tokenProvider.isTokenValid(validToken, testUserDetails);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("❌ Проверка токена для другого пользователя возвращает false")
        void isTokenValid_WrongUser_ReturnsFalse() {
            var otherUser = User.builder()
                    .username("otheruser")
                    .password("")
                    .authorities(() -> ROLE_USER)
                    .build();

            var result = tokenProvider.isTokenValid(validToken, otherUser);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Проверка токена с null пользователем возвращает false")
        void isTokenValid_NullUser_ReturnsFalse() {
            var result = tokenProvider.isTokenValid(validToken, null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Проверка null токена возвращает false")
        void isTokenValid_NullToken_ReturnsFalse() {
            var result = tokenProvider.isTokenValid(null, testUserDetails);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Проверка истекшего токена возвращает false")
        void isTokenValid_ExpiredToken_ReturnsFalse() throws InterruptedException {
            var expiredToken = generateExpiredToken();
            Thread.sleep(10);
            var result = tokenProvider.isTokenValid(expiredToken, testUserDetails);
            assertThat(result).isFalse();
        }
    }
}