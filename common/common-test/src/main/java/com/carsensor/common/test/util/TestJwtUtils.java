package com.carsensor.common.test.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public final class TestJwtUtils {

    private static final String JWT_SECRET = "mySecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    private TestJwtUtils() {
    }

    public static String createAdminToken() {
        return createToken("test-admin", List.of("ROLE_ADMIN", "CAR_DELETE", "CAR_CREATE", "CAR_EDIT"));
    }

    public static String createUserToken() {
        return createToken("test-user", List.of("ROLE_USER"));
    }

    public static String createToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 3600000); // 1 час

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(KEY)
                .compact();
    }
}