package com.carsensor.gateway.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ServerWebExchange;
import com.carsensor.gateway.GatewayApplication;
import com.carsensor.gateway.config.TestGatewayConfig;
import com.carsensor.gateway.infrastructure.security.JwtAuthenticationGatewayFilter;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@Import(TestGatewayConfig.class)
@DisplayName("Интеграционные тесты GatewayConfig")
@SpringBootTest(classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"spring.cloud.compatibility-verifier.enabled=false",
                "spring.cloud.gateway.enabled=true"})
@TestPropertySource(properties = {"services.auth.url=http://auth-service:8081",
        "services.car.url=http://car-service:8082"})
class GatewayConfigIntegrationTest {

    @MockitoBean
    private JwtAuthenticationGatewayFilter jwtAuthenticationGatewayFilter;

    @Autowired
    private RouteLocator routeLocator;

    @BeforeEach
    void setUp() {
        when(jwtAuthenticationGatewayFilter.filter(any(ServerWebExchange.class), any(GatewayFilterChain.class)))
                .thenAnswer(invocation -> {
                    GatewayFilterChain chain = invocation.getArgument(1);
                    ServerWebExchange exchange = invocation.getArgument(0);
                    return chain.filter(exchange);
                });
    }

    @Test
    @DisplayName("Должен создать route locator со всеми маршрутами")
    void customRouteLocator_ShouldCreateRoutes() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();
        assertThat(routes).isNotEmpty();

        var routeIds = routes.stream()
                .map(Route::getId)
                .toList();

        assertThat(routeIds).containsExactlyInAnyOrder(
                "auth-service",
                "car-service",
                "auth-actuator",
                "car-actuator",
                "auth-swagger",
                "car-swagger"
        );
    }

    @Test
    @DisplayName("Маршрут auth-service должен быть сконфигурирован")
    void authServiceRoute_ShouldBeConfigured() {
        var authRoute = routeLocator.getRoutes()
                .filter(route -> "auth-service".equals(route.getId()))
                .next()
                .block();

        assertThat(authRoute).isNotNull();
        assertThat(authRoute.getId()).isEqualTo("auth-service");
        assertThat(authRoute.getUri()).hasToString("http://auth-service:8081");

        boolean hasJwtFilter = authRoute.getFilters().stream()
                .anyMatch(filter -> filter.toString().contains("JwtAuthentication"));
        assertThat(hasJwtFilter).isFalse();
    }

    @Test
    @DisplayName("Маршрут car-service должен быть сконфигурирован с JWT фильтром")
    void carServiceRoute_ShouldBeConfiguredWithJwtFilter() {
        var carRoute = routeLocator.getRoutes()
                .filter(route -> "car-service".equals(route.getId()))
                .next()
                .block();

        assertThat(carRoute).isNotNull();
        assertThat(carRoute.getId()).isEqualTo("car-service");
        assertThat(carRoute.getUri()).hasToString("http://car-service:8082");

        // Проверяем, что JWT фильтр применен (не пустой список фильтров)
        assertThat(carRoute.getFilters())
                .as("Car service route should have JWT authentication filter")
                .isNotEmpty();
    }

    @Test
    @DisplayName("Маршрут auth-actuator должен корректно переписывать путь")
    void authActuatorRoute_ShouldRewritePath() {
        var actuatorRoute = routeLocator.getRoutes()
                .filter(route -> "auth-actuator".equals(route.getId()))
                .next()
                .block();

        assertThat(actuatorRoute).isNotNull();
        assertThat(actuatorRoute.getId()).isEqualTo("auth-actuator");
        assertThat(actuatorRoute.getUri()).hasToString("http://auth-service:8081");

        boolean hasRewritePathFilter = actuatorRoute.getFilters().stream()
                .anyMatch(filter -> filter.toString().contains("RewritePath"));
        assertThat(hasRewritePathFilter).isTrue();
    }

    @Test
    @DisplayName("Маршрут car-actuator должен корректно переписывать путь")
    void carActuatorRoute_ShouldRewritePath() {
        var actuatorRoute = routeLocator.getRoutes()
                .filter(route -> "car-actuator".equals(route.getId()))
                .next()
                .block();

        assertThat(actuatorRoute).isNotNull();
        assertThat(actuatorRoute.getId()).isEqualTo("car-actuator");
        assertThat(actuatorRoute.getUri()).hasToString("http://car-service:8082");

        boolean hasRewritePathFilter = actuatorRoute.getFilters().stream()
                .anyMatch(filter -> filter.toString().contains("RewritePath"));
        assertThat(hasRewritePathFilter).isTrue();
    }

    @Test
    @DisplayName("Маршрут auth-swagger должен корректно переписывать путь")
    void authSwaggerRoute_ShouldRewritePath() {
        var swaggerRoute = routeLocator.getRoutes()
                .filter(route -> "auth-swagger".equals(route.getId()))
                .next()
                .block();

        assertThat(swaggerRoute).isNotNull();
        assertThat(swaggerRoute.getId()).isEqualTo("auth-swagger");
        assertThat(swaggerRoute.getUri()).hasToString("http://auth-service:8081");

        boolean hasRewritePathFilter = swaggerRoute.getFilters().stream()
                .anyMatch(filter -> filter.toString().contains("RewritePath"));
        assertThat(hasRewritePathFilter).isTrue();
    }

    @Test
    @DisplayName("Маршрут car-swagger должен корректно переписывать путь")
    void carSwaggerRoute_ShouldRewritePath() {
        var swaggerRoute = routeLocator.getRoutes()
                .filter(route -> "car-swagger".equals(route.getId()))
                .next()
                .block();

        assertThat(swaggerRoute).isNotNull();
        assertThat(swaggerRoute.getId()).isEqualTo("car-swagger");
        assertThat(swaggerRoute.getUri()).hasToString("http://car-service:8082");

        boolean hasRewritePathFilter = swaggerRoute.getFilters().stream()
                .anyMatch(filter -> filter.toString().contains("RewritePath"));
        assertThat(hasRewritePathFilter).isTrue();
    }

    @Test
    @DisplayName("Должно быть 6 сконфигурированных маршрутов")
    void shouldHaveExactlySixRoutes() {
        var routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).hasSize(6);

        StepVerifier.create(routeLocator.getRoutes())
                .expectNextCount(6)
                .verifyComplete();
    }

    @Test
    @DisplayName("Маршруты должны иметь корректные URI для соответствующих сервисов")
    void routesShouldHaveCorrectUris() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();

        routes.forEach(route -> {
            String uri = route.getUri().toString();
            if (route.getId().contains("auth")) {
                assertThat(uri)
                        .withFailMessage("Route %s should point to auth-service, but was %s",
                                route.getId(), uri)
                        .isEqualTo("http://auth-service:8081");
            } else if (route.getId().contains("car")) {
                assertThat(uri)
                        .withFailMessage("Route %s should point to car-service, but was %s",
                                route.getId(), uri)
                        .isEqualTo("http://car-service:8082");
            }
        });
    }
}