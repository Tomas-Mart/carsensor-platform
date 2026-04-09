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
 * 
 * <p>Использует единый секрет для всех сервисов:
 * {@code mySuperSecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026}
 * 
 * <p><b>Роли и права:</b>
 * <ul>
 *   <li>ROLE_ADMIN - полный доступ (CRUD)</li>
 *   <li>ROLE_USER - только чтение (GET)</li>
 *   <li>CAR_CREATE - право на создание</li>
 *   <li>CAR_EDIT - право на редактирование</li>
 *   <li>CAR_DELETE - право на удаление</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
public final class TestJwtUtils {

    /** Единый секретный ключ для JWT токенов во всех тестах. */
    private static final String JWT_SECRET = "mySuperSecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026";
    
    /** Секретный ключ для подписи JWT. */
    private static final SecretKey KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    /** Время жизни токена по умолчанию (1 час в миллисекундах). */
    private static final long DEFAULT_EXPIRY_MS = 3600000;

    private TestJwtUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Создает JWT токен для администратора.
     * Токен содержит роль: ROLE_ADMIN
     *
     * @return JWT токен администратора
     */
    public static String createAdminToken() {
        return createToken("admin", List.of("ROLE_ADMIN"));
    }

    /**
     * Создает JWT токен для обычного пользователя.
     * Токен содержит роль: ROLE_USER
     *
     * @return JWT токен пользователя
     */
    public static String createUserToken() {
        return createToken("user", List.of("ROLE_USER"));
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
        return createTokenWithCustomExpiry(username, roles, DEFAULT_EXPIRY_MS);
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

    /**
     * Возвращает секретный ключ, используемый для подписи токенов.
     *
     * @return секретный ключ
     */
    public static String getJwtSecret() {
        return JWT_SECRET;
    }
}
