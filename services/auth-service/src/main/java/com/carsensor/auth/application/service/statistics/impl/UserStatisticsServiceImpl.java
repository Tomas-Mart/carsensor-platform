// application/service/statistics/impl/UserStatisticsServiceImpl.java
package com.carsensor.auth.application.service.statistics.impl;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.statistics.UserStatisticsService;
import com.carsensor.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private final UserRepository userRepository;

    @Override
    public UserStatistics getUserStatistics() {
        log.debug("Fetching user statistics");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long blockedUsers = totalUsers - activeUsers;

        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);

        long newUsersToday = userRepository.countByCreatedAtAfter(today);
        long newUsersThisWeek = userRepository.countByCreatedAtAfter(weekAgo);
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(monthAgo);

        return new UserStatistics(
                totalUsers,
                activeUsers,
                blockedUsers,
                newUsersToday,
                newUsersThisWeek,
                newUsersThisMonth
        );
    }
}