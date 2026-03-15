package com.carsensor.gateway.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Gateway фильтр для проверки JWT токенов
 */
@Component
@Slf4j
public class JwtAuthenticationGatewayFilter implements GatewayFilter {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/swagger"
    );

    @Getter
    @RequiredArgsConstructor
    private enum ErrorType {
        MISSING_TOKEN("Missing JWT token", HttpStatus.UNAUTHORIZED),
        INVALID_TOKEN("Invalid JWT token", HttpStatus.UNAUTHORIZED),
        EXPIRED_TOKEN("JWT token expired", HttpStatus.UNAUTHORIZED),
        FORBIDDEN("Access forbidden", HttpStatus.FORBIDDEN),
        INTERNAL_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

        private final String message;
        private final HttpStatus status;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicPath(path)) {
            log.debug("Public path accessed: {}", path);
            return chain.filter(exchange);
        }

        String token = extractJwtFromRequest(request);

        if (token == null) {
            log.warn("Missing JWT token for path: {}", path);
            return onError(exchange, ErrorType.MISSING_TOKEN);
        }

        ValidationResult validationResult = validateToken(token);
        if (!validationResult.isValid()) {
            log.warn("Invalid JWT token for path: {}, reason: {}", path, validationResult.getReason());
            return onError(exchange, validationResult.getErrorType());
        }

        String username = extractUsername(token);
        if (username == null) {
            log.warn("Could not extract username from token for path: {}", path);
            return onError(exchange, ErrorType.INVALID_TOKEN);
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", username)
                .header("X-User-Roles", String.join(",", extractRoles(token)))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractJwtFromRequest(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private ValidationResult validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return ValidationResult.valid();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("JWT token expired: {}", e.getMessage());
            return ValidationResult.invalid(ErrorType.EXPIRED_TOKEN, e.getMessage());
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return ValidationResult.invalid(ErrorType.INVALID_TOKEN, e.getMessage());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return ValidationResult.invalid(ErrorType.INVALID_TOKEN, e.getMessage());
        }
    }

    private String extractUsername(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("roles", List.class);
        } catch (Exception e) {
            log.error("Failed to extract roles from token: {}", e.getMessage());
            return List.of();
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, ErrorType errorType) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(errorType.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorBody = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                java.time.Instant.now().toString(),
                errorType.getStatus().value(),
                errorType.getStatus().getReasonPhrase(),
                errorType.getMessage(),
                exchange.getRequest().getPath().value()
        );

        DataBuffer buffer = response.bufferFactory()
                .wrap(errorBody.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Внутренний класс для результата валидации с Lombok
     */
    @Getter
    private static class ValidationResult {
        private final boolean valid;
        private final ErrorType errorType;
        private final String reason;

        private ValidationResult(boolean valid, ErrorType errorType, String reason) {
            this.valid = valid;
            this.errorType = errorType;
            this.reason = reason;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult invalid(ErrorType errorType, String reason) {
            return new ValidationResult(false, errorType, reason);
        }
    }
}