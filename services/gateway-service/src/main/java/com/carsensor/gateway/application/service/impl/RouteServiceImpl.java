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
        if (routeDefinitionLocator == null) {
            log.warn("RouteDefinitionLocator is not available");
            return List.of();
        }

        var routes = routeDefinitionLocator.getRouteDefinitions()
                .map(this::mapToRouteInfo)
                .collectList()
                .block();

        return routes != null ? routes : List.of();
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
        if (routeDefinitionWriter == null) {
            log.warn("RouteDefinitionWriter is not available");
            return;
        }

        org.springframework.cloud.gateway.route.RouteDefinition springRouteDef =
                convertToSpringRouteDefinition(routeConfig);
        routeDefinitionWriter.save(Mono.just(springRouteDef)).subscribe();
        log.info("Route added successfully: {}", routeConfig.id());
    }

    @Override
    public void removeRoute(String routeId) {
        log.info("Removing route: {}", routeId);
        if (routeDefinitionWriter == null) {
            log.warn("RouteDefinitionWriter is not available");
            metricsMap.remove(routeId);
            return;
        }

        routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
        metricsMap.remove(routeId);
        log.info("Route removed successfully: {}", routeId);
    }

    @Override
    public void updateRoute(String routeId, RouteConfig routeConfig) {
        log.info("Updating route: {}", routeId);
        if (routeDefinitionWriter == null) {
            log.warn("RouteDefinitionWriter is not available");
            return;
        }

        org.springframework.cloud.gateway.route.RouteDefinition springRouteDef =
                convertToSpringRouteDefinition(routeConfig);

        routeDefinitionWriter.delete(Mono.just(routeId))
                .then(routeDefinitionWriter.save(Mono.just(springRouteDef)))
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

        // Добавляем предикаты, если они есть
        if (config.predicates() != null) {
            config.predicates().forEach(predicate -> {
                // Создаем предикат из строки
                var predicateDef = new org.springframework.cloud.gateway.handler.predicate.PredicateDefinition(predicate);
                def.getPredicates().add(predicateDef);
            });
        }

        // Добавляем фильтры, если они есть
        if (config.filters() != null) {
            config.filters().forEach(filter -> {
                var filterDef = new org.springframework.cloud.gateway.filter.FilterDefinition(filter);
                def.getFilters().add(filterDef);
            });
        }

        // Добавляем метаданные
        if (config.metadata() != null) {
            def.getMetadata().putAll(config.metadata());
        }

        return def;
    }

    private RouteInfo mapToRouteInfo(org.springframework.cloud.gateway.route.RouteDefinition definition) {
        if (definition == null) {
            log.warn("RouteDefinition is null");
            return null;
        }

        // Безопасное получение order из metadata
        int order = 0;
        if (definition.getMetadata() != null && definition.getMetadata().get("order") != null) {
            Object orderObj = definition.getMetadata().get("order");
            if (orderObj instanceof Integer) {
                order = (Integer) orderObj;
            }
        }

        return new RouteInfo(
                definition.getId() != null ? definition.getId() : "unknown",
                definition.getUri() != null ? definition.getUri().toString() : "",
                definition.getPredicates() != null ? definition.getPredicates().stream()
                        .map(p -> p.getName() + ": " + p.getArgs())
                        .toList() : List.of(),
                definition.getFilters() != null ? definition.getFilters().stream()
                        .map(f -> f.getName() + ": " + f.getArgs())
                        .toList() : List.of(),
                order,
                true,
                RouteStatus.UNKNOWN
        );
    }

    private String getServiceUrl(String serviceName) {
        String protocol = "http";

        return switch (serviceName) {
            case "auth-service" -> String.format("%s://auth-service:8081", protocol);
            case "car-service" -> String.format("%s://car-service:8082", protocol);
            case "scheduler-service" -> String.format("%s://scheduler-service:8083", protocol);
            default -> String.format("%s://%s:8080", protocol, serviceName);
        };
    }

    private double calculateRequestsPerSecond() {
        long total = totalRequests.get();
        return total / 3600.0;
    }

    private long calculatePeakRequestsPerMinute() {
        return totalRequests.get() / 60;
    }

    public void updateMetrics(String routeId, int statusCode) {
        if (routeId == null) {
            log.warn("Cannot update metrics for null routeId");
            return;
        }

        totalRequests.incrementAndGet();

        routeCounters.computeIfAbsent(routeId, k -> new AtomicLong())
                .incrementAndGet();

        statusCounters.computeIfAbsent(statusCode, k -> new AtomicLong())
                .incrementAndGet();

        metricsMap.compute(routeId, (k, v) -> {
            long currentTime = System.currentTimeMillis();
            if (v == null) {
                return new RouteMetrics(routeId, 1,
                        statusCode < 400 ? 1 : 0,
                        statusCode >= 400 ? 1 : 0,
                        0, currentTime);
            }

            long newTotalRequests = v.totalRequests() + 1;
            long newSuccessful = v.successfulRequests() + (statusCode < 400 ? 1 : 0);
            long newFailed = v.failedRequests() + (statusCode >= 400 ? 1 : 0);

            double newAvgResponseTime = (v.averageResponseTime() * v.totalRequests() +
                                         (currentTime - v.lastRequestTime())) / newTotalRequests;

            return new RouteMetrics(
                    routeId,
                    newTotalRequests,
                    newSuccessful,
                    newFailed,
                    newAvgResponseTime,
                    currentTime
            );
        });
    }
}