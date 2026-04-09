package com.carsensor.gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.ServerWebExchange;
import com.carsensor.gateway.infrastructure.config.JwtProperties;
import com.carsensor.gateway.infrastructure.security.JwtAuthenticationGatewayFilter;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestGatewayConfig {

    @Bean
    @Primary
    public JwtAuthenticationGatewayFilter jwtAuthenticationGatewayFilter() {
        // Создаем реальный JwtProperties с оригинальным секретом
        JwtProperties testProperties = new JwtProperties(
                "mySuperSecretKeyForJWTTokenGenerationThatMustBeAtLeast512BitsLongInProductionEnvironment2026",
                900,
                604800
        );

        return new JwtAuthenticationGatewayFilter(testProperties) {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                // Пропускаем все запросы и добавляем заголовки
                ServerWebExchange mutated = exchange.mutate()
                        .request(r -> r.header("X-User-Id", "testuser")
                                .header("X-User-Roles", "USER,ADMIN"))
                        .build();
                return chain.filter(mutated);
            }
        };
    }

    @Bean
    @Primary
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // Фиктивный декодер для тестов
        return token -> {
            // Создаем фиктивный JWT для тестов
            org.springframework.security.oauth2.jwt.Jwt jwt =
                    org.springframework.security.oauth2.jwt.Jwt
                            .withTokenValue(token)
                            .header("alg", "none")
                            .claim("sub", "testuser")
                            .claim("user_id", 1)
                            .claim("roles", java.util.List.of("USER", "ADMIN"))
                            .claim("email", "test@example.com")
                            .build();
            return Mono.just(jwt);
        };
    }
}