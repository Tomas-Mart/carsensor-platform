package com.carsensor.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import com.carsensor.auth.domain.entity.User;
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
 *   <li><b>Генерация</b> - создание access и refresh токенов с необходимыми claims</li>
 *   <li><b>Валидация</b> - проверка подписи, срока действия и формата токена</li>
 *   <li><b>Извлечение данных</b> - получение username, expiration и других claims</li>
 *   <li><b>Типизация токенов</b> - различение access и refresh токенов по типу</li>
 * </ul>
 *
 * <p><b>Типы токенов:</b>
 * <table border="1">
 *   <caption>Типы JWT токенов</caption>
 *   <thead>
 *     <tr>
 *       <th>Тип</th>
 *       <th>Время жизни</th>
 *       <th>Содержимое</th>
 *       <th>Назначение</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>Access Token</td>
 *       <td>15 минут (по умолчанию)</td>
 *       <td>username, roles, user_id, email, token_type=access</td>
 *       <td>Доступ к защищенным API</td>
 *     </tr>
 *     <tr>
 *       <td>Refresh Token</td>
 *       <td>7 дней (по умолчанию)</td>
 *       <td>username, token_type=refresh</td>
 *       <td>Обновление access токена</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p><b>Алгоритм подписи:</b>
 * <ul>
 *   <li><b>Алгоритм:</b> HMAC-SHA512 (HS512)</li>
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
 * <p><b>Пример использования:</b>
 * <pre>{@code
 * @Autowired
 * private JwtTokenProvider tokenProvider;
 *
 * // Генерация токенов
 * String accessToken = tokenProvider.generateAccessToken(user);
 * String refreshToken = tokenProvider.generateRefreshToken(user);
 *
 * // Валидация
 * boolean isValid = tokenProvider.validateToken(token);
 *
 * // Извлечение данных
 * String username = tokenProvider.extractUsername(token);
 * Date expiration = tokenProvider.extractExpiration(token);
 *
 * // Проверка типа
 * boolean isRefresh = tokenProvider.isRefreshToken(token);
 * }</pre>
 *
 * @see User
 * @see PlatformException
 * @see io.jsonwebtoken.Jwts
 * @since 1.0
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /**
     * Секретный ключ для подписи JWT токенов.
     * Должен быть не менее 512 бит (64 байта) для HS512 алгоритма.
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Время жизни access токена в секундах (по умолчанию 15 минут = 900 секунд).
     */
    @Value("${app.jwt.access-token-expiration:900}")
    private long accessTokenExpiration;

    /**
     * Время жизни refresh токена в секундах (по умолчанию 7 дней = 604800 секунд).
     */
    @Value("${app.jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration;

    /**
     * Тип access токена.
     */
    private static final String TOKEN_TYPE_ACCESS = "access";

    /**
     * Тип refresh токена.
     */
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * Claim для ролей пользователя.
     */
    private static final String CLAIM_ROLES = "roles";

    /**
     * Claim для идентификатора пользователя.
     */
    private static final String CLAIM_USER_ID = "user_id";

    /**
     * Claim для email пользователя.
     */
    private static final String CLAIM_EMAIL = "email";

    /**
     * Claim для типа токена.
     */
    private static final String CLAIM_TOKEN_TYPE = "token_type";

    /**
     * Получает ключ для подписи JWT токенов.
     *
     * <p>Ключ создается из секретной строки с использованием HMAC-SHA512.
     * Строка преобразуется в байты в кодировке UTF-8.
     * Для безопасности ключ должен быть не менее 512 бит (64 байта).
     *
     * @return секретный ключ для подписи JWT
     * @throws IllegalArgumentException если секретный ключ пустой или слишком короткий
     */
    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            log.error("JWT secret is not configured");
            throw new PlatformException.InvalidTokenException("JWT секрет не настроен");
        }

        if (jwtSecret.length() < 64) {
            log.warn("JWT secret is too short ({} chars). Recommended at least 64 characters for HS512.",
                    jwtSecret.length());
        }

        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Генерирует access токен для пользователя.
     *
     * <p>Access токен содержит следующие claims:
     * <ul>
     *   <li><b>sub</b> - username пользователя</li>
     *   <li><b>roles</b> - список ролей пользователя</li>
     *   <li><b>user_id</b> - идентификатор пользователя</li>
     *   <li><b>email</b> - email пользователя</li>
     *   <li><b>token_type</b> - тип токена ("access")</li>
     *   <li><b>iat</b> - время выдачи (issued at)</li>
     *   <li><b>exp</b> - время истечения (expiration)</li>
     * </ul>
     *
     * @param user пользователь, для которого генерируется токен
     * @return JWT access токен
     * @throws IllegalArgumentException если user == null
     */
    public String generateAccessToken(User user) {
        log.debug("Generating access token for user: {}", user.getUsername());

        var claims = new HashMap<String, Object>();
        claims.put(CLAIM_ROLES, user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

        return buildToken(claims, user.getUsername(), accessTokenExpiration);
    }

    /**
     * Генерирует refresh токен для пользователя.
     *
     * <p>Refresh токен содержит минимальный набор данных:
     * <ul>
     *   <li><b>sub</b> - username пользователя</li>
     *   <li><b>token_type</b> - тип токена ("refresh")</li>
     *   <li><b>iat</b> - время выдачи (issued at)</li>
     *   <li><b>exp</b> - время истечения (expiration)</li>
     * </ul>
     *
     * @param user пользователь, для которого генерируется токен
     * @return JWT refresh токен
     * @throws IllegalArgumentException если user == null
     */
    public String generateRefreshToken(User user) {
        log.debug("Generating refresh token for user: {}", user.getUsername());

        var claims = new HashMap<String, Object>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);

        return buildToken(claims, user.getUsername(), refreshTokenExpiration);
    }

    /**
     * Строит JWT токен с указанными claims.
     *
     * <p>Процесс построения токена:
     * <ol>
     *   <li>Добавляет все claims из переданной карты</li>
     *   <li>Устанавливает subject (username)</li>
     *   <li>Устанавливает время выдачи (iat)</li>
     *   <li>Устанавливает время истечения (exp)</li>
     *   <li>Подписывает токен с использованием HS512 алгоритма</li>
     * </ol>
     *
     * @param claims     дополнительные данные для токена
     * @param subject    имя пользователя (subject)
     * @param expiration время жизни токена в секундах
     * @return подписанный JWT токен
     */
    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        var now = new Date();
        var expiryDate = new Date(now.getTime() + expiration * 1000L);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
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
        var claims = extractAllClaims(token);
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
     * @param token JWT токен
     * @param user  пользователь для проверки
     * @return true если токен валиден для пользователя, false в противном случае
     */
    public boolean isTokenValid(String token, User user) {
        if (token == null || user == null) {
            return false;
        }

        try {
            var username = extractUsername(token);
            return username.equals(user.getUsername()) && !isTokenExpired(token);
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
        var expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    /**
     * Получает время жизни access токена в секундах.
     *
     * @return время жизни access токена в секундах
     */
    public long getAccessTokenValidityInSeconds() {
        return accessTokenExpiration;
    }

    /**
     * Получает время жизни refresh токена в секундах.
     *
     * @return время жизни refresh токена в секундах
     */
    public long getRefreshTokenValidityInSeconds() {
        return refreshTokenExpiration;
    }

    /**
     * Определяет, является ли токен refresh токеном.
     *
     * <p>Проверяет claim token_type в payload токена.
     *
     * @param token JWT токен
     * @return true если токен является refresh токеном, false в противном случае
     */
    public boolean isRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            var claims = extractAllClaims(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE));
        } catch (Exception e) {
            log.debug("Failed to determine token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Определяет, является ли токен access токеном.
     *
     * <p>Проверяет claim token_type в payload токена.
     *
     * @param token JWT токен
     * @return true если токен является access токеном, false в противном случае
     */
    public boolean isAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            var claims = extractAllClaims(token);
            return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE));
        } catch (Exception e) {
            log.debug("Failed to determine token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Извлекает идентификатор пользователя из токена.
     *
     * @param token JWT токен
     * @return идентификатор пользователя
     * @throws PlatformException.InvalidTokenFormatException если токен невалиден
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_ID, Long.class));
    }

    /**
     * Извлекает email пользователя из токена.
     *
     * @param token JWT токен
     * @return email пользователя
     * @throws PlatformException.InvalidTokenFormatException если токен невалиден
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_EMAIL, String.class));
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
}