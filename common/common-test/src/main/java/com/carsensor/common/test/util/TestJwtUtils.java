package com.carsensor.common.test.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Утилитный класс для создания JWT токенов в тестах.
 * Предоставляет методы для генерации токенов с различными ролями.
 */
public final class TestJwtUtils {

    private static final String JWT_SECRET = "mySecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    private TestJwtUtils() {
        // Приватный конструктор для утилитного класса
    }

    /**
     * Создает JWT токен для администратора.
     * Токен содержит роли: ROLE_ADMIN, CAR_DELETE, CAR_CREATE, CAR_EDIT
     *
     * @return JWT токен администратора
     */
    public static String createAdminToken() {
        return createToken("test-admin", List.of("ROLE_ADMIN", "CAR_DELETE", "CAR_CREATE", "CAR_EDIT"));
    }

    /**
     * Создает JWT токен для обычного пользователя.
     * Токен содержит роль: ROLE_USER
     *
     * @return JWT токен пользователя
     */
    public static String createUserToken() {
        return createToken("test-user", List.of("ROLE_USER"));
    }

    /**
     * Создает JWT токен с указанным именем пользователя и ролями.
     * Токен действителен 1 час.
     *
     * @param username имя пользователя
     * @param roles    список ролей
     * @return JWT токен
     */
    public static String createToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 3600000); // 1 час

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Создает JWT токен с кастомными параметрами.
     *
     * @param username имя пользователя
     * @param roles    список ролей
     * @param expiryMs время жизни токена в миллисекундах
     * @return JWT токен
     */
    public static String createTokenWithCustomExpiry(String username, List<String> roles, long expiryMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }
}