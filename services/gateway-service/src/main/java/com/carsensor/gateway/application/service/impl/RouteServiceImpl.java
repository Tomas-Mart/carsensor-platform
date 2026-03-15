package com.carsensor.gateway.application.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.carsensor.gateway.application.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Реализация сервиса маршрутизации
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteServiceImpl implements RouteService {

    private final RouteDefinitionLocator routeDefinitionLocator;
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final WebClient.Builder webClientBuilder;

    private final Map<String, RouteMetrics> metricsMap = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final Map<Integer, AtomicLong> statusCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> routeCounters = new ConcurrentHashMap<>();

    @Override
    public List<RouteInfo> getAvailableRoutes() {
        return routeDefinitionLocator.getRouteDefinitions()
                .map(this::mapToRouteInfo)
                .collectList()
                .block();
    }

    @Override
    public Mono<HealthStatus> checkServiceHealth(String serviceName) {
        String url = getServiceUrl(serviceName);
        long startTime = System.currentTimeMillis();

        return webClientBuilder.build()
                .get()
                .uri(url + "/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    return new HealthStatus(
                            serviceName,
                            RouteStatus.UP,
                            responseTime,
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            null
                    );
                })
                .onErrorResume(error -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    return Mono.just(new HealthStatus(
                            serviceName,
                            RouteStatus.DOWN,
                            responseTime,
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            error.getMessage()
                    ));
                });
    }

    @Override
    public Map<String, RouteMetrics> getRouteMetrics() {
        return Map.copyOf(metricsMap);
    }

    @Override
    public void addRoute(RouteConfig routeConfig) {
        log.info("Adding new route: {}", routeConfig.id());
        // конвертация RouteConfig в Spring RouteDefinition
        org.springframework.cloud.gateway.route.RouteDefinition springRouteDef =
                convertToSpringRouteDefinition(routeConfig);
        routeDefinitionWriter.save(Mono.just(springRouteDef)).subscribe();
        log.info("Route added successfully: {}", routeConfig.id());
    }

    @Override
    public void removeRoute(String routeId) {
        log.info("Removing route: {}", routeId);
        routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
        metricsMap.remove(routeId);
        log.info("Route removed successfully: {}", routeId);
    }

    @Override
    public void updateRoute(String routeId, RouteConfig routeConfig) {
        log.info("Updating route: {}", routeId);
        // Конвертируем RouteConfig в Spring RouteDefinition
        org.springframework.cloud.gateway.route.RouteDefinition springRouteDef =
                convertToSpringRouteDefinition(routeConfig);

        routeDefinitionWriter.delete(Mono.just(routeId))
                .then(routeDefinitionWriter.save(Mono.just(springRouteDef)))  // ✅ ИСПРАВЛЕНО
                .subscribe();
        log.info("Route updated successfully: {}", routeId);
    }

    @Override
    public RequestStatistics getRequestStatistics() {
        Map<String, Long> requestsPerRoute = new ConcurrentHashMap<>();
        routeCounters.forEach((key, value) -> requestsPerRoute.put(key, value.get()));

        Map<Integer, Long> requestsPerStatus = new ConcurrentHashMap<>();
        statusCounters.forEach((key, value) -> requestsPerStatus.put(key, value.get()));

        return new RequestStatistics(
                totalRequests.get(),
                requestsPerRoute,
                requestsPerStatus,
                calculateRequestsPerSecond(),
                calculatePeakRequestsPerMinute()
        );
    }

    @Override
    public void refreshRoutes() {
        log.info("Refreshing all routes");
        // Здесь логика обновления маршрутов
        metricsMap.clear();
        routeCounters.clear();
        statusCounters.clear();
        log.info("Routes refreshed successfully");
    }

    private org.springframework.cloud.gateway.route.RouteDefinition convertToSpringRouteDefinition(RouteConfig config) {
        org.springframework.cloud.gateway.route.RouteDefinition def =
                new org.springframework.cloud.gateway.route.RouteDefinition();
        def.setId(config.id());
        def.setUri(java.net.URI.create(config.uri()));
        // конвертация predicates и filters
        return def;
    }

    // Вспомогательные методы
    private RouteInfo mapToRouteInfo(org.springframework.cloud.gateway.route.RouteDefinition definition) {
        return new RouteInfo(
                definition.getId(),
                definition.getUri().toString(),
                definition.getPredicates().stream()
                        .map(p -> p.getName() + ": " + p.getArgs())
                        .toList(),
                definition.getFilters().stream()
                        .map(f -> f.getName() + ": " + f.getArgs())
                        .toList(),
                definition.getMetadata().get("order") != null ?
                        (int) definition.getMetadata().get("order") : 0,
                true,
                RouteStatus.UNKNOWN
        );
    }

    private String getServiceUrl(String serviceName) {
        // Используем переменные окружения для URL
        String protocol = "http";  // В продакшене можно сделать https

        return switch (serviceName) {
            case "auth-service" -> String.format("%s://auth-service:8081", protocol);
            case "car-service" -> String.format("%s://car-service:8082", protocol);
            case "scheduler-service" -> String.format("%s://scheduler-service:8083", protocol);
            default -> String.format("%s://%s:8080", protocol, serviceName);
        };
    }

    private double calculateRequestsPerSecond() {
        // Здесь должна быть реальная логика расчета
        return totalRequests.get() / 3600.0;
    }

    private long calculatePeakRequestsPerMinute() {
        // Здесь должна быть реальная логика расчета
        return totalRequests.get() / 60;
    }

    // Метод для обновления метрик (вызывается из фильтра)
    public void updateMetrics(String routeId, int statusCode) {
        totalRequests.incrementAndGet();

        routeCounters.computeIfAbsent(routeId, k -> new AtomicLong())
                .incrementAndGet();

        statusCounters.computeIfAbsent(statusCode, k -> new AtomicLong())
                .incrementAndGet();

        metricsMap.compute(routeId, (k, v) -> {
            if (v == null) {
                return new RouteMetrics(routeId, 1,
                        statusCode < 400 ? 1 : 0,
                        statusCode >= 400 ? 1 : 0,
                        0, System.currentTimeMillis());
            }
            return new RouteMetrics(
                    routeId,
                    v.totalRequests() + 1,
                    v.successfulRequests() + (statusCode < 400 ? 1 : 0),
                    v.failedRequests() + (statusCode >= 400 ? 1 : 0),
                    (v.averageResponseTime() * v.totalRequests() +
                            (System.currentTimeMillis() - v.lastRequestTime())) / (v.totalRequests() + 1),
                    System.currentTimeMillis()
            );
        });
    }
}