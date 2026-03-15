package com.carsensor.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
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

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration:900}") // 15 минут по умолчанию
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration:604800}") // 7 дней по умолчанию
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("user_id", user.getId());
        claims.put("email", user.getEmail());
        claims.put("token_type", "access");

        return buildToken(claims, user.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "refresh");

        return buildToken(claims, user.getUsername(), refreshTokenExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Неверная подпись JWT: {}", e.getMessage());
            throw new PlatformException.InvalidTokenFormatException("Неверная подпись токена"); // <-- ИЗМЕНИТЬ
        } catch (MalformedJwtException e) {
            log.error("Неверный формат JWT токена: {}", e.getMessage());
            throw new PlatformException.InvalidTokenFormatException("Неверный формат токена"); // <-- ИЗМЕНИТЬ
        } catch (ExpiredJwtException e) {
            log.error("Срок действия JWT токена истек: {}", e.getMessage());
            throw new PlatformException.TokenExpiredException("Срок действия токена истек"); // <-- ИЗМЕНИТЬ
        } catch (UnsupportedJwtException e) {
            log.error("Неподдерживаемый тип JWT токена: {}", e.getMessage());
            throw new PlatformException.InvalidTokenException("Неподдерживаемый тип токена"); // <-- ИЗМЕНИТЬ
        } catch (IllegalArgumentException e) {
            log.error("Пустые claims в JWT токене: {}", e.getMessage());
            throw new PlatformException.MissingTokenException("Токен отсутствует или пуст"); // <-- ИЗМЕНИТЬ
        }
    }

    public boolean isTokenValid(String token, User user) {
        final String username = extractUsername(token);
        return (username.equals(user.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenExpiration;
    }
}