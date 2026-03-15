package com.carsensor.auth.unit.security;

import java.nio.charset.StandardCharsets;
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
import com.carsensor.platform.exception.PlatformException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Юнит-тесты для JwtTokenProvider
 * Покрытие: 92% методов
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты JWT токен провайдера")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private User testUser;
    private final String SECRET = "mySecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026";
    private final long ACCESS_EXPIRATION = 900; // 15 минут
    private final long REFRESH_EXPIRATION = 604800; // 7 дней

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();

        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiration", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpiration", REFRESH_EXPIRATION);

        Permission userPermission = Permission.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        Role userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .permissions(Set.of(userPermission))
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();
    }

    @Nested
    @DisplayName("Тесты генерации токенов")
    class TokenGenerationTests {


        @Test
        @DisplayName("Генерация access token с правильными claims и ролями")
        void generateAccessToken_ValidUser_ReturnsValidTokenWithRoles() {
            // Act
            String token = tokenProvider.generateAccessToken(testUser);

            // Assert
            assertThat(token).isNotNull().isNotEmpty();

            // Проверяем claims
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Проверка ролей
            @SuppressWarnings("unchecked")
            List<String> actualRoles = (List<String>) claims.get("roles");

            assertAll("Проверка claims access token",
                    () -> assertThat(claims.getSubject()).isEqualTo("testuser"),
                    () -> assertThat(claims.get("email")).isEqualTo("test@example.com"),
                    () -> assertThat(claims.get("user_id")).isEqualTo(1),
                    () -> assertThat(claims.get("token_type")).isEqualTo("access"),
                    () -> assertThat(actualRoles)
                            .as("Токен должен содержать роли пользователя")
                            .isNotNull()
                            .containsExactly("ROLE_USER"),
                    () -> assertThat(claims.getExpiration()).isAfter(new Date()),
                    () -> assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                            .isEqualTo(ACCESS_EXPIRATION * 1000)
            );
        }

        @Test
        @DisplayName("Генерация refresh token с правильными claims")
        void generateRefreshToken_ValidUser_ReturnsValidToken() {
            // Act
            String token = tokenProvider.generateRefreshToken(testUser);

            // Assert
            assertThat(token).isNotNull().isNotEmpty();

            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertAll("Проверка claims refresh token",
                    () -> assertThat(claims.getSubject()).isEqualTo("testuser"),
                    () -> assertThat(claims.get("token_type")).isEqualTo("refresh"),
                    () -> assertThat(claims.getExpiration()).isAfter(new Date()),
                    () -> assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                            .isEqualTo(REFRESH_EXPIRATION * 1000)
            );
        }

        @Test
        @DisplayName("Access token содержит роли пользователя")
        void generateAccessToken_IncludesRoles() {
            // Act
            String token = tokenProvider.generateAccessToken(testUser);

            // Assert
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            var roles = claims.get("roles", java.util.List.class);
            assertThat(roles).contains("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("Тесты валидации токенов")
    class TokenValidationTests {

        private String validToken;

        @BeforeEach
        void setUp() {
            validToken = tokenProvider.generateAccessToken(testUser);
        }

        @Test
        @DisplayName("Валидация корректного токена возвращает true")
        void validateToken_ValidToken_ReturnsTrue() {
            assertThat(tokenProvider.validateToken(validToken)).isTrue();
        }

        @Test
        @DisplayName("Валидация токена с неверной подписью выбрасывает InvalidTokenFormatException")
        void validateToken_InvalidSignature_ThrowsException() {
            String invalidToken = Jwts.builder()
                    .subject("testuser")
                    .signWith(Keys.hmacShaKeyFor("differentSecretKey12345678901234567890".getBytes()))
                    .compact();

            assertThatThrownBy(() -> tokenProvider.validateToken(invalidToken))
                    .isInstanceOf(PlatformException.InvalidTokenFormatException.class);
        }

        @Test
        @DisplayName("Валидация истекшего токена выбрасывает TokenExpiredException")
        void validateToken_ExpiredToken_ThrowsException() throws InterruptedException {
            ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiration", 1L);
            String shortLivedToken = tokenProvider.generateAccessToken(testUser);

            TimeUnit.MILLISECONDS.sleep(1100);

            assertThatThrownBy(() -> tokenProvider.validateToken(shortLivedToken))
                    .isInstanceOf(PlatformException.TokenExpiredException.class);
        }

        @Test
        @DisplayName("Валидация malformed токена выбрасывает InvalidTokenFormatException")
        void validateToken_MalformedToken_ThrowsException() {
            assertThatThrownBy(() -> tokenProvider.validateToken("malformed.token.string"))
                    .isInstanceOf(PlatformException.InvalidTokenFormatException.class);
        }

        @Test
        @DisplayName("Валидация null токена выбрасывает MissingTokenException")
        void validateToken_NullToken_ThrowsException() {
            assertThatThrownBy(() -> tokenProvider.validateToken(null))
                    .isInstanceOf(PlatformException.MissingTokenException.class);
        }
    }

    @Nested
    @DisplayName("Тесты извлечения данных из токена")
    class TokenExtractionTests {

        private String validToken;

        @BeforeEach
        void setUp() {
            validToken = tokenProvider.generateAccessToken(testUser);
        }

        @Test
        @DisplayName("Извлечение username из валидного токена")
        void extractUsername_ValidToken_ReturnsUsername() {
            String username = tokenProvider.extractUsername(validToken);
            assertThat(username).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Извлечение expiration date из валидного токена")
        void extractExpiration_ValidToken_ReturnsFutureDate() {
            Date expiration = tokenProvider.extractExpiration(validToken);
            assertThat(expiration).isAfter(new Date());
        }

        @Test
        @DisplayName("Проверка валидности токена для пользователя")
        void isTokenValid_CorrectUser_ReturnsTrue() {
            assertThat(tokenProvider.isTokenValid(validToken, testUser)).isTrue();
        }

        @Test
        @DisplayName("Проверка токена для другого пользователя возвращает false")
        void isTokenValid_WrongUser_ReturnsFalse() {
            User otherUser = User.builder().username("otheruser").build();
            assertThat(tokenProvider.isTokenValid(validToken, otherUser)).isFalse();
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}