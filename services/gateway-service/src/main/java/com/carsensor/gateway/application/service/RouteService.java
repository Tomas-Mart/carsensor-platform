package com.carsensor.gateway.application.service;

import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Интерфейс сервиса маршрутизации
 */
public interface RouteService {

    /**
     * Получение информации о доступных маршрутах
     */
    List<RouteInfo> getAvailableRoutes();

    /**
     * Проверка доступности сервиса
     */
    Mono<HealthStatus> checkServiceHealth(String serviceName);

    /**
     * Получение метрик по маршрутам
     */
    Map<String, RouteMetrics> getRouteMetrics();

    /**
     * Динамическое добавление маршрута
     */
    void addRoute(RouteConfig routeConfig);

    /**
     * Динамическое удаление маршрута
     */
    void removeRoute(String routeId);

    /**
     * Обновление маршрута
     */
    void updateRoute(String routeId, RouteConfig routeConfig);

    /**
     * Получение статистики запросов
     */
    RequestStatistics getRequestStatistics();

    /**
     * Очистка кэша маршрутов
     */
    void refreshRoutes();

    /**
     * Информация о маршруте
     */
    record RouteInfo(
            String id,
            String uri,
            List<String> predicates,
            List<String> filters,
            int order,
            boolean enabled,
            RouteStatus status
    ) {
    }

    enum RouteStatus {
        UP, DOWN, DEGRADED, UNKNOWN
    }

    /**
     * Статус здоровья сервиса
     */
    record HealthStatus(
            String service,
            RouteStatus status,
            long responseTime,
            String lastChecked,
            String error
    ) {
    }

    /**
     * Метрики маршрута
     */
    record RouteMetrics(
            String routeId,
            long totalRequests,
            long successfulRequests,
            long failedRequests,
            double averageResponseTime,
            long lastRequestTime
    ) {
    }

    /**
     * Определение маршрута для динамического добавления
     */
    record RouteConfig(
            String id,
            String uri,
            List<String> predicates,
            List<String> filters,
            int order,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Статистика запросов
     */
    record RequestStatistics(
            long totalRequests,
            Map<String, Long> requestsPerRoute,
            Map<Integer, Long> requestsPerStatus,
            double requestsPerSecond,
            long peakRequestsPerMinute
    ) {
    }
}