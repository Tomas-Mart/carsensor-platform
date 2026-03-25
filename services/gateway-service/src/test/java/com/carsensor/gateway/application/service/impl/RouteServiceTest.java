package com.carsensor.gateway.application.service.impl;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.web.reactive.function.client.WebClient;
import com.carsensor.gateway.application.service.RouteService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты RouteServiceImpl")
class RouteServiceTest {

    @Mock
    private RouteDefinitionLocator routeDefinitionLocator;

    @Mock
    private RouteDefinitionWriter routeDefinitionWriter;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private RouteServiceImpl routeService;

    @BeforeEach
    void setUp() {
        // Инициализация при необходимости
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

        // Предварительно добавляем метрики
        routeService.updateMetrics(routeId, 200);
        routeService.updateMetrics(routeId, 200);

        // Act
        routeService.removeRoute(routeId);

        // Assert
        verify(routeDefinitionWriter, times(1)).delete(Mono.just(routeId));
        assertThat(routeService.getRouteMetrics()).doesNotContainKey(routeId);
    }

    @Test
    @DisplayName("Получение доступных маршрутов")
    void getAvailableRoutes_ShouldReturnRoutes() {
        // Arrange
        when(routeDefinitionLocator.getRouteDefinitions())
                .thenReturn(Flux.empty());

        // Act
        var routes = routeService.getAvailableRoutes();

        // Assert
        assertThat(routes).isNotNull();
    }

    @Test
    @DisplayName("Проверка здоровья сервиса - UP")
    void checkServiceHealth_WhenServiceUp_ShouldReturnUP() {
        // Arrange
        WebClient webClient = mock(WebClient.class);

        // Используем raw type для мока, но с подавлением предупреждения
        @SuppressWarnings("rawtypes")
        WebClient.RequestHeadersUriSpec spec = mock(WebClient.RequestHeadersUriSpec.class);

        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(spec);

        // Используем doReturn для обхода проблем с generics
        doReturn(spec).when(spec).uri(anyString());
        when(spec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"status\":\"UP\"}"));

        // Act
        var result = routeService.checkServiceHealth("auth-service").block();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RouteService.RouteStatus.UP);
        assertThat(result.error()).isNull();
    }

    @Test
    @DisplayName("Проверка здоровья сервиса - DOWN")
    void checkServiceHealth_WhenServiceDown_ShouldReturnDOWN() {
        // Arrange
        WebClient webClient = mock(WebClient.class);

        @SuppressWarnings("rawtypes")
        WebClient.RequestHeadersUriSpec spec = mock(WebClient.RequestHeadersUriSpec.class);

        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(spec);

        doReturn(spec).when(spec).uri(anyString());
        when(spec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        // Act
        var result = routeService.checkServiceHealth("auth-service").block();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RouteService.RouteStatus.DOWN);
        assertThat(result.error()).isNotEmpty();
    }
}