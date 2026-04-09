package com.carsensor.gateway.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import com.carsensor.gateway.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты JwtAuthenticationGatewayFilter")
class JwtAuthenticationGatewayFilterTest {

    private static final String JWT_SECRET = "mySuperSecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026";

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationGatewayFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties realProperties = new JwtProperties(JWT_SECRET, 900, 604800);
        filter = new JwtAuthenticationGatewayFilter(realProperties);
    }

    @Test
    @DisplayName("Публичные пути должны пропускаться без токена")
    void filter_WithPublicPath_ShouldAllowWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Защищённый путь без токена должен вернуть 401")
    void filter_WithProtectedPathWithoutToken_ShouldReturn401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/cars")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Защищённый путь с валидным токеном должен пропустить")
    void filter_WithProtectedPathAndValidToken_ShouldAllow() {
        String token = generateValidJwt();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/cars")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Защищённый путь с невалидным токеном должен вернуть 401")
    void filter_WithProtectedPathAndInvalidToken_ShouldReturn401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/cars")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    private String generateValidJwt() {
        var key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject("testuser")
                .claim("roles", List.of("USER"))
                .claim("is_active", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}