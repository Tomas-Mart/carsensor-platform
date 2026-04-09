package com.carsensor.gateway.config;

import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestGatewayConfig {

    @Bean
    @Primary
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return token -> {
            Jwt jwt = Jwt.withTokenValue(token)
                    .header("alg", "HS512")
                    .claims(claims -> {
                        claims.put("sub", "testuser");
                        claims.put("user_id", 1);
                        claims.put("roles", java.util.List.of("USER", "ADMIN"));
                        claims.put("email", "test@example.com");
                        claims.put("iat", Instant.now());
                        claims.put("exp", Instant.now().plusSeconds(3600));
                    })
                    .build();
            return Mono.just(jwt);
        };
    }
}