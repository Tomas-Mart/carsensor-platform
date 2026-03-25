package com.carsensor.car.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.carsensor.platform.exception.PlatformException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * Провайдер для работы с JWT (JSON Web Token) токенами.
 *
 * <p>Обеспечивает полный цикл работы с JWT токенами:
 * <ul>
 *   <li><b>Валидация</b> - проверка подписи, срока действия и формата токена</li>
 *   <li><b>Извлечение данных</b> - получение username, expiration, roles и других claims</li>
 *   <li><b>Проверка валидности</b> - для конкретного пользователя</li>
 * </ul>
 *
 * <p><b>Алгоритм подписи:</b>
 * <ul>
 *   <li><b>Алгоритм:</b> HMAC-SHA256 (HS256)</li>
 *   <li><b>Ключ:</b> секретный ключ из конфигурации (app.jwt.secret)</li>
 *   <li><b>Кодировка:</b> UTF-8</li>
 * </ul>
 *
 * <p><b>Исключения при валидации:</b>
 * <ul>
 *   <li>{@link PlatformException.InvalidTokenFormatException} - неверный формат или подпись</li>
 *   <li>{@link PlatformException.TokenExpiredException} - срок действия токена истек</li>
 *   <li>{@link PlatformException.InvalidTokenException} - неподдерживаемый тип токена</li>
 *   <li>{@link PlatformException.MissingTokenException} - токен отсутствует или пуст</li>
 * </ul>
 *
 * @see PlatformException
 * @see io.jsonwebtoken.Jwts
 * @since 1.0
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /**
     * Секретный ключ для подписи JWT токенов.
     * Должен быть не менее 256 бит (32 байта) для HS256 алгоритма.
     */
    @Value("${app.jwt.secret:mySecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026}")
    private String jwtSecret;

    /**
     * Claim для ролей пользователя.
     */
    private static final String CLAIM_ROLES = "roles";

    /**
     * Получает ключ для подписи JWT токенов.
     *
     * <p>Ключ создается из секретной строки с использованием HMAC-SHA256.
     * Строка преобразуется в байты в кодировке UTF-8.
     * Для безопасности ключ должен быть не менее 256 бит (32 байта).
     *
     * @return секретный ключ для подписи JWT
     * @throws PlatformException.InvalidTokenException если секретный ключ пустой
     */
    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            log.error("JWT secret is not configured");
            throw new PlatformException.InvalidTokenException("JWT секрет не настроен");
        }

        if (jwtSecret.length() < 32) {
            log.warn("JWT secret is too short ({} chars). Recommended at least 32 characters for HS256.",
                    jwtSecret.length());
        }

        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Извлекает имя пользователя (subject) из токена.
     *
     * @param token JWT токен
     * @return имя пользователя
     * @throws PlatformException.InvalidTokenFormatException если токен невалиден
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Извлекает дату истечения срока действия токена.
     *
     * @param token JWT токен
     * @return дата истечения
     * @throws PlatformException.InvalidTokenFormatException если токен невалиден
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Извлекает список ролей пользователя из токена.
     *
     * @param token JWT токен
     * @return список ролей
     * @throws PlatformException.InvalidTokenFormatException если токен невалиден
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get(CLAIM_ROLES));
    }

    /**
     * Извлекает конкретное claim из токена.
     *
     * <p>Пример использования:
     * <pre>{@code
     * String role = tokenProvider.extractClaim(token, claims -> claims.get("role", String.class));
     * }</pre>
     *
     * @param token          JWT токен
     * @param claimsResolver функция для извлечения claim
     * @param <T>            тип возвращаемого значения
     * @return значение claim
     * @throws PlatformException.InvalidTokenFormatException если токен невалиден
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Извлекает все claims из токена.
     *
     * <p>Выполняет парсинг и верификацию подписи токена.
     *
     * @param token JWT токен
     * @return все claims токена
     * @throws PlatformException.InvalidTokenFormatException если токен невалиден
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw new PlatformException.InvalidTokenFormatException("Неверный формат токена");
        }
    }

    /**
     * Проверяет валидность токена.
     *
     * <p>Выполняет следующие проверки:
     * <ul>
     *   <li>Формат токена (MalformedJwtException)</li>
     *   <li>Подпись токена (SignatureException)</li>
     *   <li>Срок действия (ExpiredJwtException)</li>
     *   <li>Тип токена (UnsupportedJwtException)</li>
     *   <li>Наличие токена (IllegalArgumentException)</li>
     * </ul>
     *
     * <p>При возникновении любой ошибки выбрасывается соответствующее исключение
     * из иерархии {@link PlatformException}.
     *
     * @param token JWT токен для проверки
     * @return true если токен валиден
     * @throws PlatformException.InvalidTokenFormatException при неверном формате или подписи
     * @throws PlatformException.TokenExpiredException       при истекшем сроке действия
     * @throws PlatformException.InvalidTokenException       при неподдерживаемом типе
     * @throws PlatformException.MissingTokenException       при отсутствии токена
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new PlatformException.MissingTokenException("Токен отсутствует");
        }

        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw new PlatformException.InvalidTokenFormatException("Неверная подпись токена");
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token format: {}", e.getMessage());
            throw new PlatformException.InvalidTokenFormatException("Неверный формат токена");
        } catch (ExpiredJwtException e) {
            log.error("JWT token expired: {}", e.getMessage());
            throw new PlatformException.TokenExpiredException("Срок действия токена истек");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            throw new PlatformException.InvalidTokenException("Неподдерживаемый тип токена");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw new PlatformException.MissingTokenException("Токен отсутствует");
        }
    }

    /**
     * Проверяет, что токен валиден для конкретного пользователя.
     *
     * <p>Выполняет две проверки:
     * <ol>
     *   <li>Имя пользователя в токене соответствует переданному</li>
     *   <li>Срок действия токена не истек</li>
     * </ol>
     *
     * @param token       JWT токен
     * @param userDetails данные пользователя для проверки
     * @return true если токен валиден для пользователя, false в противном случае
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (token == null || userDetails == null) {
            return false;
        }

        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет, истек ли срок действия токена.
     *
     * @param token JWT токен
     * @return true если токен истек, false если еще действителен
     */
    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.debug("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }
}