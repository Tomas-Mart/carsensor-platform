package com.carsensor.gateway.infrastructure.security;

import java.util.Base64;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
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

    private JwtAuthenticationGatewayFilter filter;

    @Mock
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationGatewayFilter();
        // Устанавливаем секрет через рефлексию (т.к. @Value)
        ReflectionTestUtils.setField(filter, "jwtSecret", "testSecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLong");
    }

    @Test
    @DisplayName("Публичные пути должны пропускаться без токена")
    void filter_WithPublicPath_ShouldAllowWithoutToken() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Защищённый путь без токена должен вернуть 401")
    void filter_WithProtectedPathWithoutToken_ShouldReturn401() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/cars")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Защищённый путь с валидным токеном должен пропустить")
    void filter_WithProtectedPathAndValidToken_ShouldAllow() {
        // Arrange
        String token = generateValidJwt();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/cars")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Защищённый путь с невалидным токеном должен вернуть 401")
    void filter_WithProtectedPathAndInvalidToken_ShouldReturn401() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/cars")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    private String generateValidJwt() {
        // Генерация простого валидного JWT для тестов
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString(
                "{\"sub\":\"testuser\",\"roles\":[\"USER\"]}".getBytes()
        );
        String signature = Base64.getUrlEncoder().encodeToString("signature".getBytes());
        return header + "." + payload + "." + signature;
    }
}