package com.carsensor.gateway.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import com.carsensor.gateway.infrastructure.security.JwtAuthenticationGatewayFilter;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты GatewayConfig")
class GatewayConfigTest {

    @Mock
    private JwtAuthenticationGatewayFilter jwtAuthenticationGatewayFilter;

    @Mock
    private ConfigurableApplicationContext applicationContext;

    private GatewayConfig gatewayConfig;

    @BeforeEach
    void setUp() {
        gatewayConfig = new GatewayConfig(jwtAuthenticationGatewayFilter);
    }

    @Test
    @DisplayName("Должен создать route locator со всеми маршрутами")
    void customRouteLocator_ShouldCreateRoutes() {
        // Arrange
        RouteLocatorBuilder builder = new RouteLocatorBuilder(applicationContext);

        // Act
        RouteLocator routeLocator = gatewayConfig.customRouteLocator(builder);

        // Assert
        assertThat(routeLocator).isNotNull();

        // Получаем и проверяем маршруты
        var routes = Flux.from(routeLocator.getRoutes()).collectList().block();

        assertThat(routes).hasSize(6);
        assertThat(routes).extracting("id")
                .containsExactlyInAnyOrder(
                        "auth-service",
                        "car-service",
                        "auth-actuator",
                        "car-actuator",
                        "auth-swagger",
                        "car-swagger"
                );
    }
}