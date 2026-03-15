package com.carsensor.gateway.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.carsensor.gateway.infrastructure.security.JwtAuthenticationGatewayFilter;

@Configuration
public class GatewayConfig {

    @Value("${services.auth.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${services.car.url:http://localhost:8082}")
    private String carServiceUrl;

    private final JwtAuthenticationGatewayFilter jwtAuthenticationGatewayFilter;

    public GatewayConfig(JwtAuthenticationGatewayFilter jwtAuthenticationGatewayFilter) {
        this.jwtAuthenticationGatewayFilter = jwtAuthenticationGatewayFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service routes
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri(authServiceUrl))

                // Car Service routes (protected)
                .route("car-service", r -> r
                        .path("/api/v1/cars/**")
                        .filters(f -> f.filter(jwtAuthenticationGatewayFilter))  // ✅ ИСПРАВЛЕНО
                        .uri(carServiceUrl))

                // Actuator routes
                .route("auth-actuator", r -> r
                        .path("/actuator/auth/**")
                        .filters(f -> f.rewritePath("/actuator/auth/(?<segment>.*)", "/actuator/${segment}"))
                        .uri(authServiceUrl))

                .route("car-actuator", r -> r
                        .path("/actuator/car/**")
                        .filters(f -> f.rewritePath("/actuator/car/(?<segment>.*)", "/actuator/${segment}"))
                        .uri(carServiceUrl))

                // Swagger UI routes
                .route("auth-swagger", r -> r
                        .path("/swagger/auth/**")
                        .filters(f -> f.rewritePath("/swagger/auth/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri(authServiceUrl))

                .route("car-swagger", r -> r
                        .path("/swagger/car/**")
                        .filters(f -> f.rewritePath("/swagger/car/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri(carServiceUrl))

                .build();
    }
}