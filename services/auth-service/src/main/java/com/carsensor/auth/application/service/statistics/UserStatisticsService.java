// application/service/UserStatisticsService.java
package com.carsensor.auth.application.service.statistics;

/**
 * Сервис для получения статистики по пользователям.
 * Отвечает за агрегированные данные.
 * Принцип: Single Responsibility - только статистика.
 */
public interface UserStatisticsService {

    /**
     * Получение статистики по пользователям
     */
    UserStatistics getUserStatistics();

    /**
     * DTO для статистики пользователей
     */
    record UserStatistics(
            long totalUsers,
            long activeUsers,
            long blockedUsers,
            long newUsersToday,
            long newUsersThisWeek,
            long newUsersThisMonth
    ) {
    }
}