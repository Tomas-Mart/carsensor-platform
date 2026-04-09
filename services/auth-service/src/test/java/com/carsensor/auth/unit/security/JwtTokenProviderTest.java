package com.carsensor.auth.unit.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.infrastructure.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты для JwtTokenProvider.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты JWT токен провайдера")
class JwtTokenProviderTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final Long TEST_USER_ID = 1L;
    private static final String ROLE_USER = "ROLE_USER";

    private static final String SECRET_KEY = "mySuperSecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026";
    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 900L;
    private static final long REFRESH_TOKEN_EXPIRATION_SECONDS = 604800L;
    private static final Duration SHORT_EXPIRATION = Duration.ofSeconds(1);

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USER_ID = "user_id";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TOKEN_TYPE = "token_type";

    private JwtTokenProvider tokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();

        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", SECRET_KEY);
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION_SECONDS);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION_SECONDS);

        var userPermission = Permission.builder()
                .id(1L)
                .name(ROLE_USER)
                .build();

        var userRole = Role.builder()
                .id(1L)
                .name(ROLE_USER)
                .permissions(Set.of(userPermission))
                .build();

        testUser = User.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .isActive(true)
                .roles(Set.of(userRole))
                .build();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    private Claims parseTokenClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String generateExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiration", SHORT_EXPIRATION.toSeconds());
        var token = tokenProvider.generateAccessToken(testUser);
        TimeUnit.MILLISECONDS.sleep(SHORT_EXPIRATION.toMillis() + 100);
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION_SECONDS);
        return token;
    }

    // ============================================================
    // ТЕСТЫ ГЕНЕРАЦИИ ТОКЕНОВ
    // ============================================================

    @Nested
    @DisplayName("Тесты генерации токенов")
    class TokenGenerationTests {

        @Test
        @DisplayName("✅ Генерация access token с правильными claims и ролями")
        void generateAccessToken_ValidUser_ReturnsValidTokenWithRoles() {
            var token = tokenProvider.generateAccessToken(testUser);
            var claims = parseTokenClaims(token);

            assertThat(token).isNotNull().isNotEmpty();

            @SuppressWarnings("unchecked")
            var actualRoles = (List<String>) claims.get(CLAIM_ROLES);

            assertThat(claims.getSubject()).isEqualTo(TEST_USERNAME);
            assertThat(claims.get(CLAIM_EMAIL)).isEqualTo(TEST_EMAIL);
            assertThat(claims.get(CLAIM_USER_ID)).isEqualTo(TEST_USER_ID.intValue());
            assertThat(claims.get(CLAIM_TOKEN_TYPE)).isEqualTo(TOKEN_TYPE_ACCESS);
            assertThat(actualRoles).containsExactly(ROLE_USER);
            assertThat(claims.getExpiration()).isAfter(new Date());
        }

        @Test
        @DisplayName("✅ Генерация refresh token с правильными claims")
        void generateRefreshToken_ValidUser_ReturnsValidToken() {
            var token = tokenProvider.generateRefreshToken(testUser);
            var claims = parseTokenClaims(token);

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(claims.getSubject()).isEqualTo(TEST_USERNAME);
            assertThat(claims.get(CLAIM_TOKEN_TYPE)).isEqualTo(TOKEN_TYPE_REFRESH);
            assertThat(claims.getExpiration()).isAfter(new Date());
        }

        @Test
        @DisplayName("✅ Access token содержит роли пользователя")
        void generateAccessToken_IncludesRoles() {
            var token = tokenProvider.generateAccessToken(testUser);
            var claims = parseTokenClaims(token);

            @SuppressWarnings("unchecked")
            var roles = (List<String>) claims.get(CLAIM_ROLES);
            assertThat(roles).contains(ROLE_USER);
        }

        @Test
        @DisplayName("✅ Access token содержит email пользователя")
        void generateAccessToken_IncludesEmail() {
            var token = tokenProvider.generateAccessToken(testUser);
            var claims = parseTokenClaims(token);

            assertThat(claims.get(CLAIM_EMAIL)).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("✅ Access token содержит user_id")
        void generateAccessToken_IncludesUserId() {
            var token = tokenProvider.generateAccessToken(testUser);
            var claims = parseTokenClaims(token);

            assertThat(claims.get(CLAIM_USER_ID)).isEqualTo(TEST_USER_ID.intValue());
        }
    }

    // ============================================================
    // ТЕСТЫ ВАЛИДАЦИИ ТОКЕНОВ (возвращают boolean, не бросают исключения)
    // ============================================================

    @Nested
    @DisplayName("Тесты валидации токенов")
    class TokenValidationTests {

        private String validAccessToken;

        @BeforeEach
        void setUp() {
            validAccessToken = tokenProvider.generateAccessToken(testUser);
        }

        @Test
        @DisplayName("✅ Валидация корректного токена возвращает true")
        void validateToken_ValidToken_ReturnsTrue() {
            var result = tokenProvider.validateToken(validAccessToken);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("❌ Валидация токена с неверной подписью возвращает false")
        void validateToken_InvalidSignature_ReturnsFalse() {
            var invalidKey = Keys.hmacShaKeyFor("differentSecretKey12345678901234567890".getBytes());
            var invalidToken = Jwts.builder()
                    .subject(TEST_USERNAME)
                    .signWith(invalidKey)
                    .compact();

            var result = tokenProvider.validateToken(invalidToken);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Валидация истекшего токена возвращает false")
        void validateToken_ExpiredToken_ReturnsFalse() throws InterruptedException {
            var expiredToken = generateExpiredToken();
            Thread.sleep(50);
            var result = tokenProvider.validateToken(expiredToken);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Валидация malformed токена возвращает false")
        void validateToken_MalformedToken_ReturnsFalse() {
            var result = tokenProvider.validateToken("malformed.token.string");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Валидация null токена возвращает false")
        void validateToken_NullToken_ReturnsFalse() {
            var result = tokenProvider.validateToken(null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Валидация пустого токена возвращает false")
        void validateToken_EmptyToken_ReturnsFalse() {
            var result = tokenProvider.validateToken("");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Валидация токена с неподдерживаемым алгоритмом возвращает false")
        void validateToken_UnsupportedAlgorithm_ReturnsFalse() {
            var unsupportedKey = Keys.hmacShaKeyFor("test-key-32-bytes-long-xxxxxxxxxxxxxxx".getBytes(StandardCharsets.UTF_8));
            var unsupportedToken = Jwts.builder()
                    .subject(TEST_USERNAME)
                    .signWith(unsupportedKey, Jwts.SIG.HS256)
                    .compact();

            var result = tokenProvider.validateToken(unsupportedToken);
            assertThat(result).isFalse();
        }
    }

    // ============================================================
    // ОСТАЛЬНЫЕ ТЕСТЫ БЕЗ ИЗМЕНЕНИЙ
    // ============================================================

    @Nested
    @DisplayName("Тесты извлечения данных из токена")
    class TokenExtractionTests {

        private String validAccessToken;

        @BeforeEach
        void setUp() {
            validAccessToken = tokenProvider.generateAccessToken(testUser);
        }

        @Test
        @DisplayName("✅ Извлечение username из валидного токена")
        void extractUsername_ValidToken_ReturnsUsername() {
            var username = tokenProvider.extractUsername(validAccessToken);
            assertThat(username).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("✅ Извлечение expiration date из валидного токена")
        void extractExpiration_ValidToken_ReturnsFutureDate() {
            var expiration = tokenProvider.extractExpiration(validAccessToken);
            assertThat(expiration).isAfter(new Date());
        }

        @Test
        @DisplayName("✅ Извлечение userId из валидного токена")
        void extractUserId_ValidToken_ReturnsUserId() {
            var userId = tokenProvider.extractUserId(validAccessToken);
            assertThat(userId).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("✅ Извлечение email из валидного токена")
        void extractEmail_ValidToken_ReturnsEmail() {
            var email = tokenProvider.extractEmail(validAccessToken);
            assertThat(email).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("✅ Извлечение ролей из валидного токена")
        void extractRoles_ValidToken_ReturnsRoles() {
            var roles = tokenProvider.extractRoles(validAccessToken);
            assertThat(roles).containsExactly(ROLE_USER);
        }
    }

    @Nested
    @DisplayName("Тесты определения типа токена")
    class TokenTypeTests {

        private String accessToken;
        private String refreshToken;

        @BeforeEach
        void setUp() {
            accessToken = tokenProvider.generateAccessToken(testUser);
            refreshToken = tokenProvider.generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("✅ Определение refresh token")
        void isRefreshToken_RefreshToken_ReturnsTrue() {
            assertThat(tokenProvider.isRefreshToken(refreshToken)).isTrue();
            assertThat(tokenProvider.isRefreshToken(accessToken)).isFalse();
        }

        @Test
        @DisplayName("✅ Определение access token")
        void isAccessToken_AccessToken_ReturnsTrue() {
            assertThat(tokenProvider.isAccessToken(accessToken)).isTrue();
            assertThat(tokenProvider.isAccessToken(refreshToken)).isFalse();
        }

        @Test
        @DisplayName("❌ Определение типа для null токена возвращает false")
        void isRefreshToken_NullToken_ReturnsFalse() {
            assertThat(tokenProvider.isRefreshToken(null)).isFalse();
            assertThat(tokenProvider.isAccessToken(null)).isFalse();
        }

        @Test
        @DisplayName("❌ Определение типа для malformed токена возвращает false")
        void isRefreshToken_MalformedToken_ReturnsFalse() {
            assertThat(tokenProvider.isRefreshToken("malformed.token")).isFalse();
            assertThat(tokenProvider.isAccessToken("malformed.token")).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты проверки валидности токена для пользователя")
    class TokenUserValidationTests {

        private String validAccessToken;

        @BeforeEach
        void setUp() {
            validAccessToken = tokenProvider.generateAccessToken(testUser);
        }

        @Test
        @DisplayName("✅ Проверка валидности токена для корректного пользователя")
        void isTokenValid_CorrectUser_ReturnsTrue() {
            var result = tokenProvider.isTokenValid(validAccessToken, testUser);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("❌ Проверка токена для другого пользователя возвращает false")
        void isTokenValid_WrongUser_ReturnsFalse() {
            var otherUser = User.builder()
                    .username("otheruser")
                    .build();

            var result = tokenProvider.isTokenValid(validAccessToken, otherUser);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Проверка токена с null пользователем возвращает false")
        void isTokenValid_NullUser_ReturnsFalse() {
            var result = tokenProvider.isTokenValid(validAccessToken, null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Проверка null токена возвращает false")
        void isTokenValid_NullToken_ReturnsFalse() {
            var result = tokenProvider.isTokenValid(null, testUser);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Проверка истекшего токена возвращает false")
        void isTokenValid_ExpiredToken_ReturnsFalse() throws InterruptedException {
            var shortLivedToken = generateExpiredToken();
            var result = tokenProvider.isTokenValid(shortLivedToken, testUser);
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты времени жизни токенов")
    class TokenValidityTests {

        @Test
        @DisplayName("✅ Получение времени жизни access токена")
        void getAccessTokenValidityInSeconds_ReturnsConfiguredValue() {
            var validity = tokenProvider.getAccessTokenValidityInSeconds();
            assertThat(validity).isEqualTo(ACCESS_TOKEN_EXPIRATION_SECONDS);
        }

        @Test
        @DisplayName("✅ Получение времени жизни refresh токена")
        void getRefreshTokenValidityInSeconds_ReturnsConfiguredValue() {
            var validity = tokenProvider.getRefreshTokenValidityInSeconds();
            assertThat(validity).isEqualTo(REFRESH_TOKEN_EXPIRATION_SECONDS);
        }
    }
}