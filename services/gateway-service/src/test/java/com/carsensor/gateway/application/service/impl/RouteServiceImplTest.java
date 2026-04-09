package com.carsensor.gateway.application.service.impl;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.web.reactive.function.client.WebClient;
import com.carsensor.gateway.application.service.RouteService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты RouteServiceImpl")
class RouteServiceImplTest {

    @Mock
    private RouteDefinitionLocator routeDefinitionLocator;

    @Mock
    private RouteDefinitionWriter routeDefinitionWriter;

    @Mock
    private WebClient.Builder webClientBuilder;

    private RouteServiceImpl routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteServiceImpl(
                routeDefinitionLocator,
                routeDefinitionWriter,
                webClientBuilder
        );
    }

    @Test
    @DisplayName("Обновление метрик")
    void updateMetrics_ShouldIncrementCounters() {
        // Act
        routeService.updateMetrics("auth-service", 200);
        routeService.updateMetrics("auth-service", 500);
        routeService.updateMetrics("car-service", 200);

        // Assert
        var stats = routeService.getRequestStatistics();
        assertThat(stats.totalRequests()).isEqualTo(3);
        assertThat(stats.requestsPerRoute()).hasSize(2)
                .containsEntry("auth-service", 2L)
                .containsEntry("car-service", 1L);
        assertThat(stats.requestsPerStatus()).hasSize(2)
                .containsEntry(200, 2L)
                .containsEntry(500, 1L);

        // Проверка метрик по маршрутам
        var metrics = routeService.getRouteMetrics();
        assertThat(metrics).hasSize(2);
        assertThat(metrics.get("auth-service").totalRequests()).isEqualTo(2);
        assertThat(metrics.get("auth-service").successfulRequests()).isEqualTo(1);
        assertThat(metrics.get("auth-service").failedRequests()).isEqualTo(1);
    }

    @Test
    @DisplayName("Добавление маршрута")
    void addRoute_ShouldSaveRouteDefinition() {
        // Arrange
        var config = new RouteService.RouteConfig(
                "test-route",
                "lb://test-service",
                List.of(),
                List.of(),
                0,
                Map.of()
        );
        when(routeDefinitionWriter.save(any())).thenReturn(Mono.empty());

        // Act
        routeService.addRoute(config);

        // Assert
        verify(routeDefinitionWriter, times(1)).save(any());
    }

    @Test
    @DisplayName("Удаление маршрута")
    void removeRoute_ShouldDeleteRouteAndClearMetrics() {
        // Arrange
        String routeId = "auth-service";
        when(routeDefinitionWriter.delete(any())).thenReturn(Mono.empty());

        routeService.updateMetrics(routeId, 200);
        routeService.updateMetrics(routeId, 200);

        // Проверяем, что метрики существуют
        assertThat(routeService.getRouteMetrics()).containsKey(routeId);

        // Act
        routeService.removeRoute(routeId);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Mono<String>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(routeDefinitionWriter, times(1)).delete(captor.capture());

        String capturedRouteId = captor.getValue().block();
        assertThat(capturedRouteId).isEqualTo(routeId);

        // Проверяем, что метрики очищены
        assertThat(routeService.getRouteMetrics()).doesNotContainKey(routeId);
    }

    @Test
    @DisplayName("Получение доступных маршрутов")
    void getAvailableRoutes_ShouldReturnRoutes() {
        // Arrange
        RouteDefinition routeDef = new RouteDefinition();
        routeDef.setId("test-route");
        routeDef.setUri(URI.create("http://test-service"));

        when(routeDefinitionLocator.getRouteDefinitions())
                .thenReturn(Flux.just(routeDef));

        // Act
        var routes = routeService.getAvailableRoutes();

        // Assert
        assertThat(routes).isNotNull();
        assertThat(routes).hasSize(1);
        assertThat(routes.getFirst().id()).isEqualTo("test-route");
    }

    @Test
    @DisplayName("Получение пустого списка маршрутов когда локатор недоступен")
    void getAvailableRoutes_WhenLocatorIsNull_ShouldReturnEmptyList() {
        // Given
        RouteServiceImpl serviceWithNullLocator = new RouteServiceImpl(
                null, routeDefinitionWriter, webClientBuilder
        );

        // Act
        var routes = serviceWithNullLocator.getAvailableRoutes();

        // Assert
        assertThat(routes).isNotNull();
        assertThat(routes).isEmpty();
    }

    @Test
    @DisplayName("Проверка здоровья сервиса - UP")
    void checkServiceHealth_WhenServiceUp_ShouldReturnUP() {
        // Arrange
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString())).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"status\":\"UP\"}"));

        // Act
        var result = routeService.checkServiceHealth("auth-service").block();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RouteService.RouteStatus.UP);
        assertThat(result.error()).isNull();
        assertThat(result.responseTime()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Проверка здоровья сервиса - DOWN")
    void checkServiceHealth_WhenServiceDown_ShouldReturnDOWN() {
        // Arrange
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString())).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        // Act
        var result = routeService.checkServiceHealth("auth-service").block();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RouteService.RouteStatus.DOWN);
        assertThat(result.error()).isNotEmpty();
        assertThat(result.error()).contains("Connection refused");
    }

    @Test
    @DisplayName("Обновление маршрута")
    void updateRoute_ShouldUpdateRouteDefinition() {
        // Arrange
        String routeId = "test-route";
        var config = new RouteService.RouteConfig(
                routeId,
                "lb://updated-service",
                List.of(),
                List.of(),
                0,
                Map.of()
        );
        when(routeDefinitionWriter.delete(any())).thenReturn(Mono.empty());
        when(routeDefinitionWriter.save(any())).thenReturn(Mono.empty());

        // Act
        routeService.updateRoute(routeId, config);

        // Assert
        verify(routeDefinitionWriter, times(1)).delete(any());
        verify(routeDefinitionWriter, times(1)).save(any());
    }

    @Test
    @DisplayName("Обновление метрик с null routeId не должно обновлять счетчики")
    void updateMetrics_WithNullRouteId_ShouldNotUpdateCounters() {
        // Act & Assert
        routeService.updateMetrics(null, 200);

        var stats = routeService.getRequestStatistics();
        assertThat(stats.totalRequests()).isEqualTo(0);
        assertThat(stats.requestsPerRoute()).isEmpty();
    }
}